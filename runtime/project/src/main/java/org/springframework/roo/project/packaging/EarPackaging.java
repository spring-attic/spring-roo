package org.springframework.roo.project.packaging;

import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;

/**
 * The Maven "pom" {@link PackagingProvider}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class EarPackaging extends AbstractCorePackagingProvider {

    /**
     * Constructor
     */
    public EarPackaging() {
        // ear-pom-template.xml doesn't exist because we won't allow ear packaging projects
        super("ear", "ear-pom-template.xml");
    }

    public Collection<Path> getPaths() {
        return null;
    }
}
