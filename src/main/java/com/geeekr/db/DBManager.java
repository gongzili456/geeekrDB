package com.geeekr.db;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().getClass());
    private static ThreadLocal<Connection> conns = new ThreadLocal<Connection>();
    private static DataSource dataSource;
    private static boolean show_sql = false;

    static {
        initDataSource(null);
    }

    private final static void initDataSource(Properties dbProperties) {
        try {
            if (dbProperties == null) {
                dbProperties = new Properties();
                dbProperties.load(DBManager.class.getClassLoader().getResourceAsStream("db.properties"));
            }

            Map<String, String> cp_pro = new HashMap<String, String>();
            for (Object key : dbProperties.keySet()) {
                String k = (String) key;
                if (k.startsWith("jdbc.")) {
                    String name = k.substring(5);
                    cp_pro.put(name, dbProperties.getProperty(k));
                    if ("show_sql".equalsIgnoreCase(name)) {
                        show_sql = "true".equalsIgnoreCase(dbProperties.getProperty(k));
                    }
                }
            }

            dataSource = (DataSource) Class.forName(cp_pro.get("datasource")).newInstance();
            if (dataSource.getClass().getName().indexOf("c3p0") > 0) {
                // Disable JMX in C3P0
                System.setProperty("com.mchange.v2.c3p0.management.ManagementCoordinator", "com.mchange.v2.c3p0.management.NullManagementCoordinator");
            }

            BeanUtils.populate(dataSource, cp_pro);

            Connection con = getConnection();
            DatabaseMetaData dm = con.getMetaData();
            LOGGER.info("Connect to :" + dm.getDatabaseProductName() + " " + dm.getDatabaseProductVersion());
            closeConnection();
        } catch (IOException | InstantiationException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | SQLException e) {
            throw new DBException(e);
        }
    }

    public final static Connection getConnection() throws SQLException {
        Connection con = conns.get();
        if (con == null || con.isClosed()) {
            con = dataSource.getConnection();
            conns.set(con);
        }
        return (show_sql && Proxy.isProxyClass(con.getClass())) ? new DebugConnection(con).getConnection() : con;
    }

    /**
     * close connection
     */
    public final static void closeConnection() {
        Connection con = conns.get();
        try {
            if (con != null && !con.isClosed()) {
                con.setAutoCommit(true);
                con.close();
            }
        } catch (SQLException e) {
            LOGGER.error("Unabled to close connection!!! ", e);
        }
        conns.set(null);
    }

    /**
     * close datasource
     * 
     * @return
     */
    public final static void closeDataSource() {
        try {
            dataSource.getClass().getMethod("close").invoke(dataSource);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            LOGGER.error("Unabled to close dataSource!!! ", e);
        }
    }

    static class DebugConnection implements InvocationHandler {

        private final static Logger LOG = LoggerFactory.getLogger(DebugConnection.class);
        private Connection connection = null;

        public DebugConnection(Connection conn) {
            this.connection = conn;
        }

        public Connection getConnection() {
            return (Connection) Proxy.newProxyInstance(connection.getClass().getClassLoader(), connection.getClass().getInterfaces(), this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("prepareStatement".equals(name) || "createStatement".equals(name)) {
                LOG.info("[SQL] >>> ", args[0]);
            }
            return method.invoke(connection, args);
        }
    }

}
