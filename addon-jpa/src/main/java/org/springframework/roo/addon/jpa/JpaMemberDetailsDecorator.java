package org.springframework.roo.addon.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.customdata.CustomDataPersistenceTags;
import org.springframework.roo.classpath.details.AbstractMemberHoldingTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsDecorator;
import org.springframework.roo.classpath.scanner.MemberDetailsImpl;
import org.springframework.roo.model.CustomDataBuilder;
import org.springframework.roo.model.JavaSymbolName;

/**
 * Decorates JPA-related {@link FieldMetadata} with custom data tags.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
@Service
@Component
public class JpaMemberDetailsDecorator implements MemberDetailsDecorator {

	public MemberDetails decorate(String requestingClass, MemberDetails memberDetails) {
		return decorateTypesAndFields(memberDetails);
	}

	private MemberDetails decorateTypesAndFields(MemberDetails memberDetails) {
		List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList = new ArrayList<MemberHoldingTypeDetails>(memberDetails.getDetails());
		boolean detailsChanged = false;
		for (MemberHoldingTypeDetails memberHoldingTypeDetails: memberDetails.getDetails()) {
			if (decorateFields(memberHoldingTypeDetailsList, memberHoldingTypeDetails)) {
				detailsChanged = true;
			}
		}
		return detailsChanged ? new MemberDetailsImpl(memberHoldingTypeDetailsList) : memberDetails;
	}

	private boolean decorateFields(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList, MemberHoldingTypeDetails memberHoldingTypeDetails) {
		boolean detailsChanged = false;
		for (FieldMetadata field : memberHoldingTypeDetails.getDeclaredFields()) {
			for (AnnotationMetadata annotation : field.getAnnotations()) {
				String annotationFullyQualifiedName = annotation.getAnnotationType().getFullyQualifiedTypeName();
				if (annotationFullyQualifiedName.equals("javax.persistence.ManyToMany") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.MANY_TO_MANY_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.MANY_TO_MANY_FIELD);
					detailsChanged = true;
				} else if (annotationFullyQualifiedName.equals("javax.persistence.Enumerated") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.ENUMERATED_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.ENUMERATED_FIELD);
					detailsChanged = true;
				} else if (annotationFullyQualifiedName.equals("javax.persistence.EmbeddedId") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.EMBEDDED_ID_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.EMBEDDED_ID_FIELD);
					detailsChanged = true;
				} else if (annotationFullyQualifiedName.equals("javax.persistence.OneToMany") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.ONE_TO_MANY_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.ONE_TO_MANY_FIELD);
					detailsChanged = true;
				} else if (annotationFullyQualifiedName.equals("javax.persistence.ManyToOne") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.MANY_TO_ONE_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.MANY_TO_ONE_FIELD);
					detailsChanged = true;
				} else if (annotationFullyQualifiedName.equals("javax.persistence.OneToOne") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.ONE_TO_ONE_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.ONE_TO_ONE_FIELD);
					detailsChanged = true;
				} else if (annotationFullyQualifiedName.equals("javax.persistence.Lob") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.LOB_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.LOB_FIELD);
					detailsChanged = true;
				} else if (annotationFullyQualifiedName.equals("javax.persistence.Transient") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.TRANSIENT_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.TRANSIENT_FIELD);
					detailsChanged = true;
				} else if (annotationFullyQualifiedName.equals("javax.persistence.Embedded") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.EMBEDDED_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.EMBEDDED_FIELD);
					detailsChanged = true;
				} else if (annotationFullyQualifiedName.equals("javax.persistence.Id") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.IDENTIFIER_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.IDENTIFIER_FIELD);
					detailsChanged = true;
				} else if (annotationFullyQualifiedName.equals("javax.persistence.Version") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.VERSION_FIELD)) {
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.VERSION_FIELD);
					detailsChanged = true;
				}
				// The @Column annotation can be used in combination with any of the annotations above
				if (annotationFullyQualifiedName.equals("javax.persistence.Column") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.COLUMN_FIELD)) {
					Map<String, Object> value = new HashMap<String, Object>();
					AnnotationAttributeValue<?> lengthAttributeValue = annotation.getAttribute(new JavaSymbolName("length"));
					if (lengthAttributeValue != null) {
						value.put("length", (Integer) annotation.getAttribute(new JavaSymbolName("length")).getValue());
					}
					AnnotationAttributeValue<?> precisionAttributeValue = annotation.getAttribute(new JavaSymbolName("precision"));
					if (precisionAttributeValue != null) {
						value.put("precision", (Integer) annotation.getAttribute(new JavaSymbolName("precision")).getValue());
					}
					AnnotationAttributeValue<?> scaleAttributeValue = annotation.getAttribute(new JavaSymbolName("scale"));
					if (scaleAttributeValue != null) {
						value.put("scale", (Integer) annotation.getAttribute(new JavaSymbolName("scale")).getValue());
					}
					AnnotationAttributeValue<?> nameAttributeValue = annotation.getAttribute(new JavaSymbolName("name"));
					if (nameAttributeValue != null) {
						value.put("name", (String) annotation.getAttribute(new JavaSymbolName("name")).getValue());
					}
					field = addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, memberHoldingTypeDetails, field, CustomDataPersistenceTags.COLUMN_FIELD, value);
					detailsChanged = true;
				}
			}
		}
		return detailsChanged;
	}

	private FieldMetadata addCustomizedMemberHoldingTypeDetailsForField(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList, MemberHoldingTypeDetails original, FieldMetadata originalField, Object tag) {
		return addCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetailsList, original, originalField, tag, null);
	}

	private FieldMetadata addCustomizedMemberHoldingTypeDetailsForField(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList, MemberHoldingTypeDetails original, FieldMetadata originalField, Object tag, Object value) {
		// Remove old MemberHoldingTypeDetails first
		removeMemberHoldingTypeDetailsFromList(memberHoldingTypeDetailsList, original);
		
		FieldMetadataBuilder newField = new FieldMetadataBuilder(originalField);
		CustomDataBuilder customDataBuilder = new CustomDataBuilder(originalField.getCustomData());
		customDataBuilder.put(tag, value);
		newField.setCustomData(customDataBuilder);
		List<FieldMetadataBuilder> fields = new ArrayList<FieldMetadataBuilder>();
		for (FieldMetadata field : original.getDeclaredFields()) {
			FieldMetadataBuilder fieldMetadataBuilder = new FieldMetadataBuilder(field);
			if (fieldMetadataBuilder.getDeclaredByMetadataId().equals(newField.getDeclaredByMetadataId()) && !fieldMetadataBuilder.getFieldName().equals(newField.getFieldName())) {
				fields.add(fieldMetadataBuilder);
			}
		}
		fields.add(newField);
		TypeDetailsBuilder typeDetailsBuilder = new TypeDetailsBuilder(original);
		typeDetailsBuilder.setDeclaredFields(fields);
		memberHoldingTypeDetailsList.add(typeDetailsBuilder.build());
		return newField.build();
	}

	private void removeMemberHoldingTypeDetailsFromList(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList, MemberHoldingTypeDetails memberHoldingTypeDetails) {
		List<MemberHoldingTypeDetails> toRemove = new ArrayList<MemberHoldingTypeDetails>();
		for (MemberHoldingTypeDetails aMemberHoldingTypeDetailsList : memberHoldingTypeDetailsList) {
			if (aMemberHoldingTypeDetailsList.getDeclaredByMetadataId().equals(memberHoldingTypeDetails.getDeclaredByMetadataId())) {
				toRemove.add(aMemberHoldingTypeDetailsList);
			}
		}
		memberHoldingTypeDetailsList.removeAll(toRemove);
	}

	class TypeDetailsBuilder extends AbstractMemberHoldingTypeDetailsBuilder<MemberHoldingTypeDetails> {
		private MemberHoldingTypeDetails existing;

		protected TypeDetailsBuilder(MemberHoldingTypeDetails existing) {
			super(existing.getDeclaredByMetadataId(), existing);
			this.existing = existing;
		}

		public MemberHoldingTypeDetails build() {
			if (existing instanceof ItdTypeDetails) {
				ItdTypeDetailsBuilder builder = new ItdTypeDetailsBuilder((ItdTypeDetails) existing);
				// We know we changed fields or type custom data, so let's make sure we add them to the builder
				builder.setDeclaredFields(this.getDeclaredFields());
				return builder.build();
			} else if (existing instanceof ClassOrInterfaceTypeDetails) {
				ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder((ClassOrInterfaceTypeDetails) existing);
				// We know we changed fields or type custom data, so let's make sure we add them to the builder
				builder.setDeclaredFields(this.getDeclaredFields());
				return builder.build();
			} else {
				throw new IllegalStateException("Unknown instance of MemberHoldingTypeDetails");
			}
		}
	}
}
