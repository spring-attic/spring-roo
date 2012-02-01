package org.springframework.roo.addon.jpa;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Provides information related to JDBC database configuration.
 * 
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public enum JdbcDatabase {

    DATABASE_DOT_COM("DATABASE.COM", "",
            "force://HOST_NAME;user=USER_NAME;password=PASSWORD"), DB2_400(
            "DB2_400", "com.ibm.as400.access.AS400JDBCDriver",
            "jdbc:as400://HOST_NAME"), DB2_EXPRESS_C("DB2_EXPRESS_C",
            "com.ibm.db2.jcc.DB2Driver", "jdbc:db2://HOST_NAME:50000"), DERBY_CLIENT(
            "DERBY_CLIENT", "org.apache.derby.jdbc.ClientDriver",
            "jdbc:derby://HOST_NAME:1527/TO_BE_CHANGED_BY_ADDON;create=true"), DERBY_EMBEDDED(
            "DERBY_EMBEDDED", "org.apache.derby.jdbc.EmbeddedDriver",
            "jdbc:derby:TO_BE_CHANGED_BY_ADDON;create=true"), FIREBIRD(
            "FIREBIRD", "org.firebirdsql.jdbc.FBDriver",
            "jdbc:firebirdsql://HOST_NAME:3050"), GOOGLE_APP_ENGINE("GAE", "",
            "appengine"), H2_IN_MEMORY("H2", "org.h2.Driver",
            "jdbc:h2:mem:TO_BE_CHANGED_BY_ADDON;DB_CLOSE_DELAY=-1"), HYPERSONIC_IN_MEMORY(
            "HYPERSONIC", "org.hsqldb.jdbcDriver",
            "jdbc:hsqldb:mem:TO_BE_CHANGED_BY_ADDON"), HYPERSONIC_PERSISTENT(
            "HYPERSONIC", "org.hsqldb.jdbcDriver",
            "jdbc:hsqldb:file:TO_BE_CHANGED_BY_ADDON;shutdown=true"), MSSQL(
            "MSSQL", "net.sourceforge.jtds.jdbc.Driver",
            "jdbc:jtds:sqlserver://HOST_NAME:1433/TO_BE_CHANGED_BY_ADDON"), MYSQL(
            "MYSQL", "com.mysql.jdbc.Driver", "jdbc:mysql://HOST_NAME:3306"), ORACLE(
            "ORACLE", "oracle.jdbc.OracleDriver",
            "jdbc:oracle:thin:@HOST_NAME:1521"), POSTGRES("POSTGRES",
            "org.postgresql.Driver", "jdbc:postgresql://HOST_NAME:5432"), SYBASE(
            "SYBASE", "net.sourceforge.jtds.jdbc.Driver",
            "jdbc:jtds:sybase://HOST_NAME:5000/TO_BE_CHANGED_BY_ADDON;TDS=4.2");

    private final String connectionString;
    private final String driverClassName;
    private final String key;

    /**
     * Constructor
     * 
     * @param key the internal name for this type of database (required)
     * @param driverClassName
     * @param connectionString the JDBC connection URL template for this type of
     *            database (required)
     */
    private JdbcDatabase(final String key, final String driverClassName,
            final String connectionString) {
        Validate.notBlank(connectionString, "Connection string is required");
        Validate.notBlank(key, "Key is required");
        this.connectionString = connectionString;
        this.driverClassName = driverClassName;
        this.key = key;
    }

    public String getConfigPrefix() {
        return "/configuration/databases/database[@id='" + key + "']";
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("name", name());
        builder.append("key", key);
        builder.append("driver class name", driverClassName);
        builder.append("connection string", connectionString);
        return builder.toString();
    }
}
