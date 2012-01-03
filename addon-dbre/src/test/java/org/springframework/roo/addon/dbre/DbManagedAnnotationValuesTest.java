package org.springframework.roo.addon.dbre;

import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link DbManagedAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class DbManagedAnnotationValuesTest extends
        AnnotationValuesTestCase<RooDbManaged, DbManagedAnnotationValues> {

    @Override
    protected Class<RooDbManaged> getAnnotationClass() {
        return RooDbManaged.class;
    }

    @Override
    protected Class<DbManagedAnnotationValues> getValuesClass() {
        return DbManagedAnnotationValues.class;
    }
}
