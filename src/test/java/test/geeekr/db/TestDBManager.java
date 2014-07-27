package test.geeekr.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.geeekr.db.QueryHelper;

public class TestDBManager {

    @Test
    public void testConnection() throws SQLException {
        Map<Connection, Integer> map = new HashMap<Connection, Integer>();
        Connection c1 = null;
        for (int i = 0; i < 40; i++) {
            c1 = QueryHelper.getConnection();
            map.put(c1, i);
            System.out.println(i + " >>> " + c1);
            // DBManager.closeConnection();
        }
        System.out.println(map.size());
        for (Connection k : map.keySet()) {
            System.out.println(k + " >>> " + map.get(k));
        }
    }
}
