package jackli.deerseek.db.structure;

import java.sql.SQLException;

public class SqlType {

    public enum DataType {
        BOOL(0,1),
        BYTE(1,3),SHORT(2,5),INT(3,10),LONG(4,19),
        FLOAT(5,6),DOUBLE(6,12),
        STRING(7,10000000);

        int id;
        int colSize;

        DataType(int id,int colSize) {
            this.id = id;
            this.colSize = colSize;
        }

        public int getID() {
            return id;
        }
        public int getSize() {
            return colSize;
        }
    }

    private static void judge(Object a,Object b) throws SQLException {
        if (a instanceof Boolean) {
            if (!(b instanceof Boolean)) throw new SQLException("BOOL only can compare with BOOL.");
        }
    }

    public static Object convert(Object a) {
        Object ca;
        try {
            ca = typeConvert(a,DataType.LONG);
        } catch (Exception e) {
            try {
                ca = typeConvert(a,DataType.DOUBLE);
            } catch (Exception e2) {
                try {
                    if ("true".equals(a) || "false".equals(a) || "TRUE".equals(a) || "FALSE".equals(a)) ca = typeConvert(a,DataType.BOOL);
                    else ca = String.valueOf(a);
                } catch (Exception e3) {
                    ca = String.valueOf(a);
                }
            }
        }
        return ca;
    }

    private static Number expandNumber(Number a) {
        if (a instanceof Float || a instanceof Double) return a.doubleValue();
        return a.longValue();
    }

    public static int compare(Object a,Object b) throws SQLException {
        // 策略：优先数字比较，优先整数比较，再是bool

        judge(a,b);
        judge(b,a);

        // bool比较
        if (a instanceof Boolean && b instanceof Boolean) {
            boolean aa = (Boolean) a;
            boolean bb = (Boolean) b;
            if (aa == bb) return 0;
            if (aa) return 1;
            return -1;
        }

        // Number比较
        if (a instanceof Number && b instanceof Number) {
            a = expandNumber((Number) a);
            b = expandNumber((Number) b);
            if (a instanceof Double || b instanceof Double) {
                if (a instanceof Long) a = ((Number) a).doubleValue();
                if (b instanceof Long) b = ((Number) b).doubleValue();
                return ((Double) a).compareTo((Double) b);
            } else return ((Long) a).compareTo((Long) b);
        }

        // Number和String比较
        if (a instanceof Number) a = String.valueOf(a);
        if (b instanceof Number) b = String.valueOf(b);

        // String比较
        return ((String) a).compareTo((String) b);
    }

    public static boolean equals(Object a,Object b) throws SQLException {
        // Number比较
        if (a instanceof Number && b instanceof Number) {
            a = expandNumber((Number) a);
            b = expandNumber((Number) b);
            if (a instanceof Double || b instanceof Double) {
                if (a instanceof Long) a = ((Number) a).doubleValue();
                if (b instanceof Long) b = ((Number) b).doubleValue();
                return a.equals(b);
            } else a.equals(b);
        }

        // 其他比较
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    public static boolean or(Object a,Object b) throws SQLException {
        if (!(a instanceof Boolean && b instanceof Boolean)) throw new SQLException("BOOL only can calculate with BOOL.");
        return ((Boolean) a) || ((Boolean) b);
    }

    public static boolean and(Object a,Object b) throws SQLException {
        if (!(a instanceof Boolean && b instanceof Boolean)) throw new SQLException("BOOL only can calculate with BOOL.");
        return ((Boolean) a) && ((Boolean) b);
    }

    public static Object plus(Object a,Object b) throws SQLException {
        if (a instanceof Number && b instanceof Number) {
            a = expandNumber((Number) a);
            b = expandNumber((Number) b);
            if (a instanceof Double || b instanceof Double) {
                return ((Number) a).doubleValue() + ((Number) b).doubleValue();
            }
            return (Long) a + (Long) b;
        }
        if (a instanceof String || b instanceof String) {
            return String.valueOf(a) + b;
        }
        if (a instanceof Boolean && b instanceof Boolean) return or(a,b);

        throw new SQLException("`" + a + "` and `" + b + "` can't plus.");
    }

    public static Object minus(Object a,Object b) throws SQLException {
        if (a instanceof Number && b instanceof Number) {
            a = expandNumber((Number) a);
            b = expandNumber((Number) b);
            if (a instanceof Double || b instanceof Double) {
                return ((Number) a).doubleValue() - ((Number) b).doubleValue();
            }
            return (Long) a - (Long) b;
        }
        throw new SQLException("`" + a + "` and `" + b + "` can't minus.");
    }

    public static Object times(Object a,Object b) throws SQLException {
        if (a instanceof Number && b instanceof Number) {
            a = expandNumber((Number) a);
            b = expandNumber((Number) b);
            if (a instanceof Double || b instanceof Double) {
                return ((Number) a).doubleValue() * ((Number) b).doubleValue();
            }
            return (Long) a * (Long) b;
        }
        if (a instanceof Boolean && b instanceof Boolean) return and(a,b);

        throw new SQLException("`" + a + "` and `" + b + "` can't times.");
    }

    public static Object divide(Object a,Object b) throws SQLException {
        if (a instanceof Number && b instanceof Number) {
            a = expandNumber((Number) a);
            b = expandNumber((Number) b);
            if (a instanceof Double || b instanceof Double) {
                return ((Number) a).doubleValue() / ((Number) b).doubleValue();
            }
            return (Long) a / (Long) b;
        }

        throw new SQLException("`" + a + "` and `" + b + "` can't times.");
    }

    public static Object typeConvert(Object src, DataType type) throws Exception {
        if (src == null) return null;
        switch (type) {
            case BOOL:
                if (src instanceof Number) {
                    if (new Integer(1).equals(src)) return true;
                    else if (new Integer(0).equals(src)) return false;
                    else throw new Exception();
                }
                if (src instanceof Boolean) return src;
                return Boolean.valueOf(String.valueOf(src));
            case BYTE:
                if (src instanceof Byte) return src;
                return Byte.valueOf(String.valueOf(src));
            case SHORT:
                if (src instanceof Short) return src;
                return Short.valueOf(String.valueOf(src));
            case INT:
                if (src instanceof Integer) return src;
                return Integer.valueOf(String.valueOf(src));
            case LONG:
                if (src instanceof Long) return src;
                return Long.valueOf(String.valueOf(src));

            case FLOAT:
                if (src instanceof Float) return src;
                return Float.valueOf(String.valueOf(src));

            case DOUBLE:
                if (src instanceof Double) return src;
                return Double.valueOf(String.valueOf(src));

            case STRING:
                return String.valueOf(src);
        }
        throw new Exception();
    }

}
