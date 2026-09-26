<script lang="ts">
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import BetForm from '$lib/components/BetForm.svelte';

	let { data } = $props();
</script>

<svelte:head><title>Nouveau pari · Betgamof</title></svelte:head>

<header class="bar">
	<a href={resolve('/paris')} class="cancel">Annuler</a>
	<h1 class="display">Nouveau pari</h1>
	<span class="spacer"></span>
</header>

<div class="content">
	<BetForm
		bankrolls={data.bankrolls}
		montantes={data.montantes}
		events={data.events}
		defaultBankrollId={data.bankroll?.id ?? null}
		initialMontanteId={data.montanteId}
		onsaved={() =>
			goto(
				data.montanteId
					? resolve('/(app)/montantes/[id]', { id: String(data.montanteId) })
					: resolve('/paris')
			)}
	/>
</div>

<style>
	.bar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: calc(16px + env(safe-area-inset-top)) 24px 14px;
		max-width: 560px;
		width: 100%;
		margin: 0 auto;
	}
	h1 {
		margin: 0;
		font-size: 18px;
	}
	.cancel,
	.spacer {
		width: 60px;
		font-size: 13px;
		font-weight: 600;
		color: var(--muted);
	}
	.content {
		padding: 0 20px 20px;
		max-width: 560px;
		width: 100%;
		margin: 0 auto;
	}
</style>
