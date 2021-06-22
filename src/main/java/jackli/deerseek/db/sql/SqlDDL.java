package jackli.deerseek.db.sql;

import jackli.deerseek.db.SingleConnection;
import jackli.deerseek.db.structure.Database;
import jackli.deerseek.db.structure.SqlType;
import jackli.deerseek.db.structure.Table;
import jackli.deerseek.db.structure.TableStructure;

import java.sql.SQLException;
import java.util.*;

public abstract class SqlDDL extends SQLBasic {

    protected SqlDDL(String sql,SingleConnection conn) {
        super(sql,conn);
    }

    @Override
    protected int create(String sql) throws SQLException {
        if (sql.toLowerCase().startsWith("table")) return createTable(sql.substring(5).trim());

        throw new SQLException("Syntax Error: Can't match operate `" + sql + "`.");
    }

    private int createTable(String sql) throws SQLException {
        String name = sql.substring(0,sql.indexOf("(")).trim();

        String[] line = sql.substring(sql.indexOf("(") + 1,sql.lastIndexOf(")")).split(",");

        for (int i = 0;i < line.length;i ++) {
            line[i] = line[i].trim();
            if ("".equals(line[i])) throw new SQLException("Column can not be empty.");
        }

        Table table = new Table(name, Table.TableType.TABLE);

        // 解析列属性
        for (int i = 0;i < line.length;i ++) {
            processCol(line[i],table);
        }
        conn.database.addTable(name,table);
        conn.updateFile("Query OK. Table `" + name + "` created.",true);
        return 1;
    }

    @Override
    protected Table show(String sql) throws SQLException {
        if (sql.toLowerCase().startsWith("tables")) return showTables();

        throw new SQLException("Syntax Error: Can't match operate `" + sql + "`.");
    }

    protected Table showTables() throws SQLException {
        Table res = new Table("Tables", Table.TableType.VIEW);
        Table.Column col = new Table.Column(SqlType.DataType.STRING,null);
        res.put("tables",col);

        List<Object> row;
        for (String name : database.getTableNames()) {
            row = new ArrayList<>();
            row.add(name);
            res.insert(row);
        }
        return res;
    }

    @Override
    protected int truncate(String sql) throws SQLException {
        if (sql.toLowerCase().startsWith("table")) return truncateTable(sql.substring(5).trim());

        return 0;
    }

    protected int truncateTable(String sql) throws SQLException {
        int ed = sql.indexOf(" ");
        String tableName = sql.substring(0,ed == -1 ? sql.length() : ed);
        Table table = getTable(tableName);
        int size = table.size();
        table.truncate();
        if (table.pk_set != null) table.pk_set.clear();
        return size;
    }

    @Override
    protected int drop(String sql) throws SQLException {
        if (sql.toLowerCase().startsWith("table")) return dropTable(sql.substring(5).trim());

        return 0;
    }

    protected int dropTable(String sql) throws SQLException {
        int ed = sql.indexOf(" ");
        String tableName = sql.substring(0,ed == -1 ? sql.length() : ed);
        return database.tables.remove(tableName) != null ? 1 : 0;
    }

    protected int alter(String sql) throws SQLException {
        if (sql.toLowerCase().startsWith("table")) return alterTable(sql.substring(5).trim());

        return 0;
    }

    protected int alterTable(String sql) throws SQLException {
        String tableName = sql.substring(0,sql.indexOf(" "));
        Table table = getTable(tableName);
        sql = sql.substring(sql.indexOf(" ") + 1).trim();
        if (sql.toLowerCase().startsWith("add")) return addColumn(sql.substring(3).trim(),table);
        if (sql.toLowerCase().startsWith("drop")) return dropColumn(sql.substring(4).trim(),table);

        return 0;
    }

    protected int addColumn(String sql,Table table) throws SQLException {
        TableStructure.Column col = table.get(processCol(sql,table));
        for (int i = 0;i < table.size();i ++) {
            col.data.add(null);
        }
        return 1;
    }

    protected int dropColumn(String sql,Table table) throws SQLException {
        int ed = sql.indexOf(" ");
        String colName;
        if (ed == -1) colName = sql;
        else colName = sql.substring(0,ed);

        if (table.get(colName) == null) throw new SQLException("No such column.");

        if (table.get(colName).isPrimaryKey()) {
            table.primaryKey = null;
            table.pk_set.clear();
            table.pk_set = null;
        }
        table.remove(colName);
        return 1;
    }

    protected String processCol(String line,Table table) throws SQLException {
        line = line + " ";
        // 解析列属性
        String col_title,type;
        SqlType.DataType dataType;
        Table.Constraint.Type constraintType = null;
        String con;
        int cur;
        int len,j;
        char t;
        Set<Table.Constraint> constraint;

        col_title = type = "";
        len = line.length();

        // Get title and type
        cur = 0;
        for (j = 0;j < len;j ++) {
            t = line.charAt(j);
            if (t == ' ')
            {
                if (line.charAt(j - 1) == ' ') continue;
                cur ++;
            } else if (cur == 0) {
                col_title += t;
            } else if (cur == 1) {
                type += t;
            } else break;
        }

//            System.out.println(line[i] + "\n\t" + title + " " + type + " " + j);
        // Check title
        if ("".equals(type) || "".equals(col_title)) throw new SQLException("Syntax Error: `" + line + "`.");
        // Check type
        type = type.toUpperCase();
        try {
            dataType = SqlType.DataType.valueOf(type);
        } catch (Exception e)
        {
            throw new SQLException("Type Error: Type `" + type + "` is not support.");
        }

        // Get constraints
        constraint = new HashSet<>();
        con = "";

        for (;j < len;j ++) {
            t = line.charAt(j);
            if (t == ' ')
            {
                if (line.charAt(j - 1) == ' ') continue;
                con = con.toUpperCase();

                if ("NOT".equals(con) || "PRIMARY".equals(con)) con += "_";
                else {
                    if ("".equals(con)) {
                        throw new SQLException("Syntax Error: `" + line + "`.");
                    } else if ("para".equals(con)) {

                    } else {
                        try {
                            constraintType = Table.Constraint.Type.valueOf(con);

                        } catch (Exception e) {
                            throw new SQLException("Constraint type Error: Type `" + con + "` is not support.");
                        }
                        if (constraintType == TableStructure.Constraint.Type.PRIMARY_KEY) {
                            table.primaryKey = col_title;
                            table.pk_set = new TreeSet<>();
                        }
                        constraint.add(new Table.Constraint(constraintType));
                    }

                    con = "";
                }
            } else con += t;
        }

        // Add to table
        table.put(col_title,new Table.Column(dataType,constraint));

        return col_title;
    }
}
