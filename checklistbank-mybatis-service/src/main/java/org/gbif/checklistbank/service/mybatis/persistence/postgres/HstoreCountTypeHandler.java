package org.gbif.checklistbank.service.mybatis.persistence.postgres;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Functions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

/**
 * A mybatis type handler that translates from the typed java.util.Map<String, Integer> to the
 * postgres hstore database type. Any non integer values in hstore are silently ignored.
 *
 * As we do not map all java map types to this mybatis handler apply the handler manually for the relevant hstore fields
 * in the mapper xml, for example see DatasetMetricsMapper.xml.
 */
public abstract class HstoreCountTypeHandler<KEY extends Comparable> extends BaseTypeHandler<Map<KEY, Integer>> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Map<KEY, Integer> parameter, JdbcType jdbcType)
    throws SQLException {
    ps.setString(i, HStoreConverter.toString(parameter));
  }

  @Override
  public Map<KEY, Integer> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return fromString(rs.getString(columnName));
  }

  @Override
  public Map<KEY, Integer> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return fromString(rs.getString(columnIndex));
  }

  @Override
  public Map<KEY, Integer> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return fromString(cs.getString(columnIndex));
  }

  private Map<KEY, Integer> fromString(String hstring) {
    HashMap<KEY, Integer> typedMap = Maps.newHashMap();
    if (!Strings.isNullOrEmpty(hstring)) {
      Map<String, String> rawMap = HStoreConverter.fromString(hstring);
      for (Map.Entry<String, String> entry : rawMap.entrySet()) {
        try {
          typedMap.put(toKey(entry.getKey()), Integer.parseInt(entry.getValue()));
        } catch (IllegalArgumentException e) {
          // ignore this entry
        }
      }
    }
    return sortMap(typedMap);
  }

  /**
   * Can be overridden to return sorted maps in custom manners.
   * By default sorts the map according to the count values in descending order.
   */
  protected Map<KEY, Integer> sortMap(HashMap<KEY, Integer> map) {
    return sortMapByValuesDesc(map);
  }

  @VisibleForTesting
  protected static <KEY extends Comparable> Map<KEY, Integer> sortMapByValuesDesc(HashMap<KEY, Integer> map) {
    final Ordering<KEY> reverseValuesAndNaturalKeysOrdering =
        Ordering.natural().reverse().nullsLast().onResultOf(Functions.forMap(map, null)) // natural for values
            .compound(Ordering.natural()); // secondary - natural ordering of keys
    return ImmutableSortedMap.copyOf(map, reverseValuesAndNaturalKeysOrdering);
  }

  protected abstract KEY toKey(String key) throws IllegalArgumentException;

}
