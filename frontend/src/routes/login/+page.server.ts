import { isValidPassword, openSession } from '$lib/server/auth';
import { fail, redirect } from '@sveltejs/kit';
import type { Actions, PageServerLoad } from './$types';

function safeRedirect(target: string | null): string {
	return target && target.startsWith('/') && !target.startsWith('//') ? target : '/';
}

export const load: PageServerLoad = ({ locals, url }) => {
	if (locals.authenticated) redirect(303, safeRedirect(url.searchParams.get('redirect')));
};

export const actions: Actions = {
	default: async ({ request, cookies, url }) => {
		const data = await request.formData();
		const password = String(data.get('password') ?? '');
		if (!isValidPassword(password)) return fail(401, { invalid: true });
		openSession(cookies, url.protocol === 'https:');
		redirect(303, safeRedirect(url.searchParams.get('redirect')));
	}
};
