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
 * Commands for the 'logging' add-on to be used by the ROO shell.
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
	
	/**
	 * @return true if the "install jpa" command is available at this moment
	 */
	@CliAvailabilityIndicator("install jpa")
	public boolean isInstallJpaAvailable() {
		return jpaOperations.isInstallJpaAvailable();
	}
	
	@CliCommand(value="install jpa", help="Install a JPA persistence provider in your project")
	public void installJpa(@CliOption(key={"provider"}, mandatory=true, help="The persistence provider to support") OrmProvider ormProvider,
			@CliOption(key={"","database"}, mandatory=true, help="The database to support") JdbcDatabase jdbcDatabase) {
		jpaOperations.configureJpa(ormProvider, jdbcDatabase, true);
	}
	
	@CliCommand(value="database properties", help="Shows database configuration details")
	public SortedSet<String> databaseProperties() {
		return propFileOperations.getPropertyKeys(Path.SRC_MAIN_RESOURCES, "database.properties", true);
	}
	
	@CliCommand(value="database set", help="Changes a particular database property")
	public void databaseSet(@CliOption(key="key", mandatory=true, help="The property key that should be changed") String key, @CliOption(key="value", mandatory=true, help="The new vale for this property key") String value) {
		propFileOperations.changeProperty(Path.SRC_MAIN_RESOURCES, "database.properties", key, value);
	}

	@CliCommand(value="database remove", help="Removes a particular database property")
	public void databaseRemove(@CliOption(key={"","key"}, mandatory=true, help="The property key that should be removed") String key) {
		propFileOperations.removeProperty(Path.SRC_MAIN_RESOURCES, "database.properties", key);
	}
	
	/**
	 * @return true if the "update jpa" command is available at this moment
	 */
	@CliAvailabilityIndicator("update jpa")
	public boolean isUpdateJpaAvailable() {
		return jpaOperations.isUpdateJpaAvailable();
	}
	
	@CliCommand(value="update jpa", help="Update the JPA persistence provider in your project")
	public void updateJpa(@CliOption(key={"","provider"}, mandatory=true, help="The persistence provider to support") OrmProvider ormProvider,
			@CliOption(key={"","database"}, mandatory=true, help="The database to support") JdbcDatabase jdbcDatabase) {
		jpaOperations.configureJpa(ormProvider, jdbcDatabase, false);
	}
}