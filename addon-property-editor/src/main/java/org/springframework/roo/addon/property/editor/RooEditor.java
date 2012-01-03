package org.springframework.roo.addon.property.editor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a type that requires ROO property editor support.
 * <p>
 * This annotation will cause ROO to produce code that would typically appear in
 * a property editor which, in turn is required by MVC controllers. Importantly,
 * such code does NOT depend on any singletons and is intended to safely
 * serialise. In the current release this code will be emitted to an ITD.
 * <p>
 * There are two cases in which ROO will not emit one or more of the above
 * artifacts:
 * <ul>
 * <li>The user provides the equivalent object himself. ROO will check for this
 * by naming convention where it will look for a class name with the 'Editor'
 * suffix in the same directory for types that require editors.</li>
 * </ul>
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RooEditor {

    /**
     * Every editor is responsible for a single java object. The editor will be
     * created for the object defined here.
     */
    Class<?> providePropertyEditorFor();
}
