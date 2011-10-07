package org.springframework.roo.addon.jsf;

import static org.springframework.roo.model.RooJavaType.ROO_UPLOADED_FILE;

import java.util.Map;

import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
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
	private final FieldMetadata field;
	private boolean enumerated;
	private MemberDetails applicationTypeMemberDetails;
	private Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions;
	private Map<JavaType, MemberDetails> genericTypes;
	private boolean applicationType;
	private boolean applicationCollectionType;
	private boolean rooUploadFileField;

	public JsfFieldHolder(final FieldMetadata field, final boolean enumerated, final MemberDetails applicationTypeMemberDetails, final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions, final Map<JavaType, MemberDetails> genericTypes) {
		Assert.notNull(field, "Field required");
		this.field = field;
		this.enumerated = enumerated;
		this.applicationTypeMemberDetails = applicationTypeMemberDetails;
		this.crudAdditions = crudAdditions;
		this.genericTypes = genericTypes;
		applicationType = this.applicationTypeMemberDetails != null && !CollectionUtils.isEmpty(this.crudAdditions);
		applicationCollectionType = !CollectionUtils.isEmpty(this.genericTypes);

		for (final AnnotationMetadata annotation : field.getAnnotations()) {
			if (annotation.getAnnotationType().equals(ROO_UPLOADED_FILE)) {
				rooUploadFileField = true;
				break;
			}
		}
	}

	public FieldMetadata getField() {
		return field;
	}
	
	public boolean isEnumerated() {
		return enumerated;
	}

	public MemberDetails getApplicationTypeMemberDetails() {
		return applicationTypeMemberDetails;
	}

	public Map<MethodMetadataCustomDataKey, MemberTypeAdditions> getCrudAdditions() {
		return crudAdditions;
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

	public boolean isRooUploadFileField() {
		return rooUploadFileField;
	}
}
