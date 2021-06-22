package jackli.deerseek.db.sql;

import jackli.deerseek.db.SingleConnection;
import jackli.deerseek.db.structure.Database;
import jackli.deerseek.db.structure.Table;
import jackli.deerseek.db.structure.TableStructure;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;

public class SQLAction extends SqlDML {

    public SQLAction(String sql,SingleConnection conn) {
        super(sql,conn);
    }

    public int executeUpdate() throws SQLException {
        if (sql.toLowerCase().startsWith("drop")) return drop(sql.substring(4).trim());
        if (sql.toLowerCase().startsWith("create")) return create(sql.substring(6).trim());
        if (sql.toLowerCase().startsWith("truncate")) return truncate(sql.substring(8).trim());
        if (sql.toLowerCase().startsWith("alter")) return alter(sql.substring(5).trim());

        int cnt = 0;

        // 开始操作
        SqlNode root = getNode();
        if (SqlKind.SELECT.equals(root.getKind())) return 0;

        // INSERT INTO
        if (SqlKind.INSERT.equals(root.getKind())) {
            insert((SqlInsert) root);
            cnt = 1;
        } else if (SqlKind.DELETE.equals(root.getKind())) {
            cnt = delete((SqlDelete) root);
        } else if (SqlKind.UPDATE.equals(root.getKind())) {
            cnt = update((SqlUpdate) root);
        }









        return cnt;
    }

    public Table executeQuery() throws SQLException {
        if (sql.toLowerCase().startsWith("show")) return show(sql.substring(4).trim());

        SqlNode root = getNode();
        if (!SqlKind.SELECT.equals(root.getKind())) return null;


        return select((SqlSelect) root);
    }
}
