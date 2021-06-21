package jackli.deerseek.db.sql;

import com.sun.org.apache.xpath.internal.operations.Bool;
import jackli.deerseek.db.SingleConnection;
import jackli.deerseek.db.structure.SqlType;
import jackli.deerseek.db.structure.Table;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlSelect;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;

public abstract class SqlLogicalClause extends SqlDDL {
    protected SqlLogicalClause(String sql, SingleConnection conn) {
        super(sql,conn);
    }

    private int _curRow = 0;
    private Table table;

    protected List<Integer> where(SqlNode where, Table table) throws SQLException {
        if (where == null) return null;
        this.table = table;

        List<Integer> idx = new ArrayList<>();

        int len = table.size();
        for (int i = 0;i < len;i ++) {
            _curRow = i;
            try {
//                System.out.println(cal(where));
                if (((Boolean) cal(where))) idx.add(i);
            } catch (Exception e) {
                if (e instanceof ClassCastException)
                    throw new SQLException("WHERE Clause can't process expression that the type of return result is not bool.");
                else throw e;
            }
        }

        return idx;
    }

    protected Object cal(SqlNode node) throws SQLException {
        if (node == null) throw new SQLException("Syntax Error, AST node is null.");

        switch (node.getKind()) {
            case EQUALS:
                return equals(node);
            case IS_NULL:
                return isnull(node);
            case AND:
                return and(node);
            case OR:
                return or(node);
            case PLUS:
                return plus(node);
            case MINUS:
                return minus(node);
            case TIMES:
                return times(node);
            case DIVIDE:
                return divide(node);
            case IDENTIFIER:
                return identifier(node);
            case LITERAL:
                return literal(node);
            case LESS_THAN:
                return lessThan(node);
            case LESS_THAN_OR_EQUAL:
                return lessThanEqual(node);
            case GREATER_THAN:
                return greaterThan(node);
            case GREATER_THAN_OR_EQUAL:
                return greaterThanEqual(node);
            case IN:
                throw new SQLFeatureNotSupportedException("Not support yet.");
            case SELECT:
                return getSingleValue(new SQLAction("",conn).select((SqlSelect) node));
        }

        return null;
    }

    private Object getSingleValue(Table table) throws SQLException {
        Object res = null;
        try {
            res = table.entrySet().iterator().next().getValue().data.get(0);
        } catch (Exception e) {

        }
        return res;
    }

    protected boolean equals(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
//        System.out.println(a.getClass());
//        System.out.println(b.getClass());
//        System.out.println("Judging " + a + " and " + b + ".");
        return SqlType.equals(a,b);
    }

    protected boolean isnull(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        return SqlType.equals(a,null);
    }

    private boolean lessThan(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
        return SqlType.compare(a,b) < 0;
    }

    private boolean lessThanEqual(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
        return SqlType.compare(a,b) <= 0;
    }

    private boolean greaterThan(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
        return SqlType.compare(a,b) > 0;
    }

    private boolean greaterThanEqual(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
        return SqlType.compare(a,b) >= 0;
    }


    protected boolean and(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
        return SqlType.and(a,b);
    }

    protected boolean or(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
        return SqlType.or(a,b);
    }

    protected Object plus(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
        return SqlType.plus(a,b);
    }

    protected Object minus(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
        return SqlType.minus(a,b);
    }

    protected Object times(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
        return SqlType.times(a,b);
    }

    protected Object divide(SqlNode node) throws SQLException {
        SqlBasicCall basicCall = (SqlBasicCall) node;
        Object a = cal(basicCall.operands[0]);
        Object b = cal(basicCall.operands[1]);
        return SqlType.divide(a,b);
    }

    protected Object identifier(SqlNode node) throws SQLException {
        String name = node.toString();
        if (table.get(name) == null) throw new SQLException("Column not found.");
        return table.get(name).data.get(_curRow);
    }

    protected Object literal(SqlNode node) {
        String raw = node.toString();
        if (raw.startsWith("'") && raw.endsWith("'")) return raw.substring(1,raw.length() - 1);
        return SqlType.convert(raw);
    }


}
