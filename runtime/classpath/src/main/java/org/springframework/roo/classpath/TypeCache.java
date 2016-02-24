package org.springframework.roo.classpath;

import java.util.Set;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;

public interface TypeCache {

    void cacheFilePathAgainstTypeIdentifier(String typeFilePath,
            String typeIdentifier);

    void cacheType(String typeFilePath, ClassOrInterfaceTypeDetails cid);

    void cacheTypeAgainstModule(Pom pom, JavaType javaType);

    Set<String> getAllTypeIdentifiers();

    String getPhysicalTypeIdentifier(JavaType javaType);

    ClassOrInterfaceTypeDetails getTypeDetails(String mid);

    String getTypeIdFromTypeFilePath(String typeFilePath);

    Set<String> getTypeNamesForModuleFilePath(String moduleFilePath);

    void removeType(String typeIdentifier);
}
