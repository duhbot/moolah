package org.duh102.duhbot.moolah;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.sqlite.SQLiteConfig;

import org.duh102.duhbot.moolah.exceptions.InvalidDBConfiguration;
import org.duh102.duhbot.moolah.exceptions.InvalidEnvironment;

public class BankDB {
  private static final String DEFAULT_DB = "moolah.db";
  private static ConcurrentHashMap<String, BankDB> instanceMap;
  private String myDBFile;
  static {
    instanceMap = new ConcurrentHashMap<String, BankDB>();
  }

  private BankDB(String dbFile) throws InvalidDBConfiguration, InvalidEnvironment {
    myDBFile = dbFile;
    createTables();
  }

  private static BankDB getInstance(String dbFile) throws InvalidDBConfiguration, InvalidEnvironment {
    BankDB instance = null;
    instance = instanceMap.get(dbFile);
    if( instance == null ) {
      instance = new BankDB(dbFile);
      instanceMap.put(dbFile, instance);
    }
    return instance;
  }

  public static BankDB getDBInstance() throws InvalidDBConfiguration, InvalidEnvironment {
    return getInstance(DEFAULT_DB);
  }
  public static BankDB getMemoryInstance() throws InvalidDBConfiguration, InvalidEnvironment {
    return getInstance(":memory:");
  }

  private void createTables() throws InvalidDBConfiguration, InvalidEnvironment {
    Connection conn = getDBConnection();
    if(conn != null) {
      try {
        Statement stat = conn.createStatement();
        //This table is not safe for networks with no nickserv or usage on several
        //  networks (with different nickserv databases)! We only store the registered nick
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS bankAccount (uid INTEGER PRIMARY KEY, user TEXT UNIQUE NOT NULL, balance INTEGER NOT NULL DEFAULT 0, lastMined INTEGER NOT NULL DEFAULT (strftime('%s', datetime('now', '-1 day'))) );");
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS slotOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, slotImages TEXT NOT NULL, wager INTEGER NOT NULL, payout INTEGER NOT NULL, payoutMul REAL NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)) );");
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS hiLoOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, resultInt INTEGER NOT NULL, hiLo TEXT NOT NULL, wager INTEGER NOT NULL, payout INTEGER NOT NULL, payoutMul REAL NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)) );");
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS mineOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, mineFractions INTEGER NOT NULL, richness REAL NOT NULL, yield INTEGER NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)) );");
        conn.close();
      } catch(java.sql.SQLException sqle) {
        throw new InvalidDBConfiguration(sqle);
      }
    }
  }

  public Connection getDBConnection() throws InvalidDBConfiguration, InvalidEnvironment {
    Connection conn = null;
    try {
      Class.forName("org.sqlite.JDBC");
    } catch( java.lang.ClassNotFoundException cnfe ) {
      throw new InvalidEnvironment(cnfe);
    }
    try {
      SQLiteConfig config = new SQLiteConfig();
      config.enforceForeignKeys(true);
      conn = DriverManager.getConnection("jdbc:sqlite:" + myDBFile, config.toProperties());
    } catch(java.sql.SQLException sqle) {
      throw new InvalidDBConfiguration(sqle);
    }
    return conn;
  }
}
