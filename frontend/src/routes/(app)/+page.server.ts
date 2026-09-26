import type { Bet, Rule, Stats } from '$lib/api/types';
import { getJson } from '$lib/server/backend';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ fetch, parent }) => {
	const { bankroll } = await parent();
	const scope = bankroll ? `bankrollId=${bankroll.id}` : '';
	const [stats, recent, open, rules] = await Promise.all([
		getJson<Stats>(fetch, `/api/stats?period=ALL&${scope}`),
		getJson<Bet[]>(fetch, '/api/bets?lastDays=1'),
		getJson<Bet[]>(fetch, '/api/bets?status=OPEN'),
		getJson<Rule[]>(fetch, '/api/rules')
	]);
	const byId = new Map([...open, ...recent].map((bet) => [bet.id, bet]));
	return { stats, rules, todayBets: [...byId.values()] };
};
