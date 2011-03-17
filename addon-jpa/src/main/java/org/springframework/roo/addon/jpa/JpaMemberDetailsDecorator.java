package org.springframework.roo.addon.jpa;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsDecorator;
import org.springframework.roo.classpath.scanner.MemberDetailsImpl;
import org.springframework.roo.model.CustomDataBuilder;
import org.springframework.roo.model.JavaType;

/**
 * Decorates JPA-related field metadata with custom data tags.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 *
 */
@Service
@Component
public class JpaMemberDetailsDecorator implements MemberDetailsDecorator {

	public MemberDetails decorate(String requestingClass, MemberDetails memberDetails) {
		//decorate fields
		memberDetails = decorateFields(memberDetails);
		
		return memberDetails;
	}
	
	private MemberDetails decorateFields(MemberDetails memberDetails) {
		List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList = new ArrayList<MemberHoldingTypeDetails>(memberDetails.getDetails());
		boolean detailsChanged = false;
		for (int i = 0; i < memberHoldingTypeDetailsList.size(); i++) { // Cannot use enhanced for loop due to its use of Iterator which in turn has a bug in remove method
			MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsList.get(i);
			if (MemberFindingUtils.getAnnotationOfType(memberHoldingTypeDetails.getAnnotations(), new JavaType("org.springframework.roo.addon.entity.RooEntity")) != null) {
				for (FieldMetadata field: memberHoldingTypeDetails.getDeclaredFields()) {
					for (AnnotationMetadata annotation: field.getAnnotations()) {
						String annotationFullyQualifiedName = annotation.getAnnotationType().getFullyQualifiedTypeName();
						if (annotationFullyQualifiedName.equals("javax.persistence.ManyToMany") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.MANY_TO_MANY_FIELD)) {
							// Remove the old MemberHoldingTypeDetails
							removeMemberHoldingTypeDetailsFromList(memberHoldingTypeDetailsList, memberHoldingTypeDetails);
							// Add new MemberHoldingTypeDetails
							memberHoldingTypeDetailsList.add(getCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetails, field, CustomDataPersistenceTags.MANY_TO_MANY_FIELD));
							detailsChanged = true;
						} else if (annotationFullyQualifiedName.equals("javax.persistence.Enumerated") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.ENUMERATED_FIELD)) {
							// Remove the old MemberHoldingTypeDetails
							removeMemberHoldingTypeDetailsFromList(memberHoldingTypeDetailsList, memberHoldingTypeDetails);
							// Add new MemberHoldingTypeDetails
							memberHoldingTypeDetailsList.add(getCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetails, field, CustomDataPersistenceTags.ENUMERATED_FIELD));
							detailsChanged = true;
						} else if (annotationFullyQualifiedName.equals("javax.persistence.EmbeddedId") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.EMBEDDED_ID_FIELD)) {
							// Remove the old MemberHoldingTypeDetails
							removeMemberHoldingTypeDetailsFromList(memberHoldingTypeDetailsList, memberHoldingTypeDetails);
							// Add new MemberHoldingTypeDetails
							memberHoldingTypeDetailsList.add(getCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetails, field, CustomDataPersistenceTags.EMBEDDED_ID_FIELD));
							detailsChanged = true;
						} else if (annotationFullyQualifiedName.equals("javax.persistence.OneToMany") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.ONE_TO_MANY_FIELD)) {
							// Remove the old MemberHoldingTypeDetails
							removeMemberHoldingTypeDetailsFromList(memberHoldingTypeDetailsList, memberHoldingTypeDetails);
							// Add new MemberHoldingTypeDetails
							memberHoldingTypeDetailsList.add(getCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetails, field, CustomDataPersistenceTags.ONE_TO_MANY_FIELD));
							detailsChanged = true;
						} else if (annotationFullyQualifiedName.equals("javax.persistence.ManyToOne") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.MANY_TO_ONE_FIELD)) {
							// Remove the old MemberHoldingTypeDetails
							removeMemberHoldingTypeDetailsFromList(memberHoldingTypeDetailsList, memberHoldingTypeDetails);
							// Add new MemberHoldingTypeDetails
							memberHoldingTypeDetailsList.add(getCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetails, field, CustomDataPersistenceTags.MANY_TO_ONE_FIELD));
							detailsChanged = true;
						} else if (annotationFullyQualifiedName.equals("javax.persistence.OneToOne") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.ONE_TO_ONE_FIELD)) {
							// Remove the old MemberHoldingTypeDetails
							removeMemberHoldingTypeDetailsFromList(memberHoldingTypeDetailsList, memberHoldingTypeDetails);
							// Add new MemberHoldingTypeDetails
							memberHoldingTypeDetailsList.add(getCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetails, field, CustomDataPersistenceTags.ONE_TO_ONE_FIELD));
							detailsChanged = true;
						} else if (annotationFullyQualifiedName.equals("javax.persistence.Lob") && !field.getCustomData().keySet().contains(CustomDataPersistenceTags.LOB_FIELD)) {
							// Remove the old MemberHoldingTypeDetails
							removeMemberHoldingTypeDetailsFromList(memberHoldingTypeDetailsList, memberHoldingTypeDetails);
							// Add new MemberHoldingTypeDetails
							memberHoldingTypeDetailsList.add(getCustomizedMemberHoldingTypeDetailsForField(memberHoldingTypeDetails, field, CustomDataPersistenceTags.LOB_FIELD));
							detailsChanged = true;
						} 
					}
				}
			}
		}
		if (detailsChanged) {
//			System.out.println("details changed");
			return new MemberDetailsImpl(memberHoldingTypeDetailsList);
		} else {
			return memberDetails;
		}
	}
	
	private MemberHoldingTypeDetails getCustomizedMemberHoldingTypeDetailsForField(MemberHoldingTypeDetails original, FieldMetadata originalField, Object tag) {
		FieldMetadataBuilder newField = new FieldMetadataBuilder(originalField);
		CustomDataBuilder customDataBuilder = new CustomDataBuilder(originalField.getCustomData());
		customDataBuilder.put(tag, null);
		newField.setCustomData(customDataBuilder);
		List<FieldMetadataBuilder> fields = new ArrayList<FieldMetadataBuilder>();
		for (FieldMetadata field: original.getDeclaredFields()) {
			FieldMetadataBuilder fieldMetadataBuilder = new FieldMetadataBuilder(field);
			if (!fieldMetadataBuilder.equals(newField)) {
				fields.add(fieldMetadataBuilder);
			}
		}
		fields.add(newField);
		
		TypeDetailsBuilder typeDetailsBuilder = new TypeDetailsBuilder(original);
		typeDetailsBuilder.setDeclaredFields(fields);
		return typeDetailsBuilder.build();
	}
	
	private void removeMemberHoldingTypeDetailsFromList(List<MemberHoldingTypeDetails> memberHoldingTypeDetailsList, MemberHoldingTypeDetails memberHoldingTypeDetails) {
		for (int i = 0; i < memberHoldingTypeDetailsList.size(); i++) {
			if (memberHoldingTypeDetailsList.get(i).getDeclaredByMetadataId().equals(memberHoldingTypeDetails.getDeclaredByMetadataId())) {
				memberHoldingTypeDetailsList.remove(i);
			}
		}
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
				builder.setDeclaredFields(this.getDeclaredFields());
				return builder.build();
			} else if (existing instanceof ClassOrInterfaceTypeDetails) {
				ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder((ClassOrInterfaceTypeDetails) existing);
				builder.setDeclaredFields(this.getDeclaredFields());
				return builder.build();
			} else {
				throw new IllegalStateException("Unknown instance of MemberHoldingTypeDetails");
			}
		}
	}
}
