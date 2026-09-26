import type { Stats } from '$lib/api/types';

export type ChartPeriod = '30' | '90' | 'all';

export const CHART_PERIODS: { value: ChartPeriod; label: string }[] = [
	{ value: '30', label: '30 j' },
	{ value: '90', label: '3 mois' },
	{ value: 'all', label: 'Tout' }
];

export function periodStart(period: ChartPeriod, now: Date = new Date()): Date | undefined {
	return period === 'all' ? undefined : new Date(now.getTime() - Number(period) * 86_400_000);
}

export function curveSince(stats: Stats, from: Date | undefined): Stats['profitCurve'] {
	return from ? stats.profitCurve.filter((point) => new Date(point.at) >= from) : stats.profitCurve;
}

/** Profit made since a date, read from the cumulative curve. */
export function profitSince(stats: Stats, from: Date): number {
	const curve = stats.profitCurve;
	const before = curve.filter((point) => new Date(point.at) < from).at(-1)?.cumulativeProfit ?? 0;
	return (curve.at(-1)?.cumulativeProfit ?? 0) - before;
}

export function startOfMonth(now: Date = new Date()): Date {
	return new Date(now.getFullYear(), now.getMonth(), 1);
}

/** "4W" / "2L": current streak, as shown on the dashboard. */
export function streakLabel(stats: Stats): string {
	const streak = stats.currentStreak;
	return streak ? `${streak.length}${streak.won ? 'W' : 'L'}` : '—';
}
