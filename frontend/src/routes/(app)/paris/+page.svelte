<script lang="ts">
	import { resolve } from '$app/paths';
	import { invalidateAll } from '$app/navigation';
	import { page } from '$app/state';
	import type { Bet, BetStatus } from '$lib/api/types';
	import BetActions from '$lib/components/BetActions.svelte';
	import BetCard from '$lib/components/BetCard.svelte';
	import BetForm from '$lib/components/BetForm.svelte';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Segmented from '$lib/components/Segmented.svelte';
	import StatTile from '$lib/components/StatTile.svelte';
	import StatusPill from '$lib/components/StatusPill.svelte';
	import { betsToCsv, download } from '$lib/csv';
	import { amount, dayHeading, dayKey, odds, percent, plural, shortDate } from '$lib/format';
	import { BET_TYPE_LABEL } from '$lib/labels';

	let { data } = $props();

	type StatusFilter = 'ALL' | 'OPEN' | 'WON' | 'LOST';
	const STATUS_OPTIONS: { value: StatusFilter; label: string }[] = [
		{ value: 'ALL', label: 'Tous' },
		{ value: 'OPEN', label: 'En cours' },
		{ value: 'WON', label: 'Gagnés' },
		{ value: 'LOST', label: 'Perdus' }
	];
	const PERIODS = [
		{ value: '0', label: 'Toute la période' },
		{ value: '7', label: '7 derniers jours' },
		{ value: '30', label: '30 derniers jours' },
		{ value: '90', label: '3 derniers mois' }
	];

	const initialStatus = page.url.searchParams.get('status') as StatusFilter | null;
	let status: StatusFilter = $state(
		initialStatus && STATUS_OPTIONS.some((o) => o.value === initialStatus) ? initialStatus : 'ALL'
	);
	let query = $state(page.url.searchParams.get('q') ?? '');
	let sport = $state('');
	let bookmaker = $state('');
	let days = $state('0');
	let showFilters = $state(false);
	let showSearch = $state(page.url.searchParams.has('q'));
	let panelOpen = $state(true);

	const sports = $derived([...new Set(data.bets.map((bet) => bet.sport))].sort());
	const bookmakers = $derived([...new Set(data.bets.map((bet) => bet.bookmaker))].sort());

	function matchesStatus(bet: Bet, filter: StatusFilter): boolean {
		if (filter === 'ALL') return true;
		if (filter === 'WON')
			return bet.status === 'WON' || (bet.status === 'CASHOUT' && bet.profit > 0);
		if (filter === 'LOST')
			return bet.status === 'LOST' || (bet.status === 'CASHOUT' && bet.profit < 0);
		return bet.status === (filter as BetStatus);
	}

	const filtered = $derived.by(() => {
		const since = Number(days) > 0 ? Date.now() - Number(days) * 86_400_000 : 0;
		const text = query.trim().toLowerCase();
		return data.bets.filter(
			(bet) =>
				matchesStatus(bet, status) &&
				(!sport || bet.sport === sport) &&
				(!bookmaker || bet.bookmaker === bookmaker) &&
				new Date(bet.startsAt).getTime() >= since &&
				(!text ||
					`${bet.label} ${bet.competition} ${bet.sport} ${bet.bookmaker} ${bet.selections.map((s) => s.eventName).join(' ')}`
						.toLowerCase()
						.includes(text))
		);
	});

	const groups = $derived.by(() => {
		// Bets are sorted by kick-off, newest first: consecutive bets share a day.
		const byDay: Bet[][] = [];
		for (const bet of filtered) {
			const current = byDay.at(-1);
			if (current && dayKey(current[0].startsAt) === dayKey(bet.startsAt)) current.push(bet);
			else byDay.push([bet]);
		}
		return byDay.map((bets) => {
			const open = bets.filter((bet) => bet.status === 'OPEN');
			return {
				heading: dayHeading(bets[0].startsAt),
				bets,
				openStake: open.reduce((sum, bet) => sum + bet.stake, 0),
				profit: bets.reduce((sum, bet) => sum + bet.profit, 0),
				hasOpen: open.length > 0
			};
		});
	});

	const stats = $derived(data.stats);
	const subtitle = $derived(
		`${plural(data.bets.length, 'pari')} · ROI ${percent(stats.yield, { signed: true })} · ${percent(stats.hitRate, { digits: 0 })} de réussite`
	);

	function exportCsv() {
		download(`betgamof-paris-${dayKey(new Date())}.csv`, betsToCsv(filtered));
	}

	function context(bet: Bet): string {
		const base = `${bet.sport} · ${bet.competition}`;
		return bet.palierNumber !== null ? `${base} · palier ${bet.palierNumber}` : base;
	}
</script>

<svelte:head><title>Mes paris · Betgamof</title></svelte:head>

<PageHeader title="Mes paris" {subtitle}>
	{#snippet actions()}
		<label class="search">
			<span class="lens"></span>
			<input placeholder="Rechercher…" bind:value={query} aria-label="Rechercher un pari" />
		</label>
		<button type="button" class="btn small secondary" onclick={exportCsv}>Exporter CSV</button>
		{#if !panelOpen}
			<button type="button" class="btn small" onclick={() => (panelOpen = true)}
				>Nouveau pari</button
			>
		{/if}
	{/snippet}
	{#snippet mobileActions()}
		<button
			type="button"
			class="icon-button"
			aria-label="Filtres"
			aria-pressed={showFilters}
			onclick={() => (showFilters = !showFilters)}
		>
			<span class="burger"></span>
		</button>
		<button
			type="button"
			class="icon-button"
			aria-label="Rechercher"
			aria-pressed={showSearch}
			onclick={() => (showSearch = !showSearch)}
		>
			<span class="ring"></span>
		</button>
	{/snippet}
</PageHeader>

{#snippet filterSelects()}
	<select bind:value={sport} aria-label="Sport">
		<option value="">Sport</option>
		{#each sports as option (option)}<option>{option}</option>{/each}
	</select>
	<select bind:value={bookmaker} aria-label="Bookmaker">
		<option value="">Bookmaker</option>
		{#each bookmakers as option (option)}<option>{option}</option>{/each}
	</select>
	<select bind:value={days} aria-label="Période">
		{#each PERIODS as option (option.value)}<option value={option.value}>{option.label}</option
			>{/each}
	</select>
{/snippet}

<div class="layout">
	<div class="list-column">
		<!-- Mobile -->
		<div class="mobile-only mobile-list">
			<Segmented label="Statut" options={STATUS_OPTIONS} bind:value={status} stretch />
			{#if showSearch}
				<label class="search mobile-search">
					<span class="lens"></span>
					<input placeholder="Équipe, compétition, bookmaker…" bind:value={query} />
				</label>
			{/if}
			{#if showFilters}
				<div class="filters-mobile">
					{@render filterSelects()}
					<button type="button" class="link" onclick={exportCsv}>Exporter en CSV</button>
				</div>
			{/if}
			<div class="tiles">
				<StatTile value={String(data.bets.length)} label="Paris" size="sm" />
				<StatTile
					value={percent(stats.yield, { signed: true })}
					label="ROI"
					size="sm"
					tone={stats.yield >= 0 ? 'positive' : 'negative'}
				/>
				<StatTile value={percent(stats.hitRate, { digits: 0 })} label="Réussite" size="sm" />
			</div>
			{#each groups as group (group.heading)}
				<div class="day">
					<span class="eyebrow">{group.heading}</span>
					{#if group.hasOpen}
						<span class="day-total">{amount(group.openStake)} engagés</span>
					{:else}
						<span
							class="day-total"
							class:positive={group.profit > 0}
							class:negative={group.profit < 0}
						>
							{amount(group.profit, { signed: true })}
						</span>
					{/if}
				</div>
				<div class="cards">
					{#each group.bets as bet (bet.id)}<BetCard {bet} />{/each}
				</div>
			{:else}
				<div class="card empty">
					Aucun pari ne correspond. <a class="link" href={resolve('/paris/nouveau')}
						>Ajouter un pari</a
					>
				</div>
			{/each}
		</div>

		<!-- Desktop -->
		<div class="desktop-only desktop-list">
			<div class="toolbar">
				<Segmented label="Statut" options={STATUS_OPTIONS} bind:value={status} />
				<div class="selects">{@render filterSelects()}</div>
			</div>
			<div class="card table" role="table" aria-label="Paris">
				<div class="tr head" role="row">
					<span role="columnheader">Date</span><span role="columnheader">Pari</span>
					<span role="columnheader">Type</span><span role="columnheader">Book</span>
					<span role="columnheader">Cote</span><span role="columnheader">Mise</span>
					<span role="columnheader">Résultat</span><span role="columnheader" class="right"
						>Statut</span
					>
				</div>
				{#each filtered as bet (bet.id)}
					<div class="tr" role="row">
						<span class="muted">{shortDate(bet.startsAt)}</span>
						<span class="bet-cell">
							<b>{bet.label}</b>
							<span class="sub">{context(bet)}</span>
						</span>
						<span class="soft">{BET_TYPE_LABEL[bet.type]}</span>
						<span class="soft">{bet.bookmaker}</span>
						<span class="num">{odds(bet.odds)}</span>
						<span>{amount(bet.stake)}</span>
						{#if bet.status === 'OPEN'}
							<span class="muted">→ {amount(bet.potentialReturn)}</span>
							<span class="right"><BetActions {bet} variant="table" /></span>
						{:else}
							<span class="result" class:positive={bet.profit > 0} class:negative={bet.profit < 0}>
								{amount(bet.profit, { signed: true })}
							</span>
							<span class="right"><StatusPill status={bet.status} /></span>
						{/if}
					</div>
				{:else}
					<div class="empty-row">Aucun pari ne correspond à ces filtres.</div>
				{/each}
			</div>
		</div>
	</div>

	{#if panelOpen}
		<aside class="panel desktop-only">
			<div class="panel-head">
				<h2 class="card-title">Nouveau pari</h2>
				<button type="button" class="close" onclick={() => (panelOpen = false)}>Fermer</button>
			</div>
			<div class="panel-body">
				<BetForm
					compact
					bankrolls={data.bankrolls}
					montantes={data.montantes}
					events={data.events}
					defaultBankrollId={data.bankroll?.id ?? null}
					onsaved={() => invalidateAll()}
				/>
			</div>
		</aside>
	{/if}
</div>

<style>
	.layout {
		flex: 1;
		display: flex;
		min-height: 0;
	}
	.list-column {
		flex: 1;
		min-width: 0;
	}
	.search {
		width: 240px;
		height: 38px;
		border-radius: 12px;
		background: var(--bg);
		border: 1px solid var(--border);
		display: flex;
		align-items: center;
		gap: 9px;
		padding: 0 13px;
	}
	.search input {
		border: none;
		background: transparent;
		outline: none;
		font-size: 13px;
		flex: 1;
		min-width: 0;
	}
	.lens {
		width: 12px;
		height: 12px;
		border-radius: 50%;
		border: 2px solid var(--idle-2);
		flex: none;
	}
	.icon-button {
		width: 38px;
		height: 38px;
		border-radius: 12px;
		background: var(--surface);
		border: 1px solid var(--border);
		display: grid;
		place-items: center;
	}
	.icon-button[aria-pressed='true'] {
		border-color: var(--grass);
	}
	.burger {
		width: 14px;
		height: 2px;
		background: var(--text);
		box-shadow:
			0 5px 0 var(--text),
			0 -5px 0 var(--text);
	}
	.ring {
		width: 15px;
		height: 15px;
		border-radius: 50%;
		border: 2px solid var(--text);
	}
	.mobile-list {
		padding: 0 20px 20px;
		display: flex;
		flex-direction: column;
		gap: 14px;
	}
	.mobile-search {
		width: 100%;
		height: 44px;
		background: var(--surface);
	}
	.filters-mobile {
		display: flex;
		flex-wrap: wrap;
		gap: 8px;
		align-items: center;
	}
	select {
		font-size: 12px;
		font-weight: 600;
		padding: 9px 12px;
		border-radius: 12px;
		background: var(--surface);
		border: 1px solid var(--border);
		color: var(--text-2);
	}
	.tiles {
		display: grid;
		grid-template-columns: repeat(3, minmax(0, 1fr));
		gap: 10px;
	}
	.tiles :global(.tile) {
		border-radius: 18px;
		padding: 13px;
	}
	.tiles :global(.value) {
		font-size: 21px;
	}
	.day {
		display: flex;
		justify-content: space-between;
		align-items: baseline;
		padding: 2px 4px 0;
	}
	.day-total {
		font-size: 12px;
		font-weight: 700;
		color: var(--muted);
	}
	.day-total.positive {
		color: var(--grass-strong);
	}
	.day-total.negative {
		color: var(--loss);
	}
	.cards {
		display: flex;
		flex-direction: column;
		gap: 10px;
	}
	.empty {
		padding: 16px;
		font-size: 13px;
		color: var(--muted);
	}
	.desktop-list {
		padding: 20px 24px;
		display: flex;
		flex-direction: column;
		gap: 16px;
	}
	.toolbar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 14px;
		flex-wrap: wrap;
	}
	.selects {
		display: flex;
		gap: 8px;
	}
	.table {
		overflow: hidden;
	}
	.tr {
		display: grid;
		grid-template-columns: 58px minmax(110px, 1fr) 64px 72px 46px 60px 74px 150px;
		gap: 8px;
		padding: 14px 20px;
		border-bottom: 1px solid var(--border-soft);
		align-items: center;
		font-size: 13px;
	}
	.tr:last-child {
		border-bottom: none;
	}
	.tr.head {
		padding: 13px 20px;
		background: var(--bg);
		border-bottom: 1px solid var(--border);
		font-size: 11px;
		font-weight: 700;
		letter-spacing: 0.05em;
		text-transform: uppercase;
		color: var(--muted);
	}
	.bet-cell {
		min-width: 0;
		display: flex;
		flex-direction: column;
	}
	.sub {
		font-size: 11px;
		color: var(--muted);
		margin-top: 2px;
	}
	.soft {
		color: var(--text-2);
		overflow: hidden;
		text-overflow: ellipsis;
	}
	.result {
		font-weight: 700;
	}
	.right {
		text-align: right;
		display: flex;
		justify-content: flex-end;
	}
	.right :global(.error) {
		display: none;
	}
	.empty-row {
		padding: 20px;
		font-size: 13px;
		color: var(--muted);
	}
	.panel {
		width: 372px;
		flex: none;
		background: var(--surface);
		border-left: 1px solid var(--border);
		display: flex;
		flex-direction: column;
		position: sticky;
		top: var(--header-height);
		height: calc(100vh - var(--header-height));
	}
	.panel-head {
		padding: 18px 20px;
		border-bottom: 1px solid var(--border-soft);
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
	.close {
		border: none;
		background: none;
		font-size: 13px;
		font-weight: 600;
		color: var(--muted);
	}
	.panel-body {
		flex: 1;
		overflow: auto;
		padding: 18px 20px;
	}
</style>
