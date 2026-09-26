<script lang="ts">
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import type { StatsPeriod } from '$lib/api/types';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import ProfitBars from '$lib/components/ProfitBars.svelte';
	import Segmented from '$lib/components/Segmented.svelte';
	import StatTile from '$lib/components/StatTile.svelte';
	import { amount, odds, percent, plural, shortDate } from '$lib/format';
	import { barColor, barWidth, costliestSport, monthLabels, watchOut } from '$lib/insights';
	import { streakLabel } from '$lib/stats';

	let { data } = $props();

	const PERIODS: { value: StatsPeriod; label: string }[] = [
		{ value: 'DAYS_30', label: '30 j' },
		{ value: 'MONTHS_3', label: '3 mois' },
		{ value: 'ALL', label: 'Tout' }
	];

	const stats = $derived(data.stats);
	const sports = $derived(stats.bySport.slice(0, 5));
	const sportInsight = $derived(costliestSport(stats));
	const months = $derived(monthLabels(stats.profitCurve));
	const shortMonths = $derived(monthLabels(stats.profitCurve, true));
	const summary = $derived(stats.montantes);
	const subtitle = $derived(
		`${plural(stats.settledCount, 'pari réglé', 'paris réglés')} · ${amount(stats.staked)} misés` +
			(stats.firstBetAt ? ` · depuis le ${shortDate(stats.firstBetAt)}` : '')
	);

	function navigate(period: StatsPeriod, bankrollId: number | null) {
		const query = bankrollId ? `period=${period}&bankroll=${bankrollId}` : `period=${period}`;
		goto(resolve(`/statistiques?${query}`), {
			replaceState: true,
			keepFocus: true,
			noScroll: true
		});
	}
</script>

<svelte:head><title>Statistiques · Betgamof</title></svelte:head>

<PageHeader title="Statistiques" {subtitle}>
	{#snippet actions()}
		<Segmented
			label="Période"
			size="sm"
			options={PERIODS}
			value={data.period}
			onchange={(period) => navigate(period, data.bankrollId)}
		/>
		<select
			class="select"
			aria-label="Bankroll"
			value={data.bankrollId ?? ''}
			onchange={(e) => navigate(data.period, Number(e.currentTarget.value) || null)}
		>
			<option value="">Toutes bankrolls</option>
			{#each data.bankrolls as bankroll (bankroll.id)}
				<option value={bankroll.id}>{bankroll.name}</option>
			{/each}
		</select>
	{/snippet}
	{#snippet mobileActions()}
		<select
			class="select small"
			aria-label="Période"
			value={data.period}
			onchange={(e) => navigate(e.currentTarget.value as StatsPeriod, data.bankrollId)}
		>
			{#each PERIODS as option (option.value)}<option value={option.value}>{option.label}</option
				>{/each}
		</select>
	{/snippet}
</PageHeader>

<div class="content">
	<section class="card profit">
		<div class="profit-head">
			<div>
				<div class="caption">
					Profit cumulé<span class="mobile-only-inline"
						>{` · ${plural(stats.settledCount, 'pari')}`}</span
					>
				</div>
				<div class="big display" class:negative={stats.profit < 0}>
					{amount(stats.profit, { signed: true })}
				</div>
			</div>
			<div class="kpis desktop-only">
				<div>
					<div class="caption small">Yield</div>
					<div class="num kpi">{percent(stats.yield)}</div>
				</div>
				<div>
					<div class="caption small">Drawdown max</div>
					<div class="num kpi negative">{amount(-stats.maxDrawdown)}</div>
				</div>
			</div>
		</div>
		<div class="chart desktop-only">
			<div class="grid-lines"><span></span><span></span><span></span><span></span></div>
			<ProfitBars points={stats.profitCurve} count={20} height="fill" variant="chart" gap={5} />
		</div>
		<div class="chart-mobile mobile-only">
			<ProfitBars points={stats.profitCurve} count={15} height={80} variant="chart" gap={4} />
		</div>
		<div class="months">
			{#each months as month, index (month)}
				<span class="desktop-only">{month}</span>
				<span class="mobile-only">{shortMonths[index]}</span>
			{/each}
		</div>
	</section>

	<div class="tiles desktop-only">
		<StatTile
			value={percent(stats.hitRate, { digits: 0 })}
			label="Taux de réussite"
			hint="{plural(stats.won, 'gagné')} · {plural(stats.lost, 'perdu')}"
		/>
		<StatTile
			value={odds(stats.averageOdds)}
			label="Cote moyenne"
			hint={stats.breakEvenOdds ? `Break-even à ${odds(stats.breakEvenOdds)}` : undefined}
		/>
		<StatTile
			value={String(stats.bestWinStreak)}
			label="Meilleure série"
			hint="En cours : {streakLabel(stats)}"
			tone="positive"
		/>
		<StatTile
			value={amount(stats.averageStake)}
			label="Mise moyenne"
			hint="{percent(stats.averageStakeShare)} de la bankroll"
		/>
	</div>

	<div class="tiles mobile-only">
		<StatTile value={percent(stats.yield)} label="Yield" size="sm" />
		<StatTile value={percent(stats.hitRate, { digits: 0 })} label="Réussite" size="sm" />
		<StatTile value={odds(stats.averageOdds)} label="Cote moyenne" size="sm" />
		<StatTile value={amount(-stats.maxDrawdown)} label="Drawdown max" size="sm" tone="negative" />
	</div>

	<section class="card sports">
		<div class="card-head">
			<h2 class="card-title">Rendement par sport</h2>
			<span class="legend desktop-only">profit · yield · volume</span>
		</div>
		<div class="bars">
			{#each sports as sport, index (sport.label)}
				<div>
					<div class="bar-head">
						<b>{sport.label}</b>
						<span class="muted">
							<b class:positive={sport.profit >= 0} class:negative={sport.profit < 0}
								>{amount(sport.profit, { signed: true })}</b
							>
							<span class="desktop-only-inline">
								· {percent(sport.yield, { signed: sport.yield < 0 })}</span
							>
							· {plural(sport.count, 'pari')}
						</span>
					</div>
					<div class="track">
						<div
							style:width="{Math.round(barWidth(sport, sports) * 100)}%"
							style:background={barColor(sport, index)}
						></div>
					</div>
				</div>
			{:else}
				<p class="muted none">Pas encore de pari réglé sur la période.</p>
			{/each}
			{#if sportInsight}
				<div class="insight desktop-only">
					<span>{sportInsight}</span>
					<a href={resolve('/journal')} class="link">Ajouter une règle</a>
				</div>
			{/if}
		</div>
	</section>

	<div class="side">
		<section class="card small-card">
			<h2 class="card-title">Par tranche de cote</h2>
			<div class="ranges">
				{#each stats.byOddsRange as range, index (range.label)}
					<div class="range">
						<span class="range-label">{range.label}</span>
						<span class="track thin">
							<span
								style:width="{Math.round(barWidth(range, stats.byOddsRange) * 100)}%"
								style:background={barColor(range, index === 1 ? 0 : index === 2 ? 2 : 1)}
							></span>
						</span>
						<span
							class="range-profit"
							class:positive={range.profit >= 0}
							class:negative={range.profit < 0}
						>
							{amount(range.profit, { signed: true })}
						</span>
					</div>
				{/each}
			</div>
		</section>

		<section class="card small-card">
			<div class="card-head plain">
				<h2 class="card-title">Montantes</h2>
				<span class="legend">{summary.launched} lancée{summary.launched > 1 ? 's' : ''}</span>
			</div>
			<div class="boxes">
				<div class="box won">
					<b class="num">{summary.succeeded}</b><span>Objectif atteint</span>
				</div>
				<div class="box lost">
					<b class="num">{summary.broken}</b><span>Cassée{summary.broken > 1 ? 's' : ''}</span>
				</div>
				<div class="box"><b class="num">{summary.active}</b><span>En cours</span></div>
			</div>
			{#if summary.launched > summary.active}
				<p class="note desktop-only">
					Palier moyen atteint : {summary.averagePaliersReached.toLocaleString('fr-FR', {
						maximumFractionDigits: 1
					})}.
					{#if summary.securedCoverage !== null}
						Les gains sécurisés couvrent {percent(summary.securedCoverage, { digits: 0 })} des pertes
						de montante.
					{/if}
				</p>
			{/if}
		</section>

		<section class="watch">
			<h2 class="card-title">À surveiller</h2>
			<p>{watchOut(stats)}</p>
		</section>
	</div>
</div>

<style>
	.content {
		padding: 22px 24px;
		display: grid;
		grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
		gap: 18px;
		align-items: start;
	}
	.select {
		height: 38px;
		border-radius: 12px;
		background: var(--surface);
		border: 1px solid var(--border);
		padding: 0 13px;
		font-size: 13px;
		font-weight: 600;
		color: var(--text-2);
	}
	.select.small {
		height: 34px;
		border-radius: 11px;
		font-size: 12px;
	}
	.profit {
		padding: 22px;
		display: flex;
		flex-direction: column;
		min-height: 360px;
	}
	.profit-head {
		display: flex;
		justify-content: space-between;
		align-items: flex-start;
	}
	.caption {
		font-size: 12px;
		font-weight: 600;
		color: var(--muted);
	}
	.caption.small {
		font-size: 11px;
		font-weight: 400;
	}
	.big {
		font-size: 38px;
		letter-spacing: -0.03em;
		color: var(--grass-strong);
	}
	.big.negative {
		color: var(--loss);
	}
	.kpis {
		display: flex;
		gap: 22px;
		text-align: right;
	}
	.kpi {
		font-size: 20px;
	}
	.chart {
		flex: 1;
		position: relative;
		margin-top: 20px;
		min-height: 180px;
	}
	.grid-lines {
		position: absolute;
		inset: 0;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
	}
	.grid-lines span {
		height: 1px;
		background: var(--border-soft);
	}
	.grid-lines span:last-child {
		background: var(--border);
	}
	.chart :global(.bars) {
		position: absolute;
		inset: 0;
	}
	.months {
		display: flex;
		justify-content: space-between;
		font-size: 11px;
		color: var(--muted);
		margin-top: 10px;
	}
	.tiles {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 14px;
	}
	.sports {
		overflow: hidden;
	}
	.card-head {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 16px 20px;
		border-bottom: 1px solid var(--border-soft);
	}
	.card-head.plain {
		padding: 0;
		border: none;
		align-items: baseline;
	}
	.legend {
		font-size: 12px;
		color: var(--muted);
	}
	.bars {
		padding: 16px 20px;
		display: flex;
		flex-direction: column;
		gap: 15px;
	}
	.bar-head {
		display: flex;
		justify-content: space-between;
		align-items: baseline;
		font-size: 13px;
	}
	.track {
		height: 10px;
		margin-top: 7px;
		border-radius: 999px;
		overflow: hidden;
		background: var(--track);
		display: flex;
	}
	.track > * {
		display: block;
		height: 100%;
	}
	.track.thin {
		flex: 1;
		height: 9px;
		margin: 0;
	}
	.insight {
		margin-top: 4px;
		padding-top: 12px;
		border-top: 1px solid var(--border-soft);
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 12px;
		font-size: 12px;
		color: var(--muted);
	}
	.insight .link {
		flex: none;
	}
	.none {
		font-size: 13px;
		margin: 0;
	}
	.side {
		display: flex;
		flex-direction: column;
		gap: 14px;
	}
	.small-card {
		padding: 18px;
	}
	.small-card .card-title,
	.watch .card-title {
		font-size: 16px;
	}
	.ranges {
		display: flex;
		flex-direction: column;
		gap: 11px;
		margin-top: 14px;
	}
	.range {
		display: flex;
		align-items: center;
		gap: 12px;
	}
	.range-label {
		width: 62px;
		flex: none;
		font-size: 12px;
		font-weight: 600;
		color: var(--text-2);
	}
	.range-profit {
		width: 60px;
		flex: none;
		text-align: right;
		font-size: 12px;
		font-weight: 700;
	}
	.boxes {
		display: flex;
		gap: 10px;
		margin-top: 14px;
	}
	.box {
		flex: 1;
		border-radius: 14px;
		padding: 12px;
		background: var(--bg);
		border: 1px solid var(--border);
		display: flex;
		flex-direction: column;
		gap: 2px;
		font-size: 11px;
		color: var(--muted);
	}
	.box b {
		font-size: 20px;
		color: var(--text);
	}
	.box.won {
		background: var(--grass-soft);
		border-color: transparent;
		color: var(--text-3);
	}
	.box.won b {
		color: var(--grass-deep);
	}
	.box.lost {
		background: var(--loss-panel);
		border-color: transparent;
		color: var(--loss-panel-text);
	}
	.box.lost b {
		color: oklch(0.5 0.16 30);
	}
	.note {
		font-size: 12px;
		color: var(--muted);
		margin: 12px 0 0;
		line-height: 1.5;
	}
	.watch {
		border-radius: 22px;
		padding: 18px;
		background: var(--warn-bg);
		border: 1px solid var(--warn-border);
		color: var(--warn-text);
	}
	.watch .card-title {
		color: var(--warn-text);
	}
	.watch p {
		font-size: 12px;
		margin: 8px 0 0;
		line-height: 1.55;
	}
	.mobile-only-inline {
		display: none;
	}
	@media (max-width: 1023px) {
		.content {
			display: flex;
			flex-direction: column;
			align-items: stretch;
			padding: 0 20px 20px;
			gap: 14px;
		}
		.mobile-only-inline {
			display: inline;
		}
		.desktop-only-inline {
			display: none;
		}
		.profit {
			min-height: 0;
			border-radius: 26px;
		}
		.chart-mobile {
			margin-top: 16px;
		}
		.months {
			margin-top: 8px;
		}
		.tiles {
			gap: 10px;
		}
		.tiles :global(.tile) {
			padding: 16px;
		}
		.tiles :global(.value) {
			font-size: 24px;
		}
		.sports {
			border-radius: 24px;
		}
		.card-head {
			border: none;
			padding: 18px 18px 0;
		}
		.bars {
			padding: 14px 18px 18px;
			gap: 13px;
		}
		.track {
			height: 9px;
			margin-top: 6px;
		}
		.bar-head .muted {
			font-size: 12px;
		}
		.small-card {
			border-radius: 24px;
		}
		.watch {
			border-radius: 20px;
			padding: 14px 16px;
		}
		.watch .card-title {
			font-size: 13px;
			font-family: var(--font-body);
		}
		.watch p {
			margin-top: 6px;
		}
	}
</style>
