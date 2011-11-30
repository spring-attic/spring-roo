package org.springframework.roo.classpath;

import java.util.Set;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;

public interface TypeCache {

	void cacheType(String typeFilePath, ClassOrInterfaceTypeDetails cid);

	void cacheFilePathAgainstTypeIdentifier(String typeFilePath, String typeIdentifier);

	void cacheTypeAgainstModule(Pom pom, JavaType javaType);

	ClassOrInterfaceTypeDetails getTypeDetails(String mid);

	String getPhysicalTypeIdentifier(JavaType javaType);

	String getTypeIdFromTypeFilePath(String typeFilePath);

	void removeType(String typeIdentifier);

	Set<String> getAllTypeIdentifiers();

	Set<String> getTypeNamesForModuleFilePath(String moduleFilePath);
}
