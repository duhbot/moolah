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
            create.execute("CREATE TABLE tempBankAccount (" +
                    "    uid INTEGER PRIMARY KEY,\n" +
                    "    user TEXT UNIQUE NOT NULL,\n" +
                    "    balance TEXT NOT NULL DEFAULT '0',\n" +
                    "    lastMined TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', '-1 day', 'localtime')))" +
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
