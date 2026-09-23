import type { BetType, Selection } from '$lib/api/types';

export interface TicketItem {
	selection: Selection;
	bookmaker: string;
	startsAt: string;
}

export function sameSelection(a: Selection, b: Selection): boolean {
	return a.eventName === b.eventName && a.market === b.market && a.pick === b.pick;
}

/** Clicking a pick replaces the ticket, unless the user is building a combiné. */
export function togglePick(ticket: TicketItem[], item: TicketItem, combine: boolean): TicketItem[] {
	if (ticket.some((current) => sameSelection(current.selection, item.selection))) {
		return ticket.filter((current) => !sameSelection(current.selection, item.selection));
	}
	return combine ? [...ticket, item] : [item];
}

/** Total odds of a combiné: product of the selections, rounded like bookmakers do. */
export function combinedOdds(ticket: TicketItem[]): number {
	const product = ticket.reduce((total, item) => total * item.selection.odds, 1);
	return Math.round(product * 100) / 100;
}

export function typeFor(ticket: TicketItem[], requested: BetType): BetType {
	if (ticket.length <= 1) return 'SIMPLE';
	return requested === 'SIMPLE' ? 'COMBINE' : requested;
}

export function earliestStart(ticket: TicketItem[]): string | null {
	return ticket.map((item) => item.startsAt).sort()[0] ?? null;
}
