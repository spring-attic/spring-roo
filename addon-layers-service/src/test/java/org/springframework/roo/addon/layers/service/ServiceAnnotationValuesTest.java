package org.springframework.roo.addon.layers.service;

import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link ServiceAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class ServiceAnnotationValuesTest extends
        AnnotationValuesTestCase<RooService, ServiceAnnotationValues> {

    @Override
    protected Class<RooService> getAnnotationClass() {
        return RooService.class;
    }

    @Override
    protected Class<ServiceAnnotationValues> getValuesClass() {
        return ServiceAnnotationValues.class;
    }
}
