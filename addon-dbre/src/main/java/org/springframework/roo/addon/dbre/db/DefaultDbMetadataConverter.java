package org.springframework.roo.addon.dbre.db;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Default implementation of {@link DbMetadataConverter).
 * 
 * @author Alan Stewart
 * @since 1.1
 */
@Component
@Service
public class DefaultDbMetadataConverter implements DbMetadataConverter {

	public IdentifiableTable convertTypeToTableType(JavaType javaType) {
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
		return new IdentifiableTable(null, null, result.toString());
	}
	
	public JavaType convertTableIdentityToType(IdentifiableTable identifiableTable, JavaPackage javaPackage) {
		Assert.notNull(identifiableTable, "Table identity to convert required");
		Assert.notNull(javaPackage, "Java package required");
		
		String table = identifiableTable.getTable();
		StringBuilder result = new StringBuilder(javaPackage.getFullyQualifiedPackageName());
		if (result.length() > 0) {
			result.append(".");
		}
		result.append(getName(table, false));
		return new JavaType(result.toString());
	}

	public String getFieldName(String columnName) {
		Assert.isTrue(StringUtils.hasText(columnName), "Column name required");
		return getName(columnName, true);
	}
	
	private String getName(String str, boolean isField) {
		StringBuilder result = new StringBuilder();
		boolean isUnderscore = false;
		for (int i = 0; i < str.length(); i++) {
			Character c = str.charAt(i);
			if (i == 0) {
				result.append(isField ? Character.toLowerCase(c) : Character.toUpperCase(c));
				continue;
			} else if (i > 0 && c == '_') {
				isUnderscore = true;
				continue;
			}
			if (isUnderscore) {
				result.append(Character.toUpperCase(c));
				isUnderscore = false;
			} else {
				result.append(Character.toLowerCase(c));
			}
		}
		return result.toString();
	}
}
