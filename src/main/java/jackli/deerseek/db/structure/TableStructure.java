package jackli.deerseek.db.structure;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

public abstract class TableStructure extends LinkedHashMap<String, TableStructure.Column> {
    public String title;
    public Table.TableType type;
    public String sql;

    public String primaryKey; // TODO: 实现
    public Set<Object> pk_set;

    public TableStructure(String title, Table.TableType type) throws SQLException {
        if (title.contains(".")) throw new SQLException("`.` can't appear in title.");
        if (title.contains("`")) throw new SQLException("``` can't appear in title.");
        this.title = title;
        this.type = type;
        this.primaryKey = null;
        this.pk_set = null;
    }

    public void setPrimaryKey(String colName) {
        this.primaryKey = colName;
    }
    public String getPrimaryKeyColName() {
        return primaryKey;
    }

    public int filedSize() {
        return super.size();
    }

    @Override
    public int size() {
        if (filedSize() == 0) return 0;
        return get(keySet().iterator().next()).data.size();
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }

    public static class Column implements Serializable {

        public Set<Constraint> constraints;
        public SqlType.DataType type;

        public List<Object> data;

        public Column(SqlType.DataType type,Set<Constraint> constraints) {
            data = new ArrayList<>();
            this.type = type;
            this.constraints = constraints;
        }

        public boolean isNullable() {
            if (constraints == null) return true;
            return !constraints.contains(new Constraint(Constraint.Type.NOT_NULL)) && !isPrimaryKey();
        }

        long curIncrement = 1;

        public boolean isAutoIncrement() {
            if (constraints == null) return false;
            return constraints.contains(new Constraint(Constraint.Type.AUTO_INCREMENT));
        }

        public boolean isPrimaryKey() {
            if (constraints == null) return false;
            return constraints.contains(new Constraint(Constraint.Type.PRIMARY_KEY));
        }

        @Override
        public String toString() {
            return "Column{" +
                    "constraints=" + constraints +
                    ", type=" + type +
                    ", data=" + data +
                    '}';
        }
    }



    public static class Constraint implements Serializable {
        public enum Type {
            NULL("NULL"), // default
            NOT_NULL("NOT NULL"),
            AUTO_INCREMENT("AUTO_INCREMENT"),
            PRIMARY_KEY("PRIMARY KEY");

            String name;

            Type(String s) { this.name = s; }

            @Override
            public String toString() {
                return name;
            }
        }

        public Type type;

        public Constraint(Type t) {
            type = t;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Constraint that = (Constraint) o;
            return type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type);
        }

        @Override
        public String toString() {
            return "Constraint{" +
                    "type=" + type +
                    '}';
        }
    }

    public enum TableType {
        TABLE,
        VIEW
    }
}
