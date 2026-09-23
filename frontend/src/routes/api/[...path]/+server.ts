import { callBackend } from '$lib/server/backend';
import type { RequestHandler } from './$types';

// Browser calls go through this proxy so the API token never leaves the server.
// Access is already restricted to logged-in sessions by hooks.server.ts.
const proxy: RequestHandler = async ({ params, request, url, fetch }) => {
	const hasBody = request.method !== 'GET' && request.method !== 'HEAD';
	const response = await callBackend(fetch, `/api/${params.path}${url.search}`, {
		method: request.method,
		headers: hasBody ? { 'content-type': 'application/json' } : {},
		body: hasBody ? await request.text() : undefined
	});
	return new Response(response.body, {
		status: response.status,
		headers: { 'content-type': response.headers.get('content-type') ?? 'application/json' }
	});
};

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const DELETE = proxy;
