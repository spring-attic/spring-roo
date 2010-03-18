package org.springframework.roo.addon.jpa;

import java.util.SortedSet;

import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Commands for the 'jpa' add-on to be used by the ROO shell.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class JpaCommands implements CommandMarker {
	
	private JpaOperations jpaOperations;
	private PropFileOperations propFileOperations;

	public JpaCommands(StaticFieldConverter staticFieldConverter, JpaOperations jpaOperations, PropFileOperations propFileOperations) {
		Assert.notNull(staticFieldConverter, "Static field converter required");
		Assert.notNull(jpaOperations, "JPA operations required");
		Assert.notNull(propFileOperations, "Property file operations required");
		staticFieldConverter.add(JdbcDatabase.class);
		staticFieldConverter.add(OrmProvider.class);
		this.jpaOperations = jpaOperations;
		this.propFileOperations = propFileOperations;
	}
	
	@CliAvailabilityIndicator("persistence setup")
	public boolean isInstallJpaAvailable() {
		return jpaOperations.isJpaInstallationPossible() || jpaOperations.isJpaInstalled();
	}
	
	@CliCommand(value="persistence setup", help="Install or updates a JPA persistence provider in your project")
	public void installJpa(
			@CliOption(key={"provider"}, mandatory=true, help="The persistence provider to support") OrmProvider ormProvider,
			@CliOption(key={"","database"}, mandatory=true, help="The database to support") JdbcDatabase jdbcDatabase,			
			@CliOption(key={"jndiDataSource"}, mandatory=false, help="The JNDI datasource to use") String jndi,
			@CliOption(key={"databaseName"}, mandatory=false, help="The database name to use") String databaseName,
			@CliOption(key={"userName"}, mandatory=false, help="The username to use") String userName,
			@CliOption(key={"password"}, mandatory=false, help="The password to use") String password
			) {

		jpaOperations.configureJpa(ormProvider, jdbcDatabase, jndi);
		
		if (jndi == null || 0 == jndi.length()) {
			if (null != databaseName && 0 != databaseName.length()) {
				propFileOperations.changeProperty(Path.SPRING_CONFIG_ROOT, "database.properties", "database.url", jdbcDatabase.getConnectionString() + (databaseName.startsWith("/") ? databaseName : "/" + databaseName));
			}
			if (null != userName && 0 != userName.length()) {
				propFileOperations.changeProperty(Path.SPRING_CONFIG_ROOT, "database.properties", "database.username", userName);
			}
			if (null != password && 0 != password.length()) {
				propFileOperations.changeProperty(Path.SPRING_CONFIG_ROOT, "database.properties", "database.password", password);
			}
		}
	}
	
	@CliCommand(value="database properties list", help="Shows database configuration details")
	public SortedSet<String> databaseProperties() {
		return propFileOperations.getPropertyKeys(Path.SPRING_CONFIG_ROOT, "database.properties", true);
	}
	
	@CliCommand(value="database properties set", help="Changes a particular database property")
	public void databaseSet(@CliOption(key="key", mandatory=true, help="The property key that should be changed") String key, @CliOption(key="value", mandatory=true, help="The new vale for this property key") String value) {
		propFileOperations.changeProperty(Path.SPRING_CONFIG_ROOT, "database.properties", key, value);
	}

	@CliCommand(value="database properties remove", help="Removes a particular database property")
	public void databaseRemove(@CliOption(key={"","key"}, mandatory=true, help="The property key that should be removed") String key) {
		propFileOperations.removeProperty(Path.SPRING_CONFIG_ROOT, "database.properties", key);
	}
	
	@CliAvailabilityIndicator({"database properties remove", "database properties set", "database properties list"})
	public boolean isJpaInstalled() {
		return jpaOperations.isJpaInstalled();
	}
	
}