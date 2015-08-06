package org.gbif.checklistbank.dbunit;

import org.gbif.checklistbank.service.mybatis.postgres.ClbDbTestRule;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseSequenceFilter;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.FilteredDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.filter.ITableFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Ignore;

/**
 * A simple dbunit export script to generate dbunit xml files for IT tests.
 * This script requires an existing checklistbank database with an allready imported checklist.
 * You can generate one by using the CLB commandline tool and the squirrel test dwc archive found in the test
 * resources.
 * <p/>
 * The generator creates two files. The main full db dump plus another one for the name_usage table alone
 * which needs to be sorted specifically to not break the relational integrity when reading in.
 * Please manually replace the name_usage entries in the main file with this sorted name_usage dump.
 */
@Ignore
public class TestFileGenerator {

  public static void main(String[] args) throws Exception {
    // database connection
    Class<?> driverClass = Class.forName("org.postgresql.Driver");
    // replace with your local db to be exported
    Connection jdbcConnection = DriverManager.getConnection("jdbc:postgresql://localhost/clb-test", "postgres", "");
    IDatabaseConnection connection = new DatabaseConnection(jdbcConnection);
    for (Map.Entry<String, Object> prop : ClbDbTestRule.DB_UNIT_CLB_PROPERTIES.entrySet()) {
      connection.getConfig().setProperty(prop.getKey(), prop.getValue());
    }

    // full database export, automatically ordering tables by foreign key constraints
    ITableFilter filter = new DatabaseSequenceFilter(connection);
    IDataSet fullDataSet = new FilteredDataSet(filter, connection.createDataSet());
    File outf = new File("checklistbank-mybatis-service/src/test/resources/dbunit/squirrels-full.xml");
    FlatXmlDataSet.write(fullDataSet, new FileOutputStream(outf));

    // partial database export
    QueryDataSet partialDataSet = new QueryDataSet(connection);
    partialDataSet.addTable("name_usage", "SELECT * FROM name_usage order by lft nulls last");
    FlatXmlDataSet.write(partialDataSet,
      new FileOutputStream("checklistbank-mybatis-service/src/test/resources/dbunit/name_usage.xml"));

  }
}
