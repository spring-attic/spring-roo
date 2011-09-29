package org.springframework.roo.addon.jsf;

import java.util.Map;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

public class JsfFieldHolder {
	
	// Fields
	private FieldMetadata field;
	private boolean enumerated;
	private boolean commonCollectionType;
	private boolean applicationType;
	private MemberDetails memberDetails;
	private Map<JavaType, MemberDetails> genericTypes;
	
	public JsfFieldHolder(final FieldMetadata field, final boolean enumerated) {
		Assert.notNull(field, "Field required");
		this.field = field;
		this.enumerated = enumerated;
	}

	public FieldMetadata getField() {
		return field;
	}
	
	public boolean isEnumerated() {
		return enumerated;
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

	public Map<JavaType, MemberDetails> getGenericTypes() {
		return genericTypes;
	}

	public void setGenericTypes(final Map<JavaType, MemberDetails> genericTypes) {
		this.genericTypes = genericTypes;
		commonCollectionType = this.genericTypes == null;
	}
}
