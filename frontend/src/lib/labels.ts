import type {
	BankrollColor,
	BetStatus,
	BetType,
	MarketCategory,
	Montante,
	MontanteMode,
	MontanteStatus,
	Rule,
	RuleKind
} from '$lib/api/types';

export const OWNER = { name: 'Bergamof', initials: 'BG' };

export const BET_TYPE_LABEL: Record<BetType, string> = {
	SIMPLE: 'Simple',
	COMBINE: 'Combiné',
	SYSTEME: 'Système'
};

export const BET_STATUS_LABEL: Record<BetStatus, string> = {
	OPEN: 'En cours',
	WON: 'Gagné',
	LOST: 'Perdu',
	VOID: 'Remboursé',
	CASHOUT: 'Cash-out'
};

export const MONTANTE_STATUS_LABEL: Record<MontanteStatus, string> = {
	ACTIVE: 'En cours',
	SUCCEEDED: 'Objectif atteint',
	BROKEN: 'Cassée',
	CLOSED: 'Clôturée'
};

export const MONTANTE_MODE_LABEL: Record<MontanteMode, string> = {
	OBJECTIVE: 'Objectif',
	STEPS: 'Paliers',
	FREE: 'Libre'
};

/** "Objectif x3", "8 paliers", "Libre". */
export function montanteModeLabel(
	montante: Pick<Montante, 'mode' | 'targetMultiplier' | 'stepCount'>
): string {
	switch (montante.mode) {
		case 'OBJECTIVE':
			return `Objectif x${String(montante.targetMultiplier ?? '').replace('.', ',')}`;
		case 'STEPS':
			return `${montante.stepCount} paliers`;
		case 'FREE':
			return 'Libre';
	}
}

export const MARKET_CATEGORY_LABEL: Record<MarketCategory, string> = {
	RESULTAT: 'Résultat',
	BUTS: 'Buts',
	HANDICAP: 'Handicap',
	BUTEURS: 'Buteurs',
	MI_TEMPS: 'Mi-temps'
};

export const BANKROLL_COLORS: Record<BankrollColor, string> = {
	GAZON: 'var(--grass)',
	CIEL: 'var(--sky)',
	CITRON: 'var(--lemon)',
	ORANGE: 'var(--orange)',
	BRIQUE: 'var(--loss-dot)'
};

export const BANKROLL_COLOR_LABEL: Record<BankrollColor, string> = {
	GAZON: 'Gazon',
	CIEL: 'Ciel',
	CITRON: 'Citron',
	ORANGE: 'Orange',
	BRIQUE: 'Brique'
};

export const SPORTS = ['Football', 'Tennis', 'Basket', 'Rugby', 'Handball', 'Hockey', 'Autre'];

const SPORT_COLORS: Record<string, string> = {
	Football: 'var(--grass)',
	Tennis: 'var(--sky)',
	Basket: 'var(--orange)',
	Rugby: 'var(--loss-dot)'
};

export function sportColor(sport: string): string {
	return SPORT_COLORS[sport] ?? 'var(--idle-2)';
}

export const BOOKMAKERS = ['Winamax', 'Betclic', 'Unibet', 'PMU', 'Parions Sport', 'Bwin'];

export const RULE_KINDS: {
	kind: RuleKind;
	label: string;
	unit: string | null;
	defaultParam: number;
}[] = [
	{
		kind: 'MAX_STAKE_PCT',
		label: 'Mise maximale par pari',
		unit: '% de la bankroll',
		defaultParam: 3
	},
	{
		kind: 'PAUSE_AFTER_LOSSES',
		label: 'Pause après une série de pertes',
		unit: 'pertes',
		defaultParam: 2
	},
	{
		kind: 'SINGLE_ACTIVE_MONTANTE',
		label: 'Une seule montante active',
		unit: null,
		defaultParam: 0
	}
];

export function ruleLabel(rule: Pick<Rule, 'kind' | 'param'>): string {
	switch (rule.kind) {
		case 'MAX_STAKE_PCT':
			return `Jamais plus de ${rule.param} % de la bankroll sur un pari`;
		case 'PAUSE_AFTER_LOSSES':
			return `Pas de pari après ${rule.param} pertes d'affilée`;
		case 'SINGLE_ACTIVE_MONTANTE':
			return 'Une seule montante active à la fois';
	}
}
