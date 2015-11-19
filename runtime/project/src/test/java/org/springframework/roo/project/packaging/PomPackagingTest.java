package org.springframework.roo.project.packaging;

/**
 * Unit test of the {@link PomPackaging} {@link PackagingProvider}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PomPackagingTest extends PackagingProviderTestCase<PomPackaging> {

    @Override
    protected PomPackaging getProvider() {
        return new PomPackaging();
    }
}
