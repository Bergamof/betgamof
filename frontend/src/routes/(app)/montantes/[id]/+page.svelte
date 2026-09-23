<script lang="ts">
	import { resolve } from '$app/paths';
	import { api, mutate } from '$lib/api/client';
	import Alert from '$lib/components/Alert.svelte';
	import PageHeader from '$lib/components/PageHeader.svelte';
	import Toggle from '$lib/components/Toggle.svelte';
	import { amount, kickoff, odds, percent, shortDate } from '$lib/format';
	import { montanteModeLabel } from '$lib/labels';
	import Meter from '$lib/components/Meter.svelte';

	let { data } = $props();

	const m = $derived(data.montante);
	const next = $derived(m.nextStep);
	const openBet = $derived(next?.openBet ?? null);
	const active = $derived(m.status === 'ACTIVE');
	const bankrollLabel = $derived(`bankroll ${m.bankrollName.toLowerCase()}`);
	const gainSinceStart = $derived(m.capital + m.secured - m.engaged);
	const upcoming = $derived(
		active && m.totalPaliers !== null && m.currentPalier < m.totalPaliers
			? {
					number: m.currentPalier + 1,
					stake: next?.capitalIfWon ?? null
				}
			: null
	);
	const singleMontanteRule = $derived(data.rules.find((r) => r.kind === 'SINGLE_ACTIVE_MONTANTE'));

	let busy = $state(false);
	let error = $state<string | null>(null);

	async function run(action: () => Promise<unknown>) {
		busy = true;
		error = await mutate(action);
		busy = false;
	}

	const settle = (status: 'WON' | 'LOST') =>
		openBet && run(() => api.post(`/bets/${openBet.id}/settlement`, { status }));
	const close = () => run(() => api.post(`/montantes/${m.id}/close`));

	function closureText(): string {
		const returned = amount(m.capital + m.secured);
		switch (m.status) {
			case 'SUCCEEDED':
				return `Objectif atteint : ${returned} reportés sur la ${bankrollLabel}, soit ${amount(m.result, { signed: true })} net.`;
			case 'BROKEN':
				return `Palier perdu : les ${amount(m.engaged)} engagés sont perdus, ${amount(m.secured)} de gains sécurisés restent acquis.`;
			default:
				return `Montante clôturée : ${returned} reportés sur la ${bankrollLabel} (${amount(m.result, { signed: true })} net).`;
		}
	}

	function relanceText(): string {
		const used = `${m.relancesUsed} relance${m.relancesUsed > 1 ? 's' : ''} utilisée${m.relancesUsed > 1 ? 's' : ''} sur ${m.relancesAllowed}`;
		const left = m.relancesAllowed - m.relancesUsed;
		return left === 0
			? `${used} — la prochaine perte clôture la montante`
			: `${used} — il en reste ${left}`;
	}
</script>

<svelte:head><title>{m.name} · Betgamof</title></svelte:head>

<PageHeader
	title={m.name}
	subtitle="Lancée le {shortDate(m.createdAt)} · {bankrollLabel} · {montanteModeLabel(
		m
	).toLowerCase()}"
	back="/montantes"
>
	{#snippet actions()}
		{#if active}
			<button type="button" class="btn small secondary" onclick={close} disabled={busy || !!openBet}
				>Clôturer</button
			>
			{#if openBet}
				<button
					type="button"
					class="btn small danger"
					onclick={() => settle('LOST')}
					disabled={busy}>Perdu</button
				>
				<button type="button" class="btn small" onclick={() => settle('WON')} disabled={busy}>
					Valider le palier {m.currentPalier}
				</button>
			{:else}
				<a class="btn small" href="{resolve('/paris/nouveau')}?montante={m.id}"
					>Choisir le pari du palier {m.currentPalier}</a
				>
			{/if}
		{/if}
	{/snippet}
</PageHeader>

<div class="content">
	<div class="main-column">
		<section class="hero">
			<div class="stripes"></div>
			<div class="hero-body">
				<div class="hero-top">
					<div>
						<div class="caption">Capital courant</div>
						<div class="capital display">{amount(m.capital)}</div>
					</div>
					<span class="palier-pill mobile-only">
						{active
							? `Palier ${m.currentPalier}${m.totalPaliers ? ` / ${m.totalPaliers}` : ''}`
							: 'Terminée'}
					</span>
					{#if m.target !== null}
						<div class="target desktop-only">
							<div class="caption">Objectif</div>
							<div class="display">{amount(m.target)}</div>
						</div>
					{/if}
				</div>
				<div class="pills">
					<span class="pill strong"
						>{amount(gainSinceStart, { signed: true })} depuis le départ</span
					>
					<span class="pill mobile-only"
						>{m.excludeStake ? 'Mise hors bankroll' : 'Mise dans la bankroll'}</span
					>
					{#if m.secured > 0}<span class="pill desktop-only">{amount(m.secured)} sécurisés</span
						>{/if}
				</div>
				{#if m.progress !== null}
					<div class="progress">
						<Meter value={m.progress} color="var(--lemon)" track="rgba(255,255,255,0.28)" />
					</div>
					<div class="range mobile-only">
						<span>Départ {amount(m.startCapital)}</span>
						<span>Objectif {amount(m.target ?? 0)}</span>
					</div>
				{/if}
			</div>
		</section>

		{#if !active}
			<Alert tone={m.status === 'BROKEN' ? 'loss' : 'warn'}>{closureText()}</Alert>
		{/if}

		<!-- Mobile timeline (2d) -->
		<div class="timeline mobile-only">
			{#each m.paliers as palier (palier.number)}
				<div class="step" class:dim={palier.status === 'LOST'}>
					<span class="dot {palier.status.toLowerCase()}">{palier.number}</span>
					<div class="step-text">
						<div class="step-label">{palier.label}</div>
						<div class="step-meta">
							{amount(palier.stake)} · cote {odds(palier.odds)} · {shortDate(
								palier.startsAt
							)}{palier.isRelance ? ' · relance' : ''}
						</div>
					</div>
					<div
						class="step-result num"
						class:negative={palier.status === 'LOST'}
						class:positive={palier.status !== 'LOST'}
					>
						{palier.status === 'LOST' && palier.capitalAfter === 0
							? 'perdu'
							: amount(palier.capitalAfter)}
					</div>
				</div>
			{/each}

			{#if next}
				<div class="current">
					<div class="current-top">
						<span class="dot current-dot">{next.number}</span>
						<div class="step-text">
							{#if openBet}
								<div class="step-label">{openBet.label}</div>
								<div class="step-meta strong">
									{amount(next.stake)} engagés · cote {odds(openBet.odds)} · {kickoff(
										openBet.startsAt
									)}
								</div>
							{:else}
								<div class="step-label">
									Palier {next.number} · à jouer{next.isRelance ? ' (relance)' : ''}
								</div>
								<div class="step-meta strong">
									{amount(next.stake)} à engager{next.requiredOdds
										? ` · cote ≥ ${odds(next.requiredOdds)}`
										: ''}
								</div>
							{/if}
						</div>
					</div>
					<div class="scenarios">
						{#if openBet && next.capitalIfWon !== null}
							<span>Si ça passe : <b>{amount(next.capitalIfWon)}</b></span>
							<span class="negative">
								{next.ifLost.relance
									? `Si ça casse : relance à ${amount(next.ifLost.capitalAfter)}`
									: `Si ça casse : −${amount(next.ifLost.lostAmount)}`}
							</span>
						{:else}
							<a class="btn small" href="{resolve('/paris/nouveau')}?montante={m.id}"
								>Choisir le pari</a
							>
						{/if}
					</div>
				</div>
			{/if}

			{#if upcoming}
				<div class="step upcoming">
					<span class="dot todo">{upcoming.number}</span>
					<div class="step-text">
						<div class="step-label">Palier à venir</div>
						<div class="step-meta">
							{upcoming.stake !== null
								? `${amount(upcoming.stake)} prévus`
								: 'Capital du palier précédent'}{next?.requiredOdds
								? ` · cote ≥ ${odds(next.requiredOdds)} pour rester dans l'objectif`
								: ''}
						</div>
					</div>
				</div>
			{/if}
		</div>

		<!-- Desktop table (3c) -->
		<section class="card table desktop-only">
			<div class="tr head">
				<span>Palier</span><span>Pari</span><span>Mise</span><span>Cote</span><span>Sécurisé</span>
				<span class="right">Capital</span>
			</div>
			{#each m.paliers as palier (palier.number)}
				<div class="tr">
					<span><span class="dot small {palier.status.toLowerCase()}">{palier.number}</span></span>
					<span>
						<b>{palier.label}</b>
						<span class="sub"
							>{shortDate(palier.startsAt)} · {palier.bookmaker}{palier.isRelance
								? ' · relance'
								: ''}</span
						>
					</span>
					<span>{amount(palier.stake)}</span>
					<span class="num">{odds(palier.odds)}</span>
					<span class="muted">{palier.secured > 0 ? amount(palier.secured) : '—'}</span>
					<span
						class="right strong"
						class:positive={palier.status !== 'LOST'}
						class:negative={palier.status === 'LOST'}
					>
						{amount(palier.capitalAfter)}
					</span>
				</div>
			{/each}
			{#if next}
				<div class="tr current-row">
					<span><span class="dot small current-dot">{next.number}</span></span>
					<span>
						<b>{openBet ? openBet.label : 'Pari à choisir'}</b>
						<span class="sub">
							{openBet
								? `${kickoff(openBet.startsAt)} · ${openBet.competition} · ${openBet.bookmaker}`
								: 'Aucun pari rattaché'}
						</span>
					</span>
					<span class="strong">{amount(next.stake)}</span>
					<span class="num">{openBet ? odds(openBet.odds) : '—'}</span>
					<span class="soft"
						>{next.securedIfWon !== null ? `${amount(next.securedIfWon)} prévus` : '—'}</span
					>
					<span class="right strong"
						>{next.capitalIfWon !== null ? amount(next.capitalIfWon) : '—'}</span
					>
				</div>
			{/if}
			{#if upcoming}
				<div class="tr upcoming-row">
					<span><span class="dot small todo">{upcoming.number}</span></span>
					<span>
						<b>Palier à venir</b>
						<span class="sub">
							{next?.requiredOdds
								? `cote ≥ ${odds(next.requiredOdds)} pour rester dans l'objectif`
								: 'à définir'}
						</span>
					</span>
					<span>{upcoming.stake !== null ? amount(upcoming.stake) : '—'}</span><span>—</span><span
						>—</span
					>
					<span class="right">—</span>
				</div>
			{/if}
		</section>

		{#if active && m.relancesAllowed > 0}
			<div class="mobile-only"><Alert>{relanceText()}</Alert></div>
		{/if}
	</div>

	<aside class="side desktop-only">
		<section class="card settings">
			<h2 class="card-title small">Réglages de la montante</h2>
			<div class="setting">
				<Toggle label="Mise exclue de la bankroll" checked={m.excludeStake} disabled />
				<div>
					<div class="setting-title">
						{m.excludeStake ? 'Mise exclue de la bankroll' : 'Mise dans la bankroll'}
					</div>
					<div class="setting-text">
						{#if m.excludeStake}
							{amount(m.engaged)} sortis de la bankroll depuis le {shortDate(m.createdAt)} — le solde
							final y sera reporté à la clôture.
						{:else}
							Chaque palier fait varier la {bankrollLabel} en direct.
						{/if}
					</div>
				</div>
			</div>
			<div class="setting">
				<Toggle label="Gains sécurisés" checked={m.securePct > 0} disabled />
				<div>
					<div class="setting-title">{m.securePct} % des gains sécurisés</div>
					<div class="setting-text">
						{amount(m.secured)} déjà mis de côté sur la {bankrollLabel}.
					</div>
				</div>
			</div>
			<div class="line">
				<span>Relances autorisées</span>
				<b>{m.relancesUsed} sur {m.relancesAllowed} utilisée{m.relancesUsed > 1 ? 's' : ''}</b>
			</div>
		</section>

		{#if next}
			<section class="card scenarios-card">
				<h2 class="card-title small">Palier {next.number} · scénarios</h2>
				<div class="scenario won">
					<span>Gagné · capital</span>
					<b class="num">{next.capitalIfWon !== null ? amount(next.capitalIfWon) : '—'}</b>
				</div>
				<div class="scenario lost">
					<span>{next.ifLost.relance ? 'Perdu · relance' : 'Perdu · reste sécurisé'}</span>
					<b class="num"
						>{amount(next.ifLost.relance ? next.ifLost.capitalAfter : next.ifLost.securedKept)}</b
					>
				</div>
				<p class="setting-text">
					{#if next.ifLost.relance}
						Une perte relance la montante à {amount(m.startCapital)}, réengagés depuis la {bankrollLabel}.
					{:else}
						Une perte clôture la montante : les {amount(next.ifLost.securedKept)} sécurisés restent acquis,
						les
						{amount(m.engaged)} engagés sont perdus.
					{/if}
				</p>
			</section>
		{/if}

		{#if active}
			<section class="guard">
				<h2 class="card-title small">Garde-fou</h2>
				<p>
					{#if m.successProbability !== null && m.target !== null}
						Probabilité estimée d'atteindre {amount(m.target)} : {percent(m.successProbability, {
							digits: 0
						})}.
					{:else}
						Mode libre : fixe-toi un seuil de sortie avant de relancer.
					{/if}
					{#if singleMontanteRule}
						Ta règle « une seule montante active » {singleMontanteRule.respected
							? 'est respectée'
							: "n'est pas respectée"}.
					{/if}
				</p>
			</section>
		{/if}
	</aside>

	{#if error}<div class="form-error">{error}</div>{/if}
</div>

{#if active}
	<div class="bottom-bar mobile-only">
		<button type="button" class="btn secondary" onclick={close} disabled={busy || !!openBet}
			>Clôturer</button
		>
		{#if openBet}
			<button type="button" class="btn danger" onclick={() => settle('LOST')} disabled={busy}
				>Perdu</button
			>
			<button type="button" class="btn grow" onclick={() => settle('WON')} disabled={busy}>
				Valider le palier {m.currentPalier}
			</button>
		{:else}
			<a class="btn grow" href="{resolve('/paris/nouveau')}?montante={m.id}"
				>Choisir le pari du palier {m.currentPalier}</a
			>
		{/if}
	</div>
{/if}

<style>
	.content {
		padding: 22px 24px;
		display: grid;
		grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
		gap: 18px;
		align-items: start;
	}
	.main-column,
	.side {
		display: flex;
		flex-direction: column;
		gap: 16px;
		min-width: 0;
	}
	.side {
		gap: 14px;
	}
	.hero {
		border-radius: 22px;
		padding: 22px;
		background: var(--grass);
		color: #fff;
		position: relative;
		overflow: hidden;
	}
	.stripes {
		position: absolute;
		inset: 0;
		background: repeating-linear-gradient(
			115deg,
			rgba(255, 255, 255, 0.07) 0 34px,
			transparent 34px 68px
		);
	}
	.hero-body {
		position: relative;
	}
	.hero-top {
		display: flex;
		justify-content: space-between;
		align-items: flex-end;
	}
	.caption {
		font-size: 12px;
		font-weight: 600;
		opacity: 0.9;
	}
	.capital {
		font-size: 40px;
		letter-spacing: -0.03em;
		margin-top: 4px;
	}
	.target {
		text-align: right;
	}
	.target .display {
		font-size: 26px;
	}
	.palier-pill {
		font-size: 11px;
		font-weight: 700;
		padding: 5px 10px;
		border-radius: 999px;
		background: rgba(255, 255, 255, 0.22);
	}
	.pills {
		display: flex;
		gap: 8px;
		margin-top: 12px;
		flex-wrap: wrap;
	}
	.pill {
		font-size: 12px;
		font-weight: 600;
		padding: 6px 12px;
		border-radius: 999px;
		background: rgba(255, 255, 255, 0.22);
	}
	.pill.strong {
		background: #fff;
		color: var(--grass-strong);
		font-weight: 700;
	}
	.progress {
		margin-top: 18px;
	}
	.range {
		display: flex;
		justify-content: space-between;
		font-size: 11px;
		margin-top: 7px;
		opacity: 0.9;
	}
	.dot {
		width: 26px;
		height: 26px;
		border-radius: 50%;
		background: var(--grass);
		color: #fff;
		display: grid;
		place-items: center;
		font-size: 12px;
		font-weight: 700;
		flex: none;
	}
	.dot.small {
		width: 24px;
		height: 24px;
		font-size: 11px;
	}
	.dot.lost {
		background: var(--loss-dot);
	}
	.dot.void,
	.dot.cashout {
		background: var(--idle-2);
	}
	.current-dot {
		background: var(--lemon);
		border: 2px solid var(--grass);
		color: var(--text);
	}
	.dot.todo {
		background: transparent;
		border: 2px dashed var(--idle);
		color: var(--muted-2);
	}
	.table {
		overflow: hidden;
	}
	.tr {
		display: grid;
		grid-template-columns: 60px minmax(0, 1fr) 84px 70px 92px 100px;
		gap: 12px;
		padding: 13px 20px;
		border-bottom: 1px solid var(--border-soft);
		align-items: center;
		font-size: 13px;
	}
	.tr:last-child {
		border-bottom: none;
	}
	.tr > span:nth-child(2) {
		display: flex;
		flex-direction: column;
		min-width: 0;
	}
	.tr.head {
		background: var(--bg);
		border-bottom: 1px solid var(--border);
		font-size: 11px;
		font-weight: 700;
		letter-spacing: 0.05em;
		text-transform: uppercase;
		color: var(--muted);
	}
	.sub {
		font-size: 11px;
		color: var(--muted);
		margin-top: 2px;
	}
	.right {
		text-align: right;
	}
	.strong {
		font-weight: 700;
	}
	.soft {
		color: var(--text-2);
	}
	.current-row {
		background: var(--grass-soft);
		padding: 15px 20px;
	}
	.current-row .sub {
		color: var(--text-2);
	}
	.upcoming-row {
		color: var(--muted-2);
	}
	.upcoming-row .sub {
		color: inherit;
	}
	.card-title.small {
		font-size: 16px;
	}
	.settings,
	.scenarios-card {
		padding: 18px;
		display: flex;
		flex-direction: column;
		gap: 13px;
	}
	.setting {
		display: flex;
		gap: 11px;
		align-items: flex-start;
	}
	.setting-title {
		font-size: 13px;
		font-weight: 700;
	}
	.setting-text {
		font-size: 11px;
		color: var(--muted);
		margin: 3px 0 0;
		line-height: 1.45;
	}
	.line {
		display: flex;
		justify-content: space-between;
		align-items: center;
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
	.scenario {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 12px 14px;
		border-radius: 14px;
		font-size: 12px;
		font-weight: 600;
	}
	.scenario b {
		font-size: 17px;
	}
	.scenario.won {
		background: var(--grass-soft);
		color: var(--text-3);
	}
	.scenario.won b {
		color: var(--grass-deep);
	}
	.scenario.lost {
		background: var(--loss-panel);
		color: var(--loss-panel-text);
	}
	.scenario.lost b {
		color: oklch(0.5 0.16 30);
	}
	.guard {
		border-radius: 22px;
		padding: 18px;
		background: var(--warn-bg);
		border: 1px solid var(--warn-border);
		color: var(--warn-text);
	}
	.guard p {
		font-size: 12px;
		margin: 8px 0 0;
		line-height: 1.5;
	}
	.guard .card-title {
		color: var(--warn-text);
	}
	.form-error {
		grid-column: 1 / -1;
	}

	@media (max-width: 1023px) {
		.content {
			display: flex;
			flex-direction: column;
			align-items: stretch;
			padding: 0 20px 20px;
			gap: 14px;
		}
		.main-column {
			gap: 14px;
		}
		.hero {
			border-radius: 28px;
		}
		.stripes {
			background: var(--mowing);
		}
		.hero-top {
			align-items: center;
		}
		.capital {
			font-size: 42px;
			margin-top: 6px;
		}
		.hero-top > div:first-child {
			order: 0;
		}
		.timeline {
			display: flex;
			flex-direction: column;
			gap: 10px;
		}
		.step {
			border-radius: 20px;
			padding: 13px 16px;
			background: var(--surface);
			border: 1px solid var(--border);
			display: flex;
			align-items: center;
			gap: 12px;
		}
		.step.dim {
			opacity: 0.8;
		}
		.step-text {
			flex: 1;
			min-width: 0;
		}
		.step-label {
			font-size: 13px;
			font-weight: 700;
		}
		.step-meta {
			font-size: 11px;
			color: var(--muted);
			margin-top: 2px;
		}
		.step-meta.strong {
			color: var(--text-2);
			font-weight: 500;
		}
		.step-result {
			font-size: 14px;
			flex: none;
		}
		.current {
			border-radius: 22px;
			padding: 16px;
			background: var(--grass-soft);
			border: 2px solid var(--grass);
		}
		.current-top {
			display: flex;
			align-items: center;
			gap: 12px;
		}
		.scenarios {
			display: flex;
			justify-content: space-between;
			align-items: center;
			gap: 10px;
			margin-top: 13px;
			padding-top: 12px;
			border-top: 1px solid rgba(27, 36, 23, 0.1);
			font-size: 12px;
			font-weight: 600;
			color: var(--text-2);
		}
		.scenarios b {
			color: var(--text);
		}
		.scenarios .negative {
			font-weight: 700;
		}
		.upcoming {
			border: 1px dashed var(--idle);
			color: var(--muted-2);
		}
		.upcoming .step-meta {
			color: inherit;
		}
		.bottom-bar {
			position: sticky;
			bottom: calc(var(--tabbar-height) + 10px);
			margin: 0 0 -24px;
			padding: 14px 20px 16px;
			background: var(--surface);
			border-top: 1px solid var(--border);
			display: flex;
			gap: 10px;
		}
		.bottom-bar .btn {
			height: 52px;
			border-radius: 16px;
			font-size: 14px;
		}
		.bottom-bar .grow {
			flex: 1;
			font-size: 15px;
		}
	}
</style>
