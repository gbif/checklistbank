package org.gbif.checklistbank.service.mybatis.postgres;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
 * Adapter to handle conversion between Postgresql
 * hstore extension type and Strings.
 */
public class HstoreType extends AbstractDataType {

    /**
     * Logger for this class
     */
    private static final Logger logger = LoggerFactory.getLogger(HstoreType.class);

    public HstoreType() {
        super("hstore", Types.OTHER, String.class, false);
    }

    public Object getSqlValue(int column, ResultSet resultSet) throws SQLException, TypeCastException {
        return resultSet.getString(column);
    }

    public void setSqlValue(Object data, int column,
                            PreparedStatement statement) throws SQLException, TypeCastException {
        statement.setObject(column, getHstore(data, statement.getConnection()));
    }

    public Object typeCast(Object arg0) throws TypeCastException {
        return arg0.toString();
    }

    private Object getHstore(Object value, Connection connection) throws TypeCastException {

        logger.debug("getHstore(value={}, connection={}) - start", value, connection);

        Object obj = null;
        try {
            Class aPGObjectClass = super.loadClass("org.postgresql.util.PGobject", connection);
            Constructor ct = aPGObjectClass.getConstructor(null);
            obj = ct.newInstance(null);

            Method setTypeMethod = aPGObjectClass.getMethod("setType", new Class[]{String.class});
            setTypeMethod.invoke(obj, new Object[]{"hstore"});

            Method setValueMethod = aPGObjectClass.getMethod("setValue", new Class[]{String.class});
            setValueMethod.invoke(obj, new Object[]{value.toString()});

        } catch (ClassNotFoundException e) {
            throw new TypeCastException(value, this, e);
        } catch (InvocationTargetException e) {
            throw new TypeCastException(value, this, e);
        } catch (NoSuchMethodException e) {
            throw new TypeCastException(value, this, e);
        } catch (IllegalAccessException e) {
            throw new TypeCastException(value, this, e);
        } catch (InstantiationException e) {
            throw new TypeCastException(value, this, e);
        }

        return obj;
    }
}
