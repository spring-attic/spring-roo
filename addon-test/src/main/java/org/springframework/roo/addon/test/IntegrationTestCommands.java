package org.springframework.roo.addon.test;

import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Shell commands for {@link IntegrationTestOperations}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class IntegrationTestCommands implements CommandMarker {
	private IntegrationTestOperations integrationTestOperations;
	private ClasspathOperations classpathOperations;

	public IntegrationTestCommands(IntegrationTestOperations integrationTestOperations, ClasspathOperations classpathOperations) {
		Assert.notNull(integrationTestOperations, "Integration test operations required");
		Assert.notNull(classpathOperations, "Classpath operations required");
		this.integrationTestOperations = integrationTestOperations;
		this.classpathOperations = classpathOperations;
	}
	
	@CliAvailabilityIndicator({"test mock"})
	public boolean isAvailable() {
		return classpathOperations.isProjectAvailable();
	}
	
	@CliCommand(value="test mock", help="Creates a mock test for the specified entity")
	public void newMockTest(
			@CliOption(key="entity", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project") JavaType entity,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(entity);
		}

		integrationTestOperations.newMockTest(entity);
	}

}
