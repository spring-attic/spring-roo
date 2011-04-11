package org.springframework.roo.classpath.scanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.roo.model.CustomDataKey;
import org.springframework.roo.classpath.details.AbstractMemberHoldingTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetailsBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.CustomDataBuilder;

/**
 * Builder for {@link MemberDetails}.
 *
 * @author Alan Stewart
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.1.3
 */
public class MemberDetailsBuilder {
	private Map<String, MemberHoldingTypeDetails> memberHoldingTypeDetailsMap = new HashMap<String, MemberHoldingTypeDetails>();
	private MemberDetails originalMemberDetails;
	private boolean changed = false;

	public MemberDetailsBuilder(MemberDetails memberDetails) {
		this.originalMemberDetails = memberDetails;
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			memberHoldingTypeDetailsMap.put(memberHoldingTypeDetails.getDeclaredByMetadataId(), memberHoldingTypeDetails);
		}
	}

	public MemberDetails build() {
		return changed ? new MemberDetailsImpl(new ArrayList<MemberHoldingTypeDetails>(memberHoldingTypeDetailsMap.values())) : originalMemberDetails;
	}

	public <T> void tag(T toModify, CustomDataKey<T> key, Object value) {
		if (toModify instanceof FieldMetadata) {
			CustomDataBuilder customDataBuilder = new CustomDataBuilder();
			customDataBuilder.put(key, value);
			doModification((FieldMetadata) toModify, customDataBuilder.build());
		} else if (toModify instanceof MethodMetadata) {
			CustomDataBuilder customDataBuilder = new CustomDataBuilder();
			customDataBuilder.put(key, value);
			doModification((MethodMetadata) toModify, customDataBuilder.build());
		} else if (toModify instanceof ConstructorMetadata) {
			CustomDataBuilder customDataBuilder = new CustomDataBuilder();
			customDataBuilder.put(key, value);
			doModification((ConstructorMetadata) toModify, customDataBuilder.build());
		} else if (toModify instanceof MemberHoldingTypeDetails) {
			CustomDataBuilder customDataBuilder = new CustomDataBuilder();
			customDataBuilder.put(key, value);
			doModification((MemberHoldingTypeDetails) toModify, customDataBuilder.build());
		}
	}

	private void doModification(FieldMetadata field, CustomData customData) {
		MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap.get(field.getDeclaredByMetadataId());
		if (memberHoldingTypeDetails != null) {
			if (memberHoldingTypeDetails.getDeclaredByMetadataId().equals(field.getDeclaredByMetadataId())) {
				FieldMetadata matchedField = MemberFindingUtils.getField(memberHoldingTypeDetails, field.getFieldName());
				if (matchedField != null && !matchedField.getCustomData().keySet().containsAll(customData.keySet())) {
					TypeDetailsBuilder typeDetailsBuilder = new TypeDetailsBuilder(memberHoldingTypeDetails);
					typeDetailsBuilder.addDataToField(field, customData);
					memberHoldingTypeDetailsMap.put(field.getDeclaredByMetadataId(), typeDetailsBuilder.build());
					changed = true;
				}
			}
		}
	}

	private void doModification(MethodMetadata method, CustomData customData) {
		MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap.get(method.getDeclaredByMetadataId());
		if (memberHoldingTypeDetails != null) {
			if (memberHoldingTypeDetails.getDeclaredByMetadataId().equals(method.getDeclaredByMetadataId())) {
				MethodMetadata matchedMethod = MemberFindingUtils.getMethod(memberHoldingTypeDetails, method.getMethodName(), AnnotatedJavaType.convertFromAnnotatedJavaTypes(method.getParameterTypes()));
				if (matchedMethod != null && !matchedMethod.getCustomData().keySet().containsAll(customData.keySet())) {
					TypeDetailsBuilder typeDetailsBuilder = new TypeDetailsBuilder(memberHoldingTypeDetails);
					typeDetailsBuilder.addDataToMethod(method, customData);
					memberHoldingTypeDetailsMap.put(method.getDeclaredByMetadataId(), typeDetailsBuilder.build());
					changed = true;
				}
			}
		}
	}

	private void doModification(ConstructorMetadata constructor, CustomData customData) {
		MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap.get(constructor.getDeclaredByMetadataId());
		if (memberHoldingTypeDetails != null) {
			if (memberHoldingTypeDetails.getDeclaredByMetadataId().equals(constructor.getDeclaredByMetadataId())) {
				ConstructorMetadata matchedConstructor = MemberFindingUtils.getDeclaredConstructor(memberHoldingTypeDetails, AnnotatedJavaType.convertFromAnnotatedJavaTypes(constructor.getParameterTypes()));
				if (matchedConstructor != null && !matchedConstructor.getCustomData().keySet().containsAll(customData.keySet())) {
					TypeDetailsBuilder typeDetailsBuilder = new TypeDetailsBuilder(memberHoldingTypeDetails);
					typeDetailsBuilder.addDataToConstructor(constructor, customData);
					memberHoldingTypeDetailsMap.put(constructor.getDeclaredByMetadataId(), typeDetailsBuilder.build());
					changed = true;
				}
			}
		}

	}

	private void doModification(MemberHoldingTypeDetails type, CustomData customData) {
		MemberHoldingTypeDetails memberHoldingTypeDetails = memberHoldingTypeDetailsMap.get(type.getDeclaredByMetadataId());
		if (memberHoldingTypeDetails != null) {
			if (memberHoldingTypeDetails.getDeclaredByMetadataId().equals(type.getDeclaredByMetadataId())) {
				if (memberHoldingTypeDetails.getName().equals(type.getName()) && !memberHoldingTypeDetails.getCustomData().keySet().containsAll(customData.keySet())) {
					TypeDetailsBuilder typeDetailsBuilder = new TypeDetailsBuilder(memberHoldingTypeDetails);
					typeDetailsBuilder.getCustomData().append(customData);
					memberHoldingTypeDetailsMap.put(type.getDeclaredByMetadataId(), typeDetailsBuilder.build());
					changed = true;
				}
			}
		}

	}

	class TypeDetailsBuilder extends AbstractMemberHoldingTypeDetailsBuilder<MemberHoldingTypeDetails> {
		private MemberHoldingTypeDetails existing;

		protected TypeDetailsBuilder(MemberHoldingTypeDetails existing) {
			super(existing);
			this.existing = existing;
		}

		public MemberHoldingTypeDetails build() {
			if (existing instanceof ItdTypeDetails) {
				ItdTypeDetailsBuilder builder = new ItdTypeDetailsBuilder((ItdTypeDetails) existing);
				// Push in all members that may have been modified
				builder.setDeclaredFields(this.getDeclaredFields());
				builder.setDeclaredMethods(this.getDeclaredMethods());
				builder.setAnnotations(this.getAnnotations());
				builder.setCustomData(this.getCustomData());
				builder.setDeclaredConstructors(this.getDeclaredConstructors());
				builder.setDeclaredInitializers(this.getDeclaredInitializers());
				builder.setDeclaredInnerTypes(this.getDeclaredInnerTypes());
				builder.setExtendsTypes(this.getExtendsTypes());
				builder.setImplementsTypes(this.getImplementsTypes());
				builder.setModifier(this.getModifier());
				return builder.build();
			} else if (existing instanceof ClassOrInterfaceTypeDetails) {
				ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder((ClassOrInterfaceTypeDetails) existing);
				// Push in all members that may
				builder.setDeclaredFields(this.getDeclaredFields());
				builder.setDeclaredMethods(this.getDeclaredMethods());
				builder.setAnnotations(this.getAnnotations());
				builder.setCustomData(this.getCustomData());
				builder.setDeclaredConstructors(this.getDeclaredConstructors());
				builder.setDeclaredInitializers(this.getDeclaredInitializers());
				builder.setDeclaredInnerTypes(this.getDeclaredInnerTypes());
				builder.setExtendsTypes(this.getExtendsTypes());
				builder.setImplementsTypes(this.getImplementsTypes());
				builder.setModifier(this.getModifier());
				return builder.build();
			} else {
				throw new IllegalStateException("Unknown instance of MemberHoldingTypeDetails");
			}
		}

		public void addDataToField(FieldMetadata replacement, CustomData customData) {
			// If the MIDs don't match then the proposed can't be a replacement
			if (!replacement.getDeclaredByMetadataId().equals(getDeclaredByMetadataId())) {
				return;
			}
			for (FieldMetadataBuilder existingField : getDeclaredFields()) {
				if (existingField.getFieldName().equals(replacement.getFieldName())) {
					for (Object key : customData.keySet()) {
						existingField.putCustomData(key, customData.get(key));
					}
					break;
				}
			}
		}

		public void addDataToMethod(MethodMetadata replacement, CustomData customData) {
			// If the MIDs don't match then the proposed can't be a replacement
			if (!replacement.getDeclaredByMetadataId().equals(getDeclaredByMetadataId())) {
				return;
			}
			for (MethodMetadataBuilder existingMethod : getDeclaredMethods()) {
				if (existingMethod.getMethodName().equals(replacement.getMethodName())) {
					if (AnnotatedJavaType.convertFromAnnotatedJavaTypes(existingMethod.getParameterTypes()).equals(AnnotatedJavaType.convertFromAnnotatedJavaTypes(replacement.getParameterTypes()))) {
						for (Object key : customData.keySet()) {
							existingMethod.putCustomData(key, customData.get(key));
						}
						break;
					}
				}
			}
		}

		public void addDataToConstructor(ConstructorMetadata replacement, CustomData customData) {
			// If the MIDs don't match then the proposed can't be a replacement
			if (!replacement.getDeclaredByMetadataId().equals(getDeclaredByMetadataId())) {
				return;
			}
			for (ConstructorMetadataBuilder existingConstructor : getDeclaredConstructors()) {
				if (AnnotatedJavaType.convertFromAnnotatedJavaTypes(existingConstructor.getParameterTypes()).equals(AnnotatedJavaType.convertFromAnnotatedJavaTypes(replacement.getParameterTypes()))) {
					for (Object key : customData.keySet()) {
						existingConstructor.putCustomData(key, customData.get(key));
					}
					break;
				}
			}
		}
	}
}
