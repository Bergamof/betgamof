// French formatting used across the app: "1 088,50 €", "+33,50 €", "−15 €", cote "1.72".

export const TIME_ZONE = 'Europe/Paris';
const MINUS = '−';

const euros = new Intl.NumberFormat('fr-FR', {
	minimumFractionDigits: 2,
	maximumFractionDigits: 2
});
const wholeEuros = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });
const decimal = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 });

function withSign(text: string, value: number, signed: boolean): string {
	if (value < 0) return MINUS + text;
	return signed && value > 0 ? '+' + text : text;
}

/** Exact amount with cents: "1 088,50 €". */
export function money(value: number, { signed = false } = {}): string {
	return withSign(`${euros.format(Math.abs(value))} €`, value, signed);
}

/** Amount rounded to the euro, unless it has meaningful cents: "288 €", "33,50 €". */
export function amount(value: number, { signed = false } = {}): string {
	const abs = Math.abs(value);
	const hasCents = Math.round(abs * 100) % 100 !== 0 && abs < 100;
	const text = hasCents ? euros.format(abs) : wholeEuros.format(Math.round(abs));
	return withSign(`${text} €`, value, signed);
}

export function odds(value: number): string {
	return value.toFixed(2);
}

/** Ratio to percentage: 0.184 → "18,4 %". */
export function percent(ratio: number, { signed = false, digits = 1 } = {}): string {
	const formatter =
		digits === 1 ? decimal : new Intl.NumberFormat('fr-FR', { maximumFractionDigits: digits });
	return withSign(`${formatter.format(Math.abs(ratio * 100))} %`, ratio, signed);
}

/** Parses user input accepting both "1,72" and "1.72". */
export function parseNumber(input: string | number): number {
	if (typeof input === 'number') return input;
	const value = Number.parseFloat(input.replace(/\s/g, '').replace(',', '.'));
	return Number.isFinite(value) ? value : 0;
}

function parts(date: Date): { day: string; month: string; weekday: string; time: string } {
	const get = (options: Intl.DateTimeFormatOptions) =>
		new Intl.DateTimeFormat('fr-FR', { timeZone: TIME_ZONE, ...options }).format(date);
	return {
		day: get({ day: 'numeric' }),
		month: get({ month: 'short' }),
		weekday: get({ weekday: 'long' }),
		time: get({ hour: '2-digit', minute: '2-digit' })
	};
}

/** Calendar day key in Paris time, e.g. "2026-09-11". */
export function dayKey(iso: string | Date): string {
	return new Intl.DateTimeFormat('en-CA', { timeZone: TIME_ZONE }).format(new Date(iso));
}

export function shortDate(iso: string): string {
	const { day, month } = parts(new Date(iso));
	return `${day} ${month}`;
}

export function time(iso: string): string {
	return parts(new Date(iso)).time;
}

/** "Aujourd'hui · 11 sept.", "Hier · 10 sept.", "Mardi · 9 sept.". */
export function dayHeading(iso: string, now: Date = new Date()): string {
	const key = dayKey(iso);
	const today = dayKey(now);
	const yesterday = dayKey(new Date(now.getTime() - 86_400_000));
	const { weekday } = parts(new Date(iso));
	const label =
		key === today
			? "Aujourd'hui"
			: key === yesterday
				? 'Hier'
				: weekday.charAt(0).toUpperCase() + weekday.slice(1);
	return `${label} · ${shortDate(iso)}`;
}

/** "ce soir 21:00", "demain 17:00", "12 sept. 20:45". */
export function kickoff(iso: string, now: Date = new Date()): string {
	const key = dayKey(iso);
	const hour = time(iso);
	if (key === dayKey(now))
		return `${Number(hour.slice(0, 2)) >= 18 ? 'ce soir' : "aujourd'hui"} ${hour}`;
	if (key === dayKey(new Date(now.getTime() + 86_400_000))) return `demain ${hour}`;
	return `${shortDate(iso)} ${hour}`;
}

/** "Jeudi 11 septembre". */
export function longDate(date: Date): string {
	const text = new Intl.DateTimeFormat('fr-FR', {
		timeZone: TIME_ZONE,
		weekday: 'long',
		day: 'numeric',
		month: 'long'
	}).format(date);
	return text.charAt(0).toUpperCase() + text.slice(1);
}

export function plural(count: number, singular: string, pluralForm = `${singular}s`): string {
	return `${count} ${count > 1 ? pluralForm : singular}`;
}
