<script lang="ts">
	let {
		value,
		height = 8,
		color = 'var(--grass)',
		track = 'var(--track)'
	}: { value: number; height?: number; color?: string; track?: string } = $props();

	const width = $derived(`${Math.round(Math.min(1, Math.max(0, value)) * 100)}%`);
</script>

<div
	class="meter"
	style:height="{height}px"
	style:background={track}
	role="meter"
	aria-valuemin={0}
	aria-valuemax={100}
	aria-valuenow={Math.round(value * 100)}
>
	<div class="fill" style:width style:background={color}></div>
</div>

<style>
	.meter {
		border-radius: 999px;
		overflow: hidden;
	}
	.fill {
		height: 100%;
		border-radius: 999px;
	}
</style>
