package org.springframework.roo.addon.javabean;

import static org.springframework.roo.model.GoogleJavaType.GAE_DATASTORE_KEY;
import static org.springframework.roo.model.GoogleJavaType.GAE_DATASTORE_KEY_FACTORY;
import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;
import static org.springframework.roo.model.JdkJavaType.HASH_SET;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.SET;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.MANY_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_MANY;
import static org.springframework.roo.model.JpaJavaType.ONE_TO_ONE;
import static org.springframework.roo.model.JpaJavaType.TRANSIENT;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.DeclaredFieldAnnotationDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooJavaBean}.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
public class JavaBeanMetadata extends
		AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = JavaBeanMetadata.class
			.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils
			.create(PROVIDES_TYPE_STRING);

	public static String createIdentifier(final JavaType javaType,
			final LogicalPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(
				PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(
				PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static LogicalPath getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
				metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
				metadataIdentificationString);
	}

	private JavaBeanAnnotationValues annotationValues;

	private Map<FieldMetadata, JavaSymbolName> declaredFields;

	private List<? extends MethodMetadata> interfaceMethods;

	/**
	 * Constructor
	 * 
	 * @param identifier
	 *            the ID of the metadata to create (must be a valid ID)
	 * @param aspectName
	 *            the name of the ITD to be created (required)
	 * @param governorPhysicalTypeMetadata
	 *            the governor (required)
	 * @param annotationValues
	 *            the values of the {@link RooJavaBean} annotation (required)
	 * @param declaredFields
	 *            the fields declared in the governor (required, can be empty)
	 */
	public JavaBeanMetadata(final String identifier, final JavaType aspectName,
			final PhysicalTypeMetadata governorPhysicalTypeMetadata,
			final JavaBeanAnnotationValues annotationValues,
			final Map<FieldMetadata, JavaSymbolName> declaredFields,
			List<? extends MethodMetadata> interfaceMethods) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Validate.isTrue(
				isValid(identifier),
				"Metadata identification string '%s' does not appear to be a valid",
				identifier);
		Validate.notNull(annotationValues, "Annotation values required");
		Validate.notNull(declaredFields, "Declared fields required");

		if (!isValid()) {
			return;
		}
		if (declaredFields.isEmpty()) {
			return; // N.B. the MD is still valid, just empty
		}

		this.annotationValues = annotationValues;
		this.declaredFields = declaredFields;
		this.interfaceMethods = interfaceMethods;

		// Add getters and setters
		for (final Entry<FieldMetadata, JavaSymbolName> entry : declaredFields
				.entrySet()) {
			final FieldMetadata field = entry.getKey();
			final MethodMetadataBuilder accessorMethod = getDeclaredGetter(field);
			final MethodMetadataBuilder mutatorMethod = getDeclaredSetter(field);

			// Check to see if GAE is interested
			if (entry.getValue() != null) {
				JavaSymbolName hiddenIdFieldName;
				if (field.getFieldType().isCommonCollectionType()) {
					hiddenIdFieldName = governorTypeDetails
							.getUniqueFieldName(field.getFieldName()
									.getSymbolName() + "Keys");
					builder.getImportRegistrationResolver().addImport(
							GAE_DATASTORE_KEY_FACTORY);
					builder.addField(getMultipleEntityIdField(hiddenIdFieldName));
				} else {
					hiddenIdFieldName = governorTypeDetails
							.getUniqueFieldName(field.getFieldName()
									.getSymbolName() + "Id");
					builder.addField(getSingularEntityIdField(hiddenIdFieldName));
				}

				processGaeAnnotations(field);

				accessorMethod.setBodyBuilder(getGaeAccessorBody(field,
						hiddenIdFieldName));
				mutatorMethod.setBodyBuilder(getGaeMutatorBody(field,
						hiddenIdFieldName));
			}

			builder.addMethod(accessorMethod);
			builder.addMethod(mutatorMethod);
		}

		// Implements interface methods if exists
		if (interfaceMethods != null) {
			for (MethodMetadata interfaceMethod : interfaceMethods) {
				MethodMetadataBuilder methodBuilder = getInterfaceMethod(interfaceMethod);
				// ROO-3584: JavaBean implementing Interface defining getters and setters
				if(!checkIfInterfaceMethodWasImplemented(methodBuilder)){
					builder.addMethod(methodBuilder);
				}
			}
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	/**
	 * Obtains the specific accessor method that is either contained within the
	 * normal Java compilation unit or will be introduced by this add-on via an
	 * ITD.
	 * 
	 * @param field
	 *            that already exists on the type either directly or via
	 *            introduction (required; must be declared by this type to be
	 *            located)
	 * @return the method corresponding to an accessor, or null if not found
	 */
	private MethodMetadataBuilder getDeclaredGetter(final FieldMetadata field) {
		Validate.notNull(field, "Field required");

		// Compute the mutator method name
		final JavaSymbolName methodName = BeanInfoUtils
				.getAccessorMethodName(field);

		// See if the type itself declared the accessor
		if (governorHasMethod(methodName)) {
			return null;
		}

		// Decide whether we need to produce the accessor method (see ROO-619
		// for reason we allow a getter for a final field)
		if (annotationValues.isGettersByDefault()
				&& !Modifier.isTransient(field.getModifier())
				&& !Modifier.isStatic(field.getModifier())) {
			final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("return this."
					+ field.getFieldName().getSymbolName() + ";");

			return new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
					methodName, field.getFieldType(), bodyBuilder);
		}

		return null;
	}

	/**
	 * Obtains the specific mutator method that is either contained within the
	 * normal Java compilation unit or will be introduced by this add-on via an
	 * ITD.
	 * 
	 * @param field
	 *            that already exists on the type either directly or via
	 *            introduction (required; must be declared by this type to be
	 *            located)
	 * @return the method corresponding to a mutator, or null if not found
	 */
	private MethodMetadataBuilder getDeclaredSetter(final FieldMetadata field) {
		Validate.notNull(field, "Field required");

		// Compute the mutator method name
		final JavaSymbolName methodName = BeanInfoUtils
				.getMutatorMethodName(field);

		// Compute the mutator method parameters
		final JavaType parameterType = field.getFieldType();

		// See if the type itself declared the mutator
		if (governorHasMethod(methodName, parameterType)) {
			return null;
		}

		// Compute the mutator method parameter names
		final List<JavaSymbolName> parameterNames = Arrays.asList(field
				.getFieldName());

		// Decide whether we need to produce the mutator method (disallowed for
		// final fields as per ROO-36)
		if (annotationValues.isSettersByDefault()
				&& !Modifier.isTransient(field.getModifier())
				&& !Modifier.isStatic(field.getModifier())
				&& !Modifier.isFinal(field.getModifier())) {
			final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
			bodyBuilder.appendFormalLine("this."
					+ field.getFieldName().getSymbolName() + " = "
					+ field.getFieldName().getSymbolName() + ";");

			return new MethodMetadataBuilder(getId(), Modifier.PUBLIC,
					methodName, JavaType.VOID_PRIMITIVE,
					AnnotatedJavaType.convertFromJavaTypes(parameterType),
					parameterNames, bodyBuilder);
		}

		return null;
	}

	/**
	 * Obtains a valid MethodMetadataBuilder with necessary configuration
	 * 
	 * @param method
	 * @return MethodMetadataBuilder
	 */
	private MethodMetadataBuilder getInterfaceMethod(final MethodMetadata method) {

		// Compute the method name
		final JavaSymbolName methodName = method.getMethodName();
		// See if the type itself declared the accessor
		if (governorHasMethod(methodName)) {
			return null;
		}
		// Getting return type
		JavaType returnType = method.getReturnType();
		// Generating body
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		// If return type is not primitive, return null
		if (returnType.isPrimitive()) {
			JavaType baseType = returnType.getBaseType();
			if (baseType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("return false;");
			} else if (baseType.equals(JavaType.BYTE_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("return 0;");
			} else if (baseType.equals(JavaType.SHORT_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("return 0;");
			} else if (baseType.equals(JavaType.INT_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("return 0;");
			} else if (baseType.equals(JavaType.LONG_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("return 0;");
			} else if (baseType.equals(JavaType.FLOAT_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("return 0;");
			} else if (baseType.equals(JavaType.DOUBLE_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("return 0.00;");
			} else if (baseType.equals(JavaType.CHAR_PRIMITIVE)) {
				bodyBuilder.appendFormalLine("return '\0';");
			}
		} else {
			bodyBuilder.appendFormalLine("return null;");
		}
		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName,
				returnType, bodyBuilder);

	}

	private InvocableMemberBodyBuilder getEntityCollectionAccessorBody(
			final FieldMetadata field, final JavaSymbolName entityIdsFieldName) {
		final String entityCollectionName = field.getFieldName()
				.getSymbolName();
		final String entityIdsName = entityIdsFieldName.getSymbolName();
		final String localEnitiesName = "local"
				+ StringUtils.capitalize(entityCollectionName);

		final JavaType collectionElementType = field.getFieldType()
				.getParameters().get(0);
		final String simpleCollectionElementTypeName = collectionElementType
				.getSimpleTypeName();

		JavaType collectionType = field.getFieldType();
		builder.getImportRegistrationResolver().addImport(collectionType);

		final String collectionName = field
				.getFieldType()
				.getNameIncludingTypeParameters()
				.replace(
						field.getFieldType().getPackage()
								.getFullyQualifiedPackageName()
								+ ".", "");
		String instantiableCollection = collectionName;

		// GAE only supports java.util.List and java.util.Set collections and we
		// need a concrete implementation of either.
		if (collectionType.getFullyQualifiedTypeName().equals(
				LIST.getFullyQualifiedTypeName())) {
			collectionType = new JavaType(
					ARRAY_LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE,
					null, collectionType.getParameters());
			instantiableCollection = collectionType
					.getNameIncludingTypeParameters().replace(
							collectionType.getPackage()
									.getFullyQualifiedPackageName() + ".", "");
		} else if (collectionType.getFullyQualifiedTypeName().equals(
				SET.getFullyQualifiedTypeName())) {
			collectionType = new JavaType(HASH_SET.getFullyQualifiedTypeName(),
					0, DataType.TYPE, null, collectionType.getParameters());
			instantiableCollection = collectionType
					.getNameIncludingTypeParameters().replace(
							collectionType.getPackage()
									.getFullyQualifiedPackageName() + ".", "");
		}

		builder.getImportRegistrationResolver().addImport(collectionType);

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(collectionName + " " + localEnitiesName
				+ " = new " + instantiableCollection + "();");
		bodyBuilder.appendFormalLine("for (Key key : " + entityIdsName + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(simpleCollectionElementTypeName
				+ " entity = " + simpleCollectionElementTypeName + ".find"
				+ simpleCollectionElementTypeName + "(key.getId());");
		bodyBuilder.appendFormalLine("if (entity != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(localEnitiesName + ".add(entity);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("this." + entityCollectionName + " = "
				+ localEnitiesName + ";");
		bodyBuilder.appendFormalLine("return " + localEnitiesName + ";");

		return bodyBuilder;
	}

	private InvocableMemberBodyBuilder getEntityCollectionMutatorBody(
			final FieldMetadata field, final JavaSymbolName entityIdsFieldName) {
		final String entityCollectionName = field.getFieldName()
				.getSymbolName();
		final String entityIdsName = entityIdsFieldName.getSymbolName();
		final JavaType collectionElementType = field.getFieldType()
				.getParameters().get(0);
		final String localEnitiesName = "local"
				+ StringUtils.capitalize(entityCollectionName);

		JavaType collectionType = field.getFieldType();
		builder.getImportRegistrationResolver().addImport(collectionType);

		final String collectionName = field
				.getFieldType()
				.getNameIncludingTypeParameters()
				.replace(
						field.getFieldType().getPackage()
								.getFullyQualifiedPackageName()
								+ ".", "");
		String instantiableCollection = collectionName;
		if (collectionType.getFullyQualifiedTypeName().equals(
				LIST.getFullyQualifiedTypeName())) {
			collectionType = new JavaType(
					ARRAY_LIST.getFullyQualifiedTypeName(), 0, DataType.TYPE,
					null, collectionType.getParameters());
			instantiableCollection = collectionType
					.getNameIncludingTypeParameters().replace(
							collectionType.getPackage()
									.getFullyQualifiedPackageName() + ".", "");
		} else if (collectionType.getFullyQualifiedTypeName().equals(
				SET.getFullyQualifiedTypeName())) {
			collectionType = new JavaType(HASH_SET.getFullyQualifiedTypeName(),
					0, DataType.TYPE, null, collectionType.getParameters());
			instantiableCollection = collectionType
					.getNameIncludingTypeParameters().replace(
							collectionType.getPackage()
									.getFullyQualifiedPackageName() + ".", "");
		}

		builder.getImportRegistrationResolver().addImports(collectionType,
				LIST, ARRAY_LIST);

		final String identifierMethodName = getIdentifierMethodName(field)
				.getSymbolName();

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(collectionName + " " + localEnitiesName
				+ " = new " + instantiableCollection + "();");
		bodyBuilder
				.appendFormalLine("List<Long> longIds = new ArrayList<Long>();");
		bodyBuilder.appendFormalLine("for (Key key : " + entityIdsName + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (!longIds.contains(key.getId())) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("longIds.add(key.getId());");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("for ("
				+ collectionElementType.getSimpleTypeName() + " entity : "
				+ entityCollectionName + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (!longIds.contains(entity."
				+ identifierMethodName + "())) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("longIds.add(entity."
				+ identifierMethodName + "());");
		bodyBuilder.appendFormalLine(entityIdsName
				+ ".add(KeyFactory.createKey("
				+ collectionElementType.getSimpleTypeName()
				+ ".class.getName(), entity." + identifierMethodName + "()));");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine(localEnitiesName + ".add(entity);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("this." + entityCollectionName + " = "
				+ localEnitiesName + ";");

		return bodyBuilder;
	}

	private InvocableMemberBodyBuilder getGaeAccessorBody(
			final FieldMetadata field, final JavaSymbolName hiddenIdFieldName) {
		return field.getFieldType().isCommonCollectionType() ? getEntityCollectionAccessorBody(
				field, hiddenIdFieldName) : getSingularEntityAccessor(field,
				hiddenIdFieldName);
	}

	private InvocableMemberBodyBuilder getGaeMutatorBody(
			final FieldMetadata field, final JavaSymbolName hiddenIdFieldName) {
		return field.getFieldType().isCommonCollectionType() ? getEntityCollectionMutatorBody(
				field, hiddenIdFieldName) : getSingularEntityMutator(field,
				hiddenIdFieldName);
	}

	private InvocableMemberBodyBuilder getInterfaceMethodBody(
			JavaType returnType) {
		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("// Interface Implementation");
		bodyBuilder.appendFormalLine("return 0;");
		return bodyBuilder;
	}

	private JavaSymbolName getIdentifierMethodName(final FieldMetadata field) {
		final JavaSymbolName identifierAccessorMethodName = declaredFields
				.get(field);
		return identifierAccessorMethodName != null ? identifierAccessorMethodName
				: new JavaSymbolName("getId");
	}

	private FieldMetadataBuilder getMultipleEntityIdField(
			final JavaSymbolName fieldName) {
		builder.getImportRegistrationResolver().addImport(HASH_SET);
		return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName,
				new JavaType(SET.getFullyQualifiedTypeName(), 0, DataType.TYPE,
						null, Collections.singletonList(GAE_DATASTORE_KEY)),
				"new HashSet<Key>()");
	}

	private InvocableMemberBodyBuilder getSingularEntityAccessor(
			final FieldMetadata field, final JavaSymbolName hiddenIdFieldName) {
		final String entityName = field.getFieldName().getSymbolName();
		final String entityIdName = hiddenIdFieldName.getSymbolName();
		final String simpleFieldTypeName = field.getFieldType()
				.getSimpleTypeName();

		final String identifierMethodName = getIdentifierMethodName(field)
				.getSymbolName();

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder
				.appendFormalLine("if (this." + entityIdName + " != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (this." + entityName
				+ " == null || this." + entityName + "." + identifierMethodName
				+ "() != this." + entityIdName + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("this." + entityName + " = "
				+ simpleFieldTypeName + ".find" + simpleFieldTypeName
				+ "(this." + entityIdName + ");");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("this." + entityName + " = null;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return this." + entityName + ";");

		return bodyBuilder;
	}

	private FieldMetadataBuilder getSingularEntityIdField(
			final JavaSymbolName fieldName) {
		return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, fieldName,
				LONG_OBJECT, null);
	}

	private InvocableMemberBodyBuilder getSingularEntityMutator(
			final FieldMetadata field, final JavaSymbolName hiddenIdFieldName) {
		final String entityName = field.getFieldName().getSymbolName();
		final String entityIdName = hiddenIdFieldName.getSymbolName();
		final String identifierMethodName = getIdentifierMethodName(field)
				.getSymbolName();

		final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (" + entityName + " != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (" + entityName + "."
				+ identifierMethodName + " () == null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(entityName + ".persist();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("this." + entityIdName + " = "
				+ entityName + "." + identifierMethodName + "();");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("} else {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("this." + entityIdName + " = null;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("this." + entityName + " = " + entityName
				+ ";");

		return bodyBuilder;
	}

	private void processGaeAnnotations(final FieldMetadata field) {
		for (final AnnotationMetadata annotation : field.getAnnotations()) {
			if (annotation.getAnnotationType().equals(ONE_TO_ONE)
					|| annotation.getAnnotationType().equals(MANY_TO_ONE)
					|| annotation.getAnnotationType().equals(ONE_TO_MANY)
					|| annotation.getAnnotationType().equals(MANY_TO_MANY)) {
				builder.addFieldAnnotation(new DeclaredFieldAnnotationDetails(
						field, new AnnotationMetadataBuilder(annotation
								.getAnnotationType()).build(), true));
				builder.addFieldAnnotation(new DeclaredFieldAnnotationDetails(
						field, new AnnotationMetadataBuilder(TRANSIENT).build()));
				break;
			}
		}
	}
	
	/**
	 * To check if current method was implemented on _JavaBean.aj.
	 * If method was implemented, is not necessary to add again.
	 * 
	 * @param methodBuilder
	 * @return
	 */
	private boolean checkIfInterfaceMethodWasImplemented(
			MethodMetadataBuilder methodBuilder) {
		// Obtain current declared methods
		List<MethodMetadataBuilder> declaredMethods = builder.getDeclaredMethods();
		
		for(MethodMetadataBuilder method : declaredMethods){
			// If current method equals to interface method, return false
			if(method.getMethodName().equals(methodBuilder.getMethodName())){
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		final ToStringBuilder builder = new ToStringBuilder(this);
		builder.append("identifier", getId());
		builder.append("valid", valid);
		builder.append("aspectName", aspectName);
		builder.append("destinationType", destination);
		builder.append("governor", governorPhysicalTypeMetadata.getId());
		builder.append("itdTypeDetails", itdTypeDetails);
		return builder.toString();
	}
}
