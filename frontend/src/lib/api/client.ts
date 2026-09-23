import { invalidateAll } from '$app/navigation';

export class ApiError extends Error {
	constructor(
		readonly status: number,
		message: string
	) {
		super(message);
	}
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
	const response = await fetch(`/api${path}`, {
		method,
		headers: body === undefined ? {} : { 'content-type': 'application/json' },
		body: body === undefined ? undefined : JSON.stringify(body)
	});
	if (!response.ok) {
		const payload = await response.json().catch(() => ({}));
		throw new ApiError(response.status, payload.message ?? 'Une erreur est survenue');
	}
	return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

/** Browser-side access to the API through the SvelteKit proxy (`/api/...`). */
export const api = {
	get: <T>(path: string) => request<T>('GET', path),
	post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
	put: <T>(path: string, body: unknown) => request<T>('PUT', path, body),
	delete: (path: string) => request<void>('DELETE', path)
};

/** Runs a mutation, refreshes every `load`, and returns the error message if it failed. */
export async function mutate(action: () => Promise<unknown>): Promise<string | null> {
	try {
		await action();
		await invalidateAll();
		return null;
	} catch (cause) {
		return cause instanceof Error ? cause.message : 'Une erreur est survenue';
	}
}
