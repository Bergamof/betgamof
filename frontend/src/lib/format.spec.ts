import { describe, expect, it } from 'vitest';
import { amount, dayHeading, money, odds, parseNumber, percent } from './format';

// Intl uses narrow no-break spaces in French; normalise them for readable assertions.
const plain = (text: string) => text.replace(/[\u202f\u00a0]/g, ' ');

describe('format', () => {
	it('formats euros the French way', () => {
		expect(plain(money(1088.5))).toBe('1 088,50 €');
		expect(plain(money(-15, { signed: true }))).toBe('−15,00 €');
		expect(plain(amount(288))).toBe('288 €');
		expect(plain(amount(33.5, { signed: true }))).toBe('+33,50 €');
	});

	it('formats odds and percentages', () => {
		expect(odds(1.7)).toBe('1.70');
		expect(plain(percent(0.184, { signed: true }))).toBe('+18,4 %');
	});

	it('parses decimal input with a comma', () => {
		expect(parseNumber('1,72')).toBe(1.72);
		expect(parseNumber('abc')).toBe(0);
	});

	it('names today and yesterday', () => {
		const now = new Date('2026-09-11T18:00:00Z');
		expect(dayHeading('2026-09-11T19:00:00Z', now)).toBe("Aujourd'hui · 11 sept.");
		expect(dayHeading('2026-09-10T19:00:00Z', now)).toBe('Hier · 10 sept.');
	});
});
