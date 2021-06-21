package jackli.deerseek.db;

import jackli.deerseek.db.structure.Database;
import jackli.deerseek.util.ProjectConfig;

public class Operates {
    public static Database createDatabase(String name) {
        return new Database(name);
    }
}
