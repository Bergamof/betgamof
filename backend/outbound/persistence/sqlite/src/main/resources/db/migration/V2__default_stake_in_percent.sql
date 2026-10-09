-- The default stake of a bankroll is either an amount or a percentage of its balance (never both).
ALTER TABLE bankrolls RENAME COLUMN fixed_stake_cents TO default_stake_cents;
ALTER TABLE bankrolls ADD COLUMN default_stake_percent REAL;
