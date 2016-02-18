package org.gbif.checklistbank.service.mybatis.postgres;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mybatis type handler that translates from typed java.util.Set<T> to typed postgres arrays.
 */
public abstract class ArraySetTypeHandler<T> extends BaseTypeHandler<Set<T>> {

  private static final Logger LOG = LoggerFactory.getLogger(ArraySetTypeHandler.class);
  private final String baseType;

  /**
   * @param baseType postgres base type of the postgres array
   */
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
      if (!Strings.isNullOrEmpty(n)) {
        for (String x : n.split(",")) {
          // remove potential quotes. These might even exist for non text datatypes
          // see https://github.com/pgjdbc/pgjdbc/issues/517#issuecomment-185647055
          try {
            T val = convert(StringUtils.strip(x, "\""));
            if (val != null) {
              set.add(val);
            }
          } catch (IllegalArgumentException e) {
            LOG.warn("Ignore invalid array element value {}. {}", x, e.getMessage());
          }
        }
      }
    }
    return set;
  }

  protected abstract T convert(String x) throws IllegalArgumentException;

}
