import type { Stake, StakeUnit } from '$lib/api/types';
import { amount, percent } from '$lib/format';

const MAX_PERCENT_DECIMALS = 6;

const round = (value: number, decimals: number) => Number(value.toFixed(decimals));
const roundToCents = (value: number) => round(value, 2);

/** Euros staked: a percentage is taken from the bankroll balance, rounded to the cent. */
export function stakeAmount(stake: Stake, balance: number): number {
	const euros = stake.unit === 'PERCENT' ? (balance * stake.value) / 100 : stake.value;
	return Math.max(0, roundToCents(euros));
}

/** The same stake expressed in the other unit, e.g. 5 € → 5 % of a 100 € bankroll. */
export function convertStake(stake: Stake, to: StakeUnit, balance: number): Stake {
	if (stake.unit === to) return stake;
	if (to === 'EUR') return { unit: to, value: stakeAmount(stake, balance) };
	return { unit: to, value: percentOf(stake.value, balance) };
}

/** Shortest percentage of the balance giving back the same amount: 5 € of 235 € → 2.128 %, not 2.13 % (5.01 €). */
function percentOf(euros: number, balance: number): number {
	if (balance <= 0) return 0;
	const exact = (euros / balance) * 100;
	const target = roundToCents(euros);
	for (let decimals = 0; decimals < MAX_PERCENT_DECIMALS; decimals++) {
		const candidate = round(exact, decimals);
		if (stakeAmount({ unit: 'PERCENT', value: candidate }, balance) === target) return candidate;
	}
	return round(exact, MAX_PERCENT_DECIMALS);
}

/** "5 €" or "2,5 %". */
export function formatStake(stake: Stake): string {
	return stake.unit === 'PERCENT' ? percent(stake.value / 100, { digits: 2 }) : amount(stake.value);
}
