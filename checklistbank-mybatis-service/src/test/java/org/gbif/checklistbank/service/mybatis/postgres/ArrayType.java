package org.gbif.checklistbank.service.mybatis.postgres;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.dbunit.dataset.datatype.AbstractDataType;
import org.dbunit.dataset.datatype.TypeCastException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter to handle conversion between Postgresql arrays and Strings.
 * Only one dimensional arrays are supported and a single value is not allowed to have a semicolon as semicolons are
 * used to delimit the array in dbunit dumps.
 * This type can handle any primitive postgres type including custom enums.
 */
public class ArrayType extends AbstractDataType {

  private final String baseType;
  /**
   * Logger for this class
   */
  private static final Logger logger = LoggerFactory.getLogger(ArrayType.class);

  public ArrayType(String baseType) {
    super(baseType + "[]", Types.ARRAY, String.class, false);
    this.baseType = baseType;
  }

  public Object getSqlValue(int column, ResultSet resultSet) throws SQLException, TypeCastException {
    return resultSet.getString(column);
  }

  public void setSqlValue(Object data, int column, PreparedStatement statement) throws SQLException, TypeCastException {
    statement.setArray(column, getArray(data, statement.getConnection()));
  }

  public Object typeCast(Object arg0) throws TypeCastException {
    return arg0.toString();
  }

  private Array getArray(Object value, Connection connection) throws TypeCastException {

    logger.debug("getArray(value={}, connection={}) - start", value, connection);

    Array obj = null;
    try {
      obj = connection.createArrayOf(baseType, value.toString().split(";"));
    } catch (SQLException e) {
      throw new TypeCastException(value, this, e);
    }

    return obj;
  }

}
