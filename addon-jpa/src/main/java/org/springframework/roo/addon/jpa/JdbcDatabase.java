package org.springframework.roo.addon.jpa;

import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Provides information related to JDBC database configuration.
 *
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
public class JdbcDatabase implements Comparable<JdbcDatabase> {

	private String key;
	private String connectionString;
	private String driverClassName;

	public static final JdbcDatabase H2_IN_MEMORY = new JdbcDatabase("H2", "org.h2.Driver", "jdbc:h2:mem:TO_BE_CHANGED_BY_ADDON;DB_CLOSE_DELAY=-1");
	public static final JdbcDatabase HYPERSONIC_IN_MEMORY = new JdbcDatabase("HYPERSONIC", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:TO_BE_CHANGED_BY_ADDON");
	public static final JdbcDatabase HYPERSONIC_PERSISTENT = new JdbcDatabase("HYPERSONIC", "org.hsqldb.jdbcDriver", "jdbc:hsqldb:${user.home}/TO_BE_CHANGED_BY_ADDON;shutdown=true");
	public static final JdbcDatabase POSTGRES = new JdbcDatabase("POSTGRES", "org.postgresql.Driver", "jdbc:postgresql://localhost:5432");
	public static final JdbcDatabase MYSQL = new JdbcDatabase("MYSQL", "com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306");
	public static final JdbcDatabase ORACLE = new JdbcDatabase("ORACLE", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@localhost:1521");
	public static final JdbcDatabase SYBASE = new JdbcDatabase("SYBASE", "com.sybase.jdbc2.jdbc.SybDriver", "jdbc:sybase:Tds:localhost:4100");
	public static final JdbcDatabase MSSQL = new JdbcDatabase("MSSQL", "com.microsoft.sqlserver.jdbc.SQLServerDriver", "jdbc:microsoft:sqlserver://localhost:1433");
	public static final JdbcDatabase DB2 = new JdbcDatabase("DB2", "com.ibm.db2.jcc.DB2Driver", "jdbc:db2://localhost:50000");

	public JdbcDatabase(String key, String driverClassName, String connectionString) {
		Assert.hasText(key, "Key required");
		Assert.hasText(connectionString, "Connection String required");
		this.key = key;
		this.driverClassName = driverClassName;
		this.connectionString = connectionString;
	}

	public String getKey() {
		return key;
	}

	public String getConnectionString() {
		return connectionString;
	}

	public String getDriverClassName(){
		return driverClassName;
	}

	public final int hashCode() {
		return this.key.hashCode();
	}

	public final boolean equals(Object obj) {
		return obj != null && obj instanceof JdbcDatabase && this.compareTo((JdbcDatabase)obj) == 0;
	}

	public final int compareTo(JdbcDatabase o) {
		if (o == null) return -1;
		return this.key.compareTo(o.key);
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("key", key);
		tsc.append("driver class name", driverClassName);
		tsc.append("connection string", connectionString);
		return tsc.toString();
	}

}
