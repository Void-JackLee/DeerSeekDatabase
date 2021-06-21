package jackli.deerseek.db.structure;

import java.io.Serializable;
import java.util.*;

public class Database implements Serializable {
    public String name;
    public Map<String,Table> tables;

    public Database(String name) {
        this.name = name;
        tables = new LinkedHashMap<>();
    }

    public Set<String> getTableNames() {
        return tables.keySet();
    }
    public void addTable(String name,Table table) {
        tables.put(name,table);
    }
}
