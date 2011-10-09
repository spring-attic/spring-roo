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
	private final String genericTypePlural;
	private Map<JavaType, String> genericTypes;
	private MemberDetails applicationTypeMemberDetails;
	private Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions;
	private boolean applicationType;
	private boolean genericType;
	private boolean rooUploadFileField;

	public JsfFieldHolder(final FieldMetadata field, final boolean enumerated, final String genericTypePlural, final Map<JavaType, String> genericTypes, final MemberDetails applicationTypeMemberDetails, final Map<MethodMetadataCustomDataKey, MemberTypeAdditions> crudAdditions) {
		Assert.notNull(field, "Field required");
		this.field = field;
		this.enumerated = enumerated;
		this.genericTypePlural = genericTypePlural;
		this.genericTypes = genericTypes;
		this.crudAdditions = crudAdditions;
		this.applicationTypeMemberDetails = applicationTypeMemberDetails;
		applicationType = this.applicationTypeMemberDetails != null && !CollectionUtils.isEmpty(this.crudAdditions);
		genericType = !CollectionUtils.isEmpty(this.genericTypes) && this.genericTypes.size() == 1;

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

	public String getGenericTypePlural() {
		return genericTypePlural;
	}

	public Map<JavaType, String> getGenericTypes() {
		return genericTypes;
	}

	public MemberDetails getApplicationTypeMemberDetails() {
		return applicationTypeMemberDetails;
	}

	public Map<MethodMetadataCustomDataKey, MemberTypeAdditions> getCrudAdditions() {
		return crudAdditions;
	}

	public boolean isApplicationType() {
		return applicationType;
	}

	public boolean isGenericType() {
		return genericType;
	}

	public boolean isRooUploadFileField() {
		return rooUploadFileField;
	}
}
