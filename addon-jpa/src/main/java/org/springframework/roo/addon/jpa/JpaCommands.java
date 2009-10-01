package org.springframework.roo.addon.jpa;

import java.util.SortedSet;

import org.springframework.roo.addon.propfiles.PropFileOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
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
	private MetadataService metadataService;
	
	public JpaCommands(StaticFieldConverter staticFieldConverter, JpaOperations jpaOperations, PropFileOperations propFileOperations, MetadataService metadataService) {
		Assert.notNull(staticFieldConverter, "Static field converter required");
		Assert.notNull(jpaOperations, "JPA operations required");
		Assert.notNull(propFileOperations, "Property file operations required");
		Assert.notNull(metadataService, "Metadata service required");
		staticFieldConverter.add(JdbcDatabase.class);
		staticFieldConverter.add(OrmProvider.class);
		this.jpaOperations = jpaOperations;
		this.propFileOperations = propFileOperations;
		this.metadataService = metadataService;
	}
	
	@CliAvailabilityIndicator("persistence setup")
	public boolean isInstallJpaAvailable() {
		return jpaOperations.isJpaInstallationPossible() || jpaOperations.isJpaInstalled();
	}
	
	@CliCommand(value="persistence setup", help="Install or updates a JPA persistence provider in your project")
	public void installJpa(@CliOption(key={"provider"}, mandatory=true, help="The persistence provider to support") OrmProvider ormProvider,
			@CliOption(key={"","database"}, mandatory=true, help="The database to support") JdbcDatabase jdbcDatabase,			
			@CliOption(key={"jndiDataSource"}, mandatory=false, help="The JNDI datasource to use") String jndi) {
		jpaOperations.configureJpa(ormProvider, jdbcDatabase, jndi, !jpaOperations.isJpaInstalled());
	}
	
	@CliCommand(value="persistence exception translation", help="Installs support for JPA exception translation")
	public void exceptionTranslation(@CliOption(key={"package"}, mandatory=false, help="The package in which the JPA exception translation aspect will be installed") JavaPackage aspectPackage) {		
		if (aspectPackage == null) {
			ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
			aspectPackage = projectMetadata.getTopLevelPackage();
		} 
		jpaOperations.installExceptionTranslation(aspectPackage);
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
	
	@CliAvailabilityIndicator({"database properties remove", "database properties set", "database properties list", "persistence exception translation"})
	public boolean isJpaInstalled() {
		return jpaOperations.isJpaInstalled();
	}
	
}