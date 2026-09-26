import type { Rule } from '$lib/api/types';
import { getJson } from '$lib/server/backend';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ fetch }) => ({
	rules: await getJson<Rule[]>(fetch, '/api/rules')
});
