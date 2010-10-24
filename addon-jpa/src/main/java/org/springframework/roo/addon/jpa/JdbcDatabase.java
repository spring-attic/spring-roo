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
	HYPERSONIC_PERSISTENT("HYPERSONIC", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:TO_BE_CHANGED_BY_ADDON;shutdown=true"), 
	POSTGRES("POSTGRES", "org.postgresql.Driver", "jdbc:postgresql://HOST_NAME:5432"), 
	MYSQL("MYSQL", "com.mysql.jdbc.Driver", "jdbc:mysql://HOST_NAME:3306"), 
	ORACLE("ORACLE", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@HOST_NAME:1521"), 
	SYBASE("SYBASE", "net.sourceforge.jtds.jdbc.Driver", "jdbc:jtds:sybase://HOST_NAME:5000/TO_BE_CHANGED_BY_ADDON;TDS=4.2"), 
	MSSQL("MSSQL", "net.sourceforge.jtds.jdbc.Driver", "jdbc:jtds:sqlserver://HOST_NAME:1433/TO_BE_CHANGED_BY_ADDON"), 
	DB2("DB2", "com.ibm.db2.jcc.DB2Driver", "jdbc:db2://HOST_NAME:50000"), 
	DB2400("DB2400", "com.ibm.as400.access.AS400JDBCDriver", "jdbc:as400://HOST_NAME"), 
	DERBY("DERBY", "org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:TO_BE_CHANGED_BY_ADDON;create=true"),
	FIREBIRD("FIREBIRD", "org.firebirdsql.jdbc.FBDriver", "jdbc:firebirdsql://HOST_NAME:3050/"),
	GOOGLE_APP_ENGINE("GAE", "", "appengine"),
	VMFORCE("VMFORCE", "", "sfdc:${sfdc.endPoint}/services/Soap/u/${sfdc.apiVersion}");

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
