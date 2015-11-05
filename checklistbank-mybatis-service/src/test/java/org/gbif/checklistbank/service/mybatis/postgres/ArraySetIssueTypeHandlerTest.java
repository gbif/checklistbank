package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.api.vocabulary.NameUsageIssue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArraySetIssueTypeHandlerTest {

    @Mock
    ResultSet rs;

    @Rule
    public ClbDbTestRule dbSetup = ClbDbTestRule.empty();

    @Test
    public void testConvert() throws Exception {
        Connection con = dbSetup.getConnection();
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