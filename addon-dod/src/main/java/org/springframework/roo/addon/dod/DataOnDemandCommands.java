package org.springframework.roo.addon.dod;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.util.Assert;

/**
 * Shell commands for creating data-on-demand (DoD) classes.
 * 
 * @author Alan Stewart
 * @since 1.1.3
 */
@Component
@Service
public class DataOnDemandCommands implements CommandMarker {
	@Reference private DataOnDemandOperations dataOnDemandOperations;
	
	@CliAvailabilityIndicator( { "dod" })
	public boolean isPersistentClassAvailable() {
		return dataOnDemandOperations.isPersistentClassAvailable();
	}

	@CliCommand(value = "dod", help = "Creates a new data on demand for the specified entity")
	public void newDod(
		@CliOption(key = "entity", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The entity which this data on demand class will create and modify as required") JavaType entity, 
		@CliOption(key = "class", mandatory = false, help = "The class which will be created to hold this data on demand provider (defaults to the entity name + 'DataOnDemand')") JavaType clazz, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(entity);
		}

		Assert.isTrue(BeanInfoUtils.isEntityReasonablyNamed(entity), "Cannot create data on demand for an entity named 'Test' or 'TestCase' under any circumstances");

		if (clazz == null) {
			clazz = new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand");
		}

		dataOnDemandOperations.newDod(entity, clazz, Path.SRC_TEST_JAVA);
	}
}
