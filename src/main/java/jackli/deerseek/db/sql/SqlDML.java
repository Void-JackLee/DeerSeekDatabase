package jackli.deerseek.db.sql;

import jackli.deerseek.db.SingleConnection;
import jackli.deerseek.db.structure.Database;
import jackli.deerseek.db.structure.SqlType;
import jackli.deerseek.db.structure.Table;
import jackli.deerseek.db.structure.TableStructure;
import org.apache.calcite.sql.*;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;

public abstract class SqlDML extends SqlLogicalClause {
    protected SqlDML(String sql, SingleConnection conn) {
        super(sql,conn);
    }

    @Override
    public Table select(SqlSelect select) throws SQLException {
        if (select.getFrom() == null && select.getWhere() == null) {
            List<SqlNode> sqlNodes = select.getSelectList();
            if (sqlNodes.size() == 1 && "tables".equalsIgnoreCase(sqlNodes.get(0).toString())) return showTables();
            throw new SQLException("Can't SELECT " + sqlNodes + ".");
        }

        // 获取表名
        String tableName = null;
        SqlNode from = select.getFrom();
        String fromString;
        try {
            fromString = from.toString();
        } catch (Exception e) {
            throw new SQLFeatureNotSupportedException("Only support single table query.");
        }

        if (SqlKind.IDENTIFIER.equals(from.getKind())){
            tableName = from.toString();
        } else {
            if (SqlKind.AS.equals(from.getKind())) {
                tableName = ((SqlBasicCall) from).operands[0].toString();
            }
        }
        if (tableName == null) throw new SQLFeatureNotSupportedException("Not support for statement: " + fromString);
        Table table = getTable(tableName);

//        System.out.println("tableName: " + tableName);

        // 获取选择的列表
        List<String> cols = new ArrayList<>();
        String t;
        for (SqlNode col : select.getSelectList()) {
            t = col.toString();
            cols.add(t.substring(t.lastIndexOf(".") + 1));
        }
//        System.out.println(cols);

        if (cols.size() == 1 && "*".equals(cols.get(0))) {
            cols.clear();
            cols.addAll(table.keySet());
        }

        List<SqlType.DataType> types = new ArrayList<>();
        List<Set<TableStructure.Constraint>> cons = new ArrayList<>();
        TableStructure.Column col;
        for (String i : cols) {
            col = table.get(i);
            if (col == null) throw new SQLException("Column `" + i + "` can't find.");
            types.add(col.type);
            cons.add(col.constraints);
        }

        Table res = new Table(table.title,table.type);
        res.create(cols,types,cons);

        // 处理where子句
        List<Integer> ac = where(select.getWhere(),table);
        if (ac == null) {
            // 全表查询
            int len = table.size();
            for (String colName : cols) {
                for (int i = 0;i < len;i ++) {
                    res.add(colName,table.get(colName).data.get(i));
                }
            }
        } else {
            int len = ac.size();
            for (String colName : cols) {
                for (int i = 0;i < len;i ++) {
                    res.add(colName,table.get(colName).data.get(ac.get(i)));
                }
            }
        }

        return res;
    }

    @Override
    protected void insert(SqlInsert insert) throws SQLException {
        // 解析列名
        SqlNodeList colNodes = insert.getTargetColumnList();
        List<String> cols = new ArrayList<>();
        if (colNodes != null) {
            for (SqlNode i : colNodes) {
                cols.add(i.toString());
            }
        }

        // 获取表并检查列名是否match
        String tableName = insert.getTargetTable().toString();
        if (tableName.contains(".")) tableName = tableName.substring(tableName.lastIndexOf(".") + 1);
        Table table = database.tables.get(tableName);
        if (table == null) throw new SQLException("No such table `" + tableName + "`.");
        if (cols.size() > table.filedSize()) throw new SQLException("Column size mismatch. Required is " + table.filedSize() + " but actual is " + cols.size() + ".");
        for (String i : cols) {
            if (table.get(i) == null) {
                throw new SQLException("No such column: `" + i + "`.");
            }
        }

        // 解析列数据
        String data = ((SqlBasicCall) insert.getSource()).operand(0).toString().substring(4);
        data = data.substring(0,data.length() - 1);
        String[] values = data.split(", ");
        for (int i = 0;i < values.length;i ++) {
            if ("NULL".equals(values[i])) values[i] = null;
            else if (values[i].startsWith("'") && values[i].endsWith("'")) values[i] = values[i].substring(1,values[i].length() - 1);
        }
        if (cols.size() != 0 && values.length != cols.size()) throw new SQLException("The number of argument mismatched. Required is " + cols.size() + " but actual is " + values.length + ".");
        if (cols.size() == 0 && values.length != table.filedSize()) throw new SQLException("The number of argument mismatched. Required is " + table.filedSize() + " but actual is " + values.length + ".");

        // 插入数据
        Object[] ins = new Object[table.filedSize()];

        int len;

        if (cols.size() == 0) {
            len = table.filedSize();
            for (int i = 0;i < len;i ++) {
                ins[i] = values[i];
            }
        } else {
            Map<String, Integer> mp = new HashMap<>();
            int i = 0;
            for (Iterator<String> it = table.keySet().iterator(); it.hasNext(); i++) {
                mp.put(it.next(), i);
            }

            len = cols.size();
            for (i = 0; i < len; i++) {
                ins[mp.get(cols.get(i))] = values[i];
            }
        }

        table.insert(ins);
    }

    @Override
    protected int delete(SqlDelete delete) throws SQLException {
        String tableName = delete.getTargetTable().toString();
        tableName = tableName.substring(tableName.lastIndexOf(".") + 1);
        Table table = getTable(tableName);

        SqlNode where = delete.getCondition();

        List<Integer> ac = where(where,table);
        int cnt = 0;
        if (ac == null) {
            // 全表删除😱
            cnt = table.size();
            table.deleteAll();
        } else {
            int already = 0;
            cnt = ac.size();
            for (Integer i : ac) {
                table.delete(i - already ++);
            }
        }
        return cnt;
    }

    @Override
    protected int update(SqlUpdate update) throws SQLException {

        Table table = getTable(update.getTargetTable().toString());
//        System.out.println(table);

        Set<String> cols = new LinkedHashSet<>();
        String name;
        for (SqlNode node : update.getTargetColumnList()) {
            name = node.toString();
            if (table.get(name) == null) throw new SQLException("Column `" + name + "` not found.");
            if (cols.contains(name)) throw new SQLException("Column `" + name + "` duplicated.");
            cols.add(name);
        }

        List<Object> vals = new ArrayList<>();
        for (SqlNode node : update.getSourceExpressionList()) {
            vals.add(cal(node));
        }

        if (vals.size() != cols.size()) throw new SQLException("Unexpected ERROR.");

        SqlNode where = update.getCondition();
        List<Integer> ac = where(where,table);
        if (ac == null) {
            // 全部替换😱
            int len = table.size();
            int j;
            for (int i = 0;i < len;i ++) {
                j = 0;
                for (Iterator<String> it = cols.iterator();it.hasNext();j ++) {
                    table.update(it.next(),i,vals.get(j));
                }
            }
            return table.size();
        } else {
            int j;
            for (Integer integer : ac) {
                j = 0;
                for (Iterator<String> it = cols.iterator(); it.hasNext(); j++) {
                    table.update(it.next(), integer, vals.get(j));
                }
            }
            return ac.size();
        }
    }
}
