package org.springframework.roo.application.config;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test of {@link ApplicationConfigServiceImpl} N.B. for this test to pass,
 * the following folder must be on the classpath:
 * <code>org.springframework.roo.application.config/src/test/resources</code>
 * This is automatically the case when run by Maven, but not in Eclipse/STS,
 * where the first time you run this test, you need to add the above folder
 * explicitly via the "Run As -> Run Configurations..." dialog (in the
 * "Classpath" tab, click "Advanced" and add the above path as a "folder").
 * 
 * @author Juan Carlos García
 * @since 2.0.0
 */
public class ApplicationConfigServiceImplTest {

  // Fixture
  private ApplicationConfigService applicationConfigService;

  @Before
  public void setUp() throws IllegalArgumentException, IllegalAccessException {
    applicationConfigService = new ApplicationConfigServiceImpl();
  }

  @Test
  public void testAddProperty() {
    // TODO
  }
}
