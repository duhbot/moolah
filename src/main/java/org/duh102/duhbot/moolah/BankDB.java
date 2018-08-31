package org.duh102.duhbot.moolah;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.sqlite.SQLiteConfig;

import org.duh102.duhbot.moolah.exceptions.InvalidDBConfiguration;
import org.duh102.duhbot.moolah.exceptions.InvalidEnvironment;
import org.duh102.duhbot.moolah.exceptions.RecordFailure;
import org.duh102.duhbot.moolah.exceptions.AccountAlreadyExists;
import org.duh102.duhbot.moolah.exceptions.AccountDoesNotExist;

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
    createTables(null);
  }
  private void createTables(Connection conn) throws InvalidDBConfiguration, InvalidEnvironment {
    boolean handleConn = conn == null;
    try {
      if( handleConn )
        conn = getDBConnection();
      Statement stat = conn.createStatement();
      //This table is not safe for networks with no nickserv or usage on several
      //  networks (with different nickserv databases)! We only store the registered nick
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS bankAccount (uid INTEGER PRIMARY KEY, user TEXT UNIQUE NOT NULL, balance INTEGER NOT NULL DEFAULT 0, lastMined INTEGER NOT NULL DEFAULT (strftime('%s', datetime('now', '-1 day'))) );");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS slotOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, slotImages TEXT NOT NULL, wager INTEGER NOT NULL, payout INTEGER NOT NULL, payoutMul REAL NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)) );");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS hiLoOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, resultInt INTEGER NOT NULL, hiLo TEXT NOT NULL, wager INTEGER NOT NULL, payout INTEGER NOT NULL, payoutMul REAL NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)) );");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS mineOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, mineFractions INTEGER NOT NULL, richness REAL NOT NULL, yield INTEGER NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)) );");
    } catch(SQLException sqle) {
      throw new InvalidDBConfiguration(sqle);
    } finally {
      if( handleConn ) {
        try {
          conn.close();
        } catch( SQLException sqle ) {
          throw new InvalidDBConfiguration(sqle);
        }
      }
    }
  }

  public Connection getDBConnection() throws InvalidDBConfiguration, InvalidEnvironment {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch( java.lang.ClassNotFoundException cnfe ) {
      throw new InvalidEnvironment(cnfe);
    }
    try {
      SQLiteConfig config = new SQLiteConfig();
      config.enforceForeignKeys(true);
      return DriverManager.getConnection("jdbc:sqlite:" + myDBFile, config.toProperties());
    } catch(SQLException sqle) {
      throw new InvalidDBConfiguration(sqle);
    }
  }

  public Connection handleMakeConnection() throws RecordFailure {
    try {
      return getDBConnection();
    } catch( InvalidDBConfiguration idc ) {
      throw new RecordFailure(idc);
    } catch( InvalidEnvironment ie ) {
      throw new RecordFailure(ie);
    }
  }

  public Long openAccount(String user) throws AccountAlreadyExists, RecordFailure {
    return openAccount(null, user);
  }
  public Long openAccount(Connection conn, String user) throws AccountAlreadyExists, RecordFailure {
    boolean handleConn = conn == null;
    Long uid = null;
    try {
      if( handleConn ) {
        conn = handleMakeConnection();
      }
      uid = checkUsername(conn, user);
      if( uid != null )
        throw new AccountAlreadyExists(String.format("User %s registered as %d", user, uid));

      try {
        PreparedStatement stat = conn.prepareStatement("INSERT INTO bankAccount (user) VALUES (?);");
        stat.setString(1, user);
        stat.executeUpdate();
      } catch( SQLException sqle ) {
        throw new RecordFailure(sqle);
      }
      try {
        uid = checkUsernameExcept(conn, user);
      } catch( AccountDoesNotExist adne ) {
        throw new RecordFailure(adne);
      }
    } finally {
      if( handleConn ) {
        try {
          conn.close();
        } catch( SQLException sqle ) {
          throw new RecordFailure(sqle);
        }
      }
    }
    return uid;
  }

  public Long checkUsernameExcept(String user) throws AccountDoesNotExist, RecordFailure {
    return checkUsernameExcept(null, user);
  }
  public Long checkUsernameExcept(Connection conn, String user) throws AccountDoesNotExist, RecordFailure {
    Long uid = checkUsername(conn, user);
    if( uid == null)
      throw new AccountDoesNotExist(user);
    return uid;
  }
  public Long checkUsername(String user) throws RecordFailure {
    return checkUsername(null, user);
  }
  public Long checkUsername(Connection conn, String user) throws RecordFailure {
    boolean handleConn = conn == null;
    Long uid = null;
    try {
      if( handleConn )
        conn = handleMakeConnection();
      try {
        PreparedStatement stat = conn.prepareStatement("SELECT uid FROM bankAccount where user = ? LIMIT 1;");
        stat.setString(1, user);
        ResultSet rs = stat.executeQuery();
        while (rs.next()) {
          uid = rs.getLong("uid");
        }
        return uid;
      } catch( SQLException sqle ) {
        //may want to check for SQLITE_CONSTRAINT(19) here
        throw new RecordFailure(sqle);
      }
    } finally {
      if( handleConn ) {
        try {
          conn.close();
        } catch( SQLException sqle ) {
          throw new RecordFailure(sqle);
        }
      }
    }
  }

  public void recordSlotOutcome(Connection conn, long uid, String slotState, long wager, long payout, double multiplier, long timestamp) throws RecordFailure {
    boolean handleConn = conn == null;
    try {
      if( handleConn )
        conn = handleMakeConnection();
      try {
        PreparedStatement stat = conn.prepareStatement("INSERT INTO slotOutcome (uid, slotImages, wager, payout, payoutMul, timestamp) values (?, ?, ?, ?, ?, ?);");
        stat.setLong(1, uid);
        stat.setString(2, slotState);
        stat.setLong(3, wager);
        stat.setLong(4, payout);
        stat.setDouble(5, multiplier);
        stat.setLong(6, timestamp);
        stat.executeUpdate();
      } catch( SQLException sqle ) {
        //may want to check for SQLITE_CONSTRAINT(19) here
        throw new RecordFailure(sqle);
      }
    } finally {
      if( handleConn ) {
        try {
          conn.close();
        } catch( SQLException sqle ) {
          throw new RecordFailure(sqle);
        }
      }
    }
  }
}
