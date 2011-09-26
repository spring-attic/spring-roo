package org.springframework.roo.project.packaging;

/**
 * Unit test of the {@link Pom} {@link PackagingType}
 *
 * @author Andrew Swan
 * @since 1.2.0
 */
public class PomTest extends PackagingTypeTestCase<Pom> {

	@Override
	protected Pom getPackagingType() {
		return new Pom();
	}
}
