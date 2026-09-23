import { describe, expect, it } from 'vitest';
import { combinedOdds, togglePick, typeFor, type TicketItem } from './bet-ticket';

function item(pick: string, odds: number): TicketItem {
	return {
		selection: {
			eventId: 'lens-lyon',
			eventName: 'Lens – Lyon',
			sport: 'Football',
			competition: 'Ligue 1',
			market: 'Total de buts',
			pick,
			odds
		},
		bookmaker: 'Winamax',
		startsAt: '2026-09-11T19:00:00Z'
	};
}

describe('bet ticket', () => {
	it('replaces the pick of a simple bet', () => {
		const ticket = togglePick([item('Plus de 1,5', 1.72)], item('Plus de 2,5', 2.05), false);
		expect(ticket.map((i) => i.selection.pick)).toEqual(['Plus de 2,5']);
	});

	it('adds picks when building a combiné and removes a pick clicked twice', () => {
		let ticket = togglePick([item('Plus de 1,5', 1.72)], item('Oui', 1.66), true);
		expect(ticket).toHaveLength(2);
		ticket = togglePick(ticket, item('Oui', 1.66), false);
		expect(ticket).toHaveLength(1);
	});

	it('multiplies odds and picks the bet type', () => {
		const ticket = [item('A', 1.55), item('B', 1.5), item('C', 1.87)];
		expect(combinedOdds(ticket)).toBe(4.35);
		expect(typeFor(ticket, 'SIMPLE')).toBe('COMBINE');
		expect(typeFor(ticket.slice(0, 1), 'SYSTEME')).toBe('SIMPLE');
	});
});
