CREATE TABLE IF NOT EXISTS bankAccount (
    uid INTEGER PRIMARY KEY,
    user TEXT UNIQUE NOT NULL,
    balance INTEGER NOT NULL DEFAULT 0,
    lastMined TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', '-1 day', 'localtime')))
);
CREATE TABLE IF NOT EXISTS slotOutcome (
    outcomeid INTEGER PRIMARY KEY,
    uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL,
    slotImages TEXT NOT NULL,
    wager INTEGER NOT NULL,
    payout INTEGER NOT NULL,
    payoutMul REAL NOT NULL,
    timestamp TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))
);
CREATE TABLE IF NOT EXISTS hiLoOutcome (
    outcomeid INTEGER PRIMARY KEY,
    uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL,
    resultInt INTEGER NOT NULL,
    hiLo TEXT NOT NULL,
    wager INTEGER NOT NULL,
    payout INTEGER NOT NULL,
    payoutMul REAL NOT NULL,
    timestamp TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))
);
CREATE TABLE IF NOT EXISTS mineOutcome (
    outcomeid INTEGER PRIMARY KEY,
    uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL,
    mineFractions INTEGER NOT NULL,
    richness REAL NOT NULL,
    yield INTEGER NOT NULL,
    timestamp INTEGER NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))
);
CREATE TABLE IF NOT EXISTS transferRecord (
    outcomeid INTEGER PRIMARY KEY,
    uidSource INTEGER REFERENCES bankAccount(uid) ON DELETE SET NULL ON UPDATE CASCADE NOT NULL,
    uidDest INTEGER REFERENCES bankAccount(uid) ON DELETE SET NULL ON UPDATE CASCADE NOT NULL,
    amount INTEGER NOT NULL,
    timestamp INTEGER NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))
);