package org.duh102.duhbot.moolah.db;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.text.ParseException;

import org.duh102.duhbot.moolah.*;
import org.duh102.duhbot.moolah.db.dao.*;
import org.duh102.duhbot.moolah.db.migration.MigrationManager;
import org.sqlite.SQLiteConfig;

import org.duh102.duhbot.moolah.exceptions.*;

public class BankDB {
  private static final String DEFAULT_DB = "moolah.db";
  private static ConcurrentHashMap<String, BankDB> instanceMap;
  private String myDBFile;
  private Connection connection = null;
  static {
    instanceMap = new ConcurrentHashMap<>();
  }

  private BankDB(String dbFile) {
    myDBFile = dbFile;
  }

  private static BankDB getInstance(String dbFile) {
    BankDB instance;
    instance = instanceMap.get(dbFile);
    if( instance == null ) {
      instance = new BankDB(dbFile);
      instanceMap.put(dbFile, instance);
    }
    return instance;
  }

  public static BankDB getDBInstance() {
    return getInstance(DEFAULT_DB);
  }
  public static BankDB getDBInstance(String dbFile) {
    return getInstance(dbFile);
  }
  public static BankDB getMemoryInstance() {
    return getInstance(":memory:");
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
}
