<script lang="ts">
	import { resolve } from '$app/paths';
	import Alert from '$lib/components/Alert.svelte';
	import Avatar from '$lib/components/Avatar.svelte';
	import BankrollPicker from '$lib/components/BankrollPicker.svelte';
	import BetRow from '$lib/components/BetRow.svelte';
	import Logo from '$lib/components/Logo.svelte';
	import MontanteCard from '$lib/components/MontanteCard.svelte';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import ProfitBars from '$lib/components/ProfitBars.svelte';
	import RulesCard from '$lib/components/RulesCard.svelte';
	import SportLine from '$lib/components/SportLine.svelte';
	import StatTile from '$lib/components/StatTile.svelte';
	import { amount, dayKey, longDate, money, odds, percent, plural } from '$lib/format';
	import { BET_STATUS_LABEL, BET_TYPE_LABEL, OWNER } from '$lib/labels';
	import {
		CHART_PERIODS,
		curveSince,
		periodStart,
		profitSince,
		startOfMonth,
		streakLabel,
		type ChartPeriod
	} from '$lib/stats';
	import Segmented from '$lib/components/Segmented.svelte';

	let { data } = $props();

	let period: ChartPeriod = $state('30');
	const now = new Date();
	const today = dayKey(now);

	const bankroll = $derived(data.bankroll);
	const stats = $derived(data.stats);
	const montante = $derived(data.montantes.find((m) => m.status === 'ACTIVE') ?? null);
	const todayBets = $derived(
		data.todayBets
			.filter((bet) => bet.status === 'OPEN' || dayKey(bet.startsAt) === today)
			.sort((a, b) => a.startsAt.localeCompare(b.startsAt))
	);
	const openBets = $derived(data.todayBets.filter((bet) => bet.status === 'OPEN'));
	const openStake = $derived(openBets.reduce((sum, bet) => sum + bet.stake, 0));
	const monthProfit = $derived(profitSince(stats, startOfMonth(now)));
	const greeting = now.getHours() >= 18 ? 'Bonsoir' : 'Bonjour';
	const leadingSport = $derived(stats.bySport[0]?.label);
</script>

<PageHeader
	mobile={false}
	title="{greeting}, {OWNER.name}"
	subtitle="{longDate(now)} · {plural(openBets.length, 'pari')} en cours · {amount(
		openStake
	)} engagés"
>
	{#snippet actions()}
		<form action="/paris" class="search">
			<span class="lens"></span>
			<input name="q" placeholder="Rechercher un pari, une équipe…" aria-label="Rechercher" />
		</form>
		<BankrollPicker bankrolls={data.bankrolls} selected={bankroll} />
	{/snippet}
</PageHeader>

<header class="mobile-only brand-row">
	<Logo size={25} />
	<Avatar />
</header>

{#if !bankroll}
	<div class="content">
		<div class="card empty">
			<h2 class="card-title">Bienvenue sur Betgamof</h2>
			<p>Commence par créer une bankroll : c'est le capital sur lequel tes paris seront suivis.</p>
			<a href="{resolve('/bankrolls')}?nouvelle=1" class="btn">Créer ma première bankroll</a>
		</div>
	</div>
{:else}
	<div class="content dashboard">
		<section class="hero">
			<div class="stripes"></div>
			<div class="hero-body">
				<div class="hero-top">
					<div>
						<div class="hero-label">Bankroll {bankroll.name.toLowerCase()}</div>
						<div class="balance display">{money(bankroll.balance)}</div>
						<div class="pills">
							<span class="pill strong">{percent(bankroll.roi, { signed: true })} ROI</span>
							<span class="pill">{amount(monthProfit, { signed: true })} ce mois</span>
							{#if bankroll.outsideMontantes > 0}
								<span class="pill desktop-only"
									>{amount(bankroll.outsideMontantes)} hors bankroll</span
								>
							{/if}
						</div>
					</div>
					<div class="desktop-only">
						<Segmented
							label="Période du graphique"
							options={CHART_PERIODS}
							bind:value={period}
							size="sm"
						/>
					</div>
				</div>
				<div class="desktop-only chart">
					<ProfitBars
						points={curveSince(stats, periodStart(period))}
						from={periodStart(period)}
						count={15}
						height={96}
					/>
				</div>
				<a
					href={resolve('/statistiques')}
					class="mobile-only chart"
					aria-label="Voir les statistiques"
				>
					<ProfitBars points={stats.profitCurve} count={10} height={40} gap={5} />
				</a>
			</div>
		</section>

		<div class="tiles desktop-only">
			<StatTile
				value={streakLabel(stats)}
				label="Série en cours"
				hint="Meilleure série : {stats.bestWinStreak}"
				tone={stats.currentStreak?.won === false ? 'negative' : 'positive'}
			/>
			<StatTile
				value={percent(stats.hitRate, { digits: 0 })}
				label="Taux de réussite"
				hint="{plural(stats.settledCount, 'pari')} cumulés"
			/>
			<StatTile
				value={amount(stats.averageStake)}
				label="Mise moyenne"
				hint="{percent(stats.averageStakeShare)} de la bankroll"
			/>
			<StatTile
				value={odds(stats.averageOdds)}
				label="Cote moyenne"
				hint={leadingSport ? `${leadingSport} en tête` : undefined}
			/>
		</div>

		<div class="tiles-mobile mobile-only">
			<StatTile value={streakLabel(stats)} label="Série" size="sm" tone="accent" />
			<StatTile value={String(openBets.length)} label="Ouverts" size="sm" />
			<StatTile value={amount(stats.averageStake)} label="Mise moy." size="sm" />
		</div>

		{#if montante}
			<div class="mobile-only"><MontanteCard {montante} variant="dots" /></div>
		{/if}

		<section class="card today desktop-only">
			<div class="today-head">
				<h2 class="card-title">Paris du jour</h2>
				<a href={resolve('/paris')} class="link">Tous les paris</a>
			</div>
			{#each todayBets as bet (bet.id)}
				<BetRow {bet} />
			{:else}
				<p class="none">
					Aucun pari aujourd'hui. <a class="link" href="{resolve('/paris')}?ajout=1"
						>Ajouter un pari</a
					>
				</p>
			{/each}
		</section>

		<section class="today-mobile mobile-only">
			<div class="section-head">
				<h2 class="card-title">Paris du jour</h2>
				<a href={resolve('/paris')} class="link muted-link">Tout voir</a>
			</div>
			{#each todayBets as bet (bet.id)}
				<a href={resolve('/paris')} class="mini-bet" class:won={bet.status === 'WON'}>
					<div class="mini-info">
						<SportLine sport={bet.sport} text="{bet.sport} · {bet.competition}" />
						<div class="mini-label">{bet.label}</div>
						<div class="mini-meta">
							{BET_TYPE_LABEL[bet.type]} · {bet.bookmaker} · {amount(bet.stake)}
						</div>
					</div>
					<div class="mini-side">
						<div class="num mini-odds">{odds(bet.odds)}</div>
						<div class="mini-status">{BET_STATUS_LABEL[bet.status]}</div>
					</div>
				</a>
			{:else}
				<p class="none card">Aucun pari aujourd'hui.</p>
			{/each}
		</section>

		<div class="side">
			{#if montante}
				<div class="desktop-only"><MontanteCard {montante} /></div>
			{/if}
			{#if bankroll.stopLoss !== null && bankroll.stopLossMargin !== null}
				<Alert tone={bankroll.stopLossMargin <= 0 ? 'loss' : 'warn'}>
					{#if bankroll.stopLossMargin <= 0}
						Stop-loss atteint ({amount(bankroll.stopLoss)}) — on souffle avant de rejouer.
					{:else}
						Stop-loss à {amount(bankroll.stopLoss)} — il reste {money(bankroll.stopLossMargin)} de marge
					{/if}
				</Alert>
			{/if}
			<div class="desktop-only rules-slot"><RulesCard rules={data.rules} /></div>
		</div>
	</div>
{/if}

<style>
	.content {
		padding: 22px 24px;
	}
	.empty {
		padding: 24px;
		max-width: 520px;
		display: flex;
		flex-direction: column;
		gap: 12px;
		align-items: flex-start;
	}
	.empty p {
		margin: 0;
		color: var(--text-2);
	}
	.dashboard {
		display: grid;
		grid-template-columns: minmax(0, 1.65fr) minmax(0, 1fr);
		grid-template-rows: auto 1fr;
		gap: 18px;
	}
	.hero {
		border-radius: 22px;
		padding: 24px;
		background: var(--grass);
		color: #fff;
		position: relative;
		overflow: hidden;
	}
	.stripes {
		position: absolute;
		inset: 0;
		background: repeating-linear-gradient(
			115deg,
			rgba(255, 255, 255, 0.07) 0 34px,
			transparent 34px 68px
		);
	}
	.hero-body {
		position: relative;
	}
	.hero-top {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
		gap: 12px;
	}
	.hero-top :global(.segmented) {
		background: transparent;
		border: none;
		padding: 0;
	}
	.hero-top :global(.segmented button) {
		font-size: 11px;
		background: rgba(255, 255, 255, 0.22);
		color: #fff;
		padding: 6px 11px;
	}
	.hero-top :global(.segmented button.selected) {
		background: #fff;
		color: var(--grass-deep);
	}
	.hero-label {
		font-size: 12px;
		font-weight: 600;
		opacity: 0.9;
	}
	.balance {
		font-size: 44px;
		letter-spacing: -0.03em;
		margin-top: 4px;
	}
	.pills {
		display: flex;
		gap: 8px;
		margin-top: 12px;
		flex-wrap: wrap;
	}
	.pill {
		font-size: 12px;
		font-weight: 600;
		padding: 6px 12px;
		border-radius: 999px;
		background: rgba(255, 255, 255, 0.22);
	}
	.pill.strong {
		background: #fff;
		color: var(--grass-strong);
		font-weight: 700;
	}
	.chart {
		display: block;
		margin-top: 20px;
	}
	.tiles {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 14px;
	}
	.today {
		overflow: hidden;
		align-self: start;
	}
	.today-head {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 16px 20px;
		border-bottom: 1px solid var(--border-soft);
	}
	.none {
		padding: 16px 20px;
		margin: 0;
		font-size: 13px;
		color: var(--muted);
	}
	.side {
		display: flex;
		flex-direction: column;
		gap: 14px;
	}
	.side :global(.alert) {
		border-radius: 22px;
		padding: 16px 18px;
	}
	.search {
		width: 280px;
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
		font-size: 13px;
		outline: none;
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
	.brand-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: calc(16px + env(safe-area-inset-top)) 24px 16px;
	}

	@media (max-width: 1023px) {
		.content {
			padding: 0 20px 20px;
		}
		.dashboard {
			display: flex;
			flex-direction: column;
			gap: 14px;
		}
		.hero {
			border-radius: 28px;
		}
		.stripes {
			background: var(--mowing);
		}
		.hero-label {
			opacity: 0.85;
		}
		.balance {
			font-size: 42px;
			margin-top: 8px;
		}
		.chart {
			margin-top: 16px;
		}
		.tiles-mobile {
			display: grid;
			grid-template-columns: repeat(3, minmax(0, 1fr));
			gap: 10px;
		}
		.today-mobile {
			display: flex;
			flex-direction: column;
			gap: 10px;
		}
		.section-head {
			display: flex;
			justify-content: space-between;
			align-items: baseline;
			padding: 0 4px;
		}
		.muted-link {
			font-weight: 600;
			color: oklch(0.5 0.13 145);
		}
		.mini-bet {
			border-radius: 20px;
			padding: 14px 16px;
			background: var(--surface);
			border: 1px solid var(--border);
			display: flex;
			justify-content: space-between;
			gap: 12px;
		}
		.mini-bet.won {
			background: var(--grass-soft);
			border-color: var(--grass-soft-border);
		}
		.mini-info {
			min-width: 0;
		}
		.mini-label {
			font-size: 14px;
			font-weight: 700;
			margin-top: 5px;
		}
		.mini-meta {
			font-size: 11px;
			color: var(--muted);
			margin-top: 3px;
		}
		.mini-side {
			text-align: right;
			flex: none;
		}
		.mini-odds {
			font-size: 19px;
		}
		.won .mini-odds,
		.won .mini-status {
			color: var(--grass-strong);
			font-weight: 600;
		}
		.mini-status {
			font-size: 10px;
			color: var(--muted);
		}
		.side :global(.alert) {
			border-radius: 18px;
			padding: 12px 15px;
		}
	}
</style>
