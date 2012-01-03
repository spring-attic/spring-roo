package org.springframework.roo.addon.property.editor;

import org.springframework.roo.classpath.details.annotations.populator.AnnotationValuesTestCase;

/**
 * Unit test of {@link EditorAnnotationValues}
 * 
 * @author Andrew Swan
 * @since 1.2.0
 */
public class EditorAnnotationValuesTest extends
        AnnotationValuesTestCase<RooEditor, EditorAnnotationValues> {

    @Override
    protected Class<RooEditor> getAnnotationClass() {
        return RooEditor.class;
    }

    @Override
    protected Class<EditorAnnotationValues> getValuesClass() {
        return EditorAnnotationValues.class;
    }
}