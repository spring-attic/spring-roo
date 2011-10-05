package org.springframework.roo.addon.jsf;

import java.util.Map;

import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

public class JsfFieldHolder {

	// Fields
	private final FieldMetadata field;
	private boolean enumerated;
	private boolean commonCollectionType;
	private boolean applicationType;
	private MemberDetails memberDetails;
	private Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions;
	private String displayMethod;
	private Map<JavaType, MemberDetails> collectionGenericTypes;

	public JsfFieldHolder(final FieldMetadata field) {
		Assert.notNull(field, "Field required");
		this.field = field;
	}

	public FieldMetadata getField() {
		return field;
	}

	public boolean isEnumerated() {
		return enumerated;
	}

	public void setEnumerated(final boolean enumerated) {
		this.enumerated = enumerated;
	}

	public boolean isCommonCollectionType() {
		return commonCollectionType;
	}

	public boolean isApplicationType() {
		return applicationType;
	}

	public MemberDetails getMemberDetails() {
		return memberDetails;
	}

	public void setMemberDetails(final MemberDetails memberDetails) {
		this.memberDetails = memberDetails;
		applicationType = this.memberDetails != null;
	}

	public Map<MethodMetadataCustomDataKey, MemberTypeAdditions> getCrudAdditions() {
		return crudAdditions;
	}

	public void setCrudAdditions(final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions) {
		this.crudAdditions = crudAdditions;
	}

	public String getDisplayMethod() {
		return displayMethod;
	}

	public void setDisplayMethod(final String displayMethod) {
		this.displayMethod = displayMethod;
	}

	public Map<JavaType, MemberDetails> getCollectionGenericTypes() {
		return collectionGenericTypes;
	}

	public void setCollectionGenericTypes(final Map<JavaType, MemberDetails> collectionGenericTypes) {
		this.collectionGenericTypes = collectionGenericTypes;
		commonCollectionType = this.collectionGenericTypes == null;
	}
}
