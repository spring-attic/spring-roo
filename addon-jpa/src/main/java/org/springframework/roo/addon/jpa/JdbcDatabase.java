package org.springframework.roo.addon.jpa;

import org.springframework.roo.support.style.ToStringCreator;

/**
 * Provides information related to JDBC database configuration.
 *
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public enum JdbcDatabase  {
	H2_IN_MEMORY("org.h2.Driver", "jdbc:h2:mem:TO_BE_CHANGED_BY_ADDON;DB_CLOSE_DELAY=-1"),
	HYPERSONIC_IN_MEMORY("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:TO_BE_CHANGED_BY_ADDON"),
	HYPERSONIC_PERSISTENT("org.hsqldb.jdbcDriver", "jdbc:hsqldb:${user.home}/TO_BE_CHANGED_BY_ADDON;shutdown=true"),
	POSTGRES("org.postgresql.Driver", "jdbc:postgresql://localhost:5432"),
	MYSQL("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306"),
	ORACLE("oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@localhost:1521"),
	SYBASE("com.sybase.jdbc2.jdbc.SybDriver", "jdbc:sybase:Tds:localhost:4100"),
	MSSQL("com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:microsoft:sqlserver://localhost:1433"),
	DB2("com.ibm.db2.jcc.DB2Driver", "jdbc:db2://localhost:50000"),
	GOOGLE_APP_ENGINE("", "appengine");

	private String connectionString;
	private String driverClassName;

	private JdbcDatabase(String driverClassName, String connectionString) {
		this.driverClassName = driverClassName;
		this.connectionString = connectionString;
	}

	public String getConnectionString() {
		return connectionString;
	}

	public String getDriverClassName(){
		return driverClassName;
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name());
		tsc.append("driver class name", driverClassName);
		tsc.append("connection string", connectionString);
		return tsc.toString();
	}
}
