package org.springframework.roo.addon.test.addon.integration;

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
 * Shell commands for {@link IntegrationTestOperationsImpl}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
@Component
@Service
public class IntegrationTestCommands implements CommandMarker {

  @Reference
  private IntegrationTestOperations integrationTestOperations;
  @Reference
  private TypeLocationService typeLocationService;

  @CliAvailabilityIndicator({"test integration"})
  public boolean isPersistentClassAvailable() {
    return integrationTestOperations.isIntegrationTestInstallationPossible();
  }

  @CliCommand(value = "test integration",
      help = "Creates a new integration test for the specified entity")
  public void newIntegrationTest(
      @CliOption(key = "entity", mandatory = false, unspecifiedDefaultValue = "*",
          optionContext = UPDATE_PROJECT,
          help = "The name of the entity to create an integration test for") final JavaType entity,
      @CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false",
          specifiedDefaultValue = "true",
          help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
      @CliOption(
          key = "transactional",
          mandatory = false,
          unspecifiedDefaultValue = "true",
          specifiedDefaultValue = "true",
          help = "Indicates whether the created test cases should be run withing a Spring transaction") final boolean transactional) {

    if (!permitReservedWords) {
      ReservedWords.verifyReservedWordsNotPresent(entity);
    }

    Validate
        .isTrue(
            BeanInfoUtils.isEntityReasonablyNamed(entity),
            "Cannot create an integration test for an entity named 'Test' or 'TestCase' under any circumstances");

    integrationTestOperations.newIntegrationTest(entity, transactional);
  }

}
