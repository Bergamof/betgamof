<script lang="ts">
	let {
		value,
		label,
		hint,
		tone = 'default',
		size = 'md'
	}: {
		value: string;
		label: string;
		hint?: string;
		tone?: 'default' | 'positive' | 'negative' | 'accent';
		size?: 'sm' | 'md';
	} = $props();
</script>

<div class="tile {size}">
	<div class="value num {tone}">{value}</div>
	<div class="label">{label}</div>
	{#if hint}<div class="hint">{hint}</div>{/if}
</div>

<style>
	.tile {
		border-radius: 20px;
		padding: 18px;
		background: var(--surface);
		border: 1px solid var(--border);
		min-width: 0;
	}
	.tile.sm {
		padding: 14px;
	}
	.value {
		font-size: 26px;
		white-space: nowrap;
	}
	.sm .value {
		font-size: 23px;
	}
	.label {
		font-size: 12px;
		color: var(--muted);
		margin-top: 3px;
	}
	.sm .label {
		font-size: 11px;
		margin-top: 2px;
	}
	.hint {
		font-size: 11px;
		color: var(--muted-2);
		margin-top: 8px;
	}
	.positive {
		color: var(--grass-strong);
	}
	.accent {
		color: var(--grass);
	}
	.negative {
		color: var(--loss);
	}
</style>
