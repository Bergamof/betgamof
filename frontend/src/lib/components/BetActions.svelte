<script lang="ts">
	import { api, mutate } from '$lib/api/client';
	import type { Bet } from '$lib/api/types';
	import BetMenu from './BetMenu.svelte';

	// One-click "Gagné" / "Perdu" on a pending bet, plus the "···" menu.
	let {
		bet,
		variant = 'card',
		showMenu = true
	}: { bet: Bet; variant?: 'card' | 'row' | 'table'; showMenu?: boolean } = $props();

	let busy = $state(false);
	let error: string | null = $state(null);
	let menuOpen = $state(false);

	async function settle(status: 'WON' | 'LOST') {
		busy = true;
		error = await mutate(() => api.post(`/bets/${bet.id}/settlement`, { status }));
		busy = false;
	}
</script>

<div class="actions {variant}">
	<button type="button" class="won" disabled={busy} onclick={() => settle('WON')}>Gagné</button>
	<button type="button" class="lost" disabled={busy} onclick={() => settle('LOST')}>Perdu</button>
	{#if showMenu}
		<button type="button" class="more" aria-label="Plus d'actions" onclick={() => (menuOpen = true)}
			>···</button
		>
	{/if}
</div>
{#if error}<div class="error">{error}</div>{/if}
<BetMenu {bet} bind:open={menuOpen} />

<style>
	.actions {
		display: flex;
		gap: 8px;
	}
	button {
		font-size: 12px;
		font-weight: 700;
		padding: 9px 0;
		border-radius: 12px;
		border: 1px solid transparent;
	}
	.won {
		background: var(--grass);
		color: #fff;
	}
	.lost {
		background: var(--surface);
		border-color: var(--loss-border);
		color: var(--loss);
	}
	.more {
		flex: none;
		width: 44px;
		background: var(--bg);
		border-color: var(--border);
		color: var(--muted);
	}
	.card .won,
	.card .lost {
		flex: 1;
	}
	.row,
	.table {
		gap: 7px;
		justify-content: flex-end;
	}
	.row button {
		padding: 8px 14px;
		border-radius: 10px;
	}
	.table button {
		font-size: 11px;
		padding: 7px 11px;
		border-radius: 9px;
	}
	.table .more {
		width: 30px;
	}
	.error {
		font-size: 11px;
		color: var(--loss);
		margin-top: 6px;
	}
</style>
