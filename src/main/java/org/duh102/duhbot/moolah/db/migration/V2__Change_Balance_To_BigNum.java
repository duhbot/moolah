package org.duh102.duhbot.moolah.db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class V2__Change_Balance_To_BigNum extends BaseJavaMigration {
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        try (Statement create = connection.createStatement()) {
            create.execute("CREATE TABLE tempBankAccount (\n" +
                    "    uid INTEGER PRIMARY KEY,\n" +
                    "    user TEXT UNIQUE NOT NULL,\n" +
                    "    balance TEXT NOT NULL DEFAULT '0',\n" +
                    "    lastMined TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', '-1 day', 'localtime')))" +
                    ");");
            create.execute("CREATE TABLE tempSlotOutcome (\n" +
                    "    outcomeid INTEGER PRIMARY KEY,\n" +
                    "    uid INTEGER REFERENCES tempBankAccount(uid) ON " +
                    "     DELETE CASCADE ON UPDATE CASCADE NOT NULL,\n" +
                    "    slotImages TEXT NOT NULL,\n" +
                    "    wager TEXT NOT NULL,\n" +
                    "    payout TEXT NOT NULL,\n" +
                    "    payoutMul REAL NOT NULL,\n" +
                    "    timestamp TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))\n" +
                    ");");
            create.execute("CREATE TABLE tempHiLoOutcome (\n" +
                    "    outcomeid INTEGER PRIMARY KEY,\n" +
                    "    uid INTEGER REFERENCES tempBankAccount(uid) ON " +
                    "     DELETE CASCADE ON UPDATE CASCADE NOT NULL,\n" +
                    "    resultInt INTEGER NOT NULL,\n" +
                    "    hiLo TEXT NOT NULL,\n" +
                    "    wager TEXT NOT NULL,\n" +
                    "    payout TEXT NOT NULL,\n" +
                    "    payoutMul REAL NOT NULL,\n" +
                    "    timestamp TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))\n" +
                    ");");
            create.execute("CREATE TABLE IF NOT EXISTS tempMineOutcome (\n" +
                    "    outcomeid INTEGER PRIMARY KEY,\n" +
                    "    uid INTEGER REFERENCES tempBankAccount(uid) ON " +
                    "     DELETE CASCADE ON UPDATE CASCADE NOT NULL,\n" +
                    "    mineFractions INTEGER NOT NULL,\n" +
                    "    richness REAL NOT NULL,\n" +
                    "    yield TEXT NOT NULL,\n" +
                    "    timestamp INTEGER NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))\n" +
                    ");");
            create.execute("CREATE TABLE IF NOT EXISTS tempTransferRecord (\n" +
                    "    outcomeid INTEGER PRIMARY KEY,\n" +
                    "    uidSource INTEGER REFERENCES tempBankAccount(uid) ON" +
                    "     DELETE SET NULL ON UPDATE CASCADE NOT NULL,\n" +
                    "    uidDest INTEGER REFERENCES tempBankAccount(uid) ON " +
                    "     DELETE SET NULL ON UPDATE CASCADE NOT NULL,\n" +
                    "    amount TEXT NOT NULL,\n" +
                    "    timestamp INTEGER NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))\n" +
                    ");");
        }
        try (Statement select = connection.createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT uid, user, " +
                    "balance, lastMined FROM bankAccount ORDER BY uid ASC;")) {
                while (rows.next()) {
                    long uid = rows.getLong(1);
                    String user = rows.getString(2);
                    BigInteger newBalance = new BigInteger(String.format("%d",
                            rows.getLong(3)));
                    String lastMined = rows.getString(4);
                    try (PreparedStatement insert =
                                 connection.prepareStatement("INSERT INTO " +
                                         "tempBankAccount (uid, user, " +
                                         "balance, lastMined) VALUES (?, ?, " +
                                         "?, ?);")) {
                        insert.setLong(1, uid);
                        insert.setString(2, user);
                        insert.setString(3, newBalance.toString());
                        insert.setString(4, lastMined);
                        insert.execute();
                    }
                }
            }
            try (ResultSet rows = select.executeQuery("SELECT outcomeid, uid, " +
                    "slotImages, wager, payout, payoutMul, timestamp FROM slotOutcome " +
                    "ORDER BY outcomeid ASC;")) {
                while (rows.next()) {
                    long outcomeid = rows.getLong(1);
                    long uid = rows.getLong(2);
                    String slotImages = rows.getString(3);
                    BigInteger newWager = new BigInteger(String.format("%d",
                            rows.getLong(4)));
                    BigInteger newPayout = new BigInteger(String.format("%d"
                            , rows.getLong(5)));
                    double payoutMul = rows.getDouble(6);
                    String timestamp = rows.getString(7);
                    try (PreparedStatement insert =
                                 connection.prepareStatement("INSERT INTO " +
                                         "tempSlotOutcome (outcomeid, uid, " +
                                         "slotImages, wager, payout, payoutMul, timestamp) VALUES (?, ?, " +
                                         "?, ?, ?, ?, ?);")) {
                        insert.setLong(1, outcomeid);
                        insert.setLong(2, uid);
                        insert.setString(3, slotImages);
                        insert.setString(4, newWager.toString());
                        insert.setString(5, newPayout.toString());
                        insert.setDouble(6, payoutMul);
                        insert.setString(7, timestamp);
                        insert.execute();
                    }
                }
            }
            try (ResultSet rows = select.executeQuery("SELECT outcomeid, uid, " +
                    "resultInt, hiLo, wager, payout, payoutMul, timestamp " +
                    "FROM hiLoOutcome ORDER BY outcomeid ASC;")) {
                while (rows.next()) {
                    long outcomeid = rows.getLong(1);
                    long uid = rows.getLong(2);
                    int resultInt = rows.getInt(3);
                    String hiLo = rows.getString(4);
                    BigInteger newWager = new BigInteger(String.format("%d",
                            rows.getLong(5)));
                    BigInteger newPayout = new BigInteger(String.format("%d",
                            rows.getLong(6)));
                    double payoutMul = rows.getDouble(7);
                    String timestamp = rows.getString(8);
                    try (PreparedStatement insert =
                                 connection.prepareStatement("INSERT INTO " +
                                         "tempHiLoOutcome (outcomeid, uid, " +
                                         "resultInt, hiLo, wager, payout, " +
                                         "payoutMul, timestamp) " +
                                         "VALUES (?, ?, " +
                                         "?, ?, ?, ?, ?, ?);")) {
                        insert.setLong(1, outcomeid);
                        insert.setLong(2, uid);
                        insert.setInt(3, resultInt);
                        insert.setString(4, hiLo);
                        insert.setString(5, newWager.toString());
                        insert.setString(6, newPayout.toString());
                        insert.setDouble(7, payoutMul);
                        insert.setString(8, timestamp);
                        insert.execute();
                    }
                }
            }
            try (ResultSet rows = select.executeQuery("SELECT outcomeid, uid, " +
                    "mineFractions, richness, yield, timestamp FROM " +
                    "mineOutcome ORDER BY outcomeid ASC;")) {
                while (rows.next()) {
                    long outcomeid = rows.getLong(1);
                    long uid = rows.getLong(2);
                    int mineFractions = rows.getInt(3);
                    double richness = rows.getDouble(4);
                    BigInteger yield = new BigInteger(String.format("%d",
                            rows.getLong(4)));
                    String timestamp = rows.getString(5);
                    try (PreparedStatement insert =
                                 connection.prepareStatement("INSERT INTO " +
                                         "tempMineOutcome (outcomeid, uid, " +
                                         "mineFractions, richness, " +
                                         "yield, timestamp) VALUES (?, " +
                                         "?, ?, ?, ?, ?);")) {
                        insert.setLong(1, outcomeid);
                        insert.setLong(2, uid);
                        insert.setInt(3, mineFractions);
                        insert.setDouble(4, richness);
                        insert.setString(5, yield.toString());
                        insert.setString(6, timestamp);
                        insert.execute();
                    }
                }
            }
            try (ResultSet rows = select.executeQuery("SELECT outcomeid, uidSource, " +
                    "uidDest, amount, timestamp FROM " +
                    "transferRecord ORDER BY outcomeid ASC;")) {
                while (rows.next()) {
                    long outcomeid = rows.getLong(1);
                    long uidSource = rows.getLong(2);
                    long uidDest = rows.getLong(3);
                    BigInteger amount = new BigInteger(String.format("%d",
                            rows.getLong(4)));
                    String timestamp = rows.getString(5);
                    try (PreparedStatement insert =
                                 connection.prepareStatement("INSERT INTO " +
                                         "tempTransferRecord (outcomeid, " +
                                         "uidSource, uidDest, " +
                                         "amount, timestamp) VALUES (?, " +
                                         "?, ?, ?, ?);")) {
                        insert.setLong(1, outcomeid);
                        insert.setLong(2, uidSource);
                        insert.setLong(3, uidDest);
                        insert.setString(5, amount.toString());
                        insert.setString(6, timestamp);
                        insert.execute();
                    }
                }
            }
        }
        try (Statement drop = connection.createStatement()) {
            drop.execute("DROP TABLE transferRecord;");
            drop.execute("DROP TABLE mineOutcome;");
            drop.execute("DROP TABLE hiLoOutcome;");
            drop.execute("DROP TABLE slotOutcome;");
            drop.execute("DROP TABLE bankAccount;");
        }
        try (Statement alter = connection.createStatement()) {
            alter.execute("ALTER TABLE tempBankAccount RENAME TO bankAccount;");
            alter.execute("ALTER TABLE tempSlotOutcome RENAME TO slotOutcome;");
            alter.execute("ALTER TABLE tempHiLoOutcome RENAME TO hiLoOutcome;");
            alter.execute("ALTER TABLE tempMineOutcome RENAME TO mineOutcome;");
            alter.execute("ALTER TABLE tempTransferRecord RENAME TO " +
                    "transferRecord;");
        }
    }
}
