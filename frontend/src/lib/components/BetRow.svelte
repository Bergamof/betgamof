<script lang="ts">
	import type { Bet } from '$lib/api/types';
	import { amount, odds, time } from '$lib/format';
	import { BET_TYPE_LABEL, sportColor } from '$lib/labels';
	import BetActions from './BetActions.svelte';

	// Row of the desktop "Paris du jour" list (3a).
	let { bet }: { bet: Bet } = $props();

	const context = $derived(
		bet.palierNumber !== null
			? `Palier ${bet.palierNumber} de la montante`
			: `${BET_TYPE_LABEL[bet.type]} · ${bet.bookmaker}`
	);
</script>

<div class="row">
	<div class="info">
		<span class="dot" style:background={sportColor(bet.sport)}></span>
		<div class="text">
			<div class="label">{bet.label}</div>
			<div class="meta">{bet.competition} · {time(bet.startsAt)} · {context}</div>
		</div>
	</div>
	<div class="side">
		<div class="figures">
			<div class="odds num" class:positive={bet.status === 'WON'}>{odds(bet.odds)}</div>
			<div class="meta">
				{#if bet.status === 'OPEN'}
					{amount(bet.stake)} → {amount(bet.potentialReturn)}
				{:else}
					{amount(bet.stake)} · {bet.status === 'WON'
						? 'gagné'
						: bet.status === 'LOST'
							? 'perdu'
							: 'réglé'}
				{/if}
			</div>
		</div>
		{#if bet.status === 'OPEN'}
			<BetActions {bet} variant="row" showMenu={false} />
		{:else}
			<span class="result" class:lost={bet.profit < 0}>{amount(bet.profit, { signed: true })}</span>
		{/if}
	</div>
</div>

<style>
	.row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 12px;
		padding: 14px 20px;
		border-bottom: 1px solid var(--border-soft);
	}
	.row:last-child {
		border-bottom: none;
	}
	.info {
		display: flex;
		align-items: center;
		gap: 12px;
		min-width: 0;
	}
	.dot {
		width: 9px;
		height: 9px;
		border-radius: 50%;
		flex: none;
	}
	.text {
		min-width: 0;
	}
	.label {
		font-size: 14px;
		font-weight: 700;
	}
	.meta {
		font-size: 11px;
		color: var(--muted);
		margin-top: 2px;
	}
	.side {
		display: flex;
		align-items: center;
		gap: 18px;
		flex: none;
	}
	.figures {
		text-align: right;
	}
	.odds {
		font-size: 16px;
	}
	.result {
		font-size: 12px;
		font-weight: 700;
		padding: 8px 14px;
		border-radius: 999px;
		background: var(--grass-soft);
		color: var(--grass-deep);
	}
	.result.lost {
		background: var(--loss-soft);
		color: var(--loss-soft-text);
	}
</style>
