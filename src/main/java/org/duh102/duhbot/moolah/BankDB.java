package org.duh102.duhbot.moolah;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.text.ParseException;
import org.sqlite.SQLiteConfig;

import org.duh102.duhbot.moolah.exceptions.*;

public class BankDB {
  private static final String DEFAULT_DB = "moolah.db";
  private static ConcurrentHashMap<String, BankDB> instanceMap;
  private String myDBFile;
  private Connection connection = null;
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
    Connection conn = null;
    try {
      conn = makeDBConnection();
    } catch( DBAlreadyConnected dac ) {
      conn = connection;
    }
    try {
      Statement stat = conn.createStatement();
      //This table is not safe for networks with no nickserv or usage on several
      //  networks (with different nickserv databases)! We only store the registered nick
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS bankAccount (uid INTEGER PRIMARY KEY, user TEXT UNIQUE NOT NULL, balance INTEGER NOT NULL DEFAULT 0, lastMined TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', '-1 day', 'localtime'))) );");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS slotOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, slotImages TEXT NOT NULL, wager INTEGER NOT NULL, payout INTEGER NOT NULL, payoutMul REAL NOT NULL, timestamp TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime'))) );");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS hiLoOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, resultInt INTEGER NOT NULL, hiLo TEXT NOT NULL, wager INTEGER NOT NULL, payout INTEGER NOT NULL, payoutMul REAL NOT NULL, timestamp TEXT NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime'))) );");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS mineOutcome (outcomeid INTEGER PRIMARY KEY, uid INTEGER REFERENCES bankAccount(uid) ON DELETE CASCADE ON UPDATE CASCADE NOT NULL, mineFractions INTEGER NOT NULL, richness REAL NOT NULL, yield INTEGER NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime'))) );");
      stat.executeUpdate("CREATE TABLE IF NOT EXISTS transferRecord (outcomeid INTEGER PRIMARY KEY, uidSource INTEGER REFERENCES bankAccount(uid) ON DELETE SET NULL ON UPDATE CASCADE NOT NULL, uidDest INTEGER REFERENCES bankAccount(uid) ON DELETE SET NULL ON UPDATE CASCADE NOT NULL, amount INTEGER NOT NULL, timestamp INTEGER NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f', datetime('now', 'localtime'))) );");
    } catch(SQLException sqle) {
      throw new InvalidDBConfiguration(sqle);
    }
  }

  public synchronized Connection makeDBConnection() throws InvalidEnvironment, InvalidDBConfiguration, DBAlreadyConnected {
    if( connection != null )
      throw new DBAlreadyConnected();
    try {
      Class.forName("org.sqlite.JDBC");
    } catch( java.lang.ClassNotFoundException cnfe ) {
      throw new InvalidEnvironment(cnfe);
    }
    try {
      SQLiteConfig config = new SQLiteConfig();
      config.enforceForeignKeys(true);
      connection = DriverManager.getConnection("jdbc:sqlite:" + myDBFile, config.toProperties());
      return connection;
    } catch(SQLException sqle) {
      throw new InvalidDBConfiguration(sqle);
    }
  }

  public synchronized void destroyDBConnection() throws SQLException, DBNotConnected {
    if( connection == null )
      throw new DBNotConnected();
    connection.close();
    connection = null;
  }

  public synchronized Connection getDBConnection() throws RecordFailure {
    if( connection != null )
      return connection;
    try {
      return makeDBConnection();
    } catch( InvalidEnvironment ie ) {
      throw new RecordFailure(ie);
    } catch( InvalidDBConfiguration idc ) {
      throw new RecordFailure(idc);
    } catch( DBAlreadyConnected dac ) {
      // this should not happen
      return connection;
    }
  }

  public BankAccount openAccount(String user) throws AccountAlreadyExists, RecordFailure {
    return openAccount(user, 0l);
  }

  public BankAccount openAccount(String user, long balance) throws AccountAlreadyExists, RecordFailure {
    Connection conn = getDBConnection();
    BankAccount account = null;
    account = getAccount(user);
    if( account != null )
      throw new AccountAlreadyExists(String.format("User %s registered as %d", user, account.uid));

    try {
      PreparedStatement stat = conn.prepareStatement("INSERT INTO bankAccount (user, balance) VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS);
      stat.setString(1, user.toLowerCase());
      stat.setLong(2, balance);
      stat.executeUpdate();
      ResultSet rs = stat.getGeneratedKeys();
      if (rs.next()) {
        long genUID = rs.getLong(1);
        account = getAccount(genUID);
      }
      else {
        try {
          account = getAccountExcept(user);
        } catch( AccountDoesNotExist adne ) {
          throw new RecordFailure(adne);
        }
      }
      rs.close();
      return account;
    } catch( SQLException sqle ) {
      throw new RecordFailure(sqle);
    }
  }

  public BankAccount getAccountExcept(String user) throws AccountDoesNotExist, RecordFailure {
    Connection conn = getDBConnection();
    BankAccount account = getAccount(user);
    if( account == null)
      throw new AccountDoesNotExist(user);
    return account;
  }
  public BankAccount getAccount(String user) throws RecordFailure {
    Connection conn = getDBConnection();
    BankAccount account = null;
    try {
      PreparedStatement stat = conn.prepareStatement("SELECT uid, balance, lastMined FROM bankAccount where user = ? LIMIT 1;");
      stat.setString(1, user.toLowerCase());
      ResultSet rs = stat.executeQuery();
      while (rs.next()) {
        long uid = rs.getLong("uid");
        long balance = rs.getLong("balance");
        Timestamp lastMined = null;
        try {
          lastMined = LocalTimestamp.parse(rs.getString("lastMined"));
        } catch( ParseException pe ) {
          //If we can't read the last mined timestamp, just set it to now
          lastMined = LocalTimestamp.now();
        }
        try {
          account = new BankAccount(uid, user.toLowerCase(), balance, lastMined);
        } catch( ImproperBalanceAmount iba ) {
          //this should never happen, since we're grabbing from the database
          throw new RecordFailure(iba);
        }
      }
      rs.close();
      return account;
    } catch( SQLException sqle ) {
      //may want to check for SQLITE_CONSTRAINT(19) here
      throw new RecordFailure(sqle);
    }
  }

  public BankAccount getAccountExcept(long uid) throws AccountDoesNotExist, RecordFailure {
    Connection conn = getDBConnection();
    BankAccount account = getAccount(uid);
    if( account == null)
      throw new AccountDoesNotExist(String.format("%d", uid));
    return account;
  }
  public BankAccount getAccount(long uid) throws RecordFailure {
    Connection conn = getDBConnection();
    BankAccount account = null;
    try {
      PreparedStatement stat = conn.prepareStatement("SELECT user, balance, lastMined FROM bankAccount where uid = ? LIMIT 1;");
      stat.setLong(1, uid);
      ResultSet rs = stat.executeQuery();
      while (rs.next()) {
        String user = rs.getString("user");
        long balance = rs.getLong("balance");
        Timestamp lastMined = null;
        try {
          lastMined = LocalTimestamp.parse(rs.getString("lastMined"));
        } catch( ParseException pe ) {
          //If we can't read the last mined timestamp, just set it to now
          lastMined = LocalTimestamp.now();
        }
        try {
          account = new BankAccount(uid, user, balance, lastMined);
        } catch( ImproperBalanceAmount iba ) {
          //this should never happen, since we're grabbing from the database
          throw new RecordFailure(iba);
        }
      }
      rs.close();
      return account;
    } catch( SQLException sqle ) {
      //may want to check for SQLITE_CONSTRAINT(19) here
      throw new RecordFailure(sqle);
    }
  }

  // Sync an account to the database
  public BankAccount pushAccount(BankAccount account) throws RecordFailure, AccountDoesNotExist {
    Connection conn = getDBConnection();
    BankAccount dbAccount = getAccountExcept(account.uid);
    try {
      PreparedStatement stat = conn.prepareStatement("UPDATE bankAccount SET user = ?, balance = ?, lastMined = ? WHERE uid = ?;");
      stat.setString(1, account.user);
      stat.setLong(2, account.balance);
      stat.setString(3, LocalTimestamp.format(account.lastMined));
      stat.setLong(4, account.uid);
      stat.executeUpdate();
    } catch( SQLException sqle ) {
      throw new RecordFailure(sqle);
    }
    return account;
  }

  public SlotRecord recordSlotRecord(SlotRecord record) throws RecordFailure {
    return recordSlotRecord(record.uid, record.slotImages, record.wager, record.payout, record.multiplier, record.timestamp);
  }
  public SlotRecord recordSlotRecord(long uid, SlotReelImage[] slotState, long wager, long payout, double multiplier, Timestamp timestamp) throws RecordFailure {
    Connection conn = getDBConnection();
    try {
      PreparedStatement stat = conn.prepareStatement("INSERT INTO slotOutcome (uid, slotImages, wager, payout, payoutMul, timestamp) values (?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
      stat.setLong(1, uid);
      stat.setString(2, SlotRecord.getRegexString(slotState));
      stat.setLong(3, wager);
      stat.setLong(4, payout);
      stat.setDouble(5, multiplier);
      stat.setString(6, LocalTimestamp.format(timestamp));
      stat.executeUpdate();
      ResultSet rs = stat.getGeneratedKeys();
      try {
        if (rs.next()) {
          long genOID = rs.getLong(1);
          SlotRecord outcome = new SlotRecord(genOID, uid, slotState, wager, payout, multiplier, timestamp);
          return outcome;
        }
        else {
          throw new RecordFailure("No generated key");
        }
      } finally {
        rs.close();
      }
    } catch( SQLException sqle ) {
      //may want to check for SQLITE_CONSTRAINT(19) here
      throw new RecordFailure(sqle);
    }
  }

  public HiLoRecord recordHiLoRecord(HiLoRecord record) throws RecordFailure {
    return recordHiLoRecord(record.uid, record.resultInt, record.hiLo, record.wager, record.payout, record.multiplier, record.timestamp);
  }
  public HiLoRecord recordHiLoRecord(long uid, int resultInt, HiLoBetType hiLo, long wager, long payout, double multiplier, Timestamp timestamp) throws RecordFailure {
    Connection conn = getDBConnection();
    try {
      PreparedStatement stat = conn.prepareStatement("INSERT INTO hiLoOutcome (uid, resultInt, hiLo, wager, payout, payoutMul, timestamp) values (?, ?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
      stat.setLong(1, uid);
      stat.setInt(2, resultInt);
      stat.setString(3, hiLo.toString());
      stat.setLong(4, wager);
      stat.setLong(5, payout);
      stat.setDouble(6, multiplier);
      stat.setString(7, LocalTimestamp.format(timestamp));
      stat.executeUpdate();
      ResultSet rs = stat.getGeneratedKeys();
      try {
        if (rs.next()) {
          long genOID = rs.getLong(1);
          HiLoRecord outcome = new HiLoRecord(genOID, uid, resultInt, hiLo, wager, payout, multiplier, timestamp);
          return outcome;
        }
        else {
          throw new RecordFailure("No generated key");
        }
      } finally {
        rs.close();
      }
    } catch( SQLException sqle ) {
      //may want to check for SQLITE_CONSTRAINT(19) here
      throw new RecordFailure(sqle);
    }
  }

  public MineRecord recordMineOutcome(MineRecord record) throws RecordFailure {
    return recordMineOutcome(record.uid, record.mineFractions, record.richness, record.yield, record.timestamp);
  }
  public MineRecord recordMineOutcome(long uid, int mineFractions, double richness, long yield, Timestamp timestamp) throws RecordFailure {
    Connection conn = getDBConnection();
    try {
      PreparedStatement stat = conn.prepareStatement("INSERT INTO mineOutcome (uid, mineFractions, richness, yield, timestamp) values (?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
      stat.setLong(1, uid);
      stat.setInt(2, mineFractions);
      stat.setDouble(3, richness);
      stat.setLong(4, yield);
      stat.setString(5, LocalTimestamp.format(timestamp));
      stat.executeUpdate();
      ResultSet rs = stat.getGeneratedKeys();
      try {
        if (rs.next()) {
          long genOID = rs.getLong(1);
          MineRecord outcome = new MineRecord(genOID, uid, mineFractions, richness, yield, timestamp);
          return outcome;
        }
        else {
          throw new RecordFailure("No generated key");
        }
      } finally {
        rs.close();
      }
    } catch( SQLException sqle ) {
      //may want to check for SQLITE_CONSTRAINT(19) here
      throw new RecordFailure(sqle);
    }
  }

  public TransferRecord recordTransfer(TransferRecord record) throws RecordFailure {
    return recordTransfer(record.uidSource, record.uidDestination, record.amount, record.timestamp);
  }
  public TransferRecord recordTransfer(long uidSource, long uidDestination, long amount, Timestamp timestamp) throws RecordFailure {
    Connection conn = getDBConnection();
    try {
      PreparedStatement stat = conn.prepareStatement("INSERT INTO transferRecord (uidSource, uidDest, amount, timestamp) values (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
      stat.setLong(1, uidSource);
      stat.setLong(2, uidDestination);
      stat.setLong(3, amount);
      stat.setString(4, LocalTimestamp.format(timestamp));
      stat.executeUpdate();
      ResultSet rs = stat.getGeneratedKeys();
      try {
        if (rs.next()) {
          long genOID = rs.getLong(1);
          TransferRecord outcome = new TransferRecord(genOID, uidSource, uidDestination, amount, timestamp);
          return outcome;
        }
        else {
          throw new RecordFailure("No generated key");
        }
      } finally {
        rs.close();
      }
    } catch( SQLException sqle ) {
      //may want to check for SQLITE_CONSTRAINT(19) here
      throw new RecordFailure(sqle);
    }
  }

  // Returns the sum total of all accounts on record
  public long getAccountTotal() throws RecordFailure {
    Connection conn = getDBConnection();
    try {
      PreparedStatement stat = conn.prepareStatement("SELECT sum(balance) AS total FROM bankAccount;");
      ResultSet rs = stat.executeQuery();
      try {
        if (rs.next()) {
          return rs.getLong("total");
        } else
          throw new RecordFailure("Could not get total");
      } finally {
        rs.close();
      }
    } catch( SQLException sqle ) {
      throw new RecordFailure(sqle);
    }
  }
}
