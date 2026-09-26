<script lang="ts">
	import { api, mutate } from '$lib/api/client';
	import type { RuleKind } from '$lib/api/types';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import { parseNumber } from '$lib/format';
	import { RULE_KINDS, ruleLabel } from '$lib/labels';

	let { data } = $props();

	let kind = $state<RuleKind>('MAX_STAKE_PCT');
	let param = $state(String(RULE_KINDS[0].defaultParam));
	let error = $state<string | null>(null);
	let busy = $state(false);

	const definition = $derived(RULE_KINDS.find((k) => k.kind === kind) ?? RULE_KINDS[0]);
	const respected = $derived(data.rules.filter((rule) => rule.respected).length);

	async function run(action: () => Promise<unknown>) {
		busy = true;
		error = await mutate(action);
		busy = false;
	}

	const add = () =>
		run(() =>
			api.post('/rules', { kind, param: definition.unit ? Math.round(parseNumber(param)) : 0 })
		);
	const remove = (id: number) => run(() => api.delete(`/rules/${id}`));
</script>

<svelte:head><title>Journal · Betgamof</title></svelte:head>

<PageHeader
	title="Journal de discipline"
	subtitle="{respected} règle{respected > 1 ? 's' : ''} respectée{respected > 1
		? 's'
		: ''} sur {data.rules.length} · 30 derniers jours"
	back="/bankrolls"
/>

<div class="content">
	<section class="card rules">
		{#each data.rules as rule (rule.id)}
			<div class="rule" class:broken={!rule.respected}>
				<span class="box"></span>
				<div class="text">
					<div class="label">{ruleLabel(rule)}</div>
					<div class="state">
						{rule.respected ? 'Respectée' : 'Enfreinte sur les 30 derniers jours'}
					</div>
				</div>
				<button type="button" class="delete" onclick={() => remove(rule.id)} disabled={busy}
					>Retirer</button
				>
			</div>
		{:else}
			<p class="empty">
				Aucune règle pour l'instant. Les meilleurs parieurs se fixent des garde-fous avant d'en
				avoir besoin.
			</p>
		{/each}
	</section>

	<section class="card add">
		<h2 class="card-title">Ajouter une règle</h2>
		<label class="field">
			<span>Règle</span>
			<select
				bind:value={kind}
				onchange={() =>
					(param = String(RULE_KINDS.find((k) => k.kind === kind)?.defaultParam ?? 0))}
			>
				{#each RULE_KINDS as option (option.kind)}<option value={option.kind}>{option.label}</option
					>{/each}
			</select>
		</label>
		{#if definition.unit}
			<label class="field">
				<span>Seuil ({definition.unit})</span>
				<input class="big" inputmode="numeric" bind:value={param} />
			</label>
		{/if}
		<p class="preview">« {ruleLabel({ kind, param: Math.round(parseNumber(param)) })} »</p>
		{#if error}<div class="form-error">{error}</div>{/if}
		<button type="button" class="btn" onclick={add} disabled={busy}>Ajouter</button>
	</section>
</div>

<style>
	.content {
		padding: 22px 24px;
		display: grid;
		grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
		gap: 18px;
		align-items: start;
	}
	.rules {
		overflow: hidden;
	}
	.rule {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 16px 18px;
		border-bottom: 1px solid var(--border-soft);
	}
	.rule:last-child {
		border-bottom: none;
	}
	.box {
		width: 20px;
		height: 20px;
		border-radius: 6px;
		background: var(--grass);
		flex: none;
	}
	.broken .box {
		background: transparent;
		border: 2px solid var(--idle);
	}
	.text {
		flex: 1;
		min-width: 0;
	}
	.label {
		font-size: 14px;
		font-weight: 600;
		color: var(--text-3);
	}
	.state {
		font-size: 11px;
		color: var(--grass-strong);
		margin-top: 3px;
		font-weight: 600;
	}
	.broken .state {
		color: var(--loss);
	}
	.delete {
		border: 1px solid var(--border);
		background: var(--bg);
		border-radius: 10px;
		padding: 7px 11px;
		font-size: 12px;
		font-weight: 600;
		color: var(--muted);
	}
	.empty {
		margin: 0;
		padding: 18px;
		font-size: 13px;
		color: var(--muted);
	}
	.add {
		padding: 18px;
		display: flex;
		flex-direction: column;
		gap: 12px;
	}
	.add .field {
		background: var(--bg);
	}
	.preview {
		margin: 0;
		font-size: 13px;
		color: var(--text-2);
	}
	@media (max-width: 1023px) {
		.content {
			display: flex;
			flex-direction: column;
			align-items: stretch;
			padding: 0 20px 20px;
			gap: 14px;
		}
	}
</style>
