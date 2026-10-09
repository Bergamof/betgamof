import { describe, expect, it } from 'vitest';
import { convertStake, formatStake, quickStakes, stakeAmount } from './stake';

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

	it('scales quick stakes in euros to the balance, as round amounts', () => {
		expect(quickStakes(1209.37)).toEqual([10, 25, 50, 100]);
		expect(quickStakes(235)).toEqual([2.5, 5, 10, 25]);
		expect(quickStakes(100)).toEqual([1, 2, 5, 10]);
		expect(quickStakes(5000)).toEqual([50, 100, 250, 500]);
	});

	it('never suggests less than 1 € nor the same stake twice', () => {
		expect(quickStakes(30)).toEqual([1, 2.5]);
		expect(quickStakes(0)).toEqual([1]);
	});

	it('formats a stake in its unit', () => {
		expect(plain(formatStake({ unit: 'EUR', value: 5 }))).toBe('5 €');
		expect(plain(formatStake({ unit: 'PERCENT', value: 2.5 }))).toBe('2,5 %');
	});
});
