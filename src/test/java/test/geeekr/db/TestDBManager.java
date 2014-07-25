package test.geeekr.db;

import java.sql.SQLException;

import org.junit.Test;

import com.geeekr.db.DBManager;

public class TestDBManager {

    @Test
    public void testConnection() throws SQLException {
        DBManager.getConnection();
    }

}
