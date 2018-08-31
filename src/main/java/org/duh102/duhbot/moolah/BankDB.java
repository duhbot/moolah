package org.duh102.duhbot.moolah;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.sqlite.SQLiteConfig;

import org.duh102.duhbot.moolah.exceptions.ImproperBalanceAmount;
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
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS transferRecord (outcomeid INTEGER PRIMARY KEY, uidSource INTEGER REFERENCES bankAccount(uid) ON DELETE SET NULL ON UPDATE CASCADE NOT NULL, uidDest INTEGER REFERENCES bankAccount(uid) ON DELETE SET NULL ON UPDATE CASCADE NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)) );");
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

  public BankAccount openAccount(String user) throws AccountAlreadyExists, RecordFailure {
    return openAccount(null, user);
  }
  public BankAccount openAccount(Connection conn, String user) throws AccountAlreadyExists, RecordFailure {
    boolean handleConn = conn == null;
    BankAccount account = null;
    try {
      if( handleConn ) {
        conn = handleMakeConnection();
      }
      account = getAccount(conn, user);
      if( account != null )
        throw new AccountAlreadyExists(String.format("User %s registered as %d", user, account.uid));

      try {
        PreparedStatement stat = conn.prepareStatement("INSERT INTO bankAccount (user) VALUES (?);");
        stat.setString(1, user);
        stat.executeUpdate();
      } catch( SQLException sqle ) {
        throw new RecordFailure(sqle);
      }
      try {
        account = getAccountExcept(conn, user);
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
    return account;
  }

  public BankAccount getAccountExcept(String user) throws AccountDoesNotExist, RecordFailure {
    return getAccountExcept(null, user);
  }
  public BankAccount getAccountExcept(Connection conn, String user) throws AccountDoesNotExist, RecordFailure {
    BankAccount account = getAccount(conn, user);
    if( account == null)
      throw new AccountDoesNotExist(user);
    return account;
  }
  public BankAccount getAccount(String user) throws RecordFailure {
    return getAccount(null, user);
  }
  public BankAccount getAccount(Connection conn, String user) throws RecordFailure {
    boolean handleConn = conn == null;
    BankAccount account = null;
    try {
      if( handleConn )
        conn = handleMakeConnection();
      try {
        PreparedStatement stat = conn.prepareStatement("SELECT uid, balance, lastMined FROM bankAccount where user = ? LIMIT 1;");
        stat.setString(1, user);
        ResultSet rs = stat.executeQuery();
        while (rs.next()) {
          long uid = rs.getLong("uid");
          long balance = rs.getLong("balance");
          long lastMined = rs.getLong("lastMined");
          try {
            account = new BankAccount(uid, user, balance, lastMined);
          } catch( ImproperBalanceAmount iba ) {
            //this should never happen, since we're grabbing from the database
            throw new RecordFailure(iba);
          }
        }
        return account;
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

  public BankAccount getAccountExcept(long uid) throws AccountDoesNotExist, RecordFailure {
    return getAccountExcept(null, uid);
  }
  public BankAccount getAccountExcept(Connection conn, long uid) throws AccountDoesNotExist, RecordFailure {
    BankAccount account = getAccount(conn, uid);
    if( account == null)
      throw new AccountDoesNotExist(String.format("%d", uid));
    return account;
  }
  public BankAccount getAccount(long uid) throws RecordFailure {
    return getAccount(null, uid);
  }
  public BankAccount getAccount(Connection conn, long uid) throws RecordFailure {
    boolean handleConn = conn == null;
    BankAccount account = null;
    try {
      if( handleConn )
        conn = handleMakeConnection();
      try {
        PreparedStatement stat = conn.prepareStatement("SELECT user, balance, lastMined FROM bankAccount where uid = ? LIMIT 1;");
        stat.setLong(1, uid);
        ResultSet rs = stat.executeQuery();
        while (rs.next()) {
          String user = rs.getString("user");
          long balance = rs.getLong("balance");
          long lastMined = rs.getLong("lastMined");
          try {
            account = new BankAccount(uid, user, balance, lastMined);
          } catch( ImproperBalanceAmount iba ) {
            //this should never happen, since we're grabbing from the database
            throw new RecordFailure(iba);
          }
        }
        return account;
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

  public void recordHiLoOutcome(Connection conn, long uid, int resultInt, String hiLo, long wager, long payout, double multiplier, long timestamp) throws RecordFailure {
    boolean handleConn = conn == null;
    try {
      if( handleConn )
        conn = handleMakeConnection();
      try {
        PreparedStatement stat = conn.prepareStatement("INSERT INTO hiLoOutcome (uid, resultInt, hiLo, wager, payout, payoutMul, timestamp) values (?, ?, ?, ?, ?, ?, ?);");
        stat.setLong(1, uid);
        stat.setInt(2, resultInt);
        stat.setString(3, hiLo);
        stat.setLong(4, wager);
        stat.setLong(5, payout);
        stat.setDouble(6, multiplier);
        stat.setLong(7, timestamp);
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

  public void recordMineOutcome(Connection conn, long uid, int mineFractions, double richness, long yield, long timestamp, boolean updateLastMined) throws RecordFailure {
    boolean handleConn = conn == null;
    try {
      if( handleConn )
        conn = handleMakeConnection();
      try {
        PreparedStatement stat = conn.prepareStatement("INSERT INTO mineOutcome (uid, mineFractions, richness, yield, timestamp) values (?, ?, ?, ?, ?, ?, ?);");
        stat.setLong(1, uid);
        stat.setInt(2, mineFractions);
        stat.setDouble(3, richness);
        stat.setLong(4, yield);
        stat.setLong(5, timestamp);
        stat.executeUpdate();
      } catch( SQLException sqle ) {
        //may want to check for SQLITE_CONSTRAINT(19) here
        throw new RecordFailure(sqle);
      }
      //if( updateLastMined )
      //  recordLastMined(conn, uid, timestamp);
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
