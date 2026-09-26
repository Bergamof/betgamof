<script lang="ts">
	// The mock-ups use small geometric marks instead of icons, one shape per section.
	import type { NavShape } from './nav';

	let {
		shape,
		active = false,
		size = 10
	}: { shape: NavShape; active?: boolean; size?: number } = $props();
</script>

<span
	class="mark {shape}"
	class:active
	style:width="{size}px"
	style:height="{size}px"
	aria-hidden="true"
></span>

<style>
	.mark {
		flex: none;
		display: inline-block;
		background: var(--idle);
	}
	.square {
		border-radius: 30%;
	}
	.tile {
		border-radius: 20%;
	}
	.circle {
		border-radius: 50%;
	}
	.square-outline,
	.circle-outline {
		background: transparent;
		border: 2px solid var(--idle);
	}
	.square-outline {
		border-radius: 30%;
	}
	.circle-outline {
		border-radius: 50%;
	}
	.active {
		background: var(--grass);
		border-color: var(--grass);
	}
</style>
