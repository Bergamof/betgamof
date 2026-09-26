import type { Bet } from '$lib/api/types';
import { BET_STATUS_LABEL, BET_TYPE_LABEL } from '$lib/labels';

const HEADERS = [
	'Date',
	'Pari',
	'Sport',
	'Compétition',
	'Type',
	'Bookmaker',
	'Bankroll',
	'Cote',
	'Mise',
	'Statut',
	'Résultat'
];

function cell(value: string | number): string {
	const text = typeof value === 'number' ? value.toString().replace('.', ',') : value;
	return /[";\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

/** Semicolon-separated CSV, the default for French spreadsheet software. */
export function betsToCsv(bets: Bet[]): string {
	const rows = bets.map((bet) => [
		bet.startsAt.slice(0, 10),
		bet.label,
		bet.sport,
		bet.competition,
		BET_TYPE_LABEL[bet.type],
		bet.bookmaker,
		bet.montanteName ?? bet.bankrollName,
		bet.odds,
		bet.stake,
		BET_STATUS_LABEL[bet.status],
		bet.profit
	]);
	return [HEADERS, ...rows].map((row) => row.map(cell).join(';')).join('\n');
}

export function download(filename: string, content: string): void {
	const url = URL.createObjectURL(new Blob(['﻿' + content], { type: 'text/csv;charset=utf-8' }));
	const link = Object.assign(document.createElement('a'), { href: url, download: filename });
	link.click();
	URL.revokeObjectURL(url);
}
