import type { Pathname } from '$app/types';

export type NavShape = 'square' | 'circle' | 'square-outline' | 'circle-outline' | 'tile';

export interface NavItem {
	href: Pathname;
	label: string;
	shape: NavShape;
	mobile: boolean;
}

export const NAV_ITEMS: NavItem[] = [
	{ href: '/', label: 'Accueil', shape: 'square', mobile: true },
	{ href: '/paris', label: 'Paris', shape: 'square', mobile: true },
	{ href: '/montantes', label: 'Montantes', shape: 'circle', mobile: true },
	{ href: '/bankrolls', label: 'Bankrolls', shape: 'square-outline', mobile: true },
	{ href: '/statistiques', label: 'Statistiques', shape: 'tile', mobile: false },
	{ href: '/journal', label: 'Journal', shape: 'circle-outline', mobile: false }
];

export function isActive(href: Pathname, pathname: string): boolean {
	return href === '/' ? pathname === '/' : pathname === href || pathname.startsWith(`${href}/`);
}
