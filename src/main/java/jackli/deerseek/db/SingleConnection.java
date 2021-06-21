package jackli.deerseek.db;

import jackli.deerseek.db.sql.SQLAction;
import jackli.deerseek.db.structure.Database;
import jackli.deerseek.db.structure.Table;
import jackli.deerseek.util.ProjectConfig;

import java.io.*;
import java.sql.SQLException;

public class SingleConnection {

    File file;
    public Database database;
    public static String FILETYPE = "dsdb";

    public SingleConnection(String path) throws Exception{
        // path validation
        this.file = new File(path);
        sync();
        print("Open database `" + database.name + "` OK.");
    }

    public void sync() throws Exception
    {
        String path = file.getPath();
        if (!path.endsWith(FILETYPE)) throw new NoSuchFieldException("File type not support.");
        int s = path.lastIndexOf(File.separator);
        if (s != -1) {
            path = path.substring(s + 1);
        }
        path = path.substring(0,path.lastIndexOf("."));


        if (file.isDirectory()) throw new IOException(path + " Not a file! Abort.");
        if (!file.exists()) {
            System.out.println(path + " No such file! Creating...");
            database = Operates.createDatabase(path);
            updateFile("Create database `" + path + "` successfully.",true);
            return;
        }

        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(file));
            database = (Database) ois.readObject();
            ois.close();
        } catch (Exception e) {
            if (ois != null) ois.close();
            throw new IOException("Not a support file!");
        }
    }

    /**
     * 更新文件
     * @param msg 提示消息
     * @param write false - 无需磁盘操作 true - 写入
     * @throws IOException
     */
    public void updateFile(String msg,boolean write) throws SQLException {

        if (write) {
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(new FileOutputStream(file));
                oos.writeObject(database);
                oos.flush();
                oos.close();
            } catch (Exception e) {
                try {
                    if (oos != null) oos.close();
                } catch (Exception ignore) {}
                throw new SQLException(e.getMessage());
            }
        }

        print(msg);
    }

    public int executeUpdate(String sql) throws SQLException {
        sql = sql.trim();
        String sqls[] = sql.split(";");
        int cnt = 0;
        for (String i : sqls) {
            if ("".equals(i)) continue;
            cnt += new SQLAction(rescape(i),this).executeUpdate();
        }
        updateFile("Query OK, " + cnt + " row affected.",true);
        return cnt;
    }

    public Table executeQuery(String sql) throws SQLException {
        sql = sql.trim();
        if (sql.endsWith(";")) sql = sql.substring(0,sql.length() - 1);
        try {
            sync();
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
        return new SQLAction(sql,this).executeQuery();
    }

    public static void print(String msg) {
        if (ProjectConfig.getInstance().driver.debug) {
            System.out.println(msg);
        }
    }

    public void close() {

    }

    private String rescape(String str)
    {
        str = str.replaceAll("&com\\\\-",";");
        return str;
    }
}
