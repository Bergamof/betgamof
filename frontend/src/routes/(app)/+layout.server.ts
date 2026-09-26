import type { Bankroll, Montante } from '$lib/api/types';
import { getJson } from '$lib/server/backend';
import { selectedBankroll } from '$lib/server/preferences';
import type { LayoutServerLoad } from './$types';

export const load: LayoutServerLoad = async ({ fetch, cookies }) => {
	const [bankrolls, montantes] = await Promise.all([
		getJson<Bankroll[]>(fetch, '/api/bankrolls'),
		getJson<Montante[]>(fetch, '/api/montantes')
	]);
	return { bankrolls, montantes, bankroll: selectedBankroll(cookies, bankrolls) };
};
