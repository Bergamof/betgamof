<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		tone = 'warn',
		children,
		action
	}: { tone?: 'warn' | 'lemon' | 'loss'; children: Snippet; action?: Snippet } = $props();
</script>

<div class="alert {tone}" role={tone === 'loss' ? 'alert' : 'status'}>
	<span class="dot"></span>
	<span class="text">{@render children()}</span>
	{@render action?.()}
</div>

<style>
	.alert {
		display: flex;
		gap: 10px;
		align-items: center;
		border-radius: 18px;
		padding: 12px 15px;
		font-size: 12px;
		font-weight: 600;
	}
	.text {
		flex: 1;
	}
	.dot {
		width: 8px;
		height: 8px;
		border-radius: 50%;
		flex: none;
	}
	.warn {
		background: var(--warn-bg);
		border: 1px solid var(--warn-border);
		color: var(--warn-text);
	}
	.warn .dot {
		background: var(--warn-dot);
	}
	.lemon {
		background: var(--lemon-soft);
		color: var(--lemon-text);
		border-radius: 14px;
		padding: 11px 14px;
	}
	.lemon .dot {
		background: var(--lemon-dot);
	}
	.loss {
		background: var(--loss-panel);
		border: 1px solid var(--loss-border);
		color: var(--loss-panel-text);
	}
	.loss .dot {
		background: var(--loss-dot);
	}
</style>
