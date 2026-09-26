import { env } from '$env/dynamic/private';
import { error } from '@sveltejs/kit';

/** Base URL of the Kotlin API, reachable only from the SvelteKit server. */
export function backendUrl(): string {
	return env.BETGAMOF_BACKEND_URL ?? 'http://localhost:8080';
}

function apiToken(): string {
	const token = env.BETGAMOF_API_TOKEN;
	if (!token) throw new Error('BETGAMOF_API_TOKEN is not set');
	return token;
}

/** Forwards a request to the backend with the API token. */
export function callBackend(
	fetcher: typeof fetch,
	path: string,
	init: RequestInit = {}
): Promise<Response> {
	const headers = new Headers(init.headers);
	headers.set('Authorization', `Bearer ${apiToken()}`);
	return fetcher(`${backendUrl()}${path}`, { ...init, headers });
}

/** GET a JSON resource for a server `load`, turning backend failures into SvelteKit errors. */
export async function getJson<T>(fetcher: typeof fetch, path: string): Promise<T> {
	let response: Response;
	try {
		response = await callBackend(fetcher, path);
	} catch {
		error(503, { message: "Le serveur Betgamof ne répond pas. Vérifie que l'API est démarrée." });
	}
	if (!response.ok) {
		const body = await response.json().catch(() => ({ message: response.statusText }));
		error(response.status, { message: body.message ?? 'Erreur du serveur' });
	}
	return response.json() as Promise<T>;
}
