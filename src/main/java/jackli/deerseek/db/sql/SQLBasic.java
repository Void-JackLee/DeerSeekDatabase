package jackli.deerseek.db.sql;

import jackli.deerseek.db.SingleConnection;
import jackli.deerseek.db.structure.Database;
import jackli.deerseek.db.structure.Table;
import jackli.deerseek.db.structure.TableStructure;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SQLBasic {

    protected Database database;
    protected SingleConnection conn;
    protected String sql;
    protected SqlParser parser;


    protected SQLBasic(String sql,SingleConnection conn) {
        this.sql = sql.trim();
        this.conn = conn;
        this.database = conn.database;
        SqlParser.Config config = SqlParser.config()
                .withLex(Lex.MYSQL);
        this.parser = SqlParser.create(sql,config);
    }

    protected SqlNode getNode() throws SQLException {
        SqlParser.Config config = SqlParser.config()
                .withLex(Lex.MYSQL);
        SqlParser parser = SqlParser.create(sql,config);

        // 开始操作
        SqlNode root;
        try {
            root = parser.parseStmt();
        } catch (Exception e) {
            String msg = e.getMessage();
            throw new SQLException(msg.substring(0,msg.indexOf("\n")));
        }
        return root;
    }
    protected Table getTable(String tableName) throws SQLException {
        if (tableName.contains(".")) tableName = tableName.substring(tableName.lastIndexOf(".") + 1);

        Table table = database.tables.get(tableName);
        if (table == null) throw new SQLException("Table `" + tableName + "` not exists.");
        return table;
    }

    // DDL Begin
    protected abstract int create(String sql) throws Exception;

    protected abstract Table show(String sql) throws SQLException;

    protected abstract int truncate(String sql) throws SQLException;

    protected abstract int drop(String sql) throws SQLException;

    // DML Begin
    protected abstract void insert(SqlInsert insert) throws SQLException;

    protected abstract int delete(SqlDelete delete) throws SQLException;

    protected abstract Table select(SqlSelect select) throws SQLException;

    protected abstract int update(SqlUpdate update) throws SQLException;


}
