package jackli.deerseek.jdbc;

import jackli.deerseek.DeerSeek;
import jackli.deerseek.db.SingleConnection;
import jackli.deerseek.db.structure.SqlType;
import jackli.deerseek.db.structure.Table;
import jackli.deerseek.db.structure.TableStructure;
import jackli.deerseek.util.ProjectConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;

public class DeerSeekMetaData implements DatabaseMetaData {

    private String url;
    private DeerSeekConnection conn;
    private SingleConnection dataConn;
    private String usrName;

    public final String TERM_SCHEMA = "database;";
    public final String TERM_PROCEDURE = "function";
    public final String TERM_CATALOG = "catalog";

    DeerSeekMetaData(String url,DeerSeekConnection conn) {
        this(url,conn,"local");
    }

    DeerSeekMetaData(String url,DeerSeekConnection conn,String usr) {
        this.url = url;
        this.conn = conn;
        this.dataConn = conn.conn;
        this.usrName = usr;
    }



    @Override
    public String getProcedureTerm() throws SQLException {
        return TERM_PROCEDURE;
    }

    @Override
    public String getCatalogTerm() throws SQLException {
        return TERM_CATALOG;
    }

    @Override
    public String getSchemaTerm() throws SQLException {
        return TERM_SCHEMA;
    }

    @Override
    public String getCatalogSeparator() throws SQLException {
        return ".";
    }

    @Override
    public String getURL() throws SQLException {
        return url;
    }

    @Override
    public String getUserName() throws SQLException {
        return usrName;
    }

    // ------------------------------------------------------------

    // 获取table的类型，典型的有TABLE和VIEW
    @Override
    public ResultSet getTableTypes() throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());

        Table table = new Table("table_type", Table.TableType.VIEW);

        Table.Column column = new Table.Column(SqlType.DataType.STRING,null);
        column.data = new ArrayList<>();
        column.data.add("TABLE");
        column.data.add("VIEW");

        table.put("TABLE_TYPE",column);

        DeerSeekResultSet rs = new DeerSeekResultSet(null);
        rs.setData(table);
        return rs;
    }

    // 获取目录（有哪些数据库）
    @Override
    public ResultSet getCatalogs() throws SQLException {
        Table table = new Table("catalog", Table.TableType.VIEW);

        Table.Column column = new Table.Column(SqlType.DataType.STRING,null);
        column.data = new ArrayList<>();
        column.data.add(usrName); // 由于本数据库采用文件形式，这边就直接丢入用户名了，scheme才是真正的数据库

        table.put("TABLE_CAT",column);

        DeerSeekResultSet rs = new DeerSeekResultSet(null);
        rs.setData(table);
        return rs;
    }

    @Override
    public ResultSet getSchemas() throws SQLException {
        return getSchemas(conn.databaseName,"%");
    }

    // 获取schema
    @Override
    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        Table table = new Table("schemas", Table.TableType.VIEW);

        Table.Column TABLE_SCHEM = new Table.Column(SqlType.DataType.STRING,null);
        TABLE_SCHEM.data = new ArrayList<>();
        table.put("TABLE_SCHEM",TABLE_SCHEM);

        Table.Column TABLE_CATALOG = new Table.Column(SqlType.DataType.STRING,null);
        TABLE_CATALOG.data = new ArrayList<>();
        table.put("TABLE_CATALOG",TABLE_CATALOG);

        table.insert(new String[]{conn.databaseName,"main"});

        DeerSeekResultSet rs = new DeerSeekResultSet(null);
        rs.setData(table);

        return rs;
    }

    // 获取有哪些Table
    @Override
    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
        Table table = new Table("tables", Table.TableType.VIEW);
        table.create(new String[]{"TABLE_CAT","TABLE_SCHEM","TABLE_NAME","TABLE_TYPE","REMARKS","TYPE_CAT","TYPE_SCHEM","TYPE_NAME","SELF_REFERENCING_COL_NAME","REF_GENERATION"},
                new SqlType.DataType[]{SqlType.DataType.STRING,SqlType.DataType.STRING,SqlType.DataType.STRING,SqlType.DataType.STRING,SqlType.DataType.STRING,
                        SqlType.DataType.STRING,SqlType.DataType.STRING,SqlType.DataType.STRING,SqlType.DataType.STRING,SqlType.DataType.STRING},null);

        for (String tableName : dataConn.database.getTableNames()) {
            table.insert(new String[]{catalog, schemaPattern,tableName,dataConn.database.tables.get(tableName).type.toString(),null,null,null,null,null,null});
        }

        DeerSeekResultSet rs = new DeerSeekResultSet(null);
        rs.setData(table);

        return rs;
    }

    // 获取列信息
    @Override
    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        Table colMeta = new Table("columns", Table.TableType.VIEW);
        colMeta.create(new String[]{"TABLE_CAT","TABLE_SCHEM","TABLE_NAME","COLUMN_NAME","DATA_TYPE",
                        "TYPE_NAME","COLUMN_SIZE","BUFFER_LENGTH","DECIMAL_DIGITS", "NUM_PREC_RADIX",
                        "NULLABLE","REMARKS","COLUMN_DEF","SQL_DATA_TYPE","SQL_DATETIME_SUB",
                        "CHAR_OCTET_LENGTH","ORDINAL_POSITION","IS_NULLABLE","SCOPE_CATALOG","SCOPE_SCHEMA",
                        "SCOPE_TABLE", "SOURCE_DATA_TYPE","IS_AUTOINCREMENT","IS_GENERATEDCOLUMN"},
                new SqlType.DataType[]{SqlType.DataType.STRING, SqlType.DataType.STRING, SqlType.DataType.STRING, SqlType.DataType.STRING, SqlType.DataType.INT,
                        SqlType.DataType.STRING, SqlType.DataType.INT, SqlType.DataType.INT, SqlType.DataType.INT, SqlType.DataType.INT,
                        SqlType.DataType.INT, SqlType.DataType.STRING, SqlType.DataType.STRING, SqlType.DataType.INT, SqlType.DataType.INT,
                        SqlType.DataType.INT, SqlType.DataType.INT, SqlType.DataType.STRING, SqlType.DataType.STRING, SqlType.DataType.STRING,
                        SqlType.DataType.STRING, SqlType.DataType.SHORT, SqlType.DataType.STRING, SqlType.DataType.STRING},
                null);

        Table table = dataConn.database.tables.get(tableNamePattern);
        Table.Column col;
        int i = 1;
        String name;
        for (Iterator<String> it = table.keySet().iterator(); it.hasNext();i ++) {
            name = it.next();
            col = table.get(name);
            colMeta.insert(new Object[]{catalog,schemaPattern,tableNamePattern,name,col.type.getID(),
                    col.type.toString(),col.type.getSize(),65535,null,col.type == SqlType.DataType.BOOL ? 2 : 10,
                    col.isNullable() ? columnNullable : columnNoNulls,null,null /*默认值，目前暂时不支持*/,null,null,
                    null /* for char types the maximum number of bytes in the column */,i,col.isNullable() ? "YES" : "NO",null,null,
                    null /* 外键表名 */,null /* 外键在那张表中的类型 */,col.isAutoIncrement(),"NO"});
        }


        DeerSeekResultSet rs = new DeerSeekResultSet(null);
        rs.setData(colMeta);
        return rs;
    }

    // 获取主键们
    @Override
    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        Table result = new Table("pk", Table.TableType.VIEW);
        result.create(new String[]{"TABLE_CAT","TABLE_SCHEM","TABLE_NAME","COLUMN_NAME","KEY_SEQ","PK_NAME"},
                new SqlType.DataType[]{SqlType.DataType.STRING, SqlType.DataType.STRING, SqlType.DataType.STRING, SqlType.DataType.STRING, SqlType.DataType.SHORT, SqlType.DataType.STRING},
                null);

        Table data = dataConn.database.tables.get(table);
        if (data.primaryKey != null) {
            result.insert(new Object[]{catalog,schema,table,data.primaryKey,1,null});
        }
        return new DeerSeekResultSet(null,result);
    }

    // -------------------------------------------------

    @Override
    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        if (ProjectConfig.getInstance().driver.debug) System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return new DeerSeekResultSet(null);
    }

    @Override
    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        if (ProjectConfig.getInstance().driver.debug) System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return new DeerSeekResultSet(null);
    }

    @Override
    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
        if (ProjectConfig.getInstance().driver.debug) System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return new DeerSeekResultSet(null);
    }

    @Override
    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
        if (ProjectConfig.getInstance().driver.debug) System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return new DeerSeekResultSet(null);
    }

    // 暂时不实现
    @Override
    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        if (ProjectConfig.getInstance().driver.debug) System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return new DeerSeekResultSet(null);
    }

    // 暂时不实现
    @Override
    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
        if (ProjectConfig.getInstance().driver.debug) System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return new DeerSeekResultSet(null);
    }

    // -------------------------------------------------

    @Override
    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    @Override
    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    @Override
    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    @Override
    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    @Override
    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    @Override
    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    // ------------------------------------------------------

    @Override
    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    @Override
    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    @Override
    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    // ------------------------------------------------------

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    @Override
    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        System.out.println(getClass().getName() + " " + Thread.currentThread().getStackTrace()[1].getMethodName());
        return null;
    }

    @Override
    public boolean allProceduresAreCallable() throws SQLException {
        return false;
    }

    @Override
    public boolean allTablesAreSelectable() throws SQLException {
        return false;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedHigh() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedLow() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedAtStart() throws SQLException {
        return false;
    }

    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException {
        return false;
    }

    @Override
    public String getDatabaseProductName() throws SQLException {
        return ProjectConfig.getInstance().database.name;
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
        return ProjectConfig.getInstance().database.version;
    }

    @Override
    public String getDriverName() throws SQLException {
        return ProjectConfig.getInstance().driver.name;
    }

    @Override
    public String getDriverVersion() throws SQLException {
        return Driver.versionCode;
    }

    @Override
    public int getDriverMajorVersion() {
        return Driver.DRIVER_VERSION_MAJOR;
    }

    @Override
    public int getDriverMinorVersion() {
        return Driver.DRIVER_VERSION_MINOR;
    }

    @Override
    public boolean usesLocalFiles() throws SQLException {
        return false;
    }

    @Override
    public boolean usesLocalFilePerTable() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return false;
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException {
        return null;
    }

    @Override
    public String getSQLKeywords() throws SQLException {
        return null;
    }

    @Override
    public String getNumericFunctions() throws SQLException {
        return null;
    }

    @Override
    public String getStringFunctions() throws SQLException {
        return null;
    }

    @Override
    public String getSystemFunctions() throws SQLException {
        return null;
    }

    @Override
    public String getTimeDateFunctions() throws SQLException {
        return null;
    }

    @Override
    public String getSearchStringEscape() throws SQLException {
        return null;
    }

    @Override
    public String getExtraNameCharacters() throws SQLException {
        return null;
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsColumnAliasing() throws SQLException {
        return false;
    }

    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsConvert() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsConvert(int fromType, int toType) throws SQLException {
        return false;
    }

    @Override
    public boolean supportsTableCorrelationNames() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOrderByUnrelated() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGroupBy() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGroupByUnrelated() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsLikeEscapeClause() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMultipleResultSets() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMultipleTransactions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsNonNullableColumns() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsANSI92FullSQL() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOuterJoins() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsFullOuterJoins() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException {
        return false;
    }

    @Override
    public boolean isCatalogAtStart() throws SQLException {
        return true;
    }

    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsPositionedDelete() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsPositionedUpdate() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSelectForUpdate() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsStoredProcedures() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInExists() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInIns() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsUnion() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsUnionAll() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        return false;
    }

    @Override
    public int getMaxBinaryLiteralLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxCharLiteralLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInGroupBy() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInIndex() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInOrderBy() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInSelect() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxColumnsInTable() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxConnections() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxCursorNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxIndexLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxSchemaNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxProcedureNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxCatalogNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxRowSize() throws SQLException {
        return 0;
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return false;
    }

    @Override
    public int getMaxStatementLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxStatements() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxTableNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxTablesInSelect() throws SQLException {
        return 0;
    }

    @Override
    public int getMaxUserNameLength() throws SQLException {
        return 0;
    }

    @Override
    public int getDefaultTransactionIsolation() throws SQLException {
        return 0;
    }

    @Override
    public boolean supportsTransactions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
        return false;
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        return false;
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        return false;
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsResultSetType(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
        return false;
    }

    @Override
    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean ownDeletesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean ownInsertsAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersDeletesAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean othersInsertsAreVisible(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean updatesAreDetected(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean deletesAreDetected(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean insertsAreDetected(int type) throws SQLException {
        return false;
    }

    @Override
    public boolean supportsBatchUpdates() throws SQLException {
        return false;
    }

    @Override
    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
        return null;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return conn;
    }

    @Override
    public boolean supportsSavepoints() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsNamedParameters() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsMultipleOpenResults() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException {
        return false;
    }



    @Override
    public boolean supportsResultSetHoldability(int holdability) throws SQLException {
        return false;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        return 0;
    }

    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        return 0;
    }

    @Override
    public int getJDBCMajorVersion() throws SQLException {
        return 0;
    }

    @Override
    public int getJDBCMinorVersion() throws SQLException {
        return 0;
    }

    @Override
    public int getSQLStateType() throws SQLException {
        return 0;
    }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException {
        return false;
    }

    @Override
    public boolean supportsStatementPooling() throws SQLException {
        return false;
    }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        return null;
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        return false;
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return false;
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return false;
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
