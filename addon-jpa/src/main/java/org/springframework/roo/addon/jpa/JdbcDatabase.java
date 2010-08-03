package org.springframework.roo.addon.jpa;

import org.springframework.roo.support.style.ToStringCreator;

/**
 * Provides information related to JDBC database configuration.
 * 
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public enum JdbcDatabase {
	H2_IN_MEMORY("H2", "org.h2.Driver", "jdbc:h2:mem:TO_BE_CHANGED_BY_ADDON;DB_CLOSE_DELAY=-1"), 
	HYPERSONIC_IN_MEMORY("HYPERSONIC", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:TO_BE_CHANGED_BY_ADDON"), 
	HYPERSONIC_PERSISTENT("HYPERSONIC", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:${user.home}/TO_BE_CHANGED_BY_ADDON;shutdown=true"), 
	POSTGRES("POSTGRES", "org.postgresql.Driver", "jdbc:postgresql://localhost:5432"), 
	MYSQL("MYSQL", "com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306"), 
	ORACLE("ORACLE", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@localhost:1521"), 
	SYBASE("SYBASE", "com.sybase.jdbc2.jdbc.SybDriver", "jdbc:sybase:Tds:localhost:4100"), 
	MSSQL("MSSQL", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:microsoft:sqlserver://localhost:1433"), 
	DB2("DB2", "com.ibm.db2.jcc.DB2Driver", "jdbc:db2://localhost:50000"), 
	DB2400("DB2400", "com.ibm.as400.access.AS400JDBCDriver", "jdbc:as400://localhost"), 
	DERBY("DERBY", "org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:TO_BE_CHANGED_BY_ADDON;create=true"),
	GOOGLE_APP_ENGINE("GAE", "", "appengine");

	private String key;
	private String connectionString;
	private String driverClassName;

	private JdbcDatabase(String key, String driverClassName, String connectionString) {
		this.key = key;
		this.driverClassName = driverClassName;
		this.connectionString = connectionString;
	}

	public String getKey() {
		return key;
	}

	public String getDriverClassName() {
		return driverClassName;
	}
	
	public String getConnectionString() {
		return connectionString;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name());
		tsc.append("key", key);
		tsc.append("driver class name", driverClassName);
		tsc.append("connection string", connectionString);
		return tsc.toString();
	}
}
