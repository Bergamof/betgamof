<script lang="ts">
	import { resolve } from '$app/paths';
	import MontanteCard from '$lib/components/MontanteCard.svelte';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import { amount, shortDate } from '$lib/format';
	import { MONTANTE_STATUS_LABEL, montanteModeLabel } from '$lib/labels';

	let { data } = $props();

	const active = $derived(data.montantes.filter((m) => m.status === 'ACTIVE'));
	const finished = $derived(data.montantes.filter((m) => m.status !== 'ACTIVE'));
</script>

<svelte:head><title>Montantes · Betgamof</title></svelte:head>

<PageHeader
	title="Montantes"
	subtitle="{active.length} en cours · {finished.length} terminée{finished.length > 1 ? 's' : ''}"
>
	{#snippet actions()}
		<a href={resolve('/montantes/nouvelle')} class="btn small">Nouvelle montante</a>
	{/snippet}
	{#snippet mobileActions()}
		<a href={resolve('/montantes/nouvelle')} class="plus" aria-label="Nouvelle montante">+</a>
	{/snippet}
</PageHeader>

<div class="content">
	{#if active.length === 0}
		<div class="card empty">
			<h2 class="card-title">Aucune montante en cours</h2>
			<p>
				Une montante rejoue le gain de chaque palier sur le suivant. Fixe un objectif, un nombre de
				paliers ou laisse-la courir tant que ça gagne.
			</p>
			<a href={resolve('/montantes/nouvelle')} class="btn">Lancer une montante</a>
		</div>
	{:else}
		<div class="grid">
			{#each active as montante (montante.id)}
				<div class="mobile-only"><MontanteCard {montante} variant="dots" /></div>
				<div class="desktop-only"><MontanteCard {montante} /></div>
			{/each}
		</div>
	{/if}

	{#if finished.length > 0}
		<h2 class="eyebrow section">Terminées</h2>
		<div class="card finished">
			{#each finished as montante (montante.id)}
				<a href={resolve('/(app)/montantes/[id]', { id: String(montante.id) })} class="row">
					<div>
						<div class="name">{montante.name}</div>
						<div class="meta">
							{montanteModeLabel(montante)} · {montante.paliers.length} paliers · lancée le {shortDate(
								montante.createdAt
							)}
						</div>
					</div>
					<div class="side">
						<span class="status {montante.status.toLowerCase()}"
							>{MONTANTE_STATUS_LABEL[montante.status]}</span
						>
						<span
							class="num"
							class:positive={montante.result > 0}
							class:negative={montante.result < 0}
						>
							{amount(montante.result, { signed: true })}
						</span>
					</div>
				</a>
			{/each}
		</div>
	{/if}
</div>

<style>
	.content {
		padding: 22px 24px;
		display: flex;
		flex-direction: column;
		gap: 14px;
	}
	.grid {
		display: grid;
		grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
		gap: 14px;
	}
	.empty {
		padding: 24px;
		display: flex;
		flex-direction: column;
		gap: 10px;
		align-items: flex-start;
		max-width: 560px;
	}
	.empty p {
		margin: 0;
		color: var(--text-2);
		font-size: 14px;
		line-height: 1.5;
	}
	.section {
		margin: 10px 4px 0;
	}
	.finished {
		overflow: hidden;
	}
	.row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 12px;
		padding: 14px 18px;
		border-bottom: 1px solid var(--border-soft);
	}
	.row:last-child {
		border-bottom: none;
	}
	.name {
		font-size: 14px;
		font-weight: 700;
	}
	.meta {
		font-size: 11px;
		color: var(--muted);
		margin-top: 3px;
	}
	.side {
		display: flex;
		align-items: center;
		gap: 14px;
		flex: none;
	}
	.status {
		font-size: 11px;
		font-weight: 700;
		padding: 5px 10px;
		border-radius: 999px;
		background: var(--bg);
		border: 1px solid var(--border);
		color: var(--text-2);
	}
	.status.succeeded {
		background: var(--grass-soft);
		border-color: transparent;
		color: var(--grass-deep);
	}
	.status.broken {
		background: var(--loss-soft);
		border-color: transparent;
		color: var(--loss-soft-text);
	}
	.plus {
		width: 38px;
		height: 38px;
		border-radius: 12px;
		background: var(--grass);
		color: #fff;
		display: grid;
		place-items: center;
		font-size: 20px;
		font-weight: 700;
	}
	@media (max-width: 1023px) {
		.content {
			padding: 0 20px 20px;
		}
		.grid {
			grid-template-columns: 1fr;
		}
	}
</style>
