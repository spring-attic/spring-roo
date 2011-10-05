package org.springframework.roo.addon.jsf;

import java.util.Map;

import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Holder for JSF field parameters.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfFieldHolder {

	// Fields
	private FieldMetadata field;
	private boolean enumerated;
	private MemberDetails memberDetails;
	private Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions;
	private Map<JavaType, MemberDetails> genericTypes;
	private String displayMethod;
	private boolean applicationType;
	private boolean applicationCollectionType;

	public JsfFieldHolder(FieldMetadata field, boolean enumerated, MemberDetails memberDetails, Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions, Map<JavaType, MemberDetails> genericTypes, String displayMethod) {
		Assert.notNull(field, "Field required");
		this.field = field;
		this.enumerated = enumerated;
		this.memberDetails = memberDetails;
		this.crudAdditions = crudAdditions;
		this.displayMethod = displayMethod;
		this.genericTypes = genericTypes;
		applicationType = this.memberDetails != null && this.crudAdditions != null;
		applicationCollectionType = !CollectionUtils.isEmpty(this.genericTypes);
	}

	public FieldMetadata getField() {
		return field;
	}
	
	public boolean isEnumerated() {
		return enumerated;
	}
	
	public MemberDetails getMemberDetails() {
		return memberDetails;
	}

	public Map<MethodMetadataCustomDataKey, MemberTypeAdditions> getCrudAdditions() {
		return crudAdditions;
	}

	public String getDisplayMethod() {
		return displayMethod;
	}

	public Map<JavaType, MemberDetails> getGenericTypes() {
		return genericTypes;
	}
	
	public boolean isApplicationCollectionType() {
		return applicationCollectionType;
	}

	public boolean isApplicationType() {
		return applicationType;
	}
}
