// Mirrors the backend read models (backend/.../service/Views.kt). Amounts are euros.

export type BankrollColor = 'GAZON' | 'CIEL' | 'CITRON' | 'ORANGE' | 'BRIQUE';
export type BetType = 'SIMPLE' | 'COMBINE' | 'SYSTEME';
export type BetStatus = 'OPEN' | 'WON' | 'LOST' | 'VOID' | 'CASHOUT';
export type MontanteMode = 'OBJECTIVE' | 'STEPS' | 'FREE';
export type MontanteStatus = 'ACTIVE' | 'SUCCEEDED' | 'BROKEN' | 'CLOSED';
export type RuleKind = 'MAX_STAKE_PCT' | 'PAUSE_AFTER_LOSSES' | 'SINGLE_ACTIVE_MONTANTE';
export type StatsPeriod = 'DAYS_30' | 'MONTHS_3' | 'ALL';
export type MarketCategory = 'RESULTAT' | 'BUTS' | 'HANDICAP' | 'BUTEURS' | 'MI_TEMPS';

export interface Bankroll {
	id: number;
	name: string;
	color: BankrollColor;
	initialBalance: number;
	balance: number;
	stopLoss: number | null;
	stopLossMargin: number | null;
	kellyFraction: number;
	fixedStake: number | null;
	outsideMontantes: number;
	openStake: number;
	staked: number;
	profit: number;
	roi: number;
	betCount: number;
	bookmakers: string[];
}

export interface Selection {
	eventId: string | null;
	eventName: string;
	sport: string;
	competition: string;
	market: string;
	pick: string;
	odds: number;
}

export interface Bet {
	id: number;
	bankrollId: number;
	bankrollName: string;
	montanteId: number | null;
	montanteName: string | null;
	palierNumber: number | null;
	type: BetType;
	bookmaker: string;
	stake: number;
	odds: number;
	status: BetStatus;
	cashout: number | null;
	profit: number;
	potentialReturn: number;
	label: string;
	sport: string;
	competition: string;
	market: string;
	selections: Selection[];
	placedAt: string;
	startsAt: string;
	settledAt: string | null;
}

export interface Palier {
	number: number;
	betId: number;
	label: string;
	bookmaker: string;
	startsAt: string;
	stake: number;
	odds: number;
	status: BetStatus;
	secured: number;
	capitalAfter: number;
	isRelance: boolean;
}

export interface NextStep {
	number: number;
	stake: number;
	isRelance: boolean;
	requiredOdds: number | null;
	openBet: Bet | null;
	capitalIfWon: number | null;
	securedIfWon: number | null;
	ifLost: { relance: boolean; capitalAfter: number; securedKept: number; lostAmount: number };
}

export interface Montante {
	id: number;
	name: string;
	bankrollId: number;
	bankrollName: string;
	mode: MontanteMode;
	targetMultiplier: number | null;
	stepCount: number | null;
	targetOdds: number;
	excludeStake: boolean;
	securePct: number;
	relancesAllowed: number;
	relancesUsed: number;
	startCapital: number;
	status: MontanteStatus;
	capital: number;
	engaged: number;
	secured: number;
	result: number;
	target: number | null;
	plannedSteps: number | null;
	currentPalier: number;
	totalPaliers: number | null;
	progress: number | null;
	successProbability: number | null;
	createdAt: string;
	closedAt: string | null;
	paliers: Palier[];
	nextStep: NextStep | null;
}

export interface MontantePlan {
	target: number | null;
	plannedSteps: number | null;
	steps: { number: number; stake: number; secured: number; capitalAfter: number }[];
	successProbability: number;
	bankrollBalance: number;
	bankrollBalanceAfterLaunch: number;
}

export interface MontanteRequest {
	name: string;
	bankrollId: number;
	startCapital: number;
	targetOdds: number;
	mode: MontanteMode;
	targetMultiplier: number | null;
	stepCount: number | null;
	excludeStake: boolean;
	securePct: number;
	relancesAllowed: number;
}

export interface Rule {
	id: number;
	kind: RuleKind;
	param: number;
	respected: boolean;
}

export interface Kelly {
	stake: number;
	bankrollShare: number;
	probability: number;
}

export interface Segment {
	label: string;
	count: number;
	staked: number;
	profit: number;
	yield: number;
}

export interface Stats {
	settledCount: number;
	openCount: number;
	staked: number;
	profit: number;
	yield: number;
	won: number;
	lost: number;
	hitRate: number;
	averageOdds: number;
	breakEvenOdds: number | null;
	averageStake: number;
	averageStakeShare: number;
	bestWinStreak: number;
	currentStreak: { won: boolean; length: number } | null;
	maxDrawdown: number;
	firstBetAt: string | null;
	profitCurve: { at: string; cumulativeProfit: number }[];
	bySport: Segment[];
	byOddsRange: Segment[];
	byMarket: Segment[];
	lateNight: Segment;
	placedDayBefore: Segment;
	montantes: {
		launched: number;
		succeeded: number;
		broken: number;
		active: number;
		closed: number;
		averagePaliersReached: number;
		securedCoverage: number | null;
	};
}

export interface Outcome {
	pick: string;
	odds: number;
	bookmaker: string;
}

export interface Market {
	id: string;
	name: string;
	category: MarketCategory;
	popular: boolean;
	outcomes: Outcome[];
}

export interface SportEvent {
	id: string;
	sport: string;
	competition: string;
	name: string;
	startsAt: string;
	markets: Market[];
}

export interface BetRequest {
	bankrollId: number;
	montanteId: number | null;
	type: BetType;
	bookmaker: string;
	stake: number;
	odds: number;
	startsAt: string;
	selections: Selection[];
}

export interface BankrollRequest {
	name: string;
	color: BankrollColor;
	initialBalance: number;
	stopLoss: number | null;
	kellyFraction: number;
	fixedStake: number | null;
}
