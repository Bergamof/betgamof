import { closeSession } from '$lib/server/auth';
import { redirect } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const POST: RequestHandler = ({ cookies }) => {
	closeSession(cookies);
	redirect(303, '/login');
};
