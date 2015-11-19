package org.springframework.roo.project.packaging;

/**
 * Unit test of the {@link JarPackaging} {@link PackagingProvider}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JarPackagingTest extends PackagingProviderTestCase<JarPackaging> {

    @Override
    protected JarPackaging getProvider() {
        return new JarPackaging();
    }
}
