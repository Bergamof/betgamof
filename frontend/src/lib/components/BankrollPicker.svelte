<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import type { Bankroll } from '$lib/api/types';

	// Header selector "Principale ▾": remembers the bankroll shown on the dashboard.
	let { bankrolls, selected }: { bankrolls: Bankroll[]; selected: Bankroll | null } = $props();

	async function choose(event: Event) {
		const id = (event.currentTarget as HTMLSelectElement).value;
		document.cookie = `betgamof_bankroll=${id}; path=/; max-age=31536000; samesite=lax`;
		await invalidateAll();
	}
</script>

{#if bankrolls.length > 0}
	<label class="picker">
		<span class="sr-only">Bankroll affichée</span>
		<select value={selected?.id} onchange={choose}>
			{#each bankrolls as bankroll (bankroll.id)}
				<option value={bankroll.id}>{bankroll.name}</option>
			{/each}
		</select>
		<span aria-hidden="true">▾</span>
	</label>
{/if}

<style>
	.picker {
		height: 38px;
		border-radius: 12px;
		background: var(--grass-soft);
		border: 1px solid var(--grass-soft-border);
		display: flex;
		align-items: center;
		gap: 6px;
		padding: 0 13px;
		font-size: 13px;
		font-weight: 700;
		color: var(--grass-deep);
		position: relative;
	}
	select {
		appearance: none;
		border: none;
		background: transparent;
		font-weight: 700;
		color: inherit;
		padding-right: 2px;
		outline: none;
	}
</style>
