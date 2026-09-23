import type { Stats, StatsPeriod } from '$lib/api/types';
import { getJson } from '$lib/server/backend';
import type { PageServerLoad } from './$types';

const PERIODS: StatsPeriod[] = ['DAYS_30', 'MONTHS_3', 'ALL'];

export const load: PageServerLoad = async ({ fetch, url }) => {
	const requested = url.searchParams.get('period') as StatsPeriod | null;
	const period = requested && PERIODS.includes(requested) ? requested : 'MONTHS_3';
	const bankrollId = Number(url.searchParams.get('bankroll')) || null;
	const scope = bankrollId ? `&bankrollId=${bankrollId}` : '';
	return {
		period,
		bankrollId,
		stats: await getJson<Stats>(fetch, `/api/stats?period=${period}${scope}`)
	};
};
