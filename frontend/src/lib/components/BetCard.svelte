<script lang="ts">
	import type { Bet } from '$lib/api/types';
	import { amount, odds, time } from '$lib/format';
	import { BET_TYPE_LABEL } from '$lib/labels';
	import BetActions from './BetActions.svelte';
	import SportLine from './SportLine.svelte';
	import StatusPill from './StatusPill.svelte';

	// Enriched bet card of the mobile list (2a): details, then status and one-click settlement.
	let { bet }: { bet: Bet } = $props();

	const context = $derived(bet.montanteName ?? `Bankroll ${bet.bankrollName.toLowerCase()}`);
</script>

<article class="card-bet">
	<div class="top">
		<div class="info">
			<SportLine sport={bet.sport} text="{bet.sport} · {bet.competition} · {time(bet.startsAt)}" />
			<div class="label">{bet.label}</div>
			<div class="meta">{BET_TYPE_LABEL[bet.type]} · {bet.bookmaker} · {context}</div>
		</div>
		<div class="figures">
			<div class="odds num" class:positive={bet.status === 'WON'}>{odds(bet.odds)}</div>
			<div class="stake">{amount(bet.stake)}</div>
		</div>
	</div>
	{#if bet.status === 'OPEN'}
		<div class="bottom open">
			<div class="status-line">
				<StatusPill status="OPEN" />
				<span class="hint">
					{bet.palierNumber !== null
						? `Palier ${bet.palierNumber} · montante`
						: `Gain potentiel ${amount(bet.potentialReturn)}`}
				</span>
			</div>
			<BetActions {bet} />
		</div>
	{:else}
		<div class="bottom">
			<StatusPill status={bet.status} solid />
			<span class="result num" class:positive={bet.profit > 0} class:negative={bet.profit < 0}>
				{amount(bet.profit, { signed: true })}
			</span>
		</div>
	{/if}
</article>

<style>
	.card-bet {
		border-radius: 20px;
		padding: 14px 16px;
		background: var(--surface);
		border: 1px solid var(--border);
	}
	.top {
		display: flex;
		justify-content: space-between;
		gap: 12px;
	}
	.info {
		min-width: 0;
	}
	.label {
		font-size: 14px;
		font-weight: 700;
		margin-top: 5px;
	}
	.meta,
	.stake {
		font-size: 11px;
		color: var(--muted);
		margin-top: 3px;
	}
	.figures {
		text-align: right;
		flex: none;
	}
	.odds {
		font-size: 19px;
	}
	.bottom {
		margin-top: 12px;
		padding-top: 11px;
		border-top: 1px solid var(--border-soft);
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
	.bottom.open {
		flex-direction: column;
		align-items: stretch;
		gap: 10px;
	}
	.status-line {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
	.hint {
		font-size: 12px;
		font-weight: 600;
		color: var(--muted);
	}
	.result {
		font-size: 15px;
	}
</style>
