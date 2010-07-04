package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.dbre.model.Column;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DatabaseModelService;
import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.entity.RooIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooDbManaged}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DbreMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {
	private static final String PROVIDES_TYPE_STRING = DbreMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);
	private EntityMetadata entityMetadata;
	private MetadataService metadataService;

	public DbreMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, EntityMetadata entityMetadata, MetadataService metadataService, TableModelService tableModelService, DatabaseModelService databaseModelService) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		this.entityMetadata = entityMetadata;
		this.metadataService = metadataService;

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooDbManaged.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		JavaType javaType = governorPhysicalTypeMetadata.getPhysicalTypeDetails().getName();
		String tableNamePattern = tableModelService.suggestTableNameForNewType(javaType);
		Database database = databaseModelService.deserializeDatabaseMetadata();
		Table table = database.findTable(tableNamePattern);
		if (table == null) {
			return;
		}
		
		// Add fields with their respective accessors and mutators
		for (Column column : table.getColumns()) {
			JavaSymbolName fieldName = new JavaSymbolName(tableModelService.suggestFieldNameForColumn(column.getName()));
			FieldMetadata field = null;
			
			boolean isCompositeKeyField = isCompositeKeyField(fieldName, javaType);
			if ((isEmbeddedIdField(fieldName) && !isCompositeKeyField) || (isIdField(fieldName) && !column.isPrimaryKey())) {
				fieldName = getUniqueFieldName(fieldName);
				field = getField(fieldName, column, javaType);
			} else if (getIdField() != null && column.isPrimaryKey()) {
				field = null;
			} else if (!hasField(fieldName, javaType) && !isCompositeKeyField) {
				field = getField(fieldName, column, javaType);
			}
			
			if (field != null) {
				builder.addField(field);
				
				// Check for an existing accessor in the governor or in the entity metadata
				if (!hasAccessor(field)) {
					builder.addMethod(getAccessor(field));
				}
				
				// Check for an existing mutator in the governor or in the entity metadata
				if (!hasMutator(field)) {
					builder.addMethod(getMutator(field));
				}
			}
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private JavaSymbolName getUniqueFieldName(JavaSymbolName fieldName) {
		int index= -1;
		JavaSymbolName uniqueField = null;
		while (true) {
			// Compute the required field name
			index++;
			String uniqueFieldName = "";
			for (int i = 0; i < index; i++) {
				uniqueFieldName = uniqueFieldName + "_";
			}
			uniqueFieldName = uniqueFieldName + fieldName;
			uniqueField = new JavaSymbolName(uniqueFieldName);
			if (MemberFindingUtils.getField(governorTypeDetails, uniqueField) == null) {
				// Found a usable field name
				break;
			}
		}
		return uniqueField;
	}
	
	@SuppressWarnings("unchecked")
	public boolean isCompositeKeyField(JavaSymbolName fieldName, JavaType javaType) {
		// Check for identifier class and exclude fields that are part of the composite primary key
		AnnotationMetadata entityAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooEntity.class.getName()));
		AnnotationAttributeValue<?> identifierTypeAttribute = entityAnnotation.getAttribute(new JavaSymbolName("identifierType"));
		if (identifierTypeAttribute != null) {
			JavaType identifierType = (JavaType) identifierTypeAttribute.getValue();
			if (identifierType != null && !identifierType.getFullyQualifiedTypeName().startsWith("java.lang")) {
				String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
				PhysicalTypeMetadata identifierPhysicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
				if (identifierPhysicalTypeMetadata != null) {
					ClassOrInterfaceTypeDetails identifierTypeDetails = (ClassOrInterfaceTypeDetails) identifierPhysicalTypeMetadata.getPhysicalTypeDetails();
					if (identifierTypeDetails != null) {
						// Check governor for declared fields 
						List<? extends FieldMetadata> identifierFields = identifierTypeDetails.getDeclaredFields();
						for (FieldMetadata field : identifierFields) {
							if (fieldName.equals(field.getFieldName())) {
								return true;
							}
						}
						
						// Check @RooIdentifier annotation for idFields attribute
						AnnotationMetadata identifierAnnotation = MemberFindingUtils.getAnnotationOfType(identifierTypeDetails.getTypeAnnotations(), new JavaType(RooIdentifier.class.getName()));
						if (identifierAnnotation != null) {
							ArrayAttributeValue<StringAttributeValue> idFieldsAttribute = (ArrayAttributeValue<StringAttributeValue>) identifierAnnotation.getAttribute(new JavaSymbolName("idFields"));
							if (idFieldsAttribute != null) {
								List<StringAttributeValue> idFields = idFieldsAttribute.getValue();
								for (StringAttributeValue idField : idFields) {
									if (fieldName.equals(new JavaSymbolName(idField.getValue()))) {
										return true;
									}
								}
							}
						}
					}
				}
			}
		}
		
		return false;
	}

	public boolean hasFieldOnGovenor(JavaSymbolName fieldName) {
		return MemberFindingUtils.getField(governorTypeDetails, fieldName) != null;
	}
	
	public boolean isIdField(JavaSymbolName fieldName) {
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.Id"));
		if (idFields.size() > 0) {
			Assert.isTrue(idFields.size() == 1, "More than one field was annotated with @javax.persistence.Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return idFields.get(0).getFieldName().equals(fieldName);
		}
		return false;
	}
	
	public JavaSymbolName getIdField() {
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.Id"));
		if (idFields.size() > 0) {
			Assert.isTrue(idFields.size() == 1, "More than one field was annotated with @javax.persistence.Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return idFields.get(0).getFieldName();
		}
		return null;
	}
	
	public boolean  isEmbeddedIdField(JavaSymbolName fieldName) {
		List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.EmbeddedId"));
		if (embeddedIdFields.size() > 0) {
			Assert.isTrue(embeddedIdFields.size() == 1, "More than one field was annotated with @javax.persistence.EmbeddedId in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return embeddedIdFields.get(0).getFieldName().equals(fieldName);
		}
		return false;
	}
	
	public boolean hasField(JavaSymbolName fieldName, JavaType javaType) {	
		// Check governor for field
		if (MemberFindingUtils.getField(governorTypeDetails, fieldName) != null) {
			return true;
		}

		// Check entity ITD for field
		List<? extends FieldMetadata> itdFields = entityMetadata.getItdTypeDetails().getDeclaredFields();
		for (FieldMetadata field : itdFields) {
			if (field.getFieldName().equals(fieldName)) {
				return true;
			}
		}

		return false;
	}

	public FieldMetadata getField(JavaSymbolName fieldName, Column column, JavaType javaType) {
		JavaType fieldType = column.getJavaType();

		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		// Add @Column annotation
		List<AnnotationAttributeValue<?>> columnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		columnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), column.getName()));

		// Add length attribute for Strings
		if (fieldType.equals(JavaType.STRING_OBJECT)) {
			columnAttributes.add(new IntegerAttributeValue(new JavaSymbolName("length"), column.getSize()));
		}
		AnnotationMetadata columnAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.Column"), columnAttributes);
		annotations.add(columnAnnotation);

		// Add @NotNull if applicable
		if (!column.isRequired()) {
			AnnotationMetadata notNullAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.NotNull"), new ArrayList<AnnotationAttributeValue<?>>());
			annotations.add(notNullAnnotation);
		}

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldName, fieldType, null, annotations);
	}

	public boolean hasAccessor(FieldMetadata field) {
		String requiredAccessorName = getRequiredAccessorName(field);

		// Check governor for accessor method
		if (MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredAccessorName), new ArrayList<JavaType>()) != null) {
			return true;
		}

		// Check entity ITD for accessor method
		List<? extends MethodMetadata> itdMethods = entityMetadata.getItdTypeDetails().getDeclaredMethods();
		for (MethodMetadata method : itdMethods) {
			if (method.getMethodName().equals(new JavaSymbolName(requiredAccessorName))) {
				return true;
			}
		}
		
		return false;
	}

	public MethodMetadata getAccessor(FieldMetadata field) {
		Assert.notNull(field, "Field required");
		
		// Compute the accessor method name
		String requiredAccessorName = getRequiredAccessorName(field);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + field.getFieldName().getSymbolName() + ";");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredAccessorName), field.getFieldType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	private String getRequiredAccessorName(FieldMetadata field) {
		String methodName;
		if (field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE)) {
			methodName = "is" + StringUtils.capitalize(field.getFieldName().getSymbolName());
		} else {
			methodName = "get" + StringUtils.capitalize(field.getFieldName().getSymbolName());
		}
		return methodName;
	}

	public boolean hasMutator(FieldMetadata field) {
		String requiredMutatorName = getRequiredMutatorName(field);

		// Check governor for mutator method
		if (MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredMutatorName), new ArrayList<JavaType>()) != null) {
			return true;
		}

		// Check entity ITD for mutator method
		List<? extends MethodMetadata> itdMethods = entityMetadata.getItdTypeDetails().getDeclaredMethods();
		for (MethodMetadata method : itdMethods) {
			if (method.getMethodName().equals(new JavaSymbolName(requiredMutatorName))) {
				return true;
			}
		}
		
		return false;
	}

	public MethodMetadata getMutator(FieldMetadata field) {
		String requiredMutatorName = getRequiredMutatorName(field);

		List<JavaType> paramTypes = new ArrayList<JavaType>();
		paramTypes.add(field.getFieldType());
		List<JavaSymbolName> paramNames = new ArrayList<JavaSymbolName>();
		paramNames.add(field.getFieldName());

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("this." + field.getFieldName().getSymbolName() + " = " + field.getFieldName().getSymbolName() + ";");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredMutatorName), JavaType.VOID_PRIMITIVE, AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	private String getRequiredMutatorName(FieldMetadata field) {
		return "set" + StringUtils.capitalize(field.getFieldName().getSymbolName());
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
