package org.springframework.roo.addon.entity;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.InheritanceType;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooEntity}.
 *  
 * @author Ben Alex
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
public class EntityMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String ENTITY_MANAGER_METHOD_NAME = "entityManager";
	private static final String PROVIDES_TYPE_STRING = EntityMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private static final JavaType ID = new JavaType("javax.persistence.Id");
	private static final JavaType EMBEDDED_ID = new JavaType("javax.persistence.EmbeddedId");
	private static final JavaType ENTITY_MANAGER = new JavaType("javax.persistence.EntityManager");
	private static final JavaType PERSISTENCE_CONTEXT = new JavaType("javax.persistence.PersistenceContext");
	private static final JavaType COLUMN = new JavaType("javax.persistence.Column");
	private static final JavaType QUERY = new JavaType("javax.persistence.Query");

	private EntityMetadata parent;
	private EntityAnnotationValues annotationValues;
	private MemberDetails memberDetails;
	private boolean noArgConstructor;
	private String plural;
	private String identifierColumn;
	private JavaType identifierType;
	private String identifierField;
	private String identifierColumnDefinition;
	private int identifierColumnSize;
	private int identifierScale;
	private boolean isGaeEnabled;
	private boolean isDataNucleusEnabled;
	private boolean isVMforceEnabled;
	
	public EntityMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, EntityMetadata parent, ProjectMetadata projectMetadata, EntityAnnotationValues annotationValues, boolean noArgConstructor, String plural, MemberDetails memberDetails, List<Identifier> identifierServiceResult) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(projectMetadata, "Project metadata required");
		Assert.notNull(annotationValues, "Annotation values required");
		Assert.hasText(plural, "Plural required for '" + identifier + "'");
		
		if (!isValid()) {
			return;
		}
		
		this.parent = parent;
		this.annotationValues = annotationValues;
		this.memberDetails = memberDetails;
		this.noArgConstructor = noArgConstructor;
		this.plural = StringUtils.capitalize(plural);
		identifierColumn = annotationValues.getIdentifierColumn();
		identifierType = annotationValues.getIdentifierType();
		identifierField = annotationValues.getIdentifierField();
		isGaeEnabled = projectMetadata.isGaeEnabled();
		isDataNucleusEnabled = projectMetadata.isDataNucleusEnabled();
		isVMforceEnabled = projectMetadata.isVMforceEnabled();

		// Process values from the annotation, if present
		processAnnotation(identifierServiceResult);
		
		// Add @Entity or @MappedSuperclass annotation
		builder.addAnnotation(annotationValues.isMappedSuperclass() ? getMappedSuperclassAnnotation() : getEntityAnnotation());
		
		// Add @Table annotation if required
		builder.addAnnotation(getTableAnnotation());
		
		// Add @Inheritance annotation if required
		builder.addAnnotation(getInheritanceAnnotation());
		
		// Add @DiscriminatorColumn if required
		builder.addAnnotation(getDiscriminatorColumnAnnotation());

		// Determine the "entityManager" field we have access to. This is guaranteed to be accessible to the ITD.
		builder.addField(getEntityManagerField());
		
		// Obtain a no-arg constructor, if one is appropriate to provide
		builder.addConstructor(getNoArgConstructor());
		
		// Add identifier field and accessor
		builder.addField(getIdentifierField());
		builder.addMethod(getIdentifierAccessor());
		builder.addMethod(getIdentifierMutator());
		
		// Add version field and accessor
		builder.addField(getVersionField());
		builder.addMethod(getVersionAccessor());
		builder.addMethod(getVersionMutator());
		
		// Add helper methods
		builder.addMethod(getPersistMethod());
		builder.addMethod(getRemoveMethod());
		builder.addMethod(getFlushMethod());
		builder.addMethod(getClearMethod());
		builder.addMethod(getMergeMethod());
		
		// Add static methods
		builder.addMethod(getEntityManagerMethod());
		builder.addMethod(getCountMethod());
		builder.addMethod(getFindAllMethod());
		builder.addMethod(getFindMethod());
		builder.addMethod(getFindEntriesMethod());
		
		builder.putCustomData(PersistenceCustomDataKeys.DYNAMIC_FINDER_NAMES, getDynamicFinders());

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private void processAnnotation(List<Identifier> identifierServiceResult) {
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooEntity.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
			
			if (identifierServiceResult != null) {
				// We have potential identifier information from an IdentifierService.
				// We only use this identifier information if the user did NOT provide ANY identifier-related attributes on @RooEntity....
				List<JavaSymbolName> attributeNames = annotation.getAttributeNames();
				if (!attributeNames.contains(new JavaSymbolName("identifierType")) && !attributeNames.contains(new JavaSymbolName("identifierField")) && !attributeNames.contains(new JavaSymbolName("identifierColumn"))) {
					// User has not specified any identifier information, so let's use what IdentifierService offered
					Assert.isTrue(identifierServiceResult.size() == 1, "Identifier service indicates " + identifierServiceResult.size() + " fields illegally for a entity " + governorTypeDetails.getName() + " (should only be one identifier field given this is an entity, not an Identifier class)");
					Identifier id = identifierServiceResult.iterator().next();
					identifierColumn = id.getColumnName();
					identifierField = id.getFieldName().getSymbolName();
					identifierType = id.getFieldType();
					identifierColumnDefinition = id.getColumnDefinition();
					identifierColumnSize = id.getColumnSize();
					identifierScale = id.getScale();
				}
			}
		}
	}

	public AnnotationMetadata getEntityAnnotation() {
		AnnotationMetadata entityAnnotation = getTypeAnnotation(new JavaType("javax.persistence.Entity"));
		if (entityAnnotation == null) {
			return null;
		}
		
		if (StringUtils.hasText(annotationValues.getEntityName())) {
			AnnotationMetadataBuilder entityBuilder = new AnnotationMetadataBuilder(entityAnnotation);
			entityBuilder.addStringAttribute("name", annotationValues.getEntityName());
			entityAnnotation = entityBuilder.build();
		}
		
		return entityAnnotation;
	}

	public AnnotationMetadata getMappedSuperclassAnnotation() {
		return getTypeAnnotation(new JavaType("javax.persistence.MappedSuperclass"));
	}

	private AnnotationMetadata getTypeAnnotation(JavaType annotationType) {
		if (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, annotationType) != null) {
			return null;
		}
		AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(annotationType);
		return annotationBuilder.build();
	}

	public AnnotationMetadata getTableAnnotation() {
		AnnotationMetadata tableAnnotation = getTypeAnnotation(new JavaType("javax.persistence.Table"));
		if (tableAnnotation == null) {
			return null;
		}
		String table = annotationValues.getTable();
		String schema = annotationValues.getSchema();
		String catalog = annotationValues.getCatalog();
		if (StringUtils.hasText(table) || StringUtils.hasText(schema) || StringUtils.hasText(catalog)) {
			AnnotationMetadataBuilder tableBuilder = new AnnotationMetadataBuilder(tableAnnotation);
			if (StringUtils.hasText(table)) {
				tableBuilder.addStringAttribute("name", table);
			}
			if (StringUtils.hasText(schema)) {
				tableBuilder.addStringAttribute("schema", schema);
			}
			if (StringUtils.hasText(catalog)) {
				tableBuilder.addStringAttribute("catalog", catalog);
			}
			return tableBuilder.build();
		}
		return null;
	}

	private AnnotationMetadata getInheritanceAnnotation() {
		JavaType inheritanceJavaType = new JavaType("javax.persistence.Inheritance");
		if (MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, inheritanceJavaType) != null) {
			return null;
		}
		if (StringUtils.hasText(annotationValues.getInheritanceType())) {
			AnnotationMetadataBuilder inheritanceBuilder = new AnnotationMetadataBuilder(inheritanceJavaType);
			inheritanceBuilder.addEnumAttribute("strategy", new EnumDetails(new JavaType("javax.persistence.InheritanceType"), new JavaSymbolName(annotationValues.getInheritanceType())));
			return inheritanceBuilder.build();
		}
		return null;
	}

	public AnnotationMetadata getDiscriminatorColumnAnnotation() {
		if ((StringUtils.hasText(annotationValues.getInheritanceType()) && InheritanceType.SINGLE_TABLE.name().equals(annotationValues.getInheritanceType()))) {
			// Theoretically not required based on @DiscriminatorColumn JavaDocs, but Hibernate appears to fail if it's missing
			return getTypeAnnotation(new JavaType("javax.persistence.DiscriminatorColumn"));
		}
		return null;
	}

	/**
	 * Locates the entity manager field that should be used.
	 * 
	 * <p>
	 * If a parent is defined, it must provide the field.
	 * 
	 * <p>
	 * We generally expect the field to be named "entityManager" and be of type javax.persistence.EntityManager. We
	 * also require it to be public or protected, and annotated with @PersistenceContext. If there is an
	 * existing field which doesn't meet these latter requirements, we add an underscore prefix to the "entityManager" name
	 * and try again, until such time as we come up with a unique name that either meets the requirements or the
	 * name is not used and we will create it.
	 *  
	 * @return the entity manager field (never returns null)
	 */
	public FieldMetadata getEntityManagerField() {
		if (parent != null) {
			// The parent is required to guarantee this is available
			return parent.getEntityManagerField();
		}
		
		// Need to locate it ourself
		int index = -1;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = fieldName + "entityManager";
			
			JavaSymbolName fieldSymbolName = new JavaSymbolName(fieldName);
			FieldMetadata candidate = MemberFindingUtils.getField(governorTypeDetails, fieldSymbolName);
			if (candidate != null) {
				// Verify if candidate is suitable
				
				if (!Modifier.isPublic(candidate.getModifier()) && !Modifier.isProtected(candidate.getModifier()) && (Modifier.TRANSIENT != candidate.getModifier())) {
					// Candidate is not public and not protected and not simply a transient field (in which case subclasses
					// will see the inherited field), so any subsequent subclasses won't be able to see it. Give up!
					continue;
				}
				
				if (!candidate.getFieldType().equals(ENTITY_MANAGER)) {
					// Candidate isn't an EntityManager, so give up
					continue;
				}
				
				if (MemberFindingUtils.getAnnotationOfType(candidate.getAnnotations(), PERSISTENCE_CONTEXT) == null) {
					// Candidate doesn't have a PersistenceContext annotation, so give up
					continue;
				}
				
				// If we got this far, we found a valid candidate
				return candidate;
			}
			
			// Candidate not found, so let's create one
			List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
			AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(PERSISTENCE_CONTEXT);
			if (StringUtils.hasText(annotationValues.getPersistenceUnit())) {
				annotationBuilder.addStringAttribute("unitName", annotationValues.getPersistenceUnit());
			}
			annotations.add(annotationBuilder);
			
			FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.TRANSIENT, annotations, fieldSymbolName, ENTITY_MANAGER);
			return fieldBuilder.build();
		}
	}
	
	/**
	 * Locates the no-arg constructor for this class, if available.
	 * 
	 * <p>
	 * If a class defines a no-arg constructor, it is returned (irrespective of access modifiers).
	 * 
	 * <p>
	 * If a class does not define a no-arg constructor, one might be created. It will only be created if
	 * the {@link #noArgConstructor} is true AND there is at least one other constructor declared
	 * in the source file. If a constructor is created, it will have a public access modifier.
	 * 
	 * @return the constructor (may return null if no constructor is to be produced)
	 */
	public ConstructorMetadata getNoArgConstructor() {
		// Compute the mutator method parameters
		List<JavaType> paramTypes = new ArrayList<JavaType>();

		// Search for an existing constructor
		ConstructorMetadata result = MemberFindingUtils.getDeclaredConstructor(governorTypeDetails, paramTypes);
		if (result != null) {
			// Found an existing no-arg constructor on this class, so return it
			return result;
		}
		
		// To get this far, the user did not define a no-arg constructor
		
		if (!noArgConstructor) {
			// This metadata instance is prohibited from making a no-arg constructor
			return null;
		}
		
		if (governorTypeDetails.getDeclaredConstructors().size() == 0) {
			// Default constructor will apply, so quit
			return null;
		}

		// Create the constructor
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("super();");
		
		ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(getId());
		constructorBuilder.setModifier(Modifier.PUBLIC);
		constructorBuilder.setParameterTypes(AnnotatedJavaType.convertFromJavaTypes(paramTypes));
		constructorBuilder.setBodyBuilder(bodyBuilder);
		return constructorBuilder.build();
	}
	
	/**
	 * Locates the identifier field.
	 * 
	 * <p>		
	 * If a parent is defined, it must provide the field.
	 * 
	 * <p>
	 * If no parent is defined, one will be located or created. Any declared or inherited field which has the 
	 * {@link javax.persistence.Id @Id} or {@link javax.persistence.EmbeddedId @EmbeddedId} annotation will be
	 * taken as the identifier and returned. If no such field is located, a private field will be created as
	 * per the details contained in {@link RooEntity}.
	 * 
	 * @return the identifier (never returns null)
	 */
	public FieldMetadata getIdentifierField() {
		if (parent != null) {
			FieldMetadata idField = parent.getIdentifierField();
			if (idField != null) {
				if (MemberFindingUtils.getAnnotationOfType(idField.getAnnotations(), ID) != null) {
					return idField;
				} else if (MemberFindingUtils.getAnnotationOfType(idField.getAnnotations(), EMBEDDED_ID) != null) {
					return idField;
				}
			}
			return parent.getIdentifierField();
		}
		
		// Try to locate an existing field with @javax.persistence.Id
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, ID);
		if (idFields.size() > 0) {
			return getIdentifierField(idFields, ID);
		}
		
		// Try to locate an existing field with @javax.persistence.EmbeddedId
		List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, EMBEDDED_ID);
		if (embeddedIdFields.size() > 0) {
			return getIdentifierField(embeddedIdFields, EMBEDDED_ID);
		}

		if (!StringUtils.hasText(identifierField)) {
			// Force a default
			identifierField = "id";
		}
		
		// Ensure there isn't already a field called "id"; if so, compute a unique name (it's not really a fatal situation at the end of the day)
		int index= -1;
		JavaSymbolName idField;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = fieldName + identifierField;
			
			idField = new JavaSymbolName(fieldName);
			if (MemberFindingUtils.getField(governorTypeDetails, idField) == null) {
				// Found a usable field name
				break;
			}
		}
		
		// We need to create one
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		boolean hasIdClass = !(identifierType.getPackage().getFullyQualifiedPackageName().startsWith("java.") || identifierType.equals(new JavaType("com.google.appengine.api.datastore.Key")));
		JavaType annotationType = hasIdClass ? EMBEDDED_ID : ID;
		annotations.add(new AnnotationMetadataBuilder(annotationType));

		// Compute the column name, as required
		if (!hasIdClass) {
			String generationType = isGaeEnabled || isVMforceEnabled ? "IDENTITY" : "AUTO";
			
			// ROO-746: Use @GeneratedValue(strategy = GenerationType.TABLE) if the root of the governor declares @Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
			if ("AUTO".equals(generationType)) {
				AnnotationMetadata inheritence = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType("javax.persistence.Inheritance"));
				if (inheritence == null) {
					inheritence = getInheritanceAnnotation();
				}
				if (inheritence != null) {
					AnnotationAttributeValue<?> value = inheritence.getAttribute(new JavaSymbolName("strategy"));
					if (value instanceof EnumAttributeValue) {
						EnumAttributeValue enumAttributeValue = (EnumAttributeValue) value;
						EnumDetails details = enumAttributeValue.getValue();
						if (details != null && "javax.persistence.InheritanceType".equals(details.getType().getFullyQualifiedTypeName())) {
							if ("TABLE_PER_CLASS".equals(details.getField().getSymbolName())) {
								generationType = "TABLE";
							}
						}
					}
				}
			}
			
			AnnotationMetadataBuilder generatedValueBuilder = new AnnotationMetadataBuilder(new JavaType("javax.persistence.GeneratedValue"));
			generatedValueBuilder.addEnumAttribute("strategy", new EnumDetails(new JavaType("javax.persistence.GenerationType"), new JavaSymbolName(generationType)));
			annotations.add(generatedValueBuilder);

			String columnName = idField.getSymbolName();
			if (StringUtils.hasText(identifierColumn)) {
				// User has specified an alternate column name
				columnName = identifierColumn;
			}

			AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(COLUMN);
			columnBuilder.addStringAttribute("name", columnName);
			if (StringUtils.hasText(identifierColumnDefinition)) {
				columnBuilder.addStringAttribute("columnDefinition", identifierColumnDefinition);
			}
			
			// Add length attribute for String field
			if (identifierColumnSize > 0 && identifierColumnSize < 4000 && identifierType.equals(JavaType.STRING_OBJECT)) {
				columnBuilder.addIntegerAttribute("length", identifierColumnSize);
			}
			
			// Add precision and scale attributes for numeric field
			if (identifierScale > 0 && (identifierType.equals(JavaType.DOUBLE_OBJECT) || identifierType.equals(JavaType.DOUBLE_PRIMITIVE) || identifierType.equals(new JavaType("java.math.BigDecimal")))) {
				columnBuilder.addIntegerAttribute("precision", identifierColumnSize);
				columnBuilder.addIntegerAttribute("scale", identifierScale);
			}

			annotations.add(columnBuilder);
		}
		
		if (isVMforceEnabled) {
			identifierType = JavaType.STRING_OBJECT;
		}

		return new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, idField, identifierType).build();
	}
	
	public FieldMetadata getIdentifierField(List<FieldMetadata> identifierFields, JavaType identifierType) {
		Assert.isTrue(identifierFields.size() == 1, "More than one field was annotated with @" + identifierType.getSimpleTypeName() + " in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
		return new FieldMetadataBuilder(identifierFields.get(0)).build();
	}

	/**
	 * Locates the identifier accessor method.
	 * 
	 * <p>
	 * If {@link #getIdentifierField()} returns a field created by this ITD or if the field is declared within the entity itself, 
	 * a public accessor will automatically be produced in the declaring class.
	 * 
	 * @return the accessor (never returns null)
	 */
	public MethodMetadata getIdentifierAccessor() {
		if (parent != null) {
			return parent.getIdentifierAccessor();
		}

		// Locate the identifier field, and compute the name of the accessor that will be produced
		FieldMetadata id = getIdentifierField();
		String requiredAccessorName = "get" + StringUtils.capitalize(id.getFieldName().getSymbolName());

		// See if the user provided the field
		if (!getId().equals(id.getDeclaredByMetadataId())) {
			// Locate an existing accessor
			MethodMetadata method = MemberFindingUtils.getMethod(memberDetails, new JavaSymbolName(requiredAccessorName), new ArrayList<JavaType>());
			if (method != null) {
				if (Modifier.isPublic(method.getModifier())) {
					// Method exists and is public so return it
					return method;
				}
				
				// Method is not public so make the required accessor name unique 
				requiredAccessorName += "_";
			}
		}

		// We declared the field in this ITD, so produce a public accessor for it
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + id.getFieldName().getSymbolName() + ";");

		return new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredAccessorName), id.getFieldType(), bodyBuilder).build();
	}
	
	/**
	 * Locates the identifier mutator method.
	 * 
	 * <p>
	 * If {@link #getIdentifierField()} returns a field created by this ITD or if the field is declared within the entity itself, 
	 * a public mutator will automatically be produced in the declaring class.
	 * 
	 * @return the mutator (never returns null)
	 */
	public MethodMetadata getIdentifierMutator() {
		// TODO: This is a temporary workaround to support web data binding approaches; to be reviewed more thoroughly in future
		if (parent != null) {
			return parent.getIdentifierMutator();
		}
		
		// Locate the identifier field, and compute the name of the accessor that will be produced
		FieldMetadata id = getIdentifierField();
		String requiredMutatorName = "set" + StringUtils.capitalize(id.getFieldName().getSymbolName());
		
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(id.getFieldType());
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("id"));
		
		// See if the user provided the field
		if (!getId().equals(id.getDeclaredByMetadataId())) {
			// Locate an existing mutator
			MethodMetadata method = MemberFindingUtils.getMethod(memberDetails, new JavaSymbolName(requiredMutatorName), paramTypes);
			if (method != null) {
				if (Modifier.isPublic(method.getModifier())) {
					// Method exists and is public so return it
					return method;
				}
				
				// Method is not public so make the required mutator name unique 
				requiredMutatorName += "_";
			}
		}
		
		// We declared the field in this ITD, so produce a public mutator for it
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + id.getFieldName().getSymbolName() + " = id;");
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredMutatorName), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}

	/**
	 * Locates the version field.
	 * 
	 * <p>
	 * If a parent is defined, it may provide the field.
	 * 
	 * <p>
	 * If no parent is defined, one may be located or created. Any declared or inherited field which is annotated
	 * with javax.persistence.Version will be taken as the version and returned. If no such field is located,
	 * a private field may be created as per the details contained in {@link RooEntity}.
	 * 
	 * @return the version (may return null)
	 */
	public FieldMetadata getVersionField() {
		if (parent != null) {
			FieldMetadata result = parent.getVersionField();
			if (result != null) {
				return result;
			}
		}
		
		// Try to locate an existing field with @Version
		List<FieldMetadata> found = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.Version"));
		if (found.size() > 0) {
			Assert.isTrue(found.size() == 1, "More than 1 field was annotated with @Version in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return found.get(0);
		}
		
		// Quit at this stage if the user doesn't want a version field
		String versionField = annotationValues.getVersionField();
		if ("".equals(versionField)) {
			return null;
		}
		
		JavaType versionType = annotationValues.getVersionType();
		String versionColumn = annotationValues.getVersionColumn();
		if (isVMforceEnabled) {
			versionField = "lastModifiedDate";
			versionType = new JavaType("java.util.Calendar");
			versionColumn = "lastModifiedDate";
		}
		
		// Ensure there isn't already a field called "version"; if so, compute a unique name (it's not really a fatal situation at the end of the day)
		int index= -1;
		JavaSymbolName verField;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = fieldName + versionField;
			
			verField = new JavaSymbolName(fieldName);
			if (MemberFindingUtils.getField(governorTypeDetails, verField) == null) {
				// Found a usable field name
				break;
			}
		}
		
		// We're creating one
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.Version")));
		
		AnnotationMetadataBuilder columnBuilder = new AnnotationMetadataBuilder(COLUMN);
		columnBuilder.addStringAttribute("name", versionColumn);
		annotations.add(columnBuilder);
		
		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(getId(), Modifier.PRIVATE, annotations, verField, versionType);
		return fieldBuilder.build();
	}

	/**
	 * Locates the version accessor method.
	 * 
	 * <p>
	 * If {@link #getVersionField()} returns a field created by this ITD or if the version field is declared within the entity itself, 
	 * a public accessor will automatically be produced in the declaring class.
	 * 
	 * @return the version accessor (may return null if there is no version field declared in this class)
	 */
	public MethodMetadata getVersionAccessor() {
		FieldMetadata version = getVersionField();
		if (version == null) {
			// There's no version field, so there certainly won't be an accessor for it 
			return null;
		}
		
		if (parent != null) {
			FieldMetadata result = parent.getVersionField();
			if (result != null) {
				// It's the parent's responsibility to provide the accessor, not ours
				return parent.getVersionAccessor();
			}
		}
		
		// Compute the name of the accessor that will be produced
		String requiredAccessorName = "get" + StringUtils.capitalize(version.getFieldName().getSymbolName());
		
		// See if the user provided the field
		if (!getId().equals(version.getDeclaredByMetadataId())) {
			// Locate an existing accessor
			MethodMetadata method = MemberFindingUtils.getMethod(memberDetails, new JavaSymbolName(requiredAccessorName), new ArrayList<JavaType>());
			if (method != null) {
				if (Modifier.isPublic(method.getModifier())) {
					// Method exists and is public so return it
					return method;
				}
				
				// Method is not public so make the required accessor name unique 
				requiredAccessorName += "_";
			}
		}
		
		// We declared the field in this ITD, so produce a public accessor for it
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + version.getFieldName().getSymbolName() + ";");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredAccessorName), version.getFieldType(), bodyBuilder);
		return methodBuilder.build();
	}
	
	/**
	 * Locates the version mutator method.
	 * 
	 * <p>
	 * If {@link #getVersionField()} returns a field created by this ITD or if the version field is declared within the entity itself, 
	 * a public mutator will automatically be produced in the declaring class.
	 * 
	 * @return the mutator (may return null if there is no version field declared in this class)
	 */
	public MethodMetadata getVersionMutator() {
		// TODO: This is a temporary workaround to support web data binding approaches; to be reviewed more thoroughly in future
		if (parent != null) {
			return parent.getVersionMutator();
		}
		
		// Locate the version field, and compute the name of the mutator that will be produced
		FieldMetadata version = getVersionField();
		if (version == null) {
			// There's no version field, so there certainly won't be a mutator for it 
			return null;
		}
		String requiredMutatorName = "set" + StringUtils.capitalize(version.getFieldName().getSymbolName());
		
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(version.getFieldType());
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("version"));
		
		// See if the user provided the field
		if (!getId().equals(version.getDeclaredByMetadataId())) {
			// Locate an existing mutator
			MethodMetadata method = MemberFindingUtils.getMethod(memberDetails, new JavaSymbolName(requiredMutatorName), paramTypes);
			if (method != null) {
				if (Modifier.isPublic(method.getModifier())) {
					// Method exists and is public so return it
					return method;
				}
				
				// Method is not public so make the required mutator name unique 
				requiredMutatorName += "_";
			}
		}
		
		// We declared the field in this ITD, so produce a public mutator for it
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + version.getFieldName().getSymbolName() + " = version;");

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredMutatorName), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		return methodBuilder.build();
	}
	
	/**
	 * @return the persist method (may return null)
	 */
	public MethodMetadata getPersistMethod() {
		if (parent != null) {
			MethodMetadata found = parent.getPersistMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(annotationValues.getPersistMethod())) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(annotationValues.getPersistMethod()), "persist");
	}
	
	/**
	 * @return the remove method (may return null)
	 */
	public MethodMetadata getRemoveMethod() {
		if (parent != null) {
			MethodMetadata found = parent.getRemoveMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(annotationValues.getRemoveMethod())) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(annotationValues.getRemoveMethod()), "remove");
	}
	
	/**
	 * @return the flush method (never returns null)
	 */
	public MethodMetadata getFlushMethod() {
		if (parent != null) {
			MethodMetadata found = parent.getFlushMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(annotationValues.getFlushMethod())) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(annotationValues.getFlushMethod()), "flush");
	}
	
	/**
	 * @return the clear method (never returns null)
	 */
	public MethodMetadata getClearMethod() {
		if (parent != null) {
			MethodMetadata found = parent.getClearMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(annotationValues.getClearMethod())) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(annotationValues.getClearMethod()), "clear");
	}

	/**
	 * @return the merge method (never returns null)
	 */
	public MethodMetadata getMergeMethod() {
		if (parent != null) {
			MethodMetadata found = parent.getMergeMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(annotationValues.getMergeMethod())) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(annotationValues.getMergeMethod()), "merge");
	}
	
	private MethodMetadata getDelegateMethod(JavaSymbolName methodName, String entityManagerDelegate) {
		// Method definition to find or build
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			return userMethod;//tagMethod(userMethod, tag);
		}
		
		// Create the method
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>(); 

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		
		// Address non-injected entity manager field
		MethodMetadata entityManagerMethod = getEntityManagerMethod();
		Assert.notNull(entityManagerMethod, "Entity manager method should not have returned null");
		
		// Use the getEntityManager() method to acquire an entity manager (the method will throw an exception if it cannot be acquired)
		String entityManagerFieldName = getEntityManagerField().getFieldName().getSymbolName();
		bodyBuilder.appendFormalLine("if (this." + entityManagerFieldName + " == null) this." + entityManagerFieldName + " = " + entityManagerMethod.getMethodName().getSymbolName() + "();");
		
		JavaType returnType = JavaType.VOID_PRIMITIVE;
		if ("flush".equals(entityManagerDelegate)) {
			addTransactionalAnnotation(annotations);
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + ".flush();");
		} else if ("clear".equals(entityManagerDelegate)) {
			addTransactionalAnnotation(annotations);
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + ".clear();");
		} else if ("merge".equals(entityManagerDelegate)) {
			addTransactionalAnnotation(annotations);
			returnType = new JavaType(governorTypeDetails.getName().getSimpleTypeName());
			bodyBuilder.appendFormalLine(governorTypeDetails.getName().getSimpleTypeName() + " merged = this." + entityManagerFieldName + ".merge(this);");
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + ".flush();");
			bodyBuilder.appendFormalLine("return merged;");
		} else if ("remove".equals(entityManagerDelegate)) {
			addTransactionalAnnotation(annotations);
			bodyBuilder.appendFormalLine("if (this." + entityManagerFieldName + ".contains(this)) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + ".remove(this);");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("} else {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(governorTypeDetails.getName().getSimpleTypeName() + " attached = " + governorTypeDetails.getName().getSimpleTypeName() + "." + getFindMethod().getMethodName().getSymbolName() + "(this." + getIdentifierField().getFieldName().getSymbolName() + ");");
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + ".remove(attached);");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else {
			// Persist
			addTransactionalAnnotation(annotations, true);
			bodyBuilder.appendFormalLine("this." + entityManagerFieldName + "." + entityManagerDelegate  + "(this);");
		}

		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), new ArrayList<JavaSymbolName>(), bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();//tagMethod(methodBuilder.build(), tag);
	}
	
	private void addTransactionalAnnotation(List<AnnotationMetadataBuilder> annotations, boolean isPersistMethod) {
		AnnotationMetadataBuilder transactionalBuilder = new AnnotationMetadataBuilder(new JavaType("org.springframework.transaction.annotation.Transactional"));
		if (StringUtils.hasText(annotationValues.getPersistenceUnit())) {
			transactionalBuilder.addStringAttribute("value", annotationValues.getPersistenceUnit());
		}
		if (isGaeEnabled && isPersistMethod) {
			transactionalBuilder.addEnumAttribute("propagation", new EnumDetails(new JavaType("org.springframework.transaction.annotation.Propagation"), new JavaSymbolName("REQUIRES_NEW")));
		}
		annotations.add(transactionalBuilder);
	}
	
	private void addTransactionalAnnotation(List<AnnotationMetadataBuilder> annotations) {
		addTransactionalAnnotation(annotations, false);
	}

	/**
	 * @return the static utility entityManager() method used by other methods to obtain
	 * entity manager and available as a utility for user code (never returns nulls)
	 */
	public MethodMetadata getEntityManagerMethod() {
		if (parent != null) {
			// The parent is required to guarantee this is available
			return parent.getEntityManagerMethod();
		}
		
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName(ENTITY_MANAGER_METHOD_NAME);
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		JavaType returnType = ENTITY_MANAGER;
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}

		// Create method
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		if (Modifier.isAbstract(governorTypeDetails.getModifier())) {
			// Create an anonymous inner class that extends the abstract class (no-arg constructor is available as this is a JPA entity)
			bodyBuilder.appendFormalLine(ENTITY_MANAGER.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + " em = new " + governorTypeDetails.getName().getSimpleTypeName() + "() {");
			// Handle any abstract methods in this class
			bodyBuilder.indent();
			for (MethodMetadata method : MemberFindingUtils.getMethods(governorTypeDetails)) {
				if (Modifier.isAbstract(method.getModifier())) {
					StringBuilder params = new StringBuilder();
					int i = -1;
					List<AnnotatedJavaType> types = method.getParameterTypes();
					for (JavaSymbolName name : method.getParameterNames()) {
						i++;
						if (i > 0) {
							params.append(", ");
						}
						AnnotatedJavaType type = types.get(i);
						params.append(type.toString()).append(" ").append(name);
					}
					int newModifier = method.getModifier() - Modifier.ABSTRACT;
					bodyBuilder.appendFormalLine(Modifier.toString(newModifier) + " " + method.getReturnType().getNameIncludingTypeParameters() + " " + method.getMethodName().getSymbolName() + "(" + params.toString() + ") { throw new UnsupportedOperationException(); }");
				}
			}
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}." + getEntityManagerField().getFieldName().getSymbolName() + ";");
		} else {
			// Instantiate using the no-argument constructor (we know this is available as the entity must comply with the JPA no-arg constructor requirement)
			bodyBuilder.appendFormalLine(ENTITY_MANAGER.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + " em = new " + governorTypeDetails.getName().getSimpleTypeName() + "()." + getEntityManagerField().getFieldName().getSymbolName() + ";");
		}
		
		bodyBuilder.appendFormalLine("if (em == null) throw new IllegalStateException(\"Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)\");");
		bodyBuilder.appendFormalLine("return em;");
		int modifier = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), new ArrayList<JavaSymbolName>(), bodyBuilder);
		return methodBuilder.build();
	}
	
	/**
	 * @return the count method (may return null)
	 */
	public MethodMetadata getCountMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getCountMethod() + plural);
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		JavaType returnType = new JavaType("java.lang.Long", 0, DataType.PRIMITIVE, null, null);
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}
		
		String typeName = StringUtils.hasText(annotationValues.getEntityName()) ? annotationValues.getEntityName() : governorTypeDetails.getName().getSimpleTypeName();
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (isDataNucleusEnabled) {
			bodyBuilder.appendFormalLine("return ((Number) " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT COUNT(o) FROM " + typeName + " o\").getSingleResult()).longValue();");
		} else {
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT COUNT(o) FROM " + typeName + " o\", Long.class).getSingleResult();");
		}
		int modifier = Modifier.PUBLIC | Modifier.STATIC;
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}
	
	/**
	 * @return the find all method (may return null)
	 */
	public MethodMetadata getFindAllMethod() {
		if ("".equals(annotationValues.getFindAllMethod())) {
			return null;
		}
		
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getFindAllMethod() + plural);
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(governorTypeDetails.getName());
		JavaType returnType = new JavaType("java.util.List", 0, DataType.TYPE, null, typeParams);
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		String typeName = StringUtils.hasText(annotationValues.getEntityName()) ? annotationValues.getEntityName() : governorTypeDetails.getName().getSimpleTypeName();
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (isDataNucleusEnabled) {
			addSuppressWarnings(annotations);
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT o FROM " + typeName + " o\").getResultList();");
		} else {
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT o FROM " + typeName + " o\", " + governorTypeDetails.getName().getSimpleTypeName() + ".class).getResultList();");
		}
 		int modifier = Modifier.PUBLIC | Modifier.STATIC;
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	/**
	 * @return the find (by ID) method (may return null)
	 */
	public MethodMetadata getFindMethod() {
		if ("".equals(annotationValues.getFindMethod())) {
			return null;
		}
		
		// Method definition to find or build
		String idFieldName = getIdentifierField().getFieldName().getSymbolName();
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getFindMethod() + governorTypeDetails.getName().getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(getIdentifierField().getFieldType());
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName(idFieldName));
		JavaType returnType = governorTypeDetails.getName();
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		InvocableMemberBodyBuilder bodyBuilder;
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
			bodyBuilder = getGaeFindMethodBody();
		} else {
			bodyBuilder = new InvocableMemberBodyBuilder();
			if (JavaType.STRING_OBJECT.equals(getIdentifierField().getFieldType())) {
				bodyBuilder.appendFormalLine("if (" + idFieldName + " == null || " + idFieldName + ".length() == 0) return null;");
			} else if (!getIdentifierField().getFieldType().isPrimitive()) {
				bodyBuilder.appendFormalLine("if (" + idFieldName + " == null) return null;");
			}
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().find(" + governorTypeDetails.getName().getSimpleTypeName() + ".class, " + idFieldName + ");");
		}
		int modifier = Modifier.PUBLIC | Modifier.STATIC;
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private InvocableMemberBodyBuilder getGaeFindMethodBody() {
		builder.getImportRegistrationResolver().addImport(QUERY);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		String typeName = StringUtils.hasText(annotationValues.getEntityName()) ? annotationValues.getEntityName() : governorTypeDetails.getName().getSimpleTypeName();

		String idFieldName = getIdentifierField().getFieldName().getSymbolName();
		if (JavaType.STRING_OBJECT.equals(getIdentifierField().getFieldType())) {
			bodyBuilder.appendFormalLine("if (" + idFieldName + " == null || " + idFieldName + ".length() == 0) return null;");
		} else if (!getIdentifierField().getFieldType().isPrimitive()) {
			bodyBuilder.appendFormalLine("if (" + idFieldName + " == null) return null;");
		}
		bodyBuilder.appendFormalLine("Query query = " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT o FROM " + typeName + " o WHERE o." + idFieldName + " = :" + idFieldName + "\").setParameter(\"" + idFieldName + "\", " + idFieldName + ");");
 		bodyBuilder.appendFormalLine(governorTypeDetails.getName().getSimpleTypeName() + " result = null;");
		bodyBuilder.appendFormalLine("List results = query.getResultList();");
		bodyBuilder.appendFormalLine("if (results.size() > 0) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("result = (" + governorTypeDetails.getName().getSimpleTypeName() + ") results.get(0);");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return result;");
		return bodyBuilder;
	}
	
	/**
	 * @return the find entries method (may return null)
	 */
	public MethodMetadata getFindEntriesMethod() {
		if ("".equals(annotationValues.getFindEntriesMethod())) {
			return null;
		}
		
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName(annotationValues.getFindEntriesMethod() + governorTypeDetails.getName().getSimpleTypeName() + "Entries");
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(new JavaType("java.lang.Integer", 0, DataType.PRIMITIVE, null, null));
		paramTypes.add(new JavaType("java.lang.Integer", 0, DataType.PRIMITIVE, null, null));
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("firstResult"));
		paramNames.add(new JavaSymbolName("maxResults"));
		List<JavaType> typeParams = new ArrayList<JavaType>();
		typeParams.add(governorTypeDetails.getName());
		JavaType returnType = new JavaType("java.util.List", 0, DataType.TYPE, null, typeParams);
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		String typeName = StringUtils.hasText(annotationValues.getEntityName()) ? annotationValues.getEntityName() : governorTypeDetails.getName().getSimpleTypeName();
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (isDataNucleusEnabled) {
			addSuppressWarnings(annotations);
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT o FROM " + typeName + " o\").setFirstResult(firstResult).setMaxResults(maxResults).getResultList();");
		} else {
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"SELECT o FROM " + typeName + " o\", " + governorTypeDetails.getName().getSimpleTypeName() + ".class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();");
		}
 		int modifier = Modifier.PUBLIC | Modifier.STATIC;
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}
		
		MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, bodyBuilder);
		methodBuilder.setAnnotations(annotations);
		return methodBuilder.build();
	}

	private void addSuppressWarnings(List<AnnotationMetadataBuilder> annotations) {
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "unchecked"));
		annotations.add(new AnnotationMetadataBuilder(new JavaType("java.lang.SuppressWarnings"), attributes));
	}
	
	/**
	 * @return the dynamic, custom finders (never returns null, but may return an empty list)
	 */
	public List<String> getDynamicFinders() {
		List<String> result = new ArrayList<String>();
		if (annotationValues.getFinders() == null || annotationValues.getFinders().length == 0) {
			return result;
		}
		result.addAll(Arrays.asList(annotationValues.getFinders()));
		return Collections.unmodifiableList(result);
	}

	/**
	 * @return the pluralised name (never returns null or an empty string)
	 */
	public String getPlural() {
		return plural;
	}
	
	/**
	 * Return the entityName used by DynamicFinderServices for the generation of the JPA Query
	 * 
	 * @return the entityName the value of entityName attribute.
	 */
	public String getEntityName() {
		return annotationValues.getEntityName();
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("finders", annotationValues.getFinders());
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static String getMetadataIdentifierType() {
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
