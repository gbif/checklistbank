package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.checklistbank.service.mybatis.guice.InternalChecklistBankServiceMyBatisModule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import javax.sql.DataSource;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArraySetIssueTypeHandlerTest {

  @Mock
  ResultSet rs;

  @Rule
  public DatabaseDrivenChecklistBankTestRule ddt = DatabaseDrivenChecklistBankTestRule.empty();

  private DataSource getDatasource() {
    Key<DataSource> regDatasourceKey = Key.get(DataSource.class, Names.named(InternalChecklistBankServiceMyBatisModule.DATASOURCE_BINDING_NAME));
    return ddt.getInjector().getInstance(regDatasourceKey);
  }

  @Test
  public void testConvert() throws Exception {
    DataSource ds = getDatasource();
    Connection con = ds.getConnection();
    Statement st = con.createStatement();
    st.execute("drop table if exists tt");
    st.execute("create table tt (annes text[])");
    st.execute("insert into tt values ('{RANK_INVALID,RANK_INVALID,CHAINED_SYNOYM}')");
    st.execute("select annes from tt");
    ArraySetIssueTypeHandler th = new ArraySetIssueTypeHandler();
    ResultSet rs = st.getResultSet();
    rs.next();
    Set<NameUsageIssue> res = th.getNullableResult(rs, 1);
    assertEquals(2, res.size());
    assertTrue(res.contains(NameUsageIssue.RANK_INVALID));
    assertTrue(res.contains(NameUsageIssue.CHAINED_SYNOYM));
  }

}