import type { Bankroll } from '$lib/api/types';
import type { Cookies } from '@sveltejs/kit';

/** Cookie written by the bankroll picker (client side, not sensitive). */
export const BANKROLL_COOKIE = 'betgamof_bankroll';

export function selectedBankroll(cookies: Cookies, bankrolls: Bankroll[]): Bankroll | null {
	const id = Number(cookies.get(BANKROLL_COOKIE));
	return bankrolls.find((bankroll) => bankroll.id === id) ?? bankrolls[0] ?? null;
}
