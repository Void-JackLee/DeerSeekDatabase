package jackli.deerseek.jdbc;

import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import jackli.deerseek.DeerSeek;
import jackli.deerseek.util.ProjectConfig;
import org.yaml.snakeyaml.Yaml;

public class Driver implements java.sql.Driver {

    protected static String DRIVER_URL_SCHEMA = "jdbc:deer:";
    public static int DRIVER_VERSION_MAJOR;
    public static int DRIVER_VERSION_MINOR;
    public static String versionCode;
    private static boolean debug;

    static {
        try {
            // 获取版本号

            versionCode = ProjectConfig.getInstance().driver.version;
            debug = ProjectConfig.getInstance().driver.debug;

            // 计算对应的版本
            DRIVER_VERSION_MAJOR = getMajorVersionFromVersionCode(versionCode);
            DRIVER_VERSION_MINOR = getMinorVersionFromVersionCode(versionCode);

            if (debug) {
                System.out.println("DeerSeek Driver version " + versionCode + "(" + DRIVER_VERSION_MINOR + "." + DRIVER_VERSION_MAJOR + ")");
            }

            Driver driver = new Driver();
            DriverManager.registerDriver(driver);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int getMajorVersionFromVersionCode(String version) {
        return Integer.parseInt(version.substring(version.lastIndexOf('.') + 1)) + 1;
    }

    public static int getMinorVersionFromVersionCode(String version) {
        int m = 0;
        int len = version.lastIndexOf('.');
        char t;
        for (int i = 0;i < len;i ++) {
            t = version.charAt(i);
            if (t >= '0' && t <= '9') {
                m *= 10;
                m += t ^ 48;
            }
        }
        return m;
    }


    /**
     * 通过url连接
     * @param url 数据库的url
     * @param info 相关数据（用户名/密码）
     * @return
     * @throws SQLException
     */
    @Override
    public java.sql.Connection connect(String url, Properties info) throws SQLException {
        if (debug) {
            System.out.println("Connect by " + url);
        }
        if (acceptsURL(url))
        {
            try {
                return new DeerSeekConnection(url,info);
            } catch (Exception e) {
                throw new SQLException(e.getMessage());
            }
        }
        return null;
    }

    /**
     * 判断url是否被当前驱动支持
     * @param url 数据库url
     * @return 是否支持
     * @throws SQLException
     */
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith(DRIVER_URL_SCHEMA);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
//        throw new SQLException("11111");
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return DRIVER_VERSION_MAJOR;
    }

    @Override
    public int getMinorVersion() {
        return DRIVER_VERSION_MINOR;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
