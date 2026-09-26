CREATE TABLE bankrolls (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    name                  TEXT    NOT NULL,
    color                 TEXT    NOT NULL,
    initial_balance_cents INTEGER NOT NULL,
    stop_loss_cents       INTEGER,
    kelly_fraction        REAL    NOT NULL,
    fixed_stake_cents     INTEGER,
    created_at            TEXT    NOT NULL
);

CREATE TABLE montantes (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    name                TEXT    NOT NULL,
    bankroll_id         INTEGER NOT NULL REFERENCES bankrolls (id),
    start_capital_cents INTEGER NOT NULL,
    target_odds         REAL    NOT NULL,
    mode                TEXT    NOT NULL,
    target_multiplier   REAL,
    step_count          INTEGER,
    exclude_stake       INTEGER NOT NULL,
    secure_pct          INTEGER NOT NULL,
    relances_allowed    INTEGER NOT NULL,
    created_at          TEXT    NOT NULL,
    closed_at           TEXT
);

CREATE TABLE bets (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    bankroll_id   INTEGER NOT NULL REFERENCES bankrolls (id),
    montante_id   INTEGER REFERENCES montantes (id),
    type          TEXT    NOT NULL,
    bookmaker     TEXT    NOT NULL,
    stake_cents   INTEGER NOT NULL,
    odds          REAL    NOT NULL,
    status        TEXT    NOT NULL,
    cashout_cents INTEGER,
    placed_at     TEXT    NOT NULL,
    starts_at     TEXT    NOT NULL,
    settled_at    TEXT
);

CREATE INDEX bets_bankroll_idx ON bets (bankroll_id);
CREATE INDEX bets_montante_idx ON bets (montante_id);

CREATE TABLE bet_selections (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    bet_id      INTEGER NOT NULL REFERENCES bets (id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    event_id    TEXT,
    event_name  TEXT    NOT NULL,
    sport       TEXT    NOT NULL,
    competition TEXT    NOT NULL,
    market      TEXT    NOT NULL,
    pick        TEXT    NOT NULL,
    odds        REAL    NOT NULL
);

CREATE INDEX bet_selections_bet_idx ON bet_selections (bet_id);

CREATE TABLE discipline_rules (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    kind  TEXT    NOT NULL,
    param INTEGER NOT NULL
);
