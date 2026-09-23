<script lang="ts">
	import { resolve } from '$app/paths';
	import type { Montante } from '$lib/api/types';
	import { amount } from '$lib/format';
	import { MONTANTE_STATUS_LABEL, montanteModeLabel } from '$lib/labels';
	import Meter from './Meter.svelte';

	// "dots": palier track of the mobile home (1c). "meter": compact card of the desktop dashboard (3a).
	let { montante, variant = 'meter' }: { montante: Montante; variant?: 'dots' | 'meter' } =
		$props();

	const active = $derived(montante.status === 'ACTIVE');
	const counter = $derived(
		montante.totalPaliers
			? `${montante.currentPalier}/${montante.totalPaliers}`
			: `${montante.currentPalier}`
	);
	const dots = $derived.by(() => {
		const total = Math.max(
			montante.totalPaliers ?? montante.currentPalier + 2,
			montante.currentPalier
		);
		return Array.from({ length: total }, (_, index) => {
			const number = index + 1;
			const palier = montante.paliers.find((p) => p.number === number);
			if (palier) return palier.status === 'LOST' ? 'lost' : 'done';
			return number === montante.currentPalier && active ? 'current' : 'todo';
		});
	});
</script>

<a
	href={resolve('/(app)/montantes/[id]', { id: String(montante.id) })}
	class="montante {variant}"
	class:active
>
	{#if variant === 'dots'}
		<div class="head">
			<span class="title">Montante · {montanteModeLabel(montante)}</span>
			<span class="counter">{counter}</span>
		</div>
		<div class="track" aria-hidden="true">
			{#each dots as dot, index (index)}
				{#if index > 0}<span class="link" class:filled={dot === 'done' || dot === 'lost'}
					></span>{/if}
				<span class="dot {dot}"></span>
			{/each}
		</div>
		<div class="foot">
			<div>
				<div class="range num">
					{amount(montante.startCapital)} → {montante.target !== null
						? amount(montante.target)
						: amount(montante.capital)}
				</div>
				<div class="sub">
					{montante.excludeStake ? 'Mise hors bankroll' : 'Mise dans la bankroll'}
				</div>
			</div>
			<span class="palier"
				>{active
					? `Palier ${montante.currentPalier}`
					: MONTANTE_STATUS_LABEL[montante.status]}</span
			>
		</div>
	{:else}
		<div class="head">
			<span class="title">{montante.name}</span>
			<span class="counter">
				{active ? `Palier ${counter.replace('/', ' / ')}` : MONTANTE_STATUS_LABEL[montante.status]}
			</span>
		</div>
		<div class="figures">
			<span class="capital num">{amount(montante.capital)}</span>
			{#if montante.target !== null}<span class="sub">objectif {amount(montante.target)}</span>{/if}
		</div>
		{#if montante.progress !== null}
			<div class="meter"><Meter value={montante.progress} /></div>
		{/if}
		<div class="chips">
			<span class="chip"
				>{montante.excludeStake ? 'Mise hors bankroll' : 'Mise dans la bankroll'}</span
			>
			{#if montante.securePct > 0}
				<span class="chip lemon">{montante.securePct} % des gains sécurisés</span>
			{/if}
		</div>
	{/if}
</a>

<style>
	.montante {
		display: block;
		background: var(--surface);
		border: 1px solid var(--border);
	}
	.dots {
		border-radius: 26px;
		padding: 20px;
	}
	.meter {
		border-radius: 22px;
		padding: 18px;
	}
	.montante.meter.active {
		border: 2px solid var(--grass);
	}
	.head {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 10px;
	}
	.title {
		font-family: var(--font-display);
		font-size: 17px;
		font-weight: 700;
	}
	.meter .title {
		font-size: 16px;
	}
	.counter {
		font-size: 12px;
		font-weight: 700;
		color: var(--grass-strong);
		white-space: nowrap;
	}
	.track {
		display: flex;
		align-items: center;
		gap: 7px;
		margin: 16px 0 14px;
	}
	.dot {
		width: 22px;
		height: 22px;
		border-radius: 50%;
		flex: none;
		background: #e9ede4;
	}
	.dot.done {
		background: var(--grass);
	}
	.dot.lost {
		background: var(--loss-dot);
	}
	.dot.current {
		width: 26px;
		height: 26px;
		background: var(--lemon);
		border: 2px solid var(--grass);
	}
	.link {
		flex: 1;
		height: 3px;
		background: var(--border);
	}
	.link.filled {
		background: var(--grass);
	}
	.foot {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
	.range {
		font-size: 20px;
	}
	.sub {
		font-size: 11px;
		color: var(--muted);
		margin-top: 3px;
	}
	.meter .sub {
		font-size: 12px;
		margin: 0;
	}
	.palier {
		font-size: 12px;
		font-weight: 700;
		padding: 9px 15px;
		border-radius: 999px;
		background: var(--grass);
		color: #fff;
	}
	.figures {
		display: flex;
		justify-content: space-between;
		align-items: baseline;
		margin-top: 10px;
	}
	.capital {
		font-size: 24px;
	}
	.meter .meter {
		padding: 0;
		margin-top: 10px;
	}
	.chips {
		display: flex;
		gap: 7px;
		margin-top: 12px;
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
	.chip.lemon {
		background: var(--lemon-soft);
		color: var(--lemon-text);
	}
</style>
