package org.springframework.roo.addon.entity;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.DefaultConstructorMetadata;
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
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulate;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
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
 * <p>
 * Any getter produced by this metadata is automatically included in the {@link BeanInfoMetadata}.
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

	private EntityMetadata parent;
	private boolean noArgConstructor;
	private String plural;
	private boolean isGaeEnabled;
	private boolean isDataNucleusEnabled;
	
	// From annotation
	@AutoPopulate private JavaType identifierType = new JavaType(Long.class.getName());
	@AutoPopulate private String identifierField = "id";
	@AutoPopulate private String identifierColumn = "";
	@AutoPopulate private JavaType versionType = new JavaType(Integer.class.getName());
	@AutoPopulate private String versionField = "version";
	@AutoPopulate private String persistMethod = "persist";
	@AutoPopulate private String flushMethod = "flush";
	@AutoPopulate private String mergeMethod = "merge";
	@AutoPopulate private String removeMethod = "remove";
	@AutoPopulate private String countMethod = "count";
	@AutoPopulate private String findAllMethod = "findAll";
	@AutoPopulate private String findMethod = "find";
	@AutoPopulate private String findEntriesMethod = "find";
	@AutoPopulate private String[] finders;

	public EntityMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, EntityMetadata parent, boolean noArgConstructor, String plural, ProjectMetadata projectMetadata, List<Identifier> identifierServiceResult) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.hasText(plural, "Plural required for '" + identifier + "'");
		
		if (!isValid()) {
			return;
		}
		
		this.parent = parent;
		this.noArgConstructor = noArgConstructor;
		this.plural = StringUtils.capitalize(plural);
		isGaeEnabled = projectMetadata.isGaeEnabled();
		isDataNucleusEnabled = projectMetadata.isDataNucleusEnabled();

		// Process values from the annotation, if present
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
				}
			}			
		}
		
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
		builder.addMethod(getMergeMethod());
		
		// Add static methods
		builder.addMethod(getEntityManagerMethod());
		builder.addMethod(getCountMethod());
		builder.addMethod(getFindAllMethod());
		builder.addMethod(getFindMethod());
		builder.addMethod(getFindEntriesMethod());
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
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
			List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
			AnnotationMetadata annotation = new DefaultAnnotationMetadata(PERSISTENCE_CONTEXT, new ArrayList<AnnotationAttributeValue<?>>());
			annotations.add(annotation);
			
			return new DefaultFieldMetadata(getId(), Modifier.TRANSIENT, fieldSymbolName, ENTITY_MANAGER, null, annotations);
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
		
		return new DefaultConstructorMetadata(getId(), Modifier.PUBLIC, AnnotatedJavaType.convertFromJavaTypes(paramTypes), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput());
	}
	
	/**
	 * Locates the identifier field.
	 * 
	 * <p>		
	 * If a parent is defined, it must provide the field.
	 * 
	 * <p>
	 * If no parent is defined, one will be located or created. Any declared or inherited field which has the 
	 * {@link javax.persistence.Id @Id} or {@link javax.persistence.EmbeddedId @EmbeddedId} annotation will be taken as the identifier and returned. If no such field is located,
	 * a private field will be created as per the details contained in {@link RooEntity}.
	 * 
	 * @return the identifier (never returns null)
	 */
	public FieldMetadata getIdentifierField() {
		if (parent != null) {
			return parent.getIdentifierField();
		}
		
		// Try to locate an existing field with @javax.persistence.Id
		List<FieldMetadata> foundId = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, ID);
		if (foundId.size() > 0) {
			Assert.isTrue(foundId.size() == 1, "More than one field was annotated with @Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			FieldMetadata field = foundId.get(0);
			return field;
		}
		
		// Try to locate an existing field with @javax.persistence.EmbeddedId
		List<FieldMetadata> foundEmbeddedId = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, EMBEDDED_ID);
		if (foundEmbeddedId.size() > 0) {
			Assert.isTrue(foundEmbeddedId.size() == 1, "More than one field was annotated with @EmbeddedId in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			FieldMetadata field = foundEmbeddedId.get(0);
			return field;
		}

		if ("".equals(identifierField)) {
			// Force a default
			identifierField = "id";
		}
		
		// Ensure there isn't already a field called "id"; if so, compute a unique name (it's not really a fatal situation at the end of the day)
		int index= -1;
		JavaSymbolName idField = null;
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
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		boolean hasIdClass = !(identifierType.getPackage().getFullyQualifiedPackageName().startsWith("java.") || identifierType.equals(new JavaType("com.google.appengine.api.datastore.Key")));
		JavaType annotationType = hasIdClass ? EMBEDDED_ID : ID;
		AnnotationMetadata idAnnotation = new DefaultAnnotationMetadata(annotationType, new ArrayList<AnnotationAttributeValue<?>>());
		annotations.add(idAnnotation);
					
		// Compute the column name, as required
		if (!hasIdClass) {
			List<AnnotationAttributeValue<?>> generatedValueAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			String generationType = isGaeEnabled ? "IDENTITY" : "AUTO";
			
			// ROO-746: Use @GeneratedValue(strategy = GenerationType.TABLE) if the root of the governor declares @Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
			if ("AUTO".equals(generationType)) {
				AnnotationMetadata inheritence = MemberFindingUtils.getTypeAnnotation(governorTypeDetails, new JavaType("javax.persistence.Inheritance"));
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
			
			generatedValueAttributes.add(new EnumAttributeValue(new JavaSymbolName("strategy"), new EnumDetails(new JavaType("javax.persistence.GenerationType"), new JavaSymbolName(generationType))));
			AnnotationMetadata generatedValueAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.GeneratedValue"), generatedValueAttributes);
			annotations.add(generatedValueAnnotation);

			String columnName = idField.getSymbolName();
			if (!"".equals(identifierColumn)) {
				// User has specified an alternate column name
				columnName = identifierColumn;
			}

			List<AnnotationAttributeValue<?>> columnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			columnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), columnName));
			AnnotationMetadata columnAnnotation = new DefaultAnnotationMetadata(COLUMN, columnAttributes);
			annotations.add(columnAnnotation);
		}
		
		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, idField, identifierType, null, annotations);
	}
	
	/**
	 * Locates the identifier accessor method.
	 * 
	 * <p>
	 * If {@link #getIdentifierField()} returns a field created by this ITD, a public accessor will automatically be produced
	 * in the declaring class. If the field is declared within the entity itself, it is expected a public accessor
	 * is provided in the same class as declared the field. Failure to provide such an accessor will 
	 * result in an exception.
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
		
		// See if the user provided the field, and thus the accessor method
		if (!getId().equals(id.getDeclaredByMetadataId())) {
			// User is required to provide one
			MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredAccessorName), new ArrayList<JavaType>());
			Assert.isTrue(method != null && Modifier.isPublic(method.getModifier()), "User provided @Id or @EmbeddedId field but failed to provide a public '" + requiredAccessorName + "()' method in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return method;
		}
		
		// We declared the field in this ITD, so produce a public accessor for it
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + id.getFieldName().getSymbolName() + ";");
		
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredAccessorName), id.getFieldType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}
	
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
		
		// See if the user provided the field, and thus the accessor method
		if (!getId().equals(id.getDeclaredByMetadataId())) {
			// User is required to provide one
			MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredMutatorName), paramTypes);
			Assert.isTrue(method != null && Modifier.isPublic(method.getModifier()), "User provided @Id or @EmbeddedId field but failed to provide a public '" + requiredMutatorName + "(id)' method in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return method;
		}
		
		// We declared the field in this ITD, so produce a public mutator for it
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + id.getFieldName().getSymbolName() + " = id;");
		
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredMutatorName), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	/**
	 * Locates the version field.
	 * 
	 * <p>
	 * If a parent is defined, it may provide the field.
	 * 
	 * <p>
	 * If no parent is defined, one may be located or created. Any declared or inherited field which has the 
	 * @javax.persistence.Version annotation will be taken as the version and returned. If no such field is located,
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
		
		// Try to locate an existing field with @javax.persistence.Version
		List<FieldMetadata> found = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.Version"));
		if (found.size() > 0) {
			Assert.isTrue(found.size() == 1, "More than 1 field was annotated with @javax.persistence.Version in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			FieldMetadata field = found.get(0);
			return field;
		}
		
		// Quit at this stage if the user doesn't want a version field
		if ("".equals(versionField)) {
			return null;
		}
		
		// Ensure there isn't already a field called "version"; if so, compute a unique name (it's not really a fatal situation at the end of the day)
		int index= -1;
		JavaSymbolName versionField = null;
		while (true) {
			// Compute the required field name
			index++;
			String fieldName = "";
			for (int i = 0; i < index; i++) {
				fieldName = fieldName + "_";
			}
			fieldName = fieldName + this.versionField;
			
			versionField = new JavaSymbolName(fieldName);
			if (MemberFindingUtils.getField(governorTypeDetails, versionField) == null) {
				// Found a usable field name
				break;
			}
		}
		
		// We're creating one
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		AnnotationMetadata idAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.Version"), new ArrayList<AnnotationAttributeValue<?>>());
		annotations.add(idAnnotation);
		
		List<AnnotationAttributeValue<?>> columnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		columnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), versionField.getSymbolName()));
		AnnotationMetadata columnAnnotation = new DefaultAnnotationMetadata(COLUMN, columnAttributes);
		annotations.add(columnAnnotation);
		
		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, versionField, versionType, null, annotations);
	}

	/**
	 * Locates the version accessor method.
	 * 
	 * <p>
	 * If {@link #getVersionField()} returns a field created by this ITD, a public accessor will automatically be produced
	 * in the declaring class. If the field is declared within the entity itself, it is expected a public accessor
	 * is provided in the same class as declared the field. Failure to provide such an accessor will 
	 * result in an exception.
	 * 
	 * @return the version identifier (may return null if there is no version field declared in this class)
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
		
		// See if the user provided the field, and thus the accessor method
		if (!getId().equals(version.getDeclaredByMetadataId())) {
			// User is required to provide one
			MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredAccessorName), new ArrayList<JavaType>());
			Assert.notNull(method, "User provided @javax.persistence.Version field but failed to provide a public '" + requiredAccessorName + "()' method in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			Assert.isTrue(Modifier.isPublic(method.getModifier()), "User provided @javax.persistence.Version field but failed to provide a public '" + requiredAccessorName + "()' method in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return method;
		}
		
		// We declared the field in this ITD, so produce a public accessor for it
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + version.getFieldName().getSymbolName() + ";");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredAccessorName), version.getFieldType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}
	
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
		
		// See if the user provided the field, and thus the accessor method
		if (!getId().equals(version.getDeclaredByMetadataId())) {
			// User is required to provide one
			MethodMetadata method = MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredMutatorName), paramTypes);
			Assert.notNull(method, "User provided @javax.persistence.Version field but failed to provide a public '" + requiredMutatorName + "(id)' method in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			Assert.isTrue(Modifier.isPublic(method.getModifier()), "User provided @javax.persistence.Version field but failed to provide a public '" + requiredMutatorName + "(id)' method in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return method;
		}
		
		// We declared the field in this ITD, so produce a public mutator for it
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + version.getFieldName().getSymbolName() + " = version;");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredMutatorName), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}
	
	/**
	 * @return the merge method (may return null)
	 */
	public MethodMetadata getPersistMethod() {
		if (parent != null) {
			MethodMetadata found = parent.getPersistMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(persistMethod)) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(persistMethod), "persist");
	}
	
	/**
	 * @return the merge method (may return null)
	 */
	public MethodMetadata getRemoveMethod() {
		if (parent != null) {
			MethodMetadata found = parent.getRemoveMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(removeMethod)) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(removeMethod), "remove");
	}
	
	/**
	 * @return the merge method (never returns null)
	 */
	public MethodMetadata getFlushMethod() {
		if (parent != null) {
			MethodMetadata found = parent.getFlushMethod();
			if (found != null) {
				return found;
			}
		}
		if ("".equals(flushMethod)) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(flushMethod), "flush");
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
		if ("".equals(mergeMethod)) {
			return null;
		}
		return getDelegateMethod(new JavaSymbolName(mergeMethod), "merge");
	}
	
	private MethodMetadata getDelegateMethod(JavaSymbolName methodName, String entityManagerDelegate) {
		// Method definition to find or build
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			return userMethod;
		}
		
		// Create the method
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>(); 

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		
		// Address non-injected entity manager field
		MethodMetadata entityManagerMethod = getEntityManagerMethod();
		Assert.notNull(entityManagerMethod, "Entity manager method should not have returned null");
		
		// Use the getEntityManager() method to acquire an entity manager (the method will throw an exception if it cannot be acquired)
		bodyBuilder.appendFormalLine("if (this." + getEntityManagerField().getFieldName().getSymbolName() + " == null) this." + getEntityManagerField().getFieldName().getSymbolName() + " = " + entityManagerMethod.getMethodName().getSymbolName() + "();");
		
		JavaType returnType = JavaType.VOID_PRIMITIVE;
		if ("flush".equals(entityManagerDelegate)) {
			addTransactionalAnnotation(annotations);
			
			bodyBuilder.appendFormalLine("this." + getEntityManagerField().getFieldName().getSymbolName() + ".flush();");
		} else if ("merge".equals(entityManagerDelegate)) {
			addTransactionalAnnotation(annotations);
			returnType = new JavaType(governorTypeDetails.getName().getSimpleTypeName());
			bodyBuilder.appendFormalLine(governorTypeDetails.getName().getSimpleTypeName() + " merged = this." + getEntityManagerField().getFieldName().getSymbolName() + ".merge(this);");
			bodyBuilder.appendFormalLine("this." + getEntityManagerField().getFieldName().getSymbolName() + ".flush();");
			bodyBuilder.appendFormalLine("return merged;");
		} else if ("remove".equals(entityManagerDelegate)) {
			addTransactionalAnnotation(annotations);
			bodyBuilder.appendFormalLine("if (this." + getEntityManagerField().getFieldName().getSymbolName() + ".contains(this)) {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine("this." + getEntityManagerField().getFieldName().getSymbolName() + ".remove(this);");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("} else {");
			bodyBuilder.indent();
			bodyBuilder.appendFormalLine(governorTypeDetails.getName().getSimpleTypeName() + " attached = this." + getEntityManagerField().getFieldName().getSymbolName() + ".find(this.getClass(), this." + getIdentifierField().getFieldName().getSymbolName() + ");");
			bodyBuilder.appendFormalLine("this." + getEntityManagerField().getFieldName().getSymbolName() + ".remove(attached);");
			bodyBuilder.indentRemove();
			bodyBuilder.appendFormalLine("}");
		} else {
			// Persist
			addTransactionalAnnotation(annotations, true);
			bodyBuilder.appendFormalLine("this." + getEntityManagerField().getFieldName().getSymbolName() + "." + entityManagerDelegate  + "(this);");
		}

		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), new ArrayList<JavaSymbolName>(), annotations, new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	private void addTransactionalAnnotation(List<AnnotationMetadata> annotations, boolean isPersistMethod) {
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		if (isGaeEnabled && isPersistMethod) {
			attributes.add(new EnumAttributeValue(new JavaSymbolName("propagation"), new EnumDetails(new JavaType("org.springframework.transaction.annotation.Propagation"), new JavaSymbolName("REQUIRES_NEW"))));
		}
		annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.transaction.annotation.Transactional"), attributes));
	}
	
	private void addTransactionalAnnotation(List<AnnotationMetadata> annotations) {
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
			bodyBuilder.appendFormalLine(ENTITY_MANAGER.getNameIncludingTypeParameters(false, builder.getImportRegistrationResolver()) + " em = new " + governorTypeDetails.getName().getSimpleTypeName() + "(){");
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
		
		return new DefaultMethodMetadata(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), null, new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}
	
	/**
	 * @return the count method (may return null)
	 */
	public MethodMetadata getCountMethod() {
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName(countMethod + plural);
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
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (isDataNucleusEnabled) {
			bodyBuilder.appendFormalLine("return ((Number) " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"select count(o) from " + governorTypeDetails.getName().getSimpleTypeName() + " o\").getSingleResult()).longValue();");
		} else {
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"select count(o) from " + governorTypeDetails.getName().getSimpleTypeName() + " o\", Long.class).getSingleResult();");			
		}
		int modifier = Modifier.PUBLIC | Modifier.STATIC;
		
		return new DefaultMethodMetadata(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, annotations, new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}
	
	/**
	 * @return the find all method (may return null)
	 */
	public MethodMetadata getFindAllMethod() {
		if ("".equals(findAllMethod)) {
			return null;
		}
		
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName(findAllMethod + plural);
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
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (isDataNucleusEnabled) {
			addSuppressWarnings(annotations);
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"select o from " + governorTypeDetails.getName().getSimpleTypeName() + " o\").getResultList();");
		} else {
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"select o from " + governorTypeDetails.getName().getSimpleTypeName() + " o\", " + governorTypeDetails.getName().getSimpleTypeName() + ".class).getResultList();");
		}
		int modifier = Modifier.PUBLIC | Modifier.STATIC;
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}
		
		return new DefaultMethodMetadata(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, annotations, new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	/**
	 * @return the find (by ID) method (may return null)
	 */
	public MethodMetadata getFindMethod() {
		if ("".equals(findMethod)) {
			return null;
		}
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName(findMethod + governorTypeDetails.getName().getSimpleTypeName());
		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(getIdentifierField().getFieldType());
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(new JavaSymbolName("id"));
		JavaType returnType = governorTypeDetails.getName();
		
		// Locate user-defined method
		MethodMetadata userMethod = MemberFindingUtils.getMethod(governorTypeDetails, methodName, paramTypes);
		if (userMethod != null) {
			Assert.isTrue(userMethod.getReturnType().equals(returnType), "Method '" + methodName + "' on '" + governorTypeDetails.getName() + "' must return '" + returnType.getNameIncludingTypeParameters() + "'");
			return userMethod;
		}
		
		// Create method
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (JavaType.STRING_OBJECT.equals(getIdentifierField().getFieldType())) {
			bodyBuilder.appendFormalLine("if (id == null || 0 == id.length()) return null;");
		} else if (!getIdentifierField().getFieldType().isPrimitive()) {
			bodyBuilder.appendFormalLine("if (id == null) return null;");
		}
		bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().find(" + governorTypeDetails.getName().getSimpleTypeName() + ".class, id);");
		int modifier = Modifier.PUBLIC | Modifier.STATIC;
		
		return new DefaultMethodMetadata(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, annotations, new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}
	
	/**
	 * @return the find entries method (may return null)
	 */
	public MethodMetadata getFindEntriesMethod() {
		if ("".equals(findEntriesMethod)) {
			return null;
		}
		// Method definition to find or build
		JavaSymbolName methodName = new JavaSymbolName(findEntriesMethod + governorTypeDetails.getName().getSimpleTypeName() + "Entries");
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
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		if (isDataNucleusEnabled) {
			addSuppressWarnings(annotations);
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"select o from " + governorTypeDetails.getName().getSimpleTypeName() + " o\").setFirstResult(firstResult).setMaxResults(maxResults).getResultList();");
		} else {
			bodyBuilder.appendFormalLine("return " + ENTITY_MANAGER_METHOD_NAME + "().createQuery(\"select o from " + governorTypeDetails.getName().getSimpleTypeName() + " o\", " + governorTypeDetails.getName().getSimpleTypeName() + ".class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();");
		}
		int modifier = Modifier.PUBLIC | Modifier.STATIC;
		if (isGaeEnabled) {
			addTransactionalAnnotation(annotations);
		}
		
		return new DefaultMethodMetadata(getId(), modifier, methodName, returnType, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, annotations, new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	private void addSuppressWarnings(List<AnnotationMetadata> annotations) {
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("value"), "unchecked"));
		annotations.add(new DefaultAnnotationMetadata(new JavaType("java.lang.SuppressWarnings"), attributes));
	}
	
	/**
	 * @return the dynamic, custom finders (never returns null, but may return an empty list)
	 */
	public List<String> getDynamicFinders() {
		List<String> result = new ArrayList<String>();
		if (finders == null || finders.length == 0) {
			return result;
		}
		for (String finder : finders) {
			result.add(finder);
		}
		return result;
	}

	/**
	 * @return the pluralised name (never returns null or an empty string)
	 */
	public String getPlural() {
		return plural;
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("finders", finders);
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
