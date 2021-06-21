package jackli.deerseek.db.sql;

import jackli.deerseek.db.SingleConnection;
import jackli.deerseek.db.structure.Database;
import jackli.deerseek.db.structure.SqlType;
import jackli.deerseek.db.structure.Table;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SqlDDL extends SQLBasic {

    protected SqlDDL(String sql,SingleConnection conn) {
        super(sql,conn);
    }

    @Override
    protected int create(String sql) throws SQLException {
        if (sql.startsWith("table")) return createTable(sql.substring(5).trim());

        throw new SQLException("Syntax Error: Can't match operate `" + sql + "`.");
    }

    private int createTable(String sql) throws SQLException {
        String name = sql.substring(0,sql.indexOf("(")).trim();

        String[] line = sql.substring(sql.indexOf("(") + 1,sql.lastIndexOf(")")).split(",");

        for (int i = 0;i < line.length;i ++) {
            line[i] = line[i].trim() + " ";
            if ("".equals(line[i])) throw new SQLException("Column can not be empty.");
        }

        Table table = new Table(name, Table.TableType.TABLE);

        // 解析列属性
        String col_title,type;
        SqlType.DataType dataType;
        Table.Constraint.Type constraintType = null;
        String con;
        int cur;
        int len,j;
        char t;
        Set<Table.Constraint> constraint;
        for (int i = 0;i < line.length;i ++) {
            col_title = type = "";
            len = line[i].length();

            // Get title and type
            cur = 0;
            for (j = 0;j < len;j ++) {
                t = line[i].charAt(j);
                if (t == ' ')
                {
                    if (line[i].charAt(j - 1) == ' ') continue;
                    cur ++;
                } else if (cur == 0) {
                    col_title += t;
                } else if (cur == 1) {
                    type += t;
                } else break;
            }

//            System.out.println(line[i] + "\n\t" + title + " " + type + " " + j);
            // Check title
            if ("".equals(type) || "".equals(col_title)) throw new SQLException("Syntax Error: `" + line[i] + "`.");
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
                t = line[i].charAt(j);
                if (t == ' ')
                {
                    if (line[i].charAt(j - 1) == ' ') continue;
                    con = con.toUpperCase();
                    if ("NOT".equals(con)) con += "_";
                    else {
                        if ("".equals(con)) {
                            throw new SQLException("Syntax Error: `" + line[i] + "`.");
                        } else if ("para".equals(con)) {

                        } else {
                            try {
                                constraintType = Table.Constraint.Type.valueOf(con);
                            } catch (Exception e) {
                                throw new SQLException("Constraint type Error: Type `" + con + "` is not support.");
                            }
                            constraint.add(new Table.Constraint(constraintType));
                        }

                        con = "";
                    }
                } else con += t;
            }

            // Add to table
            table.put(col_title,new Table.Column(dataType,constraint));
        }

        conn.database.addTable(name,table);
        conn.updateFile("Query OK. Table `" + name + "` created.",true);
        return 1;
    }

    @Override
    protected Table show(String sql) throws SQLException {
        if (sql.startsWith("tables")) return showTables();

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
        return size;
    }

    @Override
    protected int drop(String sql) throws SQLException {
        if (sql.startsWith("table")) return dropTable(sql.substring(5).trim());

        return 0;
    }

    protected int dropTable(String sql) throws SQLException {
        int ed = sql.indexOf(" ");
        String tableName = sql.substring(0,ed == -1 ? sql.length() : ed);
        return database.tables.remove(tableName) != null ? 1 : 0;
    }
}