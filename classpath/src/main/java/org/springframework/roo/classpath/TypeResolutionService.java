package org.springframework.roo.classpath;

import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;

public interface TypeResolutionService {

    JavaType getJavaType(String fileIdentifier);

    JavaPackage getPackage(String fileIdentifier);
}
