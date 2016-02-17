package org.gbif.checklistbank.service.mybatis.postgres;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mybatis type handler that translates from typed java.util.Set<T> to typed postgres arrays.
 */
abstract class TextEnumSetTypeHandler<T extends Enum> extends BaseTypeHandler<Set<T>> {

  private static final Logger LOG = LoggerFactory.getLogger(TextEnumSetTypeHandler.class);
  private final Map<String, T> values = Maps.newHashMap();
  private final Class<T> clazz;

  private final static Splitter SPLIT_COMMA = Splitter.on(",").omitEmptyStrings().trimResults();
  private final static Joiner JOINER_COMMA = Joiner.on(",").skipNulls();

  public TextEnumSetTypeHandler(Class<T> clazz) {
    for (T val : clazz.getEnumConstants()) {
      this.values.put(val.name(), val);
    }
    this.clazz = clazz;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Set<T> parameter, JdbcType jdbcType) throws SQLException {
    String array = "{" + JOINER_COMMA.join(parameter) + "}";
    ps.setString(i, array);
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

  private Set<T> fromString(String text) {
    Set<T> set = Sets.newHashSet();
    if (!Strings.isNullOrEmpty(text) && text.length() > 2) {
      for (String val : SPLIT_COMMA.split(text.subSequence(1, text.length()-1))) {
        if (values.containsKey(val)) {
          set.add(values.get(val));
        } else {
          LOG.warn("No enum value {} found for type {}", val, clazz.getSimpleName());
        }
      }
    }
    return set;
  }

}
