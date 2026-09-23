import type { Bet, SportEvent, Stats } from '$lib/api/types';
import { getJson } from '$lib/server/backend';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ fetch }) => {
	const [bets, stats, events] = await Promise.all([
		getJson<Bet[]>(fetch, '/api/bets'),
		getJson<Stats>(fetch, '/api/stats?period=ALL'),
		getJson<SportEvent[]>(fetch, '/api/events')
	]);
	return { bets, stats, events };
};
