/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.checklistbank.service.mybatis.persistence.postgres;

    import java.sql.Array;
    import java.sql.CallableStatement;
    import java.sql.Connection;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.sql.SQLException;

    import org.apache.ibatis.type.BaseTypeHandler;
    import org.apache.ibatis.type.JdbcType;
    import org.apache.ibatis.type.MappedJdbcTypes;
    import org.apache.ibatis.type.MappedTypes;

/**
 * @author Manni Wood
 */
@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(int[].class)
public class IntegerArrayTypeHandler extends BaseTypeHandler<Integer[]> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i,
                                  Integer[] parameter, JdbcType jdbcType) throws SQLException {
    Connection c = ps.getConnection();
    Array inArray = c.createArrayOf("int", parameter);
    ps.setArray(i, inArray);
  }

  @Override
  public Integer[] getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    Array outputArray = rs.getArray(columnName);
    if (outputArray == null) {
      return null;
    }
    return (Integer[])outputArray.getArray();
  }

  @Override
  public Integer[] getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    Array outputArray = rs.getArray(columnIndex);
    if (outputArray == null) {
      return null;
    }
    return (Integer[])outputArray.getArray();
  }

  @Override
  public Integer[] getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    Array outputArray = cs.getArray(columnIndex);
    if (outputArray == null) {
      return null;
    }
    return (Integer[])outputArray.getArray();
  }
}