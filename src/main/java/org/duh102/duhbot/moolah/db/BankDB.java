package org.duh102.duhbot.moolah.db;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteConfig;

import org.duh102.duhbot.moolah.exceptions.*;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

public class BankDB {
  private static final String DEFAULT_DB = "moolah.db";
  private static final String MEMORY_DB = ":memory:";
  private static final String JDBC_PREFIX = "jdbc:sqlite:";
  private static ConcurrentHashMap<String, BankDB> instanceMap;
  private String myDBFile;
  private Connection connection = null;
  private DataSource datasource;
  static {
    instanceMap = new ConcurrentHashMap<>();
  }

  private BankDB(String dbFile) {
    myDBFile = dbFile;
    HikariConfig hConfig = new HikariConfig();
    hConfig.setDataSourceClassName("org.sqlite.SQLiteDataSource");
    hConfig.addDataSourceProperty("enforceForeignKeys", true);
    hConfig.setJdbcUrl(getDBURL());
    HikariDataSource hkds = new HikariDataSource(hConfig);
    datasource = hkds;
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
    BankDB db = getInstance(dbFile);
    return db;
  }
  public static BankDB getMemoryInstance() {
    return getInstance(MEMORY_DB);
  }

  public String getDBURL() {
    return JDBC_PREFIX + myDBFile;
  }

  public synchronized Connection makeDBConnection() throws InvalidEnvironment, InvalidDBConfiguration, DBAlreadyConnected {
    if( connection != null )
      throw new DBAlreadyConnected();
    try {
      // Create the Flyway instance and point it to the database
      Flyway flyway =
              Flyway.configure().dataSource(datasource).load();
      // Start the migration
      flyway.migrate();
      connection = datasource.getConnection();
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
