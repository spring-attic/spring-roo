package org.springframework.roo.addon.javabean;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DeclaredFieldAnnotationDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Metadata for {@link RooJavaBean}.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class JavaBeanMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = JavaBeanMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	// From annotation
	@AutoPopulate private boolean gettersByDefault = true;
	@AutoPopulate private boolean settersByDefault = true;

	private Map<FieldMetadata, FieldMetadata> declaredFields;

	public JavaBeanMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, Map<FieldMetadata, FieldMetadata> declaredFields) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		if (!isValid()) {
			return;
		}

		this.declaredFields = declaredFields;

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooJavaBean.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		// Add getters and setters
		for (FieldMetadata field : declaredFields.keySet()) {
			MethodMetadata accessorMethod = getDeclaredGetter(field);
			MethodMetadata mutatorMethod = getDeclaredSetter(field);

			// Check to see if GAE is interested
			if (declaredFields.get(field) != null) {
				JavaSymbolName hiddenIdFieldName;
				if (field.getFieldType().isCommonCollectionType()) {
					hiddenIdFieldName = getFieldName(field.getFieldName().getSymbolName() + "Keys");
					builder.getImportRegistrationResolver().addImport(new JavaType("com.google.appengine.api.datastore.KeyFactory"));
					builder.addField(getMultipleEntityIdField(hiddenIdFieldName));
				} else {
					hiddenIdFieldName = getFieldName(field.getFieldName().getSymbolName() + "Id");
					builder.addField(getSingularEntityIdField(hiddenIdFieldName));
				}

				processGaeAnnotations(field);

				MethodMetadataBuilder accessorMethodBuilder = new MethodMetadataBuilder(accessorMethod);
				accessorMethodBuilder.setBodyBuilder(getGaeAccessorBody(field, hiddenIdFieldName));
				accessorMethod = accessorMethodBuilder.build();

				MethodMetadataBuilder mutatorMethodBuilder = new MethodMetadataBuilder(mutatorMethod);
				mutatorMethodBuilder.setBodyBuilder(getGaeMutatorBody(field, hiddenIdFieldName));
				mutatorMethod = mutatorMethodBuilder.build();
			}

			builder.addMethod(accessorMethod);
			builder.addMethod(mutatorMethod);
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	/**
	 * Obtains the specific accessor method that is either contained within the normal Java compilation unit or will be introduced by this add-on via an ITD.
	 *
	 * @param field that already exists on the type either directly or via introduction (required; must be declared by this type to be located)
	 * @return the method corresponding to an accessor, or null if not found
	 */
	public MethodMetadata getDeclaredGetter(FieldMetadata field) {
		Assert.notNull(field, "Field required");

		// Compute the accessor method name
		JavaSymbolName methodName;

		if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE)) {
			methodName = new JavaSymbolName("is" + StringUtils.capitalize(field.getFieldName().getSymbolName()));
		} else {
			methodName = new JavaSymbolName("get" + StringUtils.capitalize(field.getFieldName().getSymbolName()));
		}

		// See if the type itself declared the accessor
		MethodMetadata result = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, null);
		if (result != null) {
			return result;
		}

		// Decide whether we need to produce the accessor method (see ROO-619 for reason we allow a getter for a final field)
		if (this.gettersByDefault && !Modifier.isTransient(field.getModifier()) && !Modifier.isStatic(field.getModifier())) {
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("return this." + field.getFieldName().getSymbolName() + ";");

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, field.getFieldType(), bodyBuilder);
			result = methodBuilder.build();
		}

		return result;
	}

	/**
	 * Obtains the specific mutator method that is either contained within the normal Java compilation unit or will be introduced by this add-on via an ITD.
	 *
	 * @param field that already exists on the type either directly or via introduction (required; must be declared by this type to be located)
	 * @return the method corresponding to a mutator, or null if not found
	 */
	public MethodMetadata getDeclaredSetter(FieldMetadata field) {
		Assert.notNull(field, "Field required");

		// Compute the mutator method name
		JavaSymbolName methodName = new JavaSymbolName("set" + StringUtils.capitalize(field.getFieldName().getSymbolName()));

		// Compute the mutator method parameters
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(field.getFieldType());

		// See if the type itself declared the mutator
		MethodMetadata result = MemberFindingUtils.getDeclaredMethod(governorTypeDetails, methodName, paramTypes);
		if (result != null) {
			return result;
		}

		// Compute the mutator method parameter names
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(field.getFieldName());

		// Decide whether we need to produce the mutator method (disallowed for final fields as per ROO-36)
		if (this.settersByDefault && !Modifier.isTransient(field.getModifier()) && !Modifier.isStatic(field.getModifier()) && !Modifier.isFinal(field.getModifier())) {
			InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("this." + field.getFieldName().getSymbolName() + " = " + field.getFieldName().getSymbolName() + ";");

			MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
			result = methodBuilder.build();
		}

		return result;
	}

	private InvocableMemberBodyBuilder getGaeAccessorBody(FieldMetadata field, JavaSymbolName hiddenIdFieldName) {
		InvocableMemberBodyBuilder bodyBuilder;

		if (field.getFieldType().isCommonCollectionType()) {
			bodyBuilder = getEntityCollectionAccessorBody(field, hiddenIdFieldName);
		} else {
			bodyBuilder = getSingularEntityAccessor(field, hiddenIdFieldName);
		}

		return bodyBuilder;
	}

	private InvocableMemberBodyBuilder getGaeMutatorBody(FieldMetadata field, JavaSymbolName hiddenIdFieldName) {
		InvocableMemberBodyBuilder bodyBuilder;

		if (field.getFieldType().isCommonCollectionType()) {
			bodyBuilder = getEntityCollectionMutatorBody(field, hiddenIdFieldName);
		} else {
			bodyBuilder = getSingularEntityMutator(field, hiddenIdFieldName);
		}

		return bodyBuilder;
	}

	private void processGaeAnnotations(FieldMetadata field) {
		for (AnnotationMetadata annotation : field.getAnnotations()) {
			if (annotation.getAnnotationType().equals(new JavaType("javax.persistence.OneToOne")) || annotation.getAnnotationType().equals(new JavaType("javax.persistence.ManyToOne")) || annotation.getAnnotationType().equals(new JavaType("javax.persistence.OneToMany")) || annotation.getAnnotationType().equals(new JavaType("javax.persistence.ManyToMany"))) {
				builder.addFieldAnnotation(new DeclaredFieldAnnotationDetails(field, new AnnotationMetadataBuilder(annotation.getAnnotationType()).build(), true));
				builder.addFieldAnnotation(new DeclaredFieldAnnotationDetails(field, new AnnotationMetadataBuilder(new JavaType("javax.persistence.Transient")).build()));
				break;
			}
		}
	}

	private JavaSymbolName getFieldName(String fieldName) {
		int index = -1;
		while (true) {
			// Compute the required field name
			index++;
			String tempString = "";
			for (int i = 0; i < index; i++) {
				tempString = tempString + "_";
			}
			fieldName = tempString + fieldName;

			JavaSymbolName field = new JavaSymbolName(fieldName);
			if (MemberFindingUtils.getField(governorTypeDetails, field) == null) {
				// Found a usable field name
				return field;
			}
		}
	}

	private FieldMetadata getSingularEntityIdField(JavaSymbolName fieldName) {
		FieldMetadataBuilder fieldMetadataBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName, JavaType.LONG_OBJECT, null);
		return fieldMetadataBuilder.build();
	}

	private FieldMetadata getMultipleEntityIdField(JavaSymbolName fieldName) {
		builder.getImportRegistrationResolver().addImport(new JavaType("java.util.HashSet"));
		FieldMetadataBuilder fieldMetadataBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName, new JavaType("java.util.Set", 0, DataType.TYPE, null, Collections.singletonList(new JavaType("com.google.appengine.api.datastore.Key"))), "new HashSet<Key>()");
		return fieldMetadataBuilder.build();
	}

	private InvocableMemberBodyBuilder getEntityCollectionMutatorBody(FieldMetadata field, JavaSymbolName entityIdsFieldName) {
		String entityCollectionName = field.getFieldName().getSymbolName();
		String entityIdsName = entityIdsFieldName.getSymbolName();
		JavaType collectionElementType = field.getFieldType().getParameters().get(0);
		String localEnitiesName = "local" + StringUtils.capitalize(entityCollectionName);

		JavaType collectionType = field.getFieldType();
		builder.getImportRegistrationResolver().addImport(collectionType);

		String collectionName = field.getFieldType().getNameIncludingTypeParameters().replaceAll(field.getFieldType().getPackage().getFullyQualifiedPackageName() + ".", "");
		String instantiableCollection = collectionName;
		if (collectionType.getFullyQualifiedTypeName().equals("java.util.List")) {
			collectionType = new JavaType("java.util.ArrayList", 0, DataType.TYPE, null, collectionType.getParameters());
			instantiableCollection = collectionType.getNameIncludingTypeParameters().replaceAll(collectionType.getPackage().getFullyQualifiedPackageName() + ".", "");
		} else if (collectionType.getFullyQualifiedTypeName().equals("java.util.Set")) {
			collectionType = new JavaType("java.util.HashSet", 0, DataType.TYPE, null, collectionType.getParameters());
			instantiableCollection = collectionType.getNameIncludingTypeParameters().replaceAll(collectionType.getPackage().getFullyQualifiedPackageName() + ".", "");
		}

		builder.getImportRegistrationResolver().addImport(collectionType);
		builder.getImportRegistrationResolver().addImport(new JavaType("java.util.List"));
		builder.getImportRegistrationResolver().addImport(new JavaType("java.util.ArrayList"));

		String identifierMethodName = getIdentifierMethodName(field);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(collectionName + " " + localEnitiesName + " = new " + instantiableCollection + "();");
		bodyBuilder.appendFormalLine("List<Long> longIds = new ArrayList<Long>();");
		bodyBuilder.appendFormalLine("for (Key key : " + entityIdsName + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (!longIds.contains(key.getId())) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("longIds.add(key.getId());");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("for (" + collectionElementType.getSimpleTypeName() + " entity : " + entityCollectionName + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (!longIds.contains(entity." + identifierMethodName + "())) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("longIds.add(entity." + identifierMethodName + "());");
		bodyBuilder.appendFormalLine(entityIdsName + ".add(KeyFactory.createKey(" + collectionElementType.getSimpleTypeName() + ".class.getName(), entity." + identifierMethodName + "()));");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(localEnitiesName + ".add(entity);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("this." + entityCollectionName + " = " + localEnitiesName + ";");

		return bodyBuilder;
	}

	private String getIdentifierMethodName(FieldMetadata fieldMetadata) {
		MethodMetadata identifierMethod = getIdentifierMethod(fieldMetadata);
		if (identifierMethod != null) {
			return identifierMethod.getMethodName().getSymbolName();
		}
		return "getId";
	}

	private MethodMetadata getIdentifierMethod(FieldMetadata fieldMetadata) {
		FieldMetadata identifierField = declaredFields.get(fieldMetadata);
		if (identifierField != null) {
			return getDeclaredGetter(identifierField);
		}
		return null;
	}

	private InvocableMemberBodyBuilder getEntityCollectionAccessorBody(FieldMetadata field, JavaSymbolName entityIdsFieldName) {
		String entityCollectionName = field.getFieldName().getSymbolName();
		String entityIdsName = entityIdsFieldName.getSymbolName();
		String localEnitiesName = "local" + StringUtils.capitalize(entityCollectionName);

		JavaType collectionElementType = field.getFieldType().getParameters().get(0);
		String simpleCollectionElementTypeName = collectionElementType.getSimpleTypeName();

		JavaType collectionType = field.getFieldType();
		builder.getImportRegistrationResolver().addImport(collectionType);

		String collectionName = field.getFieldType().getNameIncludingTypeParameters().replaceAll(field.getFieldType().getPackage().getFullyQualifiedPackageName() + ".", "");
		String instantiableCollection = collectionName;

		// GAE only supports java.util.List and java.util.Set collections and we need a concrete implementation of either.
		if (collectionType.getFullyQualifiedTypeName().equals("java.util.List")) {
			collectionType = new JavaType("java.util.ArrayList", 0, DataType.TYPE, null, collectionType.getParameters());
			instantiableCollection = collectionType.getNameIncludingTypeParameters().replaceAll(collectionType.getPackage().getFullyQualifiedPackageName() + ".", "");
		} else if (collectionType.getFullyQualifiedTypeName().equals("java.util.Set")) {
			collectionType = new JavaType("java.util.HashSet", 0, DataType.TYPE, null, collectionType.getParameters());
			instantiableCollection = collectionType.getNameIncludingTypeParameters().replaceAll(collectionType.getPackage().getFullyQualifiedPackageName() + ".", "");
		}

		builder.getImportRegistrationResolver().addImport(collectionType);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(collectionName + " " + localEnitiesName + " = new " + instantiableCollection + "();");
		bodyBuilder.appendFormalLine("for (Key key : " + entityIdsName + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(collectionElementType + " entity = " + simpleCollectionElementTypeName + ".find" + simpleCollectionElementTypeName + "(key.getId());");
		bodyBuilder.appendFormalLine("if (entity != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(localEnitiesName + ".add(entity);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("this." + entityCollectionName + " = " + localEnitiesName + ";");
		bodyBuilder.appendFormalLine("return " + localEnitiesName + ";");

		return bodyBuilder;
	}

	private InvocableMemberBodyBuilder getSingularEntityMutator(FieldMetadata field, JavaSymbolName hiddenIdFieldName) {
		String entityName = field.getFieldName().getSymbolName();
		String entityIdName = hiddenIdFieldName.getSymbolName();

		String identifierMethodName = getIdentifierMethodName(field);

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + entityName + " != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (" + entityName + "." + identifierMethodName + " () == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(entityName + ".persist();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("this." + entityIdName + " = " + entityName + "." + identifierMethodName + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("this." + entityIdName + " = null;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");

		return bodyBuilder;
	}

	private InvocableMemberBodyBuilder getSingularEntityAccessor(FieldMetadata field, JavaSymbolName hiddenIdFieldName) {
		String entityName = field.getFieldName().getSymbolName();
		String entityIdName = hiddenIdFieldName.getSymbolName();
		String simpleFieldTypeName = field.getFieldType().getSimpleTypeName();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (this." + entityIdName + " != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("this." + entityName + " = " + simpleFieldTypeName + ".find" + simpleFieldTypeName + "(this." + entityIdName + ");");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("this." + entityName + " = null;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return this." + entityName + ";");

		return bodyBuilder;
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
