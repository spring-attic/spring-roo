package org.springframework.roo.addon.dod;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooDataOnDemand}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class DataOnDemandMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = DataOnDemandMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	private DataOnDemandAnnotationValues annotationValues;
	private BeanInfoMetadata beanInfoMetadata;
	
	/** The "findEntityEntries(Integer,Integer):List<Entity>" static method for the entity (required) */
	private MethodMetadata findEntriesMethod;
	
	/** The "persist():void" instance method for the entity we are to create (required) */
	private MethodMetadata persistMethod;
	
	/** Mandatory methods, in order of discovery (so we can guarantee the ITD is generated in a consistent manner for SCM compatibility) */
	private List<MethodMetadata> mandatoryMutators = new ArrayList<MethodMetadata>();
	
	/** key: mandatory setter to invoke; value: the argument to present to the mutator method, expressed as a string */
	private Map<MethodMetadata,String> mutatorArguments = new HashMap<MethodMetadata, String>();
	
	/** Other entities requiring a data on demand instance; fields must exist for each of these in the class */
	private List<JavaType> requiredDataOnDemandCollaborators = new ArrayList<JavaType>();

	public DataOnDemandMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, DataOnDemandAnnotationValues annotationValues, BeanInfoMetadata beanInfoMetadata, MethodMetadata findEntriesMethod, MethodMetadata persistMethod) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(findEntriesMethod, "Find entries method required");
		Assert.notNull(persistMethod, "Persist method required");
		
		if (!isValid()) {
			return;
		}

		this.annotationValues = annotationValues;
		this.beanInfoMetadata = beanInfoMetadata;
		this.findEntriesMethod = findEntriesMethod;
		this.persistMethod = persistMethod;
		
		mutatorDiscovery();
		
		if (isComponentAnnotationIntroduced()) {
			builder.addTypeAnnotation(getComponentAnnotation());
		}
		
		builder.addField(getRndField());
		builder.addField(getDataField());
		
		Set<JavaSymbolName> fieldsAddedToItd = new HashSet<JavaSymbolName>();
		for (JavaType entityNeedingCollaborator : requiredDataOnDemandCollaborators) {
			JavaType collaboratorType = getCollaboratingType(entityNeedingCollaborator);
			String collaboratingFieldName = getCollaboratingFieldName(entityNeedingCollaborator).getSymbolName();
			
			JavaSymbolName fieldSymbolName = new JavaSymbolName(collaboratingFieldName);
			FieldMetadata candidate = MemberFindingUtils.getField(governorTypeDetails, fieldSymbolName);
			if (candidate != null) {
				// We really expect the field to be correct if we're going to rely on it
				Assert.isTrue(candidate.getFieldType().equals(collaboratorType), "Field '" + collaboratingFieldName + "' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be of type '" + collaboratorType.getFullyQualifiedTypeName() + "'");
				Assert.isTrue(Modifier.isPrivate(candidate.getModifier()), "Field '" + collaboratingFieldName + "' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be private");
				Assert.notNull(MemberFindingUtils.getAnnotationOfType(candidate.getAnnotations(), new JavaType("org.springframework.beans.factory.annotation.Autowired")), "Field '" + collaboratingFieldName + "' on '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' must be @Autowired");
				// It's ok, so we can move onto the new field
				continue;
			}
			
			// Must make the field
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.beans.factory.annotation.Autowired"), new ArrayList<AnnotationAttributeValue<?>>()));
			FieldMetadata field = new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldSymbolName, collaboratorType, null, annotations);
			
			// Add it to the ITD, if it hasn't already been
			if (!fieldsAddedToItd.contains(field.getFieldName())) {
				fieldsAddedToItd.add(field.getFieldName());
				builder.addField(field);
				fieldsAddedToItd.add(field.getFieldName());
			}
		}
		
		builder.addMethod(getNewTransientEntityMethod());
		builder.addMethod(getRandomPersistentEntityMethod());
		builder.addMethod(getModifyMethod());
		builder.addMethod(getInitMethod());
		
		itdTypeDetails = builder.build();
	}
	
	/**
	 * Adds the @org.springframework.stereotype.Component annotation to the type, unless
	 * it already exists.
	 *  
	 * @return the annotation is already exists or will be created, or null if it will not be created (required)
	 */
	public AnnotationMetadata getComponentAnnotation() {
		JavaType javaType = new JavaType("org.springframework.stereotype.Component");
		
		if (isComponentAnnotationIntroduced()) {
			return new DefaultAnnotationMetadata(javaType, new ArrayList<AnnotationAttributeValue<?>>());
		}
		
		return MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, javaType);
	}
	
	/**
	 * Indicates whether the @org.springframework.stereotype.Component annotation will
	 * be introduced via this ITD.
	 *  
	 * @return true if it will be introduced, false otherwise
	 */
	public boolean isComponentAnnotationIntroduced() {
		JavaType javaType = new JavaType("org.springframework.stereotype.Component");
		AnnotationMetadata result = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, javaType);
		
		return result == null;
	}

	/**
	 * @return the "rnd" field to use, which is either provided by the user or produced on demand (never returns null)
	 */
	public FieldMetadata getRndField() {
		int index = -1;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = fieldName + "rnd";
			
			JavaSymbolName fieldSymbolName = new JavaSymbolName(fieldName);
			FieldMetadata candidate = MemberFindingUtils.getField(governorTypeDetails, fieldSymbolName);
			if (candidate != null) {
				// Verify if candidate is suitable
				
				if (!Modifier.isPrivate(candidate.getModifier())) {
					// Candidate is not private, so we might run into naming clashes if someone subclasses this (therefore go onto the next possible name)
					continue;
				}
				
				if (!candidate.getFieldType().equals(new JavaType("java.util.Random"))) {
					// Candidate isn't a java.util.Random, so it isn't suitable
					continue;
				}
				
				// If we got this far, we found a valid candidate
				// We don't check if there is a corresponding initializer, but we assume the user knows what they're doing and have made one
				return candidate;
			}
			
			// Candidate not found, so let's create one
			FieldMetadata field = new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldSymbolName, new JavaType("java.util.Random"), new JavaType("java.security.SecureRandom"), null);
			return field;
		}
	}

	/**
	 * @return the "data" field to use, which is either provided by the user or produced on demand (never returns null)
	 */
	public FieldMetadata getDataField() {
		int index = -1;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = fieldName + "data";

			// The type parameters to be used by the field type
			List<JavaType> typeParams = new ArrayList<JavaType>();
			typeParams.add(annotationValues.getEntity());
			
			JavaSymbolName fieldSymbolName = new JavaSymbolName(fieldName);
			FieldMetadata candidate = MemberFindingUtils.getField(governorTypeDetails, fieldSymbolName);
			if (candidate != null) {
				// Verify if candidate is suitable
				
				if (!Modifier.isPrivate(candidate.getModifier())) {
					// Candidate is not private, so we might run into naming clashes if someone subclasses this (therefore go onto the next possible name)
					continue;
				}
				
				if (!candidate.getFieldType().equals(new JavaType("java.util.List", 0, false, null, typeParams))) {
					// Candidate isn't a java.util.List<theEntity>, so it isn't suitable
					// The equals method also verifies type params are present
					continue;
				}
				
				// If we got this far, we found a valid candidate
				// We don't check if there is a corresponding initializer, but we assume the user knows what they're doing and have made one
				return candidate;
			}
			
			// Candidate not found, so let's create one
			FieldMetadata field = new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldSymbolName, new JavaType("java.util.List", 0, false, null, typeParams), null, null);
			return field;
		}
	}

	/**
	 * @return the "getNewTransientEntity(int index):Entity" method (never returns null)
	 */
	public MethodMetadata getNewTransientEntityMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("getNewTransient" + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(JavaType.INT_PRIMITIVE);
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("index"));
		JavaType returnType = beanInfoMetadata.getJavaBean();
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getFullyQualifiedTypeNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine(beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + " obj = new " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "();");
		
		for (MethodMetadata mutator : mandatoryMutators) {
			String initializer = mutatorArguments.get(mutator);
			Assert.hasText(initializer, "Internal error: unable to locate initializer for " + mutator);
			bodyBuilder.appendFormalLine("obj." + mutator.getMethodName() + "(" + initializer + ");");
		}
		
		bodyBuilder.appendFormalLine("return obj;");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput());
	}

	/**
	 * @return the "modifyEntity(Entity):boolean" method (never returns null)
	 */
	public MethodMetadata getModifyMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("modify" + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(beanInfoMetadata.getJavaBean());
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("obj"));
		JavaType returnType = JavaType.BOOLEAN_PRIMITIVE;
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getFullyQualifiedTypeNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		// TODO: We should port this more fully from original code base
		bodyBuilder.appendFormalLine("return false;");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput());
	}
	
	/**
	 * @return the "getRandomEntity():Entity" method (never returns null)
	 */
	public MethodMetadata getRandomPersistentEntityMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("getRandom" + beanInfoMetadata.getJavaBean().getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		JavaType returnType = beanInfoMetadata.getJavaBean();
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getFullyQualifiedTypeNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("init();");
		bodyBuilder.appendFormalLine("return " + getDataField().getFieldName().getSymbolName() +".get(" + getRndField().getFieldName().getSymbolName() + ".nextInt(" + getDataField().getFieldName().getSymbolName() + ".size()));");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput());
	}

	/**
	 * @return the "init():void" method (never returns null)
	 */
	public MethodMetadata getInitMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName("init");
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		JavaType returnType = JavaType.VOID_PRIMITIVE;
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getFullyQualifiedTypeNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create a method
		
		// Create the annotations
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new EnumAttributeValue(new JavaSymbolName("propagation"), new EnumDetails(new JavaType("org.springframework.transaction.annotation.Propagation"), new JavaSymbolName("REQUIRES_NEW"))));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.transaction.annotation.Transactional"), attributes));
		
		// Create the body
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String dataField = getDataField().getFieldName().getSymbolName();
		bodyBuilder.appendFormalLine("if (" + dataField + " != null) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		
		bodyBuilder.appendFormalLine("");
		bodyBuilder.appendFormalLine(dataField + " = " + beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + "." + findEntriesMethod.getMethodName().getSymbolName() + "(0, " + annotationValues.getQuantity() + ");");
		bodyBuilder.appendFormalLine("if (data == null) throw new IllegalStateException(\"Find entries implementation for '" + beanInfoMetadata.getJavaBean().getSimpleTypeName() + "' illegally returned null\");");
		bodyBuilder.appendFormalLine("if (" + dataField + ".size() > 0) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");

		bodyBuilder.appendFormalLine("");
		bodyBuilder.appendFormalLine("for (int i = 0; i < " + annotationValues.getQuantity() + "; i++) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine(beanInfoMetadata.getJavaBean().getFullyQualifiedTypeName() + " obj = " + getNewTransientEntityMethod().getMethodName() + "(i);");
		bodyBuilder.appendFormalLine("obj." + persistMethod.getMethodName().getSymbolName() + "();");
		bodyBuilder.appendFormalLine(dataField + ".add(obj);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, annotations, bodyBuilder.getOutput());
	}

	private void mutatorDiscovery() {
		for (MethodMetadata mutatorMethod : beanInfoMetadata.getPublicMutators()) {
			JavaSymbolName propertyName = beanInfoMetadata.getPropertyNameForJavaBeanMethod(mutatorMethod);
			FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(propertyName);
			
			if (field == null) {
				// There is no field for this mutator, so chances are it's not mandatory
				continue;
			}
			
			// Never include id field (it shouldn't normally have a mutator anyway, but the user might have added one)
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Id")) != null) {
				continue;
			}
			
			// Never include version field (generally you won't provide a mutator for version, although in some cases it might be present)
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Version")) != null) {
				continue;
			}

			// Never include any sort of collection; user has to make such entities by hand
			if (field.getFieldType().isCommonCollectionType() || MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.OneToMany")) != null) {
				continue;
			}
			
			// Only include the field if it's really required (ie marked with JSR 303 NotNull) or it has no annotations and is therefore probably simple to invoke
			if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.NotNull")) != null || field.getAnnotations().size() == 0) {
				
				String initializer = "null";
				if (field.getFieldType().equals(new JavaType(String.class.getName()))) {
					initializer = "\"" + field.getFieldName().getSymbolName() + "_\" + index";
				} else if (field.getFieldType().equals(new JavaType(Date.class.getName()))) {
					if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Past")) != null) {
						initializer = "new java.util.Date(new java.util.Date().getTime() - 10000000L)";
					} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.validation.constraints.Future")) != null) {
						initializer = "new java.util.Date(new java.util.Date().getTime() + 10000000L)";
					} else {
						initializer = "new java.util.Date()";
					}
				} else if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)) {
					initializer = "new Boolean(true)";
				} else if (field.getFieldType().equals(JavaType.INT_OBJECT)) {
					initializer = "new Integer(index)";
				} else if (field.getFieldType().equals(JavaType.DOUBLE_OBJECT)) {
					initializer = "new Double(index)";
				} else if (field.getFieldType().equals(JavaType.FLOAT_OBJECT)) {
					initializer = "new Float(index)";
				} else if (field.getFieldType().equals(JavaType.LONG_OBJECT)) {
					initializer = "new Long(index)";
				} else if (field.getFieldType().equals(JavaType.SHORT_OBJECT)) {
					initializer = "new Short(index)";
				} else if (MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.ManyToOne")) != null) {
					requiredDataOnDemandCollaborators.add(field.getFieldType());
					String collaboratingFieldName = getCollaboratingFieldName(field.getFieldType()).getSymbolName();
					initializer = collaboratingFieldName + ".getRandom" + field.getFieldType().getSimpleTypeName() + "()";
				}
				
				mandatoryMutators.add(mutatorMethod);
				mutatorArguments.put(mutatorMethod, initializer);
			}
		}

	}
	
	private JavaSymbolName getCollaboratingFieldName(JavaType entity) {
		return new JavaSymbolName(StringUtils.uncapitalize(getCollaboratingType(entity).getSimpleTypeName()));
	}
	
	private JavaType getCollaboratingType(JavaType entity) {
		return new JavaType(entity.getFullyQualifiedTypeName() + "DataOnDemand");
	}

	/**
	 * @return the physical type identifier for the {@link BeanInfoMetadata} specified via {@link RooDataOnDemand#create()} (never null or empty unless metadata is invalid)
	 */
	public String getIdentifierForBeanInfoMetadata() {
		return beanInfoMetadata.getId();
	}
	
	/**
	 * @return the annotation values specified via {@link RooDataOnDemand} (never null unless the metadata itself is invalid)
	 */
	public DataOnDemandAnnotationValues getAnnotationValues() {
		return annotationValues;
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

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
