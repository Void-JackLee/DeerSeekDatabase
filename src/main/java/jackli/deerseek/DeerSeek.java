package jackli.deerseek;

import java.sql.DriverManager;

public class DeerSeek {
    public static void main(String[] args)
    {
        try {
            Class.forName("jackli.deerseek.jdbc.Driver");
            DriverManager.getConnection("jdbc:deer://127.0.0.1:1234");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
