<script lang="ts">
	import type { BetStatus } from '$lib/api/types';
	import { BET_STATUS_LABEL } from '$lib/labels';

	let { status, solid = false }: { status: BetStatus; solid?: boolean } = $props();
</script>

<span class="pill {status.toLowerCase()}" class:solid>{BET_STATUS_LABEL[status]}</span>

<style>
	.pill {
		display: inline-block;
		font-size: 11px;
		font-weight: 700;
		padding: 5px 10px;
		border-radius: 999px;
		background: var(--grass-soft);
		color: var(--grass-deep);
		white-space: nowrap;
	}
	.won.solid {
		background: var(--grass);
		color: #fff;
	}
	.lost {
		background: var(--loss-soft);
		color: var(--loss-soft-text);
	}
	.void,
	.cashout {
		background: var(--bg);
		border: 1px solid var(--border);
		color: var(--text-2);
	}
</style>
