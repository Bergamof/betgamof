<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import type { Bankroll } from '$lib/api/types';
	import BankrollForm from '$lib/components/BankrollForm.svelte';
	import Dialog from '$lib/components/Dialog.svelte';
	import Meter from '$lib/components/Meter.svelte';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import RulesCard from '$lib/components/RulesCard.svelte';
	import { amount, money, percent, plural } from '$lib/format';
	import { BANKROLL_COLORS } from '$lib/labels';

	let { data } = $props();

	let editing = $state<Bankroll | null>(null);
	let dialogOpen = $state(page.url.searchParams.has('nouvelle'));

	const activeMontantes = $derived(data.montantes.filter((m) => m.status === 'ACTIVE'));
	const isolated = $derived(activeMontantes.filter((m) => m.excludeStake));
	const segments = $derived([
		...data.bankrolls.map((b) => ({
			key: `b${b.id}`,
			name: b.name,
			value: Math.max(0, b.balance),
			color: BANKROLL_COLORS[b.color]
		})),
		...isolated.map((m) => ({
			key: `m${m.id}`,
			name: m.name,
			value: m.capital,
			color: 'var(--lemon)'
		}))
	]);
	const total = $derived(segments.reduce((sum, s) => sum + s.value, 0));

	function open(bankroll: Bankroll | null) {
		editing = bankroll;
		dialogOpen = true;
	}

	function description(bankroll: Bankroll): string {
		const parts = [...bankroll.bookmakers.slice(0, 2), plural(bankroll.betCount, 'pari')];
		if (bankroll.fixedStake) parts.push(`mise fixe ${amount(bankroll.fixedStake)}`);
		return parts.join(' · ');
	}
</script>

<svelte:head><title>Bankrolls · Betgamof</title></svelte:head>

<PageHeader
	title="Bankrolls"
	subtitle="{plural(data.bankrolls.length, 'bankroll')} · {money(total)} au total"
>
	{#snippet actions()}
		<button type="button" class="btn small" onclick={() => open(null)}>Nouvelle bankroll</button>
	{/snippet}
	{#snippet mobileActions()}
		<button type="button" class="plus" aria-label="Nouvelle bankroll" onclick={() => open(null)}
			>+</button
		>
	{/snippet}
</PageHeader>

<div class="content">
	<div class="main-column">
		<section class="card total">
			<div class="caption">Total engagé</div>
			<div class="amount display">{money(total)}</div>
			<div class="stack" aria-hidden="true">
				{#each segments as segment (segment.key)}
					<div style:flex={segment.value || 0.001} style:background={segment.color}></div>
				{/each}
			</div>
			<div class="legend">
				{#each segments as segment (segment.key)}
					<span><span class="dot" style:background={segment.color}></span>{segment.name}</span>
				{/each}
			</div>
		</section>

		{#each data.bankrolls as bankroll (bankroll.id)}
			<button
				type="button"
				class="card bankroll"
				class:selected={bankroll.id === data.bankroll?.id}
				onclick={() => open(bankroll)}
			>
				<div class="top">
					<div>
						<div class="name">{bankroll.name}</div>
						<div class="sub">{description(bankroll)}</div>
					</div>
					<div class="figures">
						<div class="balance num">{money(bankroll.balance)}</div>
						<div class="roi" class:negative={bankroll.roi < 0}>
							{percent(bankroll.roi, { signed: true })} ROI
						</div>
					</div>
				</div>
				{#if bankroll.stopLoss !== null && bankroll.stopLossMargin !== null}
					<div class="stop">
						<div class="stop-labels">
							<span>Stop-loss {amount(bankroll.stopLoss)}</span>
							<span>Marge {money(Math.max(0, bankroll.stopLossMargin))}</span>
						</div>
						<Meter
							value={bankroll.balance > 0 ? 1 - bankroll.stopLoss / bankroll.balance : 0}
							color={bankroll.stopLossMargin <= 0 ? 'var(--loss-dot)' : 'var(--grass)'}
						/>
					</div>
				{/if}
				{#if bankroll.outsideMontantes > 0 || bankroll.id === data.bankroll?.id}
					<div class="chips">
						{#if bankroll.outsideMontantes > 0}
							<span class="chip">{amount(bankroll.outsideMontantes)} hors bankroll (montante)</span>
						{/if}
						<span class="chip outline">Kelly {bankroll.kellyFraction * 100} %</span>
					</div>
				{/if}
			</button>
		{:else}
			<div class="card empty">
				Aucune bankroll pour l'instant.
				<button type="button" class="link" onclick={() => open(null)}>Créer la première</button>
			</div>
		{/each}

		{#each activeMontantes as montante (montante.id)}
			<a
				href={resolve('/(app)/montantes/[id]', { id: String(montante.id) })}
				class="card-soft montante"
			>
				<div>
					<div class="name">{montante.name}</div>
					<div class="sub dark">
						{montante.excludeStake ? 'Capital dédié' : 'Dans la bankroll'} · palier {montante.currentPalier}{montante.totalPaliers
							? ` / ${montante.totalPaliers}`
							: ''}
					</div>
				</div>
				<div class="figures">
					<div class="balance num">{amount(montante.capital)}</div>
					<div class="roi" class:negative={montante.capital < montante.startCapital}>
						{percent(montante.capital / montante.startCapital - 1, { signed: true, digits: 0 })} depuis
						le départ
					</div>
				</div>
			</a>
		{/each}
	</div>

	<div class="side">
		<RulesCard rules={data.rules} link="edit" />
	</div>
</div>

<Dialog
	bind:open={dialogOpen}
	title={editing ? `Modifier « ${editing.name} »` : 'Nouvelle bankroll'}
>
	<BankrollForm bankroll={editing} ondone={() => (dialogOpen = false)} />
</Dialog>

<style>
	.content {
		padding: 22px 24px;
		display: grid;
		grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
		gap: 18px;
		align-items: start;
	}
	.main-column {
		display: flex;
		flex-direction: column;
		gap: 12px;
	}
	.total {
		padding: 20px;
		border-radius: 24px;
		margin-bottom: 2px;
	}
	.caption {
		font-size: 12px;
		font-weight: 600;
		color: var(--muted);
	}
	.amount {
		font-size: 34px;
		letter-spacing: -0.03em;
		margin-top: 4px;
	}
	.stack {
		display: flex;
		gap: 6px;
		margin-top: 14px;
		height: 10px;
		border-radius: 999px;
		overflow: hidden;
	}
	.legend {
		display: flex;
		gap: 14px;
		margin-top: 12px;
		flex-wrap: wrap;
	}
	.legend span {
		display: flex;
		gap: 6px;
		align-items: center;
		font-size: 11px;
		color: var(--text-2);
	}
	.dot {
		width: 8px;
		height: 8px;
		border-radius: 50%;
	}
	.bankroll {
		text-align: left;
		padding: 18px;
		border-radius: 24px;
		width: 100%;
	}
	.bankroll.selected {
		border: 2px solid var(--grass);
		padding: 17px;
	}
	.top,
	.montante {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		gap: 12px;
	}
	.montante {
		padding: 18px;
		border-radius: 24px;
	}
	.name {
		font-size: 15px;
		font-weight: 700;
	}
	.sub {
		font-size: 11px;
		color: var(--muted);
		margin-top: 3px;
	}
	.sub.dark {
		color: var(--text-2);
	}
	.figures {
		text-align: right;
		flex: none;
	}
	.balance {
		font-size: 22px;
	}
	.roi {
		font-size: 11px;
		font-weight: 700;
		color: var(--grass-strong);
	}
	.roi.negative {
		color: var(--loss);
	}
	.stop {
		margin-top: 14px;
	}
	.stop-labels {
		display: flex;
		justify-content: space-between;
		font-size: 11px;
		color: var(--muted);
		margin-bottom: 6px;
	}
	.chips {
		display: flex;
		gap: 8px;
		margin-top: 14px;
		flex-wrap: wrap;
	}
	.chip {
		font-size: 11px;
		font-weight: 600;
		padding: 6px 11px;
		border-radius: 999px;
		background: var(--grass-soft);
		color: var(--text-3);
	}
	.chip.outline {
		background: var(--bg);
		border: 1px solid var(--border);
		color: var(--text-2);
	}
	.empty {
		padding: 18px;
		font-size: 13px;
		color: var(--muted);
	}
	.plus {
		width: 38px;
		height: 38px;
		border-radius: 12px;
		background: var(--grass);
		color: #fff;
		border: none;
		display: grid;
		place-items: center;
		font-size: 20px;
		font-weight: 700;
	}
	.side :global(.rules) {
		border-radius: 24px;
	}
	@media (max-width: 1023px) {
		.content {
			display: flex;
			flex-direction: column;
			align-items: stretch;
			padding: 0 20px 20px;
			gap: 14px;
		}
	}
</style>
