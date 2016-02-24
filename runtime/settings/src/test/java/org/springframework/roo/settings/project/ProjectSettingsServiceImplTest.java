package org.springframework.roo.settings.project;

import org.junit.Before;
import org.junit.Test;
import org.springframework.roo.settings.project.ProjectSettingsService;
import org.springframework.roo.settings.project.ProjectSettingsServiceImpl;

/**
 * Unit test of {@link ProjectSettingsServiceImpl} N.B. for this test to pass,
 * the following folder must be on the classpath:
 * <code>org.springframework.roo.project.setting/src/test/resources</code>
 * This is automatically the case when run by Maven, but not in Eclipse/STS,
 * where the first time you run this test, you need to add the above folder
 * explicitly via the "Run As -> Run Configurations..." dialog (in the
 * "Classpath" tab, click "Advanced" and add the above path as a "folder").
 *
 * @author Paula Navarro Alfonso
 * @since 2.0.0
 */
public class ProjectSettingsServiceImplTest {

  // Fixture
  private ProjectSettingsService projectSettingsService;

  @Before
  public void setUp() throws IllegalArgumentException, IllegalAccessException {
    projectSettingsService = new ProjectSettingsServiceImpl();
  }

  @Test
  public void testAddProperty() {
    // TODO
  }
}
