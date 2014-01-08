package org.gbif.checklistbank.service.mybatis.postgres;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mybatis type handler that translates from the typed java.util.Map<String, Integer> to the
 * postgres hstore database type. Any non integer values in hstore are silently ignored.
 *
 * As we do not map all java map types to this mybatis handler apply the handler manually for the relevant hstore fields
 * in the mapper xml, for example see DatasetMetricsMapper.xml.
 */
public abstract class ArraySetTypeHandler<T> extends BaseTypeHandler<Set<T>> {

  private static final Logger LOG = LoggerFactory.getLogger(ArraySetTypeHandler.class);
  private final String baseType;

  public ArraySetTypeHandler(String baseType) {
    this.baseType = baseType;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Set<T> parameter, JdbcType jdbcType)
    throws SQLException {
    Array array = ps.getConnection().createArrayOf(baseType, parameter.toArray());
    ps.setArray(i, array);
  }

  @Override
  public Set<T> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return fromString(rs.getString(columnName));
  }

  @Override
  public Set<T> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return fromString(rs.getString(columnIndex));
  }

  @Override
  public Set<T> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return fromString(cs.getString(columnIndex));
  }

  private Set<T> fromString(String array) {
    Set<T> set = Sets.newHashSet();
    if (!Strings.isNullOrEmpty(array)) {
      String n = array.substring(1, array.length()-1);
      for (String x : n.split(",")) {
        T val = convert(x);
        if (val != null) {
          set.add(val);
        }
      }
    }
    return set;
  }

  protected abstract T convert(String x);

}
