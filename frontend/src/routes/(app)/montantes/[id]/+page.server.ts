import type { Montante, Rule } from '$lib/api/types';
import { getJson } from '$lib/server/backend';
import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ fetch, params }) => {
	const id = Number(params.id);
	if (!Number.isInteger(id)) error(404, { message: 'Montante introuvable' });
	const [montante, rules] = await Promise.all([
		getJson<Montante>(fetch, `/api/montantes/${id}`),
		getJson<Rule[]>(fetch, '/api/rules')
	]);
	return { montante, rules };
};
