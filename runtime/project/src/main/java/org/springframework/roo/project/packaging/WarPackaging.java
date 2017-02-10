package org.springframework.roo.project.packaging;

import static org.springframework.roo.project.Path.SPRING_CONFIG_ROOT;
import static org.springframework.roo.project.Path.SRC_MAIN_JAVA;
import static org.springframework.roo.project.Path.SRC_MAIN_WEBAPP;
import static org.springframework.roo.project.Path.SRC_TEST_JAVA;
import static org.springframework.roo.project.Path.SRC_TEST_RESOURCES;

import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.project.Path;

/**
 * The core {@link PackagingProvider} for web modules.
 * 
 * @author Andrew Swan
 * @author Paula Navarro
 * @since 1.2.0
 */
@Component
@Service
public class WarPackaging extends AbstractCorePackagingProvider {

  public WarPackaging() {
    super("war", "war-pom-template.xml", "child-war-pom-template.xml");
  }

  public Collection<Path> getPaths() {
    return Arrays.asList(SRC_MAIN_JAVA, SRC_TEST_JAVA, SRC_TEST_RESOURCES, SPRING_CONFIG_ROOT,
        SRC_MAIN_WEBAPP);
  }
}
