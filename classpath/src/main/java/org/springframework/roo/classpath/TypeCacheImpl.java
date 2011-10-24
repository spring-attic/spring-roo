package org.springframework.roo.classpath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.maven.Pom;
import org.springframework.roo.support.util.Assert;

@Component
@Service
public class TypeCacheImpl implements TypeCache {

	private final Map<String, ClassOrInterfaceTypeDetails> midToTypeDetailsMap = new HashMap<String, ClassOrInterfaceTypeDetails>();
	private final Set<JavaType> types = new HashSet<JavaType>();
	private final Map<String, String> typeNameToMidMap = new HashMap<String, String>();
	private final Map<String, String> typeFilePathToMidMap = new HashMap<String, String>();
	private final Map<String, String> typeNameToModuleFilePathMap = new HashMap<String, String>();
	private final Map<String, String> typeNameToModuleNameMap = new HashMap<String, String>();
	private final Map<String, String> typeIdentifierToFilePathMap = new HashMap<String, String>();
	private final Map<String, Set<String>> moduleFilePathToTypeNamesMap = new HashMap<String, Set<String>>();
	private final Map<String, Set<String>> simpleTypeNameTypesMap = new HashMap<String, Set<String>>();

	public void cacheType(final String typeFilePath, final ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails) {
		Assert.hasText(typeFilePath, "Module name required");
		Assert.notNull(classOrInterfaceTypeDetails, "Type details required");

		midToTypeDetailsMap.put(classOrInterfaceTypeDetails.getDeclaredByMetadataId(), classOrInterfaceTypeDetails);
		typeFilePathToMidMap.put(typeFilePath, classOrInterfaceTypeDetails.getDeclaredByMetadataId());
		typeIdentifierToFilePathMap.put(classOrInterfaceTypeDetails.getDeclaredByMetadataId(), typeFilePath);
		types.add(classOrInterfaceTypeDetails.getName());

		String fullyQualifiedTypeName = classOrInterfaceTypeDetails.getName().getFullyQualifiedTypeName();
		String simpleTypeName = classOrInterfaceTypeDetails.getName().getSimpleTypeName();
		typeNameToMidMap.put(fullyQualifiedTypeName, classOrInterfaceTypeDetails.getDeclaredByMetadataId());
		if (!simpleTypeNameTypesMap.containsKey(simpleTypeName)) {
			simpleTypeNameTypesMap.put(simpleTypeName, new HashSet<String>());
		}

		simpleTypeNameTypesMap.get(simpleTypeName).add(fullyQualifiedTypeName);
	}

	public Set<String> getTypesForSimpleTypeName(final String simpleTypeName) {
		if (!simpleTypeNameTypesMap.containsKey(simpleTypeName)) {
			return new HashSet<String>();
		}
		return simpleTypeNameTypesMap.get(simpleTypeName);
	}

	public void cacheFilePathAgainstTypeIdentifier(final String typeFilePath, final String typeIdentifier) {
		typeFilePathToMidMap.put(typeFilePath, typeIdentifier);
	}

	public void cacheTypeAgainstModule(final Pom pom, final JavaType javaType) {
		Assert.notNull(pom, "Pom cannot be null");
		Assert.notNull(javaType, "Java type cannot be null");
   		typeNameToModuleFilePathMap.put(javaType.getFullyQualifiedTypeName(), pom.getPath());
		typeNameToModuleNameMap.put(javaType.getFullyQualifiedTypeName(), pom.getModuleName());
		if (!moduleFilePathToTypeNamesMap.containsKey(pom.getPath())) {
			moduleFilePathToTypeNamesMap.put(pom.getPath(), new HashSet<String>());
		}
		moduleFilePathToTypeNamesMap.get(pom.getPath()).add(javaType.getFullyQualifiedTypeName());
	}

	public Set<String> getAllTypeIdentifiers() {
		return new HashSet<String>(midToTypeDetailsMap.keySet());
	}

	public Set<JavaType> getAllTypes() {
		return new HashSet<JavaType>(types);
	}

	public void removeType(final String typeIdentifier) {
		Assert.hasText(typeIdentifier, "Physical type identifier required");
		ClassOrInterfaceTypeDetails cid = midToTypeDetailsMap.get(typeIdentifier);
		if (cid != null) {
			typeNameToMidMap.remove(cid.getName().getFullyQualifiedTypeName());
			typeNameToModuleFilePathMap.remove(cid.getName().getFullyQualifiedTypeName());
			typeNameToModuleNameMap.remove(cid.getName().getFullyQualifiedTypeName());
		}
		String filePath = typeIdentifierToFilePathMap.get(typeIdentifier);
		if (filePath != null) {
			typeFilePathToMidMap.remove(filePath);
			typeIdentifierToFilePathMap.remove(typeIdentifier);
		}

	}

	public Set<String> getTypeNamesForModuleFilePath(final String moduleFilePath) {
		Assert.hasText(moduleFilePath, "Pom file path required");
		if (!moduleFilePathToTypeNamesMap.containsKey(moduleFilePath)) {
			moduleFilePathToTypeNamesMap.put(moduleFilePath, new HashSet<String>());
		}
		return new HashSet<String>(moduleFilePathToTypeNamesMap.get(moduleFilePath));
	}

	public ClassOrInterfaceTypeDetails getTypeDetails(final String mid) {
		Assert.hasText(mid, "Physical type identifier required");
		return midToTypeDetailsMap.get(mid);
	}

	public String getPhysicalTypeIdentifier(final JavaType javaType) {
		Assert.notNull(javaType, "Java type cannot be null");
		return typeNameToMidMap.get(javaType.getFullyQualifiedTypeName());
	}

	public String getTypeIdFromTypeFilePath(final String typeFilePath) {
		Assert.hasText(typeFilePath, "Physical type file path required");
		return typeFilePathToMidMap.get(typeFilePath);
	}
}
