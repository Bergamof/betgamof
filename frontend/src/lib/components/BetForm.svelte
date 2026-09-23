<script lang="ts">
	import { untrack } from 'svelte';
	import { api, ApiError } from '$lib/api/client';
	import type {
		Bankroll,
		BetRequest,
		BetType,
		Kelly,
		Market,
		MarketCategory,
		Montante,
		SportEvent
	} from '$lib/api/types';
	import { amount, kickoff, money, odds as formatOdds, parseNumber, percent } from '$lib/format';
	import { BET_TYPE_LABEL, BOOKMAKERS, MARKET_CATEGORY_LABEL, SPORTS } from '$lib/labels';
	import {
		combinedOdds,
		earliestStart,
		sameSelection,
		togglePick,
		typeFor,
		type TicketItem
	} from './bet-ticket';
	import ChoiceChips from './ChoiceChips.svelte';
	import Segmented from './Segmented.svelte';
	import Toggle from './Toggle.svelte';

	let {
		bankrolls,
		montantes,
		events,
		defaultBankrollId,
		initialMontanteId = null,
		onsaved,
		compact = false
	}: {
		bankrolls: Bankroll[];
		montantes: Montante[];
		events: SportEvent[];
		defaultBankrollId: number | null;
		initialMontanteId?: number | null;
		onsaved: () => void;
		compact?: boolean;
	} = $props();

	type Source = 'event' | 'scratch';
	type CategoryFilter = 'POPULAR' | MarketCategory;
	const STAKE_SHORTCUTS = [10, 25, 50, 100];
	const CATEGORIES: { value: CategoryFilter; label: string }[] = [
		{ value: 'POPULAR', label: 'Populaires' },
		...(Object.keys(MARKET_CATEGORY_LABEL) as MarketCategory[]).map((value) => ({
			value,
			label: MARKET_CATEGORY_LABEL[value]
		}))
	];

	const openMontantes = $derived(
		montantes.filter((m) => m.status === 'ACTIVE' && m.nextStep && !m.nextStep.openBet)
	);

	// Props only seed the form: later changes of the page data must not reset what the user typed.
	const initial = untrack(() => ({
		eventId: events[0]?.id ?? '',
		bankrollId: defaultBankrollId ?? bankrolls[0]?.id ?? 0,
		montanteId: initialMontanteId
	}));

	let source: Source = $state('event');
	let eventId = $state(initial.eventId);
	let pickingEvent = $state(false);
	let eventQuery = $state('');
	let marketQuery = $state('');
	let category: CategoryFilter = $state('POPULAR');
	let showAllMarkets = $state(false);
	let ticket: TicketItem[] = $state([]);
	let combining = $state(false);
	let requestedType: BetType = $state('SIMPLE');
	let oddsInput = $state('');
	let oddsEdited = $state(false);
	let stakeInput = $state('');
	let bookmaker = $state('Winamax');
	let bankrollId = $state(initial.bankrollId);
	let attach = $state(initial.montanteId !== null);
	let montanteId: number | null = $state(initial.montanteId);
	let kelly: Kelly | null = $state(null);
	let error: string | null = $state(null);
	let saving = $state(false);

	let scratch = $state({
		sport: 'Football',
		competition: '',
		eventName: '',
		market: '',
		pick: '',
		odds: '',
		startsAt: ''
	});

	const event = $derived(events.find((e) => e.id === eventId) ?? null);
	const bankroll = $derived(bankrolls.find((b) => b.id === bankrollId) ?? null);
	const montante = $derived(
		attach ? (openMontantes.find((m) => m.id === montanteId) ?? openMontantes[0] ?? null) : null
	);
	const betType = $derived(typeFor(ticket, requestedType));
	const totalOdds = $derived(parseNumber(oddsInput));
	const stake = $derived(montante ? montante.capital : parseNumber(stakeInput));
	const potentialGain = $derived(stake * totalOdds);
	const filteredEvents = $derived(
		events.filter((e) =>
			`${e.name} ${e.competition} ${e.sport}`.toLowerCase().includes(eventQuery.toLowerCase())
		)
	);
	const matchingMarkets = $derived.by(() => {
		if (!event) return [];
		const query = marketQuery.trim().toLowerCase();
		return event.markets.filter((market) => {
			if (query)
				return `${market.name} ${market.outcomes.map((o) => o.pick).join(' ')}`
					.toLowerCase()
					.includes(query);
			return category === 'POPULAR'
				? market.popular || showAllMarkets
				: market.category === category;
		});
	});
	const hiddenMarkets = $derived(event ? event.markets.length - matchingMarkets.length : 0);

	// Keep the total odds and the bookmaker in sync with the ticket until the user edits them.
	$effect(() => {
		const computed = combinedOdds(ticket);
		if (ticket.length > 0 && !oddsEdited) oddsInput = computed.toFixed(2);
	});
	$effect(() => {
		if (montante) bankrollId = montante.bankrollId;
	});
	$effect(() => {
		if (!stakeInput && bankroll?.fixedStake) stakeInput = String(bankroll.fixedStake);
	});

	// Kelly advice for the current odds, debounced while typing.
	$effect(() => {
		const currentOdds = totalOdds;
		const id = bankrollId;
		kelly = null;
		if (!id || currentOdds <= 1) return;
		const timer = setTimeout(async () => {
			kelly = await api.get<Kelly>(`/bankrolls/${id}/kelly?odds=${currentOdds}`).catch(() => null);
		}, 250);
		return () => clearTimeout(timer);
	});

	function pick(market: Market, outcome: Market['outcomes'][number]) {
		if (!event) return;
		ticket = togglePick(
			ticket,
			{
				selection: {
					eventId: event.id,
					eventName: event.name,
					sport: event.sport,
					competition: event.competition,
					market: market.name,
					pick: outcome.pick,
					odds: outcome.odds
				},
				bookmaker: outcome.bookmaker,
				startsAt: event.startsAt
			},
			combining
		);
		combining = false;
		oddsEdited = false;
		if (ticket.length === 1) bookmaker = ticket[0].bookmaker;
	}

	function isPicked(market: Market, pickName: string): boolean {
		return ticket.some(
			(item) =>
				item.selection.eventName === event?.name &&
				item.selection.market === market.name &&
				item.selection.pick === pickName
		);
	}

	function addScratchSelection() {
		error = null;
		const selectionOdds = parseNumber(scratch.odds);
		if (!scratch.eventName.trim() || !scratch.pick.trim() || selectionOdds <= 1) {
			error = "Renseigne l'événement, le pari et une cote supérieure à 1.";
			return;
		}
		const item: TicketItem = {
			selection: {
				eventId: null,
				eventName: scratch.eventName.trim(),
				sport: scratch.sport,
				competition: scratch.competition.trim(),
				market: scratch.market.trim(),
				pick: scratch.pick.trim(),
				odds: selectionOdds
			},
			bookmaker,
			startsAt: scratch.startsAt
				? new Date(scratch.startsAt).toISOString()
				: new Date().toISOString()
		};
		if (!ticket.some((current) => sameSelection(current.selection, item.selection))) {
			ticket = togglePick(ticket, item, combining);
		}
		combining = false;
		oddsEdited = false;
		scratch = { ...scratch, pick: '', odds: '', market: '' };
	}

	function removeFromTicket(index: number) {
		ticket = ticket.filter((_, i) => i !== index);
		oddsEdited = false;
	}

	function chooseType(type: BetType) {
		requestedType = type;
		if (type !== 'SIMPLE' && ticket.length < 2) combining = true;
		if (type === 'SIMPLE' && ticket.length > 1) ticket = ticket.slice(-1);
	}

	async function save() {
		error = null;
		const startsAt = earliestStart(ticket);
		if (ticket.length === 0 || !startsAt) {
			error = 'Choisis au moins une sélection.';
			return;
		}
		if (!bankrollId) {
			error = "Crée d'abord une bankroll.";
			return;
		}
		const request: BetRequest = {
			bankrollId,
			montanteId: montante?.id ?? null,
			type: betType,
			bookmaker: bookmaker.trim(),
			stake,
			odds: totalOdds,
			startsAt,
			selections: ticket.map((item) => item.selection)
		};
		saving = true;
		try {
			await api.post('/bets', request);
			ticket = [];
			oddsInput = '';
			oddsEdited = false;
			onsaved();
		} catch (cause) {
			error = cause instanceof ApiError ? cause.message : "Impossible d'enregistrer le pari.";
		} finally {
			saving = false;
		}
	}
</script>

<div class="form" class:compact>
	<Segmented
		label="Origine du pari"
		stretch
		options={[
			{ value: 'event', label: 'Depuis un événement' },
			{ value: 'scratch', label: 'De zéro' }
		]}
		bind:value={source}
	/>

	{#if combining}
		<div class="combining">
			Choisis la sélection suivante{source === 'event' ? ' — change d’événement si besoin' : ''}.
			<button type="button" class="link" onclick={() => (combining = false)}>Annuler</button>
		</div>
	{/if}

	{#if source === 'event'}
		{#if events.length === 0}
			<div class="card empty">
				Aucun événement disponible pour le moment : passe par « De zéro ».
			</div>
		{:else if pickingEvent || !event}
			<div class="card picker">
				<input class="filter" placeholder="Rechercher un match…" bind:value={eventQuery} />
				{#each filteredEvents as candidate (candidate.id)}
					<button
						type="button"
						class="event-option"
						class:current={candidate.id === eventId}
						onclick={() => {
							eventId = candidate.id;
							pickingEvent = false;
							showAllMarkets = false;
						}}
					>
						<span class="event-name">{candidate.name}</span>
						<span class="event-meta"
							>{candidate.sport} · {candidate.competition} · {kickoff(candidate.startsAt)}</span
						>
					</button>
				{/each}
			</div>
		{:else}
			<div class="event card-soft">
				<div>
					<div class="event-title">{event.name}</div>
					<div class="event-meta">
						{event.sport} · {event.competition} · {kickoff(event.startsAt)}
					</div>
				</div>
				<button type="button" class="link" onclick={() => (pickingEvent = true)}>Changer</button>
			</div>

			<label class="search">
				<span class="lens"></span>
				<input placeholder="Filtrer les marchés · « buts », « buteur »…" bind:value={marketQuery} />
			</label>

			<div class="categories">
				{#each CATEGORIES as option (option.value)}
					<button
						type="button"
						class:selected={category === option.value}
						onclick={() => (category = option.value)}>{option.label}</button
					>
				{/each}
			</div>

			{#each matchingMarkets as market (market.id)}
				<div class="market card">
					<div class="market-head">
						<span class="market-name">{market.name}</span>
						<span class="market-book">{market.outcomes[0]?.bookmaker}</span>
					</div>
					{#each market.outcomes as outcome (outcome.pick)}
						<button
							type="button"
							class="outcome"
							class:picked={isPicked(market, outcome.pick)}
							onclick={() => pick(market, outcome)}
						>
							<span>{outcome.pick}</span>
							<span class="num">{formatOdds(outcome.odds)}</span>
						</button>
					{/each}
				</div>
			{:else}
				<div class="card empty">Aucun marché ne correspond.</div>
			{/each}
			{#if !marketQuery && category === 'POPULAR' && hiddenMarkets > 0}
				<button type="button" class="link center" onclick={() => (showAllMarkets = true)}>
					Voir les {event.markets.length} marchés de ce match
				</button>
			{/if}
		{/if}
	{:else}
		<div class="card scratch">
			<div class="row">
				<label class="field">
					<span>Sport</span>
					<select bind:value={scratch.sport}>
						{#each SPORTS as sport (sport)}<option>{sport}</option>{/each}
					</select>
				</label>
				<label class="field">
					<span>Compétition</span>
					<input bind:value={scratch.competition} placeholder="Ligue 1" />
				</label>
			</div>
			<label class="field">
				<span>Événement</span>
				<input bind:value={scratch.eventName} placeholder="Lens – Lyon" />
			</label>
			<div class="row">
				<label class="field">
					<span>Marché</span>
					<input bind:value={scratch.market} placeholder="Total de buts" />
				</label>
				<label class="field">
					<span>Pari</span>
					<input bind:value={scratch.pick} placeholder="Plus de 1,5" />
				</label>
			</div>
			<div class="row">
				<label class="field">
					<span>Cote</span>
					<input inputmode="decimal" bind:value={scratch.odds} placeholder="1.72" />
				</label>
				<label class="field">
					<span>Début</span>
					<input type="datetime-local" bind:value={scratch.startsAt} />
				</label>
			</div>
			<button type="button" class="btn secondary" onclick={addScratchSelection}>
				Ajouter au ticket
			</button>
		</div>
	{/if}

	<section class="ticket">
		<div class="ticket-head">
			<span class="ticket-title"
				>Ticket · {ticket.length} sélection{ticket.length > 1 ? 's' : ''}</span
			>
			{#if ticket.length > 0}
				<span class="ticket-odds">Cote totale {formatOdds(combinedOdds(ticket))}</span>
			{/if}
		</div>
		{#each ticket as item, index (index)}
			<div class="selection">
				<div class="selection-text">
					<div class="selection-name">{item.selection.eventName} · {item.selection.pick}</div>
					<div class="selection-meta">
						{item.selection.market || item.selection.sport} · cote {formatOdds(item.selection.odds)}
					</div>
				</div>
				<button
					type="button"
					class="remove"
					aria-label="Retirer la sélection"
					onclick={() => removeFromTicket(index)}>×</button
				>
			</div>
		{:else}
			<div class="selection placeholder">Touche une cote pour l'ajouter au ticket.</div>
		{/each}
		{#if ticket.length > 0}
			<button type="button" class="add-selection" onclick={() => (combining = true)}>
				+ Ajouter une sélection (combiné)
			</button>
		{/if}

		<div class="types">
			{#each ['SIMPLE', 'COMBINE', 'SYSTEME'] as const as type (type)}
				<button type="button" class:selected={betType === type} onclick={() => chooseType(type)}
					>{BET_TYPE_LABEL[type]}</button
				>
			{/each}
		</div>

		<div class="figures">
			<label class="figure">
				<span>Mise</span>
				<input
					class="num"
					inputmode="decimal"
					value={montante ? String(montante.capital) : stakeInput}
					oninput={(e) => (stakeInput = e.currentTarget.value)}
					disabled={montante !== null}
					aria-label="Mise en euros"
				/>
			</label>
			<label class="figure">
				<span>Cote</span>
				<input
					class="num"
					inputmode="decimal"
					bind:value={oddsInput}
					oninput={() => (oddsEdited = true)}
					aria-label="Cote totale"
				/>
			</label>
			<div class="figure">
				<span>Gain</span>
				<output class="num positive">{amount(potentialGain)}</output>
			</div>
		</div>

		{#if !montante}
			<ChoiceChips
				label="Mises rapides"
				options={STAKE_SHORTCUTS.map((value) => ({ value, label: `${value} €` }))}
				value={STAKE_SHORTCUTS.includes(stake) ? stake : null}
				onselect={(value) => (stakeInput = String(value))}
			/>
		{/if}

		<div class="row">
			<label class="field">
				<span>Bookmaker</span>
				<input list="bookmakers" bind:value={bookmaker} />
				<datalist id="bookmakers">
					{#each BOOKMAKERS as name (name)}<option value={name}></option>{/each}
				</datalist>
			</label>
			<label class="field">
				<span>Bankroll</span>
				<select bind:value={bankrollId} disabled={montante !== null}>
					{#each bankrolls as option (option.id)}
						<option value={option.id}>{option.name} · {money(option.balance)}</option>
					{/each}
				</select>
			</label>
		</div>

		{#if !montante}
			<div class="kelly">
				<span class="dot"></span>
				<span class="kelly-text">
					{#if kelly && kelly.stake > 0}
						Kelly conseille {amount(kelly.stake)} ({percent(kelly.bankrollShare)} de la bankroll)
					{:else if kelly}
						Kelly : pas d'avantage estimé à cette cote
					{:else}
						Kelly : saisis une cote
					{/if}
				</span>
				{#if kelly && kelly.stake > 0}
					<button type="button" class="follow" onclick={() => (stakeInput = String(kelly?.stake))}>
						Suivre · {amount(kelly.stake)}
					</button>
				{/if}
			</div>
		{/if}

		<div class="attach">
			<Toggle
				label="Rattacher ce pari à une montante"
				bind:checked={attach}
				disabled={openMontantes.length === 0}
			/>
			<div class="attach-text">
				<span>Rattacher ce pari à une montante</span>
				{#if openMontantes.length === 0}
					<span class="hint">Aucune montante n'attend de palier.</span>
				{:else if attach}
					<select
						value={montante?.id}
						onchange={(e) => (montanteId = Number(e.currentTarget.value))}
						aria-label="Montante"
					>
						{#each openMontantes as option (option.id)}
							<option value={option.id}
								>{option.name} · palier {option.currentPalier} · {amount(option.capital)}</option
							>
						{/each}
					</select>
				{/if}
			</div>
		</div>
	</section>

	{#if error}<div class="form-error">{error}</div>{/if}

	<button type="button" class="btn submit" onclick={save} disabled={saving || ticket.length === 0}>
		{saving ? 'Enregistrement…' : 'Enregistrer le pari'}
	</button>
</div>

<style>
	.form {
		display: flex;
		flex-direction: column;
		gap: 13px;
	}
	.combining {
		font-size: 12px;
		font-weight: 600;
		color: var(--lemon-text);
		background: var(--lemon-soft);
		border-radius: 14px;
		padding: 11px 14px;
		display: flex;
		justify-content: space-between;
		gap: 10px;
	}
	.empty {
		padding: 16px;
		font-size: 13px;
		color: var(--muted);
	}
	.picker {
		padding: 12px;
		display: flex;
		flex-direction: column;
		gap: 6px;
		max-height: 420px;
		overflow: auto;
	}
	.filter {
		border: 1px solid var(--border);
		border-radius: 12px;
		padding: 10px 12px;
		background: var(--bg);
		outline: none;
		font-size: 13px;
	}
	.event-option {
		display: flex;
		flex-direction: column;
		gap: 2px;
		text-align: left;
		padding: 10px 12px;
		border-radius: 12px;
		border: 1px solid transparent;
		background: none;
	}
	.event-option:hover,
	.event-option.current {
		background: var(--grass-soft);
		border-color: var(--grass-soft-border);
	}
	.event-name {
		font-size: 14px;
		font-weight: 700;
	}
	.event-meta {
		font-size: 11px;
		color: var(--text-2);
		margin-top: 3px;
	}
	.event {
		border-radius: 20px;
		padding: 14px 16px;
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
	.event-title {
		font-size: 15px;
		font-weight: 700;
	}
	.search {
		display: flex;
		align-items: center;
		gap: 10px;
		padding: 12px 16px;
		border-radius: 16px;
		background: var(--surface);
		border: 1px solid var(--border);
	}
	.search input {
		border: none;
		outline: none;
		flex: 1;
		min-width: 0;
		font-size: 13px;
		background: transparent;
	}
	.lens {
		width: 14px;
		height: 14px;
		border-radius: 50%;
		border: 2px solid var(--idle-2);
		flex: none;
	}
	.categories {
		display: flex;
		gap: 7px;
		flex-wrap: wrap;
	}
	.categories button {
		font-size: 11px;
		font-weight: 600;
		padding: 7px 12px;
		border-radius: 999px;
		background: var(--surface);
		border: 1px solid var(--border);
		color: var(--text-2);
	}
	.categories button.selected {
		background: var(--grass);
		border-color: var(--grass);
		color: #fff;
		font-weight: 700;
	}
	.market {
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 9px;
	}
	.market-head {
		display: flex;
		justify-content: space-between;
		align-items: baseline;
	}
	.market-name {
		font-family: var(--font-display);
		font-size: 15px;
		font-weight: 700;
	}
	.market-book {
		font-size: 11px;
		color: var(--muted);
	}
	.outcome {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 10px 14px;
		border-radius: 13px;
		background: var(--bg);
		border: 1px solid var(--border);
		font-size: 13px;
		font-weight: 600;
		text-align: left;
	}
	.outcome .num {
		font-size: 15px;
	}
	.outcome.picked {
		background: var(--grass);
		border-color: var(--grass);
		color: #fff;
	}
	.center {
		align-self: center;
		padding: 2px 0 4px;
	}
	.scratch {
		padding: 14px;
		display: flex;
		flex-direction: column;
		gap: 10px;
	}
	.scratch .field {
		background: var(--bg);
	}
	.row {
		display: flex;
		gap: 10px;
	}
	.row > * {
		flex: 1;
		min-width: 0;
	}
	.ticket {
		border-radius: 24px;
		padding: 18px;
		background: var(--grass-soft);
		border: 2px solid var(--grass);
		display: flex;
		flex-direction: column;
		gap: 13px;
	}
	.ticket-head {
		display: flex;
		justify-content: space-between;
		align-items: baseline;
	}
	.ticket-title {
		font-family: var(--font-display);
		font-size: 16px;
		font-weight: 700;
	}
	.ticket-odds {
		font-size: 11px;
		font-weight: 600;
		color: var(--text-2);
	}
	.selection {
		display: flex;
		justify-content: space-between;
		align-items: center;
		gap: 10px;
		background: var(--surface);
		border-radius: 14px;
		padding: 12px 14px;
		border: 1px solid var(--border);
	}
	.selection.placeholder {
		font-size: 12px;
		color: var(--muted);
	}
	.selection-text {
		min-width: 0;
	}
	.selection-name {
		font-size: 13px;
		font-weight: 700;
	}
	.selection-meta {
		font-size: 11px;
		color: var(--muted);
		margin-top: 2px;
	}
	.remove {
		flex: none;
		width: 26px;
		height: 26px;
		border-radius: 8px;
		background: var(--bg);
		border: 1px solid var(--border);
		font-size: 13px;
		font-weight: 700;
		color: var(--muted);
	}
	.add-selection {
		padding: 11px;
		border-radius: 14px;
		background: var(--surface);
		border: 1px dashed var(--grass);
		font-size: 12px;
		font-weight: 700;
		color: var(--grass-strong);
	}
	.types {
		display: flex;
		gap: 7px;
	}
	.types button {
		flex: 1;
		font-size: 12px;
		font-weight: 600;
		padding: 9px 0;
		border-radius: 999px;
		background: var(--surface);
		border: 1px solid var(--border);
		color: var(--text-2);
	}
	.types button.selected {
		background: var(--grass);
		border-color: var(--grass);
		color: #fff;
		font-weight: 700;
	}
	.figures {
		display: flex;
		gap: 10px;
	}
	.figure {
		flex: 1;
		min-width: 0;
		background: var(--surface);
		border-radius: 14px;
		padding: 11px 13px;
		border: 1px solid var(--border);
		display: flex;
		flex-direction: column;
	}
	.figure > span {
		font-size: 10px;
		color: var(--muted);
	}
	.figure input,
	.figure output {
		border: none;
		outline: none;
		background: transparent;
		padding: 0;
		width: 100%;
		font-size: 19px;
	}
	.figure:focus-within {
		border-color: var(--grass);
	}
	.kelly {
		display: flex;
		gap: 10px;
		align-items: center;
		background: var(--lemon-soft);
		border-radius: 14px;
		padding: 11px 14px;
		font-size: 12px;
		font-weight: 600;
		color: var(--lemon-text);
	}
	.kelly-text {
		flex: 1;
	}
	.kelly .dot {
		width: 8px;
		height: 8px;
		border-radius: 50%;
		background: var(--lemon-dot);
		flex: none;
	}
	.follow {
		flex: none;
		border: none;
		border-radius: 999px;
		padding: 7px 12px;
		background: var(--lemon-text);
		color: var(--lemon-soft);
		font-size: 11px;
		font-weight: 700;
	}
	.attach {
		display: flex;
		gap: 10px;
		align-items: center;
		padding: 0 2px;
	}
	.attach-text {
		display: flex;
		flex-direction: column;
		gap: 4px;
		font-size: 12px;
		font-weight: 600;
		color: var(--text-2);
		min-width: 0;
	}
	.attach-text select {
		border: 1px solid var(--border);
		border-radius: 10px;
		padding: 6px 8px;
		background: var(--surface);
		font-weight: 600;
		max-width: 100%;
	}
	.hint {
		font-size: 11px;
		font-weight: 500;
		color: var(--muted);
	}
	.submit {
		height: 52px;
		border-radius: 16px;
		font-size: 15px;
	}
	.compact .ticket {
		border-width: 1px;
		border-color: var(--border);
		background: var(--bg);
		border-radius: 16px;
		padding: 15px;
	}
</style>
