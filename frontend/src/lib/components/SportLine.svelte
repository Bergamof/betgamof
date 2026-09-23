<script lang="ts">
	import { sportColor } from '$lib/labels';

	let { sport, text, size = 8 }: { sport: string; text: string; size?: number } = $props();
</script>

<div class="line">
	<span
		class="dot"
		style:background={sportColor(sport)}
		style:width="{size}px"
		style:height="{size}px"
	></span>
	<span class="text">{text}</span>
</div>

<style>
	.line {
		display: flex;
		gap: 6px;
		align-items: center;
		min-width: 0;
	}
	.dot {
		border-radius: 50%;
		flex: none;
	}
	.text {
		font-size: 11px;
		font-weight: 600;
		color: var(--muted);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}
</style>
