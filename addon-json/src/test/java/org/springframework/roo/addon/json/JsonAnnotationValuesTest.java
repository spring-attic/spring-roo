package org.springframework.roo.addon.json;

import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link JsonAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class JsonAnnotationValuesTest extends
        AnnotationValuesTestCase<RooJson, JsonAnnotationValues> {

    @Override
    protected Class<RooJson> getAnnotationClass() {
        return RooJson.class;
    }

    @Override
    protected Class<JsonAnnotationValues> getValuesClass() {
        return JsonAnnotationValues.class;
    }
}
