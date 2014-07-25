package com.geeekr.db;

import java.beans.PropertyDescriptor;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.ArrayUtils;

public class QueryHelper {
    private final static QueryRunner QUERY_RUNNER = new QueryRunner();

    private final static ColumnListHandler<Object> COLUMN_LIST_HANDLER = new ColumnListHandler<Object>() {

        @Override
        protected Object handleRow(ResultSet rs) throws SQLException {
            Object obj = super.handleRow(rs);
            if (obj instanceof BigInteger) {
                return ((BigInteger) obj).longValue();
            }

            return obj;
        }
    };
    private final static ScalarHandler<Object> SCALAR_HANDLER = new ScalarHandler<Object>() {

        @Override
        public Object handle(ResultSet rs) throws SQLException {
            Object obj = super.handle(rs);
            if (obj instanceof BigInteger) {
                return ((BigInteger) obj).longValue();
            }
            return obj;
        }

    };

    private final static BeanProcessor BEAN_PROCESSOR = new BeanProcessor() {
        @Override
        protected int[] mapColumnsToProperties(ResultSetMetaData rsmd, PropertyDescriptor[] props) throws SQLException {
            int cols = rsmd.getColumnCount();
            int[] columnToProperty = new int[cols + 1];
            Arrays.fill(columnToProperty, PROPERTY_NOT_FOUND);
            for (int col = 1; col <= cols; col++) {
                String columnName = rsmd.getColumnLabel(col);
                if (null == columnName || 0 == columnName.length()) {
                    columnName = rsmd.getColumnName(col);
                }
                String propertyName = Inflector.getInstance().camelName(columnName);
                if (propertyName == null) {
                    propertyName = columnName;
                }
                for (int i = 0; i < props.length; i++) {
                    if (propertyName.equalsIgnoreCase(props[i].getName())) {
                        columnToProperty[col] = i;
                        break;
                    }
                }
            }
            return columnToProperty;
        }
    };
    private final static BasicRowProcessor BASIC_ROW_PROCESSOR = new BasicRowProcessor(BEAN_PROCESSOR);

    @SuppressWarnings("serial")
    private final static List<Class<?>> PRIMITIVECLASSES = new ArrayList<Class<?>>() {
        {
            add(Integer.class);
            add(Long.class);
            add(String.class);
            add(Date.class);
            add(java.sql.Date.class);
            add(Timestamp.class);
        }
    };

    private final static boolean isPrimitive(Class<?> clazz) {
        return clazz.isPrimitive() || PRIMITIVECLASSES.contains(clazz);
    }

    public static Connection getConnection() {
        try {
            return DBManager.getConnection();
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    /**
     * 读取某个对象
     * 
     * @param clazz
     * @param sql
     * @param params
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T read(Class<T> clazz, String sql, Object... params) {
        try {
            return (T) QUERY_RUNNER.query(getConnection(), sql, isPrimitive(clazz) ? SCALAR_HANDLER : new BeanHandler(clazz, BASIC_ROW_PROCESSOR),
                    params);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    /**
     * 对象查询
     * 
     * @param clazz
     * @param sql
     * @param params
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> List<T> query(Class<T> clazz, String sql, Object... params) {
        try {
            return (List<T>) QUERY_RUNNER.query(getConnection(), sql, isPrimitive(clazz) ? COLUMN_LIST_HANDLER : new BeanHandler(clazz,
                    BASIC_ROW_PROCESSOR), params);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    /**
     * 分页查询
     * 
     * @param clazz
     * @param sql
     * @param page
     * @param count
     * @param params
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> List<T> queryPage(Class<T> clazz, String sql, int page, int count, Object... params) {
        if (page < 0 || count < 0) {
            throw new IllegalArgumentException("Illegal parameter of 'page' or 'count', Must be positive.");
        }
        int from = (page - 1) * count;
        count = (count > 0) ? count : Integer.MAX_VALUE;
        try {
            return (List<T>) QUERY_RUNNER.query(getConnection(), sql + "LIMIT ?,?", isPrimitive(clazz) ? COLUMN_LIST_HANDLER : new BeanHandler(clazz,
                    BASIC_ROW_PROCESSOR), ArrayUtils.addAll(params, new Integer[] { from, count }));
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    /**
     * 执行统计查询语句，语句的执行结果必须只返回一个数值
     * 
     * @param sql
     * @param params
     * @return
     */
    public static long stat(String sql, Object... params) {
        try {
            Number num = (Number) QUERY_RUNNER.query(getConnection(), sql, SCALAR_HANDLER, params);
            return num != null ? num.longValue() : -1;
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    /**
     * 执行INSERT/UPDATE/DELETE语句
     * 
     * @param sql
     * @param params
     * @return
     */
    public static int update(String sql, Object... params) {
        try {
            return QUERY_RUNNER.update(getConnection(), sql, params);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    /**
     * 批量执行指定的SQL语句
     * 
     * @param sql
     * @param params
     * @return
     */
    public static int[] batch(String sql, Object[][] params) {
        try {
            return QUERY_RUNNER.batch(getConnection(), sql, params);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }
}
