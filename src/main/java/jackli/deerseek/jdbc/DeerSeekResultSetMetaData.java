package jackli.deerseek.jdbc;

import jackli.deerseek.db.structure.Table;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class DeerSeekResultSetMetaData implements ResultSetMetaData {

    List<String> title;
    Table table;
    DeerSeekResultSet rs;

    public DeerSeekResultSetMetaData(DeerSeekResultSet rs) {
        this.rs = rs;
        this.title = rs.title;
        this.table = rs.table;
    }

    @Override
    public int getColumnCount() throws SQLException {
        return title.size();
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        return table.get(title.get(column - 1)).constraints.contains(new Table.Constraint(Table.Constraint.Type.AUTO_INCREMENT));
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        Set<Table.Constraint> con = table.get(title.get(column - 1)).constraints;
        return con.contains(new Table.Constraint(Table.Constraint.Type.AUTO_INCREMENT)) || con.contains(new Table.Constraint(Table.Constraint.Type.NOT_NULL)) ? columnNoNulls : columnNullable;
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        return false;
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        return table.size();
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        return title.get(column - 1);
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return title.get(column - 1);
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        return rs.statement.connection.databaseName;
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        return 0;
    }

    @Override
    public int getScale(int column) throws SQLException {
        return 0;
    }

    @Override
    public String getTableName(int column) throws SQLException {
        return table.title;
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        return null;
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        return table.get(title.get(column - 1)).type.getID();
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        return table.get(title.get(column - 1)).type.toString();
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        return false;
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        return table.get(title.get(column - 1)).type.toString();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
