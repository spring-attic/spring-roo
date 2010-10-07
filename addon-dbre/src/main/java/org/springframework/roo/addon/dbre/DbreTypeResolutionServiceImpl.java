package org.springframework.roo.addon.dbre;

import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.entity.RooIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Implementation of {@link DbreTypeResolutionService}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component 
@Service 
public class DbreTypeResolutionServiceImpl implements DbreTypeResolutionService {
	@Reference private MetadataService metadataService;
	@Reference private TypeLocationService typeLocationService;

	public JavaType findTypeForTableName(SortedSet<JavaType> managedEntities, String tableNamePattern, JavaPackage javaPackage) {
		Assert.notNull(managedEntities, "Managed entities required");
		JavaType javaType = null;
		for (JavaType managedEntity : managedEntities) {
			if (managedEntity.getSimpleTypeName().equals(getName(tableNamePattern, false))) {
				return managedEntity;
			}
		}

		if (javaType == null) {
			javaType = convertTableNameToType(tableNamePattern, javaPackage);
			if (getPhysicalTypeMetadata(javaType) != null) {
				return javaType;
			}
		}

		return null;
	}

	public JavaType findTypeForTableName(String tableNamePattern, JavaPackage javaPackage) {
		return findTypeForTableName(getManagedEntities(), tableNamePattern, javaPackage);
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

	public SortedSet<JavaType> getManagedEntities() {
		SortedSet<JavaType> managedEntities = new TreeSet<JavaType>(new ManagedTypesComparator());
		managedEntities.addAll(typeLocationService.findTypesWithAnnotation(new JavaType(RooDbManaged.class.getName())));
		return Collections.unmodifiableSortedSet(managedEntities);
	}

	public SortedSet<JavaType> getManagedIdentifiers() {
		SortedSet<JavaType> managedIdentifiers = new TreeSet<JavaType>(new ManagedTypesComparator());
		managedIdentifiers.addAll(typeLocationService.findTypesWithAnnotation(new JavaType(RooIdentifier.class.getName())));
		return Collections.unmodifiableSortedSet(managedIdentifiers);
	}

	private PhysicalTypeMetadata getPhysicalTypeMetadata(JavaType javaType) {
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
		return (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
	}

	private static class ManagedTypesComparator implements Comparator<JavaType> {

		public int compare(JavaType o1, JavaType o2) {
			return o1.getFullyQualifiedTypeName().compareTo(o2.getFullyQualifiedTypeName());
		}
	}
}
