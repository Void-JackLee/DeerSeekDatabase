package jackli.deerseek.util;

import org.yaml.snakeyaml.Yaml;

import javax.xml.crypto.Data;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ProjectConfig {

    private static Map<String,Object> mp = new HashMap<>();
    private static ProjectConfig instance = new ProjectConfig();

    public Driver driver;
    public Database database;

    public static ProjectConfig getInstance() { return instance; }

    private ProjectConfig() {
        try {
            Yaml yaml = new Yaml();
            HashMap<String, Object> config = yaml.loadAs(new InputStreamReader(ProjectConfig.class.getClassLoader().getResourceAsStream("config.yml")), HashMap.class);

            driver = new Driver();
            database = new Database();

            driver.version = (String) ((Map)config.get("driver")).get("version");
            driver.debug = (Boolean) ((Map)config.get("driver")).get("debug");
            driver.name = (String) ((Map)config.get("driver")).get("name");

            database.name = (String) ((Map)config.get("database")).get("name");
            database.version = (String) ((Map)config.get("database")).get("version");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String,Object> getDriver(String key) {
        return (Map<String, Object>) mp.get("driver");
    }

    public static class Driver {
        public String version;
        public boolean debug;
        public String name;
    }

    public static class Database {
        public String name;
        public String version;
    }
}
