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
                    ")");
            create.execute("CREATE TABLE tempSlotOutcome (\n" +
                    "    outcomeid INTEGER PRIMARY KEY,\n" +
                    "    uid INTEGER REFERENCES tempBankAccount(uid) ON " +
                    "     DELETE CASCADE ON UPDATE CASCADE NOT NULL,\n" +
                    "    slotImages TEXT NOT NULL,\n" +
                    "    wager TEXT NOT NULL,\n" +
                    "    payout TEXT NOT NULL,\n" +
                    "    payoutMul REAL NOT NULL,\n" +
                    "    timestamp TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))\n" +
                    ")");
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
                    ")");
            create.execute("CREATE TABLE IF NOT EXISTS tempMineOutcome (\n" +
                    "    outcomeid INTEGER PRIMARY KEY,\n" +
                    "    uid INTEGER REFERENCES tempBankAccount(uid) ON " +
                    "     DELETE CASCADE ON UPDATE CASCADE NOT NULL,\n" +
                    "    mineFractions INTEGER NOT NULL,\n" +
                    "    richness REAL NOT NULL,\n" +
                    "    yield TEXT NOT NULL,\n" +
                    "    timestamp INTEGER NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))\n" +
                    ")");
            create.execute("CREATE TABLE IF NOT EXISTS tempTransferRecord (\n" +
                    "    outcomeid INTEGER PRIMARY KEY,\n" +
                    "    uidSource INTEGER REFERENCES tempBankAccount(uid) ON" +
                    "     DELETE SET NULL ON UPDATE CASCADE NOT NULL,\n" +
                    "    uidDest INTEGER REFERENCES tempBankAccount(uid) ON " +
                    "     DELETE SET NULL ON UPDATE CASCADE NOT NULL,\n" +
                    "    amount TEXT NOT NULL,\n" +
                    "    timestamp INTEGER NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime')))\n" +
                    ")");
        }
        try (Statement select = connection.createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT uid, user, " +
                    "balance, lastMined FROM bankAccount ORDER BY uid ASC")) {
                while (rows.next()) {
                    long uid = rows.getInt(1);
                    String user = rows.getString(2);
                    BigInteger newBalance = new BigInteger(String.format("%d"
                            , rows.getLong(3)));
                    String lastMined = rows.getString(4);
                    try (PreparedStatement insert =
                                 connection.prepareStatement("INSERT INTO " +
                                         "tempBankAccount (uid, user, " +
                                         "balance, lastMined) VALUES (?, ?, " +
                                         "?, ?)")) {
                        insert.setLong(1, uid);
                        insert.setString(2, user);
                        insert.setString(3, newBalance.toString());
                        insert.setString(4, lastMined);
                    }
                }
            }
        }
        try (Statement drop = connection.createStatement()) {
            drop.execute("DROP TABLE bankAccount");
        }
        try (Statement alter = connection.createStatement()) {
            alter.execute("ALTER TABLE tempBankAccount RENAME TO bankAccount");
        }
    }
}
