package org.gbif.checklistbank.service.mybatis.postgres;


import java.io.File;
import java.sql.Connection;
import java.util.Map;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.zaxxer.hikari.HikariDataSource;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.dbunit.DefaultDatabaseTester;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * A TestRule for Database driven Integration tests.
 * This sets up Guice, Liquibase and DbUnit and exposes the guice injector and the optionally given main service class
 * for quick testing.
 * @param <T> Service interface to test
 */
public class DatabaseDrivenTestRule<T> implements TestRule {

  protected final Logger log = LoggerFactory.getLogger(getClass());
  private static final String[] LIQUIBASE_FILES = {"master.xml"};

  private Connection connection;
  private DefaultDatabaseTester databaseTester;
  protected DataSource dataSource;
  private final Class<T> serviceClass;
  private T service;
  private Injector injector;

  private final Module module;
  private final String dbUnitFileName;
  private final Map<String, Object> dbUnitProperties;
  private final String datasourceBindingName;

  /**
   * @param module                the Guice Module to use to create the exposed injector
   * @param datasourceBindingName the name of the named Datasource guice binding to use
   * @param serviceClass          the class for the service we want to wire up and test
   * @param dbUnitFileName        the optional unqualified filename within the dbUnit package to be used in setting up
   *                              the db
   */
  public DatabaseDrivenTestRule(Module module, String datasourceBindingName, @Nullable Class<T> serviceClass,
                                @Nullable String dbUnitFileName, Map<String, Object> dbUnitProperties) {
    this.dbUnitFileName = dbUnitFileName;
    this.serviceClass = serviceClass;
    this.module = module;
    this.dbUnitProperties = ImmutableMap.copyOf(dbUnitProperties);
    this.datasourceBindingName = datasourceBindingName;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        before();
        try {
          base.evaluate();
        } finally {
          after();
        }
      }
    };
  }

  private void before() throws Exception {
    SLF4JBridgeHandler.install();

    // create private guice module with properties passed to constructor
    injector = Guice.createInjector(module);

    Key<DataSource> regDatasourceKey = Key.get(DataSource.class, Names.named(datasourceBindingName));
    dataSource = injector.getInstance(regDatasourceKey);
    service = serviceClass == null ? null : injector.getInstance(serviceClass);

    connection = dataSource.getConnection();

    runLiquibase(connection, LIQUIBASE_FILES);
    runDbUnit(dataSource, dbUnitFileName);
    runFinally();
  }

  /**
   * Optional hook for subclasses to add any final db modifications after liquibase and dbunit have run.
   * The default implementation does nothing.
   */
  protected void runFinally() {

  }

  private void after() throws Exception {
    if (databaseTester != null) {
      databaseTester.onTearDown();
    }
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }

    // connection pools need to be properly closed!
    if (dataSource instanceof HikariDataSource) {
      ((HikariDataSource) dataSource).close();
    }
  }

  private void runDbUnit(DataSource dataSource, @Nullable String fileName) throws Exception {
    log.debug("Updating database with dbunit");
    if (fileName == null) {
      return;
    }

    // DbUnit
    IDatabaseConnection dbUnitConnection = new DatabaseDataSourceConnection(dataSource);
    for (Map.Entry<String, Object> prop : dbUnitProperties.entrySet()) {
      dbUnitConnection.getConfig().setProperty(prop.getKey(), prop.getValue());
    }
    databaseTester = new DefaultDatabaseTester(dbUnitConnection);

    FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
    builder.setColumnSensing(true);
    IDataSet dataSet = builder.build(Resources.getResource("dbunit" + File.separatorChar + fileName));

    databaseTester.setDataSet(dataSet);
    databaseTester.onSetup();
  }

  private void runLiquibase(Connection connection, String... fileNames) throws LiquibaseException {
    log.debug("Updating database with liquibase");
    for (String fileName : fileNames) {
      Liquibase liquibase =
        new Liquibase("liquibase" + File.separatorChar + fileName, new ClassLoaderResourceAccessor(),
          new JdbcConnection(connection));
      liquibase.update(null);
    }
  }

  /**
   * @return the service instance for T or null if no serviceClass was passed to the constructor
   */
  public T getService() {
    return service;
  }

  /**
   * @return the guice injector created in this rule. New instance for every test method being executed
   */
  public Injector getInjector() {
    return injector;
  }
}
