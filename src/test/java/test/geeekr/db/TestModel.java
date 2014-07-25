package test.geeekr.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.dbutils.DbUtils;
import org.junit.Test;

import com.geeekr.db.DBException;
import com.geeekr.db.QueryHelper;

public class TestModel {

    @Test
    public void save() {
        Post p = new Post();
        p.setTitle("Hello title");
        p.setStatus("publish");
        p.setSlug("hello-title");
        p.setHtml("<a>Hello World</a>");
        p.setMarkdown("###Hello World");
        p.setCreatedAt(new Date());
        long l = p.save();
        System.out.println(l);
    }

    @Test
    public void save2() {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sql = " insert into tags(name, slug, created_at, created_by) values(?,?,?,?);";
        try {
            ps = QueryHelper.getConnection().prepareStatement(sql);
            ps.setObject(1, "java");
            ps.setObject(2, "java");
            ps.setObject(3, new Date());
            ps.setObject(4, 1L);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(ps);
        }
    }

    @Test
    public void query() {
        Post p = new Post();
        p = p.get(2L);
        System.out.println(">>>>>: " + p);
    }

    @Test
    public void update() {
        QueryHelper.update("update posts set status = ?, featured = ? where id = ?", "published", false, 2L);
    }

    @Test
    public void delete() {
        Post p = new Post();
        p.setId(2L);
        p.delete();
    }
}
