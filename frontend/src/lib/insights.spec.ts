import { describe, expect, it } from 'vitest';
import type { Segment, Stats } from '$lib/api/types';
import { costliestSport, watchOut } from './insights';

const segment = (label: string, count: number, staked: number, profit: number): Segment => ({
	label,
	count,
	staked,
	profit,
	yield: staked > 0 ? profit / staked : 0
});

function stats(overrides: Partial<Stats>): Stats {
	return {
		settledCount: 0,
		openCount: 0,
		staked: 1000,
		profit: 0,
		yield: 0,
		won: 0,
		lost: 0,
		hitRate: 0,
		averageOdds: 0,
		breakEvenOdds: null,
		averageStake: 0,
		averageStakeShare: 0,
		bestWinStreak: 0,
		currentStreak: null,
		maxDrawdown: 0,
		firstBetAt: null,
		profitCurve: [],
		bySport: [],
		byOddsRange: [],
		byMarket: [],
		lateNight: segment('late-night', 0, 0, 0),
		placedDayBefore: segment('day-before', 0, 0, 0),
		montantes: {
			launched: 0,
			succeeded: 0,
			broken: 0,
			active: 0,
			closed: 0,
			averagePaliersReached: 0,
			securedCoverage: null
		},
		...overrides
	};
}

const plain = (text: string | null) => text?.replace(/[\u202f\u00a0]/g, ' ');

describe('insights', () => {
	it('names the costliest sport', () => {
		const result = costliestSport(
			stats({ bySport: [segment('Football', 38, 800, 198), segment('Rugby', 5, 40, -61)] })
		);
		expect(plain(result)).toBe(
			'Le rugby coûte 61 € sur 5 paris — 4 % de tes mises pour 100 % de tes pertes.'
		);
	});

	it('flags late-night betting when it underperforms', () => {
		const result = watchOut(
			stats({
				lateNight: segment('late-night', 11, 100, -14),
				placedDayBefore: segment('day-before', 8, 100, 26)
			})
		);
		expect(plain(result)).toContain('après 22h : −14 %');
	});
});
