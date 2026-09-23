<script lang="ts">
	import { sampleCurve, type CurvePoint } from './profit-bars';

	// "hero": translucent white bars on the green card; "chart": graded greens on white.
	let {
		points,
		count = 15,
		height = 96,
		variant = 'hero',
		from,
		gap = 6
	}: {
		points: CurvePoint[];
		count?: number;
		height?: number | 'fill';
		variant?: 'hero' | 'chart';
		from?: Date;
		gap?: number;
	} = $props();

	const bars = $derived(sampleCurve(points, count, from));
	const highlighted = $derived(variant === 'hero' ? 2 : 1);

	function color(index: number): string {
		if (index >= bars.length - highlighted) return 'var(--lemon)';
		if (variant === 'hero') return 'rgba(255, 255, 255, 0.32)';
		if (index > 0 && bars[index] < bars[index - 1]) return 'oklch(0.84 0.07 40)';
		const lightness = 0.86 - (index / Math.max(1, bars.length - 2)) * 0.31;
		const chroma = 0.06 + (index / Math.max(1, bars.length - 2)) * 0.08;
		return `oklch(${lightness.toFixed(3)} ${chroma.toFixed(3)} 145)`;
	}
</script>

<div
	class="bars {variant}"
	style:height={height === 'fill' ? '100%' : `${height}px`}
	style:gap="{gap}px"
	aria-hidden="true"
>
	{#each bars as bar, index (index)}
		<div class="bar" style:height="{Math.round(bar * 100)}%" style:background={color(index)}></div>
	{/each}
</div>

<style>
	.bars {
		display: flex;
		align-items: flex-end;
	}
	.bar {
		flex: 1;
		border-radius: 4px;
	}
	.chart .bar {
		border-radius: 3px 3px 0 0;
	}
</style>
