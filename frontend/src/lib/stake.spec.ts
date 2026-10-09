import { describe, expect, it } from 'vitest';
import { convertStake, formatStake, stakeAmount } from './stake';

const plain = (text: string) => text.replace(/[\u202f\u00a0]/g, ' ');

describe('stake', () => {
	it('takes a percentage from the bankroll balance', () => {
		expect(stakeAmount({ unit: 'PERCENT', value: 5 }, 100)).toBe(5);
		expect(stakeAmount({ unit: 'PERCENT', value: 2.5 }, 1018.5)).toBe(25.46);
		expect(stakeAmount({ unit: 'EUR', value: 5 }, 100)).toBe(5);
	});

	it('never stakes a share of a negative balance', () => {
		expect(stakeAmount({ unit: 'PERCENT', value: 5 }, -40)).toBe(0);
	});

	it('converts between euros and percentages of the balance', () => {
		expect(convertStake({ unit: 'EUR', value: 5 }, 'PERCENT', 100)).toEqual({
			unit: 'PERCENT',
			value: 5
		});
		expect(convertStake({ unit: 'PERCENT', value: 5 }, 'EUR', 100)).toEqual({
			unit: 'EUR',
			value: 5
		});
		expect(convertStake({ unit: 'EUR', value: 5 }, 'EUR', 100)).toEqual({ unit: 'EUR', value: 5 });
		expect(convertStake({ unit: 'EUR', value: 5 }, 'PERCENT', 235)).toEqual({
			unit: 'PERCENT',
			value: 2.128
		});
		expect(convertStake({ unit: 'EUR', value: 5 }, 'PERCENT', 0)).toEqual({
			unit: 'PERCENT',
			value: 0
		});
	});

	it('formats a stake in its unit', () => {
		expect(plain(formatStake({ unit: 'EUR', value: 5 }))).toBe('5 €');
		expect(plain(formatStake({ unit: 'PERCENT', value: 2.5 }))).toBe('2,5 %');
	});
});
