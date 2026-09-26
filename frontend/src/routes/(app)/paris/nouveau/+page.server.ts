import type { SportEvent } from '$lib/api/types';
import { getJson } from '$lib/server/backend';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ fetch, url }) => {
	const montante = Number(url.searchParams.get('montante'));
	return {
		events: await getJson<SportEvent[]>(fetch, '/api/events'),
		montanteId: Number.isInteger(montante) && montante > 0 ? montante : null
	};
};
