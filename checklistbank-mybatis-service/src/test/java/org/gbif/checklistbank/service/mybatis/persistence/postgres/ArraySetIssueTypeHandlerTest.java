package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import org.gbif.api.vocabulary.NameUsageIssue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArraySetIssueTypeHandlerTest extends PostgresITBase {

  @Mock ResultSet rs;

  @Autowired
  public ArraySetIssueTypeHandlerTest(DataSource dataSource) {
    super(dataSource);
  }

  @Test
  public void testConvert() throws Exception {
    try (Connection con = dataSource.getConnection()) {
      try (Statement st = con.createStatement()) {
        st.execute("drop table if exists tt");
        st.execute("create table tt (annes text[])");
        st.execute("insert into tt values ('{RANK_INVALID,RANK_INVALID,CHAINED_SYNOYM}')");
        st.execute(
            "insert into tt values ('{\"RANK_INVALID\",\"RANK_INVALID\",\"CHAINED_SYNOYM\"}')");
        st.execute("select annes from tt");
        ArraySetIssueTypeHandler th = new ArraySetIssueTypeHandler();
        ResultSet rs = st.getResultSet();
        while (rs.next()) {
          Set<NameUsageIssue> res = th.getNullableResult(rs, 1);
          assertEquals(2, res.size());
          assertTrue(res.contains(NameUsageIssue.RANK_INVALID));
          assertTrue(res.contains(NameUsageIssue.CHAINED_SYNOYM));
        }
      }
    }
  }
}
