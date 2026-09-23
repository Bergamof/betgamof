import { env } from '$env/dynamic/private';
import { createHmac, timingSafeEqual } from 'node:crypto';
import type { Cookies } from '@sveltejs/kit';

export const SESSION_COOKIE = 'betgamof_session';
const SESSION_MAX_AGE = 60 * 60 * 24 * 30;

function password(): string {
	const value = env.BETGAMOF_APP_PASSWORD;
	if (!value) throw new Error('BETGAMOF_APP_PASSWORD is not set');
	return value;
}

/** Session token derived from the password: changing the password logs every device out. */
function sessionToken(): string {
	return createHmac('sha256', password()).update('betgamof-session-v1').digest('hex');
}

function safeEqual(a: string, b: string): boolean {
	const left = Buffer.from(a);
	const right = Buffer.from(b);
	return left.length === right.length && timingSafeEqual(left, right);
}

export function isValidPassword(candidate: string): boolean {
	return safeEqual(candidate, password());
}

export function isAuthenticated(cookies: Cookies): boolean {
	const token = cookies.get(SESSION_COOKIE);
	return token !== undefined && safeEqual(token, sessionToken());
}

export function openSession(cookies: Cookies, secure: boolean): void {
	cookies.set(SESSION_COOKIE, sessionToken(), {
		path: '/',
		httpOnly: true,
		sameSite: 'lax',
		secure,
		maxAge: SESSION_MAX_AGE
	});
}

export function closeSession(cookies: Cookies): void {
	cookies.delete(SESSION_COOKIE, { path: '/' });
}
