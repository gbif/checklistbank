package org.gbif.checklistbank.service.mybatis.postgres;

import org.gbif.common.parsers.core.ParseResult;
import org.gbif.common.parsers.date.DateParseUtils;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Type handler that reads a string db column and tries to convert ISO dates to real date instances.
 * If it fails simply null is returned and errors swallowed.
 */
public class StringDateTypeHandler extends BaseTypeHandler<Date> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType) throws SQLException {
    ps.setString(i, DateFormatUtils.ISO_DATE_FORMAT.format(parameter));
  }

  private Date string2Date(String x) {
    ParseResult<Date> parsed = DateParseUtils.parse(x);
    return parsed.isSuccessful() ? parsed.getPayload() : null;
  }

  @Override
  public Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return string2Date(rs.getString(columnName));
  }

  @Override
  public Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return string2Date(rs.getString(columnIndex));
  }

  @Override
  public Date getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return string2Date(cs.getString(columnIndex));
  }

}
