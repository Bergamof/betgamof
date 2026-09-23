import type { Segment, Stats } from '$lib/api/types';
import { amount, percent } from '$lib/format';

const MIN_SAMPLE = 5;

/** "Le rugby coûte 61 € sur 5 paris — 4 % de tes mises pour 23 % de tes pertes." */
export function costliestSport(stats: Stats): string | null {
	const losing = stats.bySport.filter((segment) => segment.profit < 0);
	const worst = losing.at(-1);
	if (!worst) return null;
	const totalLosses = losing.reduce((sum, segment) => sum - segment.profit, 0);
	const stakeShare = stats.staked > 0 ? worst.staked / stats.staked : 0;
	return (
		`${capitalizeArticle(worst.label)} coûte ${amount(-worst.profit)} sur ${worst.count} paris — ` +
		`${percent(stakeShare, { digits: 0 })} de tes mises pour ${percent(-worst.profit / totalLosses, { digits: 0 })} de tes pertes.`
	);
}

function capitalizeArticle(sport: string): string {
	return `Le ${sport.toLowerCase()}`;
}

/** Behavioural hint for the "À surveiller" card. */
export function watchOut(stats: Stats): string {
	const late = stats.lateNight;
	const dayBefore = stats.placedDayBefore;
	if (late.count >= MIN_SAMPLE && dayBefore.count >= MIN_SAMPLE && late.yield < dayBefore.yield) {
		return (
			`Tes paris posés après 22h : ${percent(late.yield, { signed: true, digits: 0 })} de yield sur ${late.count} paris. ` +
			`Ceux posés la veille : ${percent(dayBefore.yield, { signed: true, digits: 0 })}.`
		);
	}
	const weakRange = stats.byOddsRange
		.filter((segment) => segment.count >= MIN_SAMPLE && segment.profit < 0)
		.sort((a, b) => a.profit - b.profit)[0];
	if (weakRange) {
		return `Tes cotes ${weakRange.label} te coûtent ${amount(-weakRange.profit)} sur ${weakRange.count} paris : resserre ta sélection sur cette tranche.`;
	}
	return 'Rien d’alarmant sur la période : continue de noter chaque pari pour affiner ces lectures.';
}

/** Width of a bar relative to the largest absolute profit. */
export function barWidth(segment: Segment, all: Segment[]): number {
	const max = Math.max(...all.map((s) => Math.abs(s.profit)), 1);
	return Math.max(0.04, Math.abs(segment.profit) / max);
}

/** Green shades from darkest (best) to lightest, brick red for losses. */
export function barColor(segment: Segment, rank: number): string {
	if (segment.profit < 0) return 'var(--loss-dot)';
	const shades = ['oklch(0.55 0.14 145)', 'oklch(0.62 0.13 145)', 'oklch(0.7 0.11 145)'];
	return shades[Math.min(rank, shades.length - 1)];
}

/** Month labels spanning the curve: "juin", "juillet", "août", "septembre". */
export function monthLabels(points: Stats['profitCurve'], short = false): string[] {
	const format = new Intl.DateTimeFormat('fr-FR', {
		month: short ? 'short' : 'long',
		timeZone: 'Europe/Paris'
	});
	const labels = points.map((point) => format.format(new Date(point.at)));
	return [...new Set(labels)];
}
