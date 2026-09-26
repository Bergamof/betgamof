<script lang="ts">
	import { untrack } from 'svelte';
	import { api, mutate } from '$lib/api/client';
	import type { Bankroll, BankrollColor, BankrollRequest } from '$lib/api/types';
	import { parseNumber } from '$lib/format';
	import { BANKROLL_COLORS, BANKROLL_COLOR_LABEL } from '$lib/labels';
	import ChoiceChips from './ChoiceChips.svelte';

	let { bankroll = null, ondone }: { bankroll?: Bankroll | null; ondone: () => void } = $props();

	const KELLY_FRACTIONS = [0.1, 0.25, 0.5, 1];
	const initial = untrack(() => bankroll);

	let name = $state(initial?.name ?? '');
	let color = $state<BankrollColor>(initial?.color ?? 'GAZON');
	let initialBalance = $state(initial ? String(initial.initialBalance) : '');
	let stopLoss = $state(initial?.stopLoss != null ? String(initial.stopLoss) : '');
	let kellyFraction = $state(initial?.kellyFraction ?? 0.25);
	let fixedStake = $state(initial?.fixedStake != null ? String(initial.fixedStake) : '');
	let error = $state<string | null>(null);
	let busy = $state(false);

	const optional = (value: string) => (value.trim() === '' ? null : parseNumber(value));

	async function save() {
		const body: BankrollRequest = {
			name: name.trim(),
			color,
			initialBalance: parseNumber(initialBalance),
			stopLoss: optional(stopLoss),
			kellyFraction,
			fixedStake: optional(fixedStake)
		};
		busy = true;
		error = await mutate(() =>
			initial ? api.put(`/bankrolls/${initial.id}`, body) : api.post('/bankrolls', body)
		);
		busy = false;
		if (!error) ondone();
	}

	async function remove() {
		if (!initial) return;
		busy = true;
		error = await mutate(() => api.delete(`/bankrolls/${initial.id}`));
		busy = false;
		if (!error) ondone();
	}
</script>

<label class="field">
	<span>Nom</span>
	<input bind:value={name} maxlength="40" placeholder="Principale" />
</label>
<div class="colors" role="radiogroup" aria-label="Couleur">
	{#each Object.keys(BANKROLL_COLORS) as BankrollColor[] as key (key)}
		<button
			type="button"
			role="radio"
			aria-checked={color === key}
			aria-label={BANKROLL_COLOR_LABEL[key]}
			class:selected={color === key}
			style:background={BANKROLL_COLORS[key]}
			onclick={() => (color = key)}
		></button>
	{/each}
</div>
<div class="row">
	<label class="field">
		<span>Solde initial (€)</span>
		<input class="big" inputmode="decimal" bind:value={initialBalance} placeholder="1000" />
	</label>
	<label class="field">
		<span>Stop-loss (€, optionnel)</span>
		<input class="big" inputmode="decimal" bind:value={stopLoss} placeholder="900" />
	</label>
</div>
<div class="group">
	<span class="eyebrow">Fraction de Kelly</span>
	<ChoiceChips
		label="Fraction de Kelly"
		options={KELLY_FRACTIONS.map((value) => ({ value, label: `${value * 100} %` }))}
		value={kellyFraction}
		onselect={(value) => (kellyFraction = value)}
	/>
</div>
<label class="field">
	<span>Mise fixe par défaut (€, optionnel)</span>
	<input inputmode="decimal" bind:value={fixedStake} placeholder="5" />
</label>
{#if error}<div class="form-error">{error}</div>{/if}
<div class="actions">
	{#if initial}
		<button type="button" class="btn danger" onclick={remove} disabled={busy}>Supprimer</button>
	{/if}
	<button type="button" class="btn grow" onclick={save} disabled={busy || !name.trim()}>
		{initial ? 'Enregistrer' : 'Créer la bankroll'}
	</button>
</div>

<style>
	.colors {
		display: flex;
		gap: 10px;
	}
	.colors button {
		width: 34px;
		height: 34px;
		border-radius: 50%;
		border: 3px solid transparent;
		box-shadow: inset 0 0 0 2px #fff;
	}
	.colors button.selected {
		border-color: var(--text);
	}
	.row {
		display: flex;
		gap: 10px;
	}
	.row > * {
		flex: 1;
		min-width: 0;
	}
	.group {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}
	.actions {
		display: flex;
		gap: 10px;
	}
	.grow {
		flex: 1;
	}
</style>
