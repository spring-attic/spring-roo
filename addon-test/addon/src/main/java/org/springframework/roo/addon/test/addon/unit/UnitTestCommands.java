package org.springframework.roo.addon.test.addon.unit;

import static org.springframework.roo.shell.OptionContexts.UPDATE_PROJECT;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;

/**
 * Shell commands for {@link UnitTestOperationsImpl}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class UnitTestCommands implements CommandMarker {

  @Reference
  private UnitTestOperations unitTestOperations;
  @Reference
  private TypeLocationService typeLocationService;

  @CliAvailabilityIndicator({"test unit"})
  public boolean isPersistentClassAvailable() {
    return unitTestOperations.isUnitTestInstallationPossible();
  }

  @CliCommand(value = "test unit", help = "Creates a unit test class for the specified class")
  public void newMockTest(
      @CliOption(key = "class", mandatory = true, optionContext = UPDATE_PROJECT,
          help = "The name of the project class which this unit test class is targeting") final JavaType projectType,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

    // Check if specified type exists in the project
    String physicalTypeIdentifier = typeLocationService.getPhysicalTypeIdentifier(projectType);
    if (physicalTypeIdentifier == null) {
      throw new IllegalArgumentException(String.format(
          "The class '%s' doesn't exists in the project. Please, specify an existing class",
          projectType));
    }

    if (!permitReservedWords) {
      ReservedWords.verifyReservedWordsNotPresent(projectType);
    }

    unitTestOperations.newUnitTest(projectType);
  }

}
