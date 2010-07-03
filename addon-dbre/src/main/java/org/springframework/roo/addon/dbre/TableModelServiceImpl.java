package org.springframework.roo.addon.dbre;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.ItdMetadataScanner;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link TableModelService}.
 * 
 * @author Alan Stewart
 * @since 1.1 
 */
public class TableModelServiceImpl implements TableModelService {
	@Reference private PathResolver pathResolver;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private ItdMetadataScanner itdMetadataScanner;
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

	public String suggestFieldNameForColumn(String columnName) {
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

	public Set<JavaType> getDatabaseManagedEntities() {
		Set<JavaType> managedEntities = new HashSet<JavaType>();
		FileDetails srcRoot = new FileDetails(new File(pathResolver.getRoot(Path.SRC_MAIN_JAVA)), null);
		String antPath = pathResolver.getRoot(Path.SRC_MAIN_JAVA) + File.separatorChar + "**" + File.separatorChar + "*.java";
		SortedSet<FileDetails> entries = fileManager.findMatchingAntPath(antPath);

		for (FileDetails file : entries) {
			String fullPath = srcRoot.getRelativeSegment(file.getCanonicalPath());
			fullPath = fullPath.substring(1, fullPath.lastIndexOf(".java")).replace(File.separatorChar, '.'); // Ditch the first / and .java
			JavaType javaType = new JavaType(fullPath);
			String id = physicalTypeMetadataProvider.findIdentifier(javaType);
			if (id != null) {
				PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
				if (ptm == null || ptm.getPhysicalTypeDetails() == null || !(ptm.getPhysicalTypeDetails() instanceof ClassOrInterfaceTypeDetails)) {
					continue;
				}

				ClassOrInterfaceTypeDetails cid = (ClassOrInterfaceTypeDetails) ptm.getPhysicalTypeDetails();
				if (Modifier.isAbstract(cid.getModifier())) {
					continue;
				}

				Set<MetadataItem> metadata = itdMetadataScanner.getMetadata(id);
				for (MetadataItem item : metadata) {
					if (item instanceof DbreMetadata) {
						managedEntities.add(javaType);
					}
				}
			}
		}

		return Collections.unmodifiableSet(managedEntities);
	}

	private PhysicalTypeMetadata getPhysicalTypeMetadata(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		return (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
	}
}
