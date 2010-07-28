package org.springframework.roo.addon.dbre;

import java.io.File;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.RooIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link DbreTableService}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DbreTableServiceImpl implements DbreTableService {
	@Reference private PathResolver pathResolver;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private MetadataService metadataService;
	@Reference private FileManager fileManager;

	public JavaType findTypeForTableName(String tableNamePattern, JavaPackage javaPackage) {
		JavaType javaType = suggestTypeNameForNewTable(tableNamePattern, javaPackage);
		return getPhysicalTypeMetadata(javaType) != null ? javaType : null;
	}

	public String suggestTableNameForNewType(JavaType javaType) {
		return convertTypeToTableName(javaType);
	}

	public JavaType suggestTypeNameForNewTable(String tableNamePattern, JavaPackage javaPackage) {
		return convertTableNameToType(tableNamePattern, javaPackage);
	}

	public String suggestFieldName(String columnName) {
		return getFieldName(columnName);
	}

	private String convertTypeToTableName(JavaType javaType) {
		Assert.notNull(javaType, "Type to convert required");

		// Keep it simple for now
		String simpleName = javaType.getSimpleTypeName();
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < simpleName.length(); i++) {
			Character c = simpleName.charAt(i);
			if (i > 0 && Character.isUpperCase(c)) {
				result.append("_");
			}
			result.append(Character.toUpperCase(c));
		}
		return result.toString();
	}

	private JavaType convertTableNameToType(String tableNamePattern, JavaPackage javaPackage) {
		Assert.notNull(tableNamePattern, "Table name to convert required");
		Assert.notNull(javaPackage, "Java package required");

		StringBuilder result = new StringBuilder(javaPackage.getFullyQualifiedPackageName());
		if (result.length() > 0) {
			result.append(".");
		}
		result.append(getName(tableNamePattern, false));
		return new JavaType(result.toString());
	}

	private String getFieldName(String columnName) {
		Assert.isTrue(StringUtils.hasText(columnName), "Column name required");
		return getName(columnName, true);
	}

	private String getName(String str, boolean isField) {
		StringBuilder result = new StringBuilder();
		boolean isDelimChar = false;
		for (int i = 0; i < str.length(); i++) {
			Character c = str.charAt(i);
			if (i == 0) {
				result.append(isField ? Character.toLowerCase(c) : Character.toUpperCase(c));
				continue;
			} else if (i > 0 && (c == '_' || c == '-')) {
				isDelimChar = true;
				continue;
			}
			if (isDelimChar) {
				result.append(Character.toUpperCase(c));
				isDelimChar = false;
			} else {
				result.append(Character.toLowerCase(c));
			}
		}
		return result.toString();
	}

	public SortedSet<JavaType> getDatabaseManagedEntities() {
		SortedSet<JavaType> managedEntities = new TreeSet<JavaType>(new DbreManagedTypesComparator());
		FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(Path.SRC_MAIN_JAVA)), null);
		String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + "**" + File.separatorChar + "*.java";
		SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);

		for (FileDetails file : entries) {
			String fullPath = srcRoot.getRelativeSegment(file.getCanonicalPath());
			fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // Ditch the first / and .java
			JavaType javaType = new JavaType(fullPath);

			String id = physicalTypeMetadataProvider.findIdentifier(javaType);
			if (id != null) {
				// Now I've found it, let's work out the Path it is from
				Path path = PhysicalTypeIdentifier.getPath(id);
				if (isAnnotationPresentOnClassOrInterface(javaType, path, new JavaType(RooDbManaged.class.getName()))) {
					managedEntities.add(javaType);
				}
			}
		}

		return Collections.unmodifiableSortedSet(managedEntities);
	}

	public SortedSet<JavaType> getDatabaseManagedIdentifiers() {
		SortedSet<JavaType> managedIdentifiers = new TreeSet<JavaType>(new DbreManagedTypesComparator());
		FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(Path.SRC_MAIN_JAVA)), null);
		String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + "**" + File.separatorChar + "*.java";
		SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);

		for (FileDetails file : entries) {
			String fullPath = srcRoot.getRelativeSegment(file.getCanonicalPath());
			fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // Ditch the first / and .java
			JavaType javaType = new JavaType(fullPath);
			String id = physicalTypeMetadataProvider.findIdentifier(javaType);
			if (id != null) {
				// Now I've found it, let's work out the Path it is from
				Path path = PhysicalTypeIdentifier.getPath(id);
				if (isAnnotationPresentOnClassOrInterface(javaType, path, new JavaType(RooIdentifier.class.getName()))) {
					managedIdentifiers.add(javaType);
				}
			}
		}

		return Collections.unmodifiableSortedSet(managedIdentifiers);
	}

	private boolean isAnnotationPresentOnClassOrInterface(JavaType javaType, Path path, JavaType annotation) {
		String physicalTypeMid = PhysicalTypeIdentifier.createIdentifier(javaType, path);
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(physicalTypeMid);
		if (physicalTypeMetadata != null && physicalTypeMetadata.getPhysicalTypeDetails() != null && physicalTypeMetadata.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
			// We have a class or an interface
			// It's important not to ask for metadata here that depends on the identifier service being populated, as to populate it this method is required
			ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getPhysicalTypeDetails();
			return (MemberFindingUtils.getTypeAnnotation(classOrInterfaceTypeDetails, annotation) != null);
		}
		return false;
	}

	private PhysicalTypeMetadata getPhysicalTypeMetadata(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		return (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
	}
}
