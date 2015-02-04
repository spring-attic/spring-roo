package org.springframework.roo.classpath.operations.jsr303;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a field is used for storing uploaded file contents.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface RooUploadedFile {

    boolean autoUpload() default false;

    String contentType();
}
