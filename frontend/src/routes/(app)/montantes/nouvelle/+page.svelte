<script lang="ts">
	import { resolve } from '$app/paths';
	import { goto } from '$app/navigation';
	import { untrack } from 'svelte';
	import { api, ApiError } from '$lib/api/client';
	import type { Montante, MontanteMode, MontantePlan, MontanteRequest } from '$lib/api/types';
	import ChoiceChips from '$lib/components/ChoiceChips.svelte';
	import Toggle from '$lib/components/Toggle.svelte';
	import { amount, money, odds, parseNumber, percent } from '$lib/format';

	let { data } = $props();

	const MULTIPLIERS = [2, 3, 5, 10];
	const STEP_COUNTS = [4, 6, 8, 10];
	const SECURE_PRESETS = [10, 30, 50];
	const PREVIEW_ROWS = 3;

	let name = $state('Montante');
	let bankrollId = $state(untrack(() => data.bankroll?.id ?? data.bankrolls[0]?.id ?? 0));
	let startCapital = $state('100');
	let targetOdds = $state('1.75');
	let mode = $state<MontanteMode>('OBJECTIVE');
	let multiplier = $state(3);
	let stepCount = $state(8);
	let excludeStake = $state(true);
	let secure = $state(true);
	let securePreset = $state<number | 'custom'>(30);
	let customSecure = $state('20');
	let relances = $state(0);
	let showAllSteps = $state(false);
	let plan = $state<MontantePlan | null>(null);
	let error = $state<string | null>(null);
	let saving = $state(false);

	const bankroll = $derived(data.bankrolls.find((b) => b.id === bankrollId) ?? null);
	const securePct = $derived(
		secure ? (securePreset === 'custom' ? Math.round(parseNumber(customSecure)) : securePreset) : 0
	);
	const capital = $derived(parseNumber(startCapital));
	const request: MontanteRequest = $derived({
		name: name.trim(),
		bankrollId,
		startCapital: capital,
		targetOdds: parseNumber(targetOdds),
		mode,
		targetMultiplier: mode === 'OBJECTIVE' ? multiplier : null,
		stepCount: mode === 'STEPS' ? stepCount : null,
		excludeStake,
		securePct,
		relancesAllowed: relances
	});
	const firstStep = $derived(plan?.steps[0] ?? null);
	const visibleSteps = $derived(
		plan ? (showAllSteps ? plan.steps : plan.steps.slice(0, PREVIEW_ROWS)) : []
	);

	// Live preview from the backend planner, debounced while typing.
	$effect(() => {
		const body = request;
		if (!body.bankrollId || body.startCapital <= 0 || body.targetOdds <= 1) {
			plan = null;
			return;
		}
		const timer = setTimeout(async () => {
			plan = await api.post<MontantePlan>('/montantes/preview', body).catch(() => null);
		}, 250);
		return () => clearTimeout(timer);
	});

	async function launch() {
		error = null;
		saving = true;
		try {
			const montante = await api.post<Montante>('/montantes', request);
			await goto(resolve('/(app)/montantes/[id]', { id: String(montante.id) }), {
				invalidateAll: true
			});
		} catch (cause) {
			error = cause instanceof ApiError ? cause.message : 'Impossible de lancer la montante.';
		} finally {
			saving = false;
		}
	}
</script>

<svelte:head><title>Nouvelle montante · Betgamof</title></svelte:head>

<header class="bar">
	<a href={resolve('/montantes')} class="side-link">Annuler</a>
	<h1 class="display">Nouvelle montante</h1>
	<a href="#aide" class="side-link right">Aide</a>
</header>

<div class="content">
	<label class="field">
		<span>Nom</span>
		<input bind:value={name} maxlength="60" />
	</label>

	<label class="field">
		<span>Bankroll support</span>
		<select bind:value={bankrollId}>
			{#each data.bankrolls as option (option.id)}
				<option value={option.id}>{option.name} · {money(option.balance)}</option>
			{/each}
		</select>
	</label>

	<div class="row">
		<label class="field">
			<span>Capital de départ</span>
			<input class="big" inputmode="decimal" bind:value={startCapital} />
		</label>
		<label class="field">
			<span>Cote visée / palier</span>
			<input class="big" inputmode="decimal" bind:value={targetOdds} />
		</label>
	</div>

	<section class="card block">
		<div class="eyebrow">Mode de pilotage</div>
		<div class="modes" role="radiogroup" aria-label="Mode de pilotage">
			{#each [['OBJECTIVE', 'Objectif'], ['STEPS', 'Paliers'], ['FREE', 'Libre']] as const as [value, label] (value)}
				<button
					type="button"
					role="radio"
					aria-checked={mode === value}
					class:selected={mode === value}
					onclick={() => (mode = value)}>{label}</button
				>
			{/each}
		</div>
		{#if mode === 'OBJECTIVE'}
			<ChoiceChips
				label="Objectif"
				options={MULTIPLIERS.map((value) => ({ value, label: `x${value}` }))}
				value={multiplier}
				onselect={(value) => (multiplier = value)}
			/>
		{:else if mode === 'STEPS'}
			<ChoiceChips
				label="Nombre de paliers"
				options={STEP_COUNTS.map((value) => ({ value, label: String(value) }))}
				value={stepCount}
				onselect={(value) => (stepCount = value)}
			/>
		{:else}
			<p class="explain">Tu continues tant que ça gagne, et tu clôtures quand tu veux.</p>
		{/if}
		{#if plan && mode !== 'FREE'}
			<div class="summary">
				<span>Objectif : <b>{plan.target !== null ? amount(plan.target) : '—'}</b></span>
				<span>
					{plan.plannedSteps} paliers à {odds(request.targetOdds)} · succès estimé
					<b>{percent(plan.successProbability)}</b>
				</span>
			</div>
		{/if}
	</section>

	<section class="card-soft block">
		<div class="toggle-row">
			<Toggle
				label="Exclure la mise engagée de la bankroll"
				size="lg"
				bind:checked={excludeStake}
			/>
			<div>
				<div class="toggle-title">Exclure la mise engagée de la bankroll</div>
				<p class="explain">
					{#if excludeStake}
						Les {amount(capital)} sortent de la bankroll dès le lancement : elle affichera
						<b>{plan ? money(plan.bankrollBalanceAfterLaunch) : '…'}</b> et la montante vit sur son
						propre capital. À la clôture, le solde final — gains comme pertes — est reporté sur la
						bankroll. Désactive pour garder les {amount(capital)} dans la bankroll et suivre le ROI global.
					{:else}
						Les {amount(capital)} restent dans la bankroll : chaque palier gagné ou perdu fait varier
						son solde en direct. Active pour isoler la montante et ne reporter que son solde final à la
						clôture.
					{/if}
				</p>
			</div>
		</div>
	</section>

	<section class="card block">
		<div class="toggle-row center">
			<Toggle label="Sécuriser une part des gains" size="lg" bind:checked={secure} />
			<div class="toggle-title">Sécuriser une part des gains</div>
		</div>
		{#if secure}
			<ChoiceChips
				label="Part sécurisée"
				options={[
					...SECURE_PRESETS.map((value) => ({ value, label: `${value} %` })),
					{ value: 'custom' as const, label: 'Perso' }
				]}
				value={securePreset}
				onselect={(value) => (securePreset = value)}
			/>
			{#if securePreset === 'custom'}
				<label class="field inset">
					<span>Part sécurisée (%)</span>
					<input inputmode="numeric" bind:value={customSecure} />
				</label>
			{/if}
			<p class="explain">
				À chaque palier gagné, {securePct} % du gain est mis de côté et ne repart pas dans la montante.
				{#if firstStep}
					Palier 1 : <b>{amount(firstStep.secured)} sécurisés</b>, {amount(firstStep.capitalAfter)} relancés.
				{/if}
			</p>
			<div class="line">
				<span>Destination des gains sécurisés</span>
				<b>Bankroll {bankroll?.name.toLowerCase()}</b>
			</div>
		{/if}
	</section>

	<section class="card block">
		<div class="line plain">
			<div>
				<div class="toggle-title">Relances autorisées</div>
				<p class="explain">
					Après une perte, la montante repart du capital de départ, réengagé depuis la bankroll.
				</p>
			</div>
			<div class="stepper">
				<button
					type="button"
					aria-label="Moins"
					onclick={() => (relances = Math.max(0, relances - 1))}>−</button
				>
				<span class="num">{relances}</span>
				<button
					type="button"
					aria-label="Plus"
					onclick={() => (relances = Math.min(10, relances + 1))}>+</button
				>
			</div>
		</div>
	</section>

	{#if plan}
		<section class="card steps">
			<div class="steps-row head eyebrow">
				<span>Palier</span><span>Mise</span><span>Capital visé</span>
			</div>
			{#each visibleSteps as step (step.number)}
				<div class="steps-row" class:faded={!showAllSteps && step.number === PREVIEW_ROWS}>
					<b>{step.number}</b><span>{amount(step.stake)}</span>
					<span class="num">{amount(step.capitalAfter)}</span>
				</div>
			{/each}
			{#if plan.steps.length > PREVIEW_ROWS}
				<button type="button" class="link more" onclick={() => (showAllSteps = !showAllSteps)}>
					{showAllSteps ? 'Réduire' : `Voir les ${plan.steps.length} paliers`}
				</button>
			{/if}
		</section>
	{/if}

	<section id="aide" class="help">
		<b>Comment ça marche ?</b> Chaque palier mise tout le capital courant. Gagné : le gain (moins la part
		sécurisée) est rejoué au palier suivant. Perdu : la montante s'arrête, sauf s'il reste une relance.
		Les gains sécurisés sont acquis quoi qu'il arrive.
	</section>

	{#if error}<div class="form-error">{error}</div>{/if}
</div>

<div class="footer">
	<button class="btn launch" type="button" onclick={launch} disabled={saving || !plan}>
		{saving ? 'Lancement…' : 'Lancer la montante'}
	</button>
</div>

<style>
	.bar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: calc(16px + env(safe-area-inset-top)) 24px 14px;
		max-width: 600px;
		width: 100%;
		margin: 0 auto;
	}
	h1 {
		margin: 0;
		font-size: 18px;
	}
	.side-link {
		width: 60px;
		font-size: 13px;
		font-weight: 600;
		color: var(--muted);
	}
	.side-link.right {
		text-align: right;
		color: var(--idle);
	}
	.content {
		padding: 0 20px 20px;
		display: flex;
		flex-direction: column;
		gap: 14px;
		max-width: 600px;
		width: 100%;
		margin: 0 auto;
	}
	.row {
		display: flex;
		gap: 10px;
	}
	.row > * {
		flex: 1;
		min-width: 0;
	}
	.block {
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 11px;
	}
	.modes {
		display: flex;
		gap: 7px;
	}
	.modes button {
		flex: 1;
		font-size: 12px;
		font-weight: 600;
		padding: 10px 0;
		border-radius: 999px;
		background: var(--bg);
		border: 1px solid var(--border);
		color: var(--text-2);
	}
	.modes button.selected {
		background: var(--grass);
		border-color: var(--grass);
		color: #fff;
		font-weight: 700;
	}
	.summary {
		display: flex;
		justify-content: space-between;
		gap: 10px;
		flex-wrap: wrap;
		font-size: 12px;
		color: var(--muted);
	}
	.summary b {
		color: var(--text);
	}
	.toggle-row {
		display: flex;
		gap: 12px;
		align-items: flex-start;
	}
	.toggle-row.center {
		align-items: center;
	}
	.toggle-title {
		font-size: 14px;
		font-weight: 700;
	}
	.explain {
		font-size: 12px;
		color: var(--text-2);
		margin: 5px 0 0;
		line-height: 1.45;
	}
	.line {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 10px;
		padding: 11px 14px;
		border-radius: 14px;
		background: var(--bg);
		border: 1px solid var(--border);
		font-size: 12px;
		color: var(--text-2);
	}
	.line b {
		color: var(--text);
	}
	.line.plain {
		padding: 0;
		background: none;
		border: none;
	}
	.inset {
		background: var(--bg);
	}
	.stepper {
		display: flex;
		align-items: center;
		gap: 10px;
		flex: none;
	}
	.stepper button {
		width: 34px;
		height: 34px;
		border-radius: 11px;
		border: 1px solid var(--border);
		background: var(--bg);
		font-size: 16px;
		font-weight: 700;
	}
	.stepper .num {
		font-size: 18px;
		min-width: 18px;
		text-align: center;
	}
	.steps {
		overflow: hidden;
	}
	.steps-row {
		display: grid;
		grid-template-columns: 1fr 1fr 1fr;
		padding: 11px 16px;
		font-size: 13px;
		border-top: 1px solid var(--border-soft);
	}
	.steps-row > :nth-child(2) {
		text-align: center;
	}
	.steps-row > :last-child {
		text-align: right;
	}
	.steps-row.head {
		border-top: none;
		background: var(--bg);
		padding: 12px 16px;
		font-size: 11px;
	}
	.faded {
		color: var(--muted);
	}
	.more {
		display: block;
		width: 100%;
		padding: 10px 16px;
		border-top: 1px solid var(--border-soft);
	}
	.help {
		font-size: 12px;
		color: var(--muted);
		line-height: 1.5;
		padding: 0 4px;
	}
	.footer {
		position: sticky;
		bottom: 0;
		padding: 14px 20px 26px;
		background: var(--surface);
		border-top: 1px solid var(--border);
		display: flex;
		justify-content: center;
	}
	.launch {
		width: 100%;
		max-width: 560px;
		height: 52px;
		border-radius: 16px;
		font-size: 15px;
	}
	@media (max-width: 1023px) {
		.footer {
			bottom: calc(var(--tabbar-height) + 10px);
			margin-bottom: -24px;
		}
	}
</style>
