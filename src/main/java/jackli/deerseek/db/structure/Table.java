package jackli.deerseek.db.structure;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

public class Table extends TableStructure implements Serializable {

    public Table(String title, Table.TableType type) throws SQLException {
        super(title, type);
    }

    public Object judgeConstraintWhenAdd(Object data,Column col) throws SQLException {
        if (data == null && !col.isAutoIncrement()) {
            if (!col.isNullable()) throw new SQLException("Constraint ERROR: Column can't be null!");
        }
        if (col.isAutoIncrement()) {
            if (data != null && Long.parseLong(String.valueOf(data)) != col.curIncrement) throw new SQLException("Constraint ERROR: Auto Increment column value does not match!");
            if (data == null) data = col.curIncrement;
        }
        return data;
    }

    public void insert(List<Object> row) throws SQLException {
        if (row.size() != filedSize()) throw new SQLException("Illegal argument size. " + row.size() + " not match " + filedSize());
        int i = 0;
        Object data;
        Column col;
        List<Object> ans = new ArrayList<>();
        for (Iterator<String> it = keySet().iterator(); it.hasNext(); i ++) {
            col = get(it.next());
            try {
                data = SqlType.typeConvert(row.get(i), col.type);
            } catch (Exception e) {
                throw new SQLException(row.get(i) + " Can't convert to " + col.type);
            }
            data = judgeConstraintWhenAdd(data,col);
            ans.add(data);
        }

        i = 0;
        for (Iterator<String> it = keySet().iterator(); it.hasNext(); i ++) {
            col = get(it.next());
            col.data.add(ans.get(i));
            if (col.isAutoIncrement()) col.curIncrement ++;
        }

    }

    public void insert(Object[] row) throws SQLException {
        insert(Arrays.asList(row));
    }

    public void add(String col,Object obj) throws SQLException {
        Column c = get(col);
        if (c == null) throw new SQLException("No such column: `" + col + "`.");
        c.data.add(obj);
    }

    public void create(String[] colName, SqlType.DataType[] type,Set<Constraint>[] con) throws SQLException{
        if (con == null) create(Arrays.asList(colName), Arrays.asList(type), null);
        else create(Arrays.asList(colName), Arrays.asList(type), Arrays.asList(con));
    }

    public void create(List<String> colName, List<SqlType.DataType> type,List<Set<Constraint>> con) throws SQLException {
        clear();
        int len = colName.size();
        if (type.size() != len) throw new SQLException("Size not match.");
        if (con != null && con.size() != len) throw new SQLException("Size not match.");
        Column col;
        for (int i = 0;i < len;i ++) {
            if (con == null) col = new Column(type.get(i),null);
            else col = new Column(type.get(i),con.get(i));
            col.data = new ArrayList<>();
            put(colName.get(i),col);
        }
    }

    public void delete(int i) {
        for (Map.Entry<String,Column> en : entrySet()) {
            en.getValue().data.remove(i);
        }
    }

    public void deleteAll() {
        for (Map.Entry<String,Column> en : entrySet()) {
            en.getValue().data.clear();
        }
    }

    public void truncate() {
        for (Map.Entry<String,Column> en : entrySet()) {
            en.getValue().data.clear();
            en.getValue().curIncrement = 1;
        }
    }

    public void update(String colName,int idx,Object val) throws SQLException {
        Column col = get(colName);
        if (col == null) throw new SQLException("Column `" + colName + "` not found.");
        List<Object> data = col.data;

        try {
            val = SqlType.typeConvert(val, col.type);
        } catch (Exception e) {
            throw new SQLException(val + " Can't convert to " + col.type);
        }

        data.set(idx,val);
    }

}
