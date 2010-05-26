package org.springframework.roo.addon.test;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Shell commands for {@link IntegrationTestOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@Component
@Service
public class IntegrationTestCommands implements CommandMarker {
	@Reference private IntegrationTestOperations integrationTestOperations;
	@Reference private ClasspathOperations classpathOperations;

	@CliAvailabilityIndicator({"test mock"})
	public boolean isAvailable() {
		return classpathOperations.isProjectAvailable();
	}
	
	@CliCommand(value = "test mock", help = "Creates a mock test for the specified entity")
	public void newMockTest(
			@CliOption(key = "entity", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the entity this mock test is targeting") JavaType entity, 
			@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(entity);
		}

		integrationTestOperations.newMockTest(entity);
	}
}
