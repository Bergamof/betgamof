import { isAuthenticated } from '$lib/server/auth';
import { redirect, type Handle } from '@sveltejs/kit';

const PUBLIC_PATHS = ['/login'];

export const handle: Handle = async ({ event, resolve }) => {
	event.locals.authenticated = isAuthenticated(event.cookies);
	const isPublic = PUBLIC_PATHS.some((path) => event.url.pathname.startsWith(path));
	if (!event.locals.authenticated && !isPublic) {
		if (event.url.pathname.startsWith('/api/')) {
			return new Response(JSON.stringify({ message: 'Session expirée' }), {
				status: 401,
				headers: { 'content-type': 'application/json' }
			});
		}
		const target = event.url.pathname + event.url.search;
		redirect(303, `/login?redirect=${encodeURIComponent(target)}`);
	}
	return resolve(event);
};
