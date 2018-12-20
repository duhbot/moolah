package org.duh102.duhbot.moolah.db.migration.migrations;

import org.duh102.duhbot.moolah.db.migration.DatabaseVersion;
import org.duh102.duhbot.moolah.db.migration.Migrator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class Version0_0_0 implements Migrator {
    @Override
    public DatabaseVersion getVersion() {
        return DatabaseVersion.UNVERSIONED;
    }

    @Override
    public List<DatabaseVersion> getCompatibleBaseVersions() {
        return null;
    }

    @Override
    public void upgrade(Connection connection) {
        try {
            Statement stat = connection.createStatement();
            //This table is not safe for networks with no nickserv or usage on several
            //  networks (with different nickserv databases)! We only store the registered nick
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS bankAccount (uid INTEGER PRIMARY KEY, user TEXT UNIQUE NOT NULL, balance INTEGER NOT NULL DEFAULT 0, lastMined TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', '-1 day', 'localtime'))) );");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS transferRecord (outcomeid INTEGER PRIMARY KEY, uidSource INTEGER REFERENCES bankAccount(uid) ON DELETE SET NULL ON UPDATE CASCADE NOT NULL, uidDest INTEGER REFERENCES bankAccount(uid) ON DELETE SET NULL ON UPDATE CASCADE NOT NULL, amount INTEGER NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime'))) );");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS mineOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, mineFractions INTEGER NOT NULL, richness REAL NOT NULL, yield INTEGER NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime'))) );");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS slotOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, slotImages TEXT NOT NULL, wager INTEGER NOT NULL, payout INTEGER NOT NULL, payoutMul REAL NOT NULL, timestamp TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime'))) );");
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS hiLoOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, resultInt INTEGER NOT NULL, hiLo TEXT NOT NULL, wager INTEGER NOT NULL, payout INTEGER NOT NULL, payoutMul REAL NOT NULL, timestamp TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime'))) );");
        } catch(SQLException sqle) {
            sqle.printStackTrace();
        }

    }

    @Override
    public void downgrade(DatabaseVersion downgradeTo, Connection connection) {
        System.err.println("Cannot downgrade below first version");
    }

    @Override
    public boolean downgradeCausesDataLoss() {
        return false;
    }
}
