package com.geeekr.db;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.dbutils.DbUtils;

public class Model implements Serializable {

    private static final long serialVersionUID = -5785194584169295835L;
    private String _tableName;

    private Long _id = 0L;

    public Long getId() {
        return _id;
    }

    public void setId(Long _id) {
        this._id = _id;
    }

    protected String tableName() {
        if (_tableName == null) {
            _tableName = Inflector.getInstance().tableize(getClass());
        }
        return _tableName;
    }

    /**
     * 插入对象到数据库中
     * 
     * @return
     */
    public Long save() {
        if (getId() == null || getId() > 0) {
            insertObj(this);
        } else {
            setId(insertObj(this));
        }
        return getId();
    }

    /**
     * 分页查询列表
     * 
     * @param page
     * @param size
     * @return
     */
    public List<? extends Model> list(int page, int size) {
        String sql = "SELECT * FROM " + tableName() + " ORDER BY DESC";
        return QueryHelper.queryPage(getClass(), sql, page, size);
    }

    /**
     * 有条件的分页查询列表
     * 
     * @param where
     * @param page
     * @param size
     * @return
     */
    public List<? extends Model> list(String where, int page, int size) {
        String sql = "SELECT * FROM " + tableName() + " WHERE " + where + " ORDER BY DESC";
        return QueryHelper.queryPage(getClass(), sql, page, size);
    }

    /**
     * 统计此对象的记录总数
     * 
     * @param where
     * @return
     */
    public int totalCount(String where) {
        return (int) QueryHelper.stat("SELECT COUNT(*) FROM " + tableName() + " WHERE " + where);
    }

    /**
     * 根据主键id，删除对象
     * 
     * @return
     */
    public boolean delete() {
        return QueryHelper.update("DELETE FROM " + tableName() + " WHERE id = ?", getId()) == 1;
    }

    /**
     * 读取单个对象
     * 
     * @param id
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends Model> T get(Long id) {
        if (id <= 0) {
            return null;
        }
        return (T) QueryHelper.read(getClass(), "SELECT * FROM " + tableName() + " WHERE id = ?", id);
    }

    private static Long insertObj(Model model) {
        Map<String, Object> beanProp = model.ListInsertableFields();
        String[] fields = beanProp.keySet().toArray(new String[beanProp.size()]);
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(model.tableName());
        sql.append("(");
        for (int i = 0; i < fields.length; i++) {
            if (i == fields.length - 1) {
                sql.append(fields[i]);
            } else {
                sql.append(fields[i] + ",");
            }
        }
        sql.append(") VALUES(");
        for (int i = 0; i < fields.length; i++) {
            if (i == fields.length - 1) {
                sql.append("?");
            } else {
                sql.append("?, ");
            }
        }
        sql.append(")");

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {

            ps = QueryHelper.getConnection().prepareStatement(sql.toString(), PreparedStatement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < fields.length; i++) {
                ps.setObject(i + 1, beanProp.get(fields[i]));
            }
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getLong(1) : -1;
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            DbUtils.closeQuietly(rs);
            DbUtils.closeQuietly(ps);
            sql = null;
            fields = null;
            beanProp = null;
        }
    }

    protected Map<String, Object> ListInsertableFields() {
        Map<String, Object> map = new HashMap<String, Object>();
        BeanUtilsBean bub = BeanUtilsBean.getInstance();
        PropertyDescriptor[] descriptors = bub.getPropertyUtils().getPropertyDescriptors(this);
        Inflector ifc = Inflector.getInstance();
        Object o = null;
        Method m = null;
        try {
            for (PropertyDescriptor p : descriptors) {
                if ("id".equals(p.getName()) || "class".equals(p.getName())) {
                    continue;
                }
                m = bub.getPropertyUtils().getReadMethod(p);
                o = m.invoke(this);
                map.put(ifc.underscore(p.getName()), o);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return map;
    }
}
