package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jvnet.inflector.Noun;
import org.springframework.roo.addon.dbre.model.Column;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.DatabaseModelService;
import org.springframework.roo.addon.dbre.model.ForeignKey;
import org.springframework.roo.addon.dbre.model.ManyToManyAssociation;
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
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.jsr303.SetField;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaPackage;
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
	private static final JavaType ONE_TO_ONE = new JavaType("javax.persistence.OneToOne");
	private static final JavaType ONE_TO_MANY = new JavaType("javax.persistence.OneToMany");
	private static final JavaType MANY_TO_ONE = new JavaType("javax.persistence.ManyToOne");
	private static final JavaType MANY_TO_MANY = new JavaType("javax.persistence.ManyToMany");
	private static final JavaType JOIN_COLUMN = new JavaType("javax.persistence.JoinColumn");

	private EntityMetadata entityMetadata;
	private MetadataService metadataService;
	private TableModelService tableModelService;

	public DbreMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, EntityMetadata entityMetadata, MetadataService metadataService, TableModelService tableModelService, DatabaseModelService databaseModelService) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		this.entityMetadata = entityMetadata;
		this.metadataService = metadataService;
		this.tableModelService = tableModelService;

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooDbManaged.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		JavaType javaType = governorPhysicalTypeMetadata.getPhysicalTypeDetails().getName();
		JavaPackage javaPackage = javaType.getPackage();
		Database database = databaseModelService.deserializeDatabaseMetadata();
		Table table = database.findTable(tableModelService.suggestTableNameForNewType(javaType));
		if (table == null) {
			return;
		}

		// Add field for a many-valued association with many-to-many multiplicity
		addManyToManyField(javaType, javaPackage, database, table);

		// Add fields for many-valued associations with one-to-many multiplicity and
		// single-valued associations to other entities that have one-to-one multiplicity
		addOneToXXXFields(javaType, javaPackage, database, table);

		// Add fields from columns with their respective accessors and mutators
		addFields(javaType, javaPackage, table);

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private void addManyToManyField(JavaType javaType, JavaPackage javaPackage, Database database, Table table) {
		ManyToManyAssociation association = null;

		association = database.getOwningSideOfManyToManyAssociation(table);
		if (association != null) {
			JavaSymbolName fieldName = new JavaSymbolName(getInflectorPlural(tableModelService.suggestFieldName(association.getInverseSideTable().getName())));
			FieldMetadata field = getManyToManyOwningSideField(fieldName, association, javaPackage);
			addToBuilder(field);
		}

		association = database.getInverseSideOfManyToManyAssociation(table);
		if (association != null) {
			JavaSymbolName fieldName = new JavaSymbolName(getInflectorPlural(tableModelService.suggestFieldName(association.getOwningSideTable().getName())));
			FieldMetadata field = getManyToManyInverseSideField(fieldName, association, javaPackage);
			addToBuilder(field);
		}
	}

	private void addOneToXXXFields(JavaType javaType, JavaPackage javaPackage, Database database, Table table) {
		if (!database.isManyToManyJoinTable(table)) {
			for (ForeignKey exportedKey : table.getExportedKeys()) {
				String foreignTableName = exportedKey.getForeignTableName();
				if (!database.isManyToManyJoinTable(foreignTableName)) {
					Table foreignTable = database.findTable(foreignTableName);
					boolean isOneToOne = true;
					Iterator<ForeignKey> foreignKeyIterator = foreignTable.getForeignKeys().iterator();
					while (isOneToOne && foreignKeyIterator.hasNext()) {
						isOneToOne &= isOneToOne(foreignTable, foreignKeyIterator.next());
					}

					JavaType fieldType = tableModelService.findTypeForTableName(foreignTableName, javaPackage);
					JavaSymbolName mappedByFieldName = new JavaSymbolName(tableModelService.suggestFieldName(table.getName()));
					FieldMetadata field;
					if (isOneToOne) {
						JavaSymbolName fieldName = new JavaSymbolName(tableModelService.suggestFieldName(foreignTableName));
						field = getOneToOneMappedByField(fieldName, fieldType, mappedByFieldName);
					} else {
						JavaSymbolName fieldName = new JavaSymbolName(getInflectorPlural(tableModelService.suggestFieldName(foreignTableName)));
						field = getOneToManyMappedByField(fieldName, mappedByFieldName, foreignTableName, javaPackage);
					}
					addToBuilder(field);
				}
			}
		}
	}

	private void addFields(JavaType javaType, JavaPackage javaPackage, Table table) {
		Map<JavaSymbolName, FieldMetadata> uniqueFields = new LinkedHashMap<JavaSymbolName, FieldMetadata>();

		for (Column column : table.getColumns()) {
			String columnName = column.getName();
			JavaSymbolName fieldName = new JavaSymbolName(tableModelService.suggestFieldName(columnName));
			ForeignKey foreignKey = null;
			boolean isCompositeKeyField = isCompositeKeyField(fieldName, javaType);
			FieldMetadata field = null;

			if ((isEmbeddedIdField(fieldName) && !isCompositeKeyField) || (isIdField(fieldName) && !column.isPrimaryKey())) {
				fieldName = getUniqueFieldName(fieldName);
				field = getField(fieldName, column);
				uniqueFields.put(fieldName, field);
			} else if (getIdField() != null && column.isPrimaryKey()) {
				field = null;
			} else if ((foreignKey = table.findForeignKeyByLocalColumnName(columnName)) != null) {
				fieldName = new JavaSymbolName(tableModelService.suggestFieldName(foreignKey.getForeignTableName()));
				JavaType fieldType = tableModelService.suggestTypeNameForNewTable(foreignKey.getForeignTableName(), javaPackage);

				// Check for one-to-one multiplicity
				if (isOneToOne(table, foreignKey)) {
					field = getOneToOneField(fieldName, fieldType, foreignKey, column);
					uniqueFields.put(fieldName, field);
				} else {
					// Assume many-to-one multiplicity
					field = getManyToOneField(fieldName, fieldType, column);
					uniqueFields.put(fieldName, field);
				}
			} else if (!isCompositeKeyField) {
				field = getField(fieldName, column);
				uniqueFields.put(fieldName, field);
			}
		}

		for (FieldMetadata field : uniqueFields.values()) {
			addToBuilder(field);
		}
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

	public boolean isIdField(JavaSymbolName fieldName) {
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.Id"));
		if (idFields.size() > 0) {
			Assert.isTrue(idFields.size() == 1, "More than one field was annotated with @Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return idFields.get(0).getFieldName().equals(fieldName);
		}
		return false;
	}

	public JavaSymbolName getIdField() {
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.Id"));
		if (idFields.size() > 0) {
			Assert.isTrue(idFields.size() == 1, "More than one field was annotated with @Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return idFields.get(0).getFieldName();
		}
		return null;
	}

	public boolean isEmbeddedIdField(JavaSymbolName fieldName) {
		List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.EmbeddedId"));
		if (embeddedIdFields.size() > 0) {
			Assert.isTrue(embeddedIdFields.size() == 1, "More than one field was annotated with @EmbeddedId in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return embeddedIdFields.get(0).getFieldName().equals(fieldName);
		}
		return false;
	}

	private FieldMetadata getManyToManyOwningSideField(JavaSymbolName fieldName, ManyToManyAssociation association, JavaPackage javaPackage) {
		List<JavaType> params = new ArrayList<JavaType>();
		JavaType element = tableModelService.findTypeForTableName(association.getInverseSideTable().getName(), javaPackage);
		params.add(element);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(element, Path.SRC_MAIN_JAVA);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType("java.util.Set", 0, DataType.TYPE, null, params), fieldName, element, Cardinality.MANY_TO_MANY);

		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		// Add @ManyToMany annotation
		AnnotationMetadata manyToManyAnnotation = new DefaultAnnotationMetadata(MANY_TO_MANY, new ArrayList<AnnotationAttributeValue<?>>());
		annotations.add(manyToManyAnnotation);

		// Add @JoinTable annotation
		List<AnnotationAttributeValue<?>> joinTableAnnotationAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		joinTableAnnotationAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), association.getJoinTable().getName()));

		// Add joinColumns attribute containing nested @JoinColumn annotation
		List<NestedAnnotationAttributeValue> joinColumnArrayValues = new ArrayList<NestedAnnotationAttributeValue>();
		List<AnnotationAttributeValue<?>> joinColumnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		joinColumnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), association.getPrimaryKeyOfOwningSideTable()));
		AnnotationMetadata joinColumnAnnotation = new DefaultAnnotationMetadata(JOIN_COLUMN, joinColumnAttributes);
		joinColumnArrayValues.add(new NestedAnnotationAttributeValue(new JavaSymbolName("value"), joinColumnAnnotation));
		joinTableAnnotationAttributes.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(new JavaSymbolName("joinColumns"), joinColumnArrayValues));

		// Add inverseJoinColumns attribute containing nested @JoinColumn annotation
		List<NestedAnnotationAttributeValue> inverseJoinColumnArrayValues = new ArrayList<NestedAnnotationAttributeValue>();
		List<AnnotationAttributeValue<?>> inverseJoinColumnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		inverseJoinColumnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), association.getPrimaryKeyOfInverseSideTable()));
		AnnotationMetadata inverseJoinColumnAnnotation = new DefaultAnnotationMetadata(JOIN_COLUMN, inverseJoinColumnAttributes);
		inverseJoinColumnArrayValues.add(new NestedAnnotationAttributeValue(new JavaSymbolName("value"), inverseJoinColumnAnnotation));
		joinTableAnnotationAttributes.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(new JavaSymbolName("inverseJoinColumns"), inverseJoinColumnArrayValues));

		AnnotationMetadata joinTableAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.JoinTable"), joinTableAnnotationAttributes);
		annotations.add(joinTableAnnotation);

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldDetails.getFieldName(), fieldDetails.getFieldType(), null, annotations);
	}

	private FieldMetadata getManyToManyInverseSideField(JavaSymbolName fieldName, ManyToManyAssociation association, JavaPackage javaPackage) {
		List<JavaType> params = new ArrayList<JavaType>();
		JavaType element = tableModelService.findTypeForTableName(association.getOwningSideTable().getName(), javaPackage);
		params.add(element);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(element, Path.SRC_MAIN_JAVA);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType("java.util.Set", 0, DataType.TYPE, null, params), fieldName, element, Cardinality.MANY_TO_MANY);

		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		JavaSymbolName mappedByFieldName = new JavaSymbolName(getInflectorPlural(tableModelService.suggestFieldName(association.getInverseSideTable().getName())));

		attributes.add(new StringAttributeValue(new JavaSymbolName("mappedBy"), mappedByFieldName.getSymbolName()));
		AnnotationMetadata annotation = new DefaultAnnotationMetadata(MANY_TO_MANY, attributes);
		annotations.add(annotation);

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldDetails.getFieldName(), fieldDetails.getFieldType(), null, annotations);
	}

	private FieldMetadata getOneToOneMappedByField(JavaSymbolName fieldName, JavaType fieldType, JavaSymbolName mappedByFieldName) {
		// Add @OneToOne annotation
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("mappedBy"), mappedByFieldName.getSymbolName()));
		AnnotationMetadata oneToOneAnnotation = new DefaultAnnotationMetadata(ONE_TO_ONE, attributes);
		annotations.add(oneToOneAnnotation);

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldName, fieldType, null, annotations);
	}

	private FieldMetadata getOneToManyMappedByField(JavaSymbolName fieldName, JavaSymbolName mappedByFieldName, String foreignTableName, JavaPackage javaPackage) {
		List<JavaType> params = new ArrayList<JavaType>();

		JavaType element = tableModelService.findTypeForTableName(foreignTableName, javaPackage);
		params.add(element);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(element, Path.SRC_MAIN_JAVA);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType("java.util.Set", 0, DataType.TYPE, null, params), fieldName, element, Cardinality.ONE_TO_MANY);

		// Add @OneToMany annotation
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("mappedBy"), mappedByFieldName.getSymbolName()));
		AnnotationMetadata annotation = new DefaultAnnotationMetadata(ONE_TO_MANY, attributes);
		annotations.add(annotation);

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldDetails.getFieldName(), fieldDetails.getFieldType(), null, annotations);
	}

	public FieldMetadata getManyToOneField(JavaSymbolName fieldName, JavaType fieldType, Column column) {
		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		// Add @ManyToOne annotation
		AnnotationMetadata manyToOneAnnotation = new DefaultAnnotationMetadata(MANY_TO_ONE, new ArrayList<AnnotationAttributeValue<?>>());
		annotations.add(manyToOneAnnotation);

		// Add @JoinColumn annotation
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("name"), column.getName()));
		AnnotationMetadata joinColumnAnnotation = new DefaultAnnotationMetadata(JOIN_COLUMN, attributes);
		annotations.add(joinColumnAnnotation);

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldName, fieldType, null, annotations);
	}

	public boolean isOneToOne(Table table, ForeignKey foreignKey) {
		boolean equals = table.getPrimaryKeyCount() == foreignKey.getReferenceCount();
		Iterator<Column> primaryKeyIterator = table.getPrimaryKeys().iterator();
		while (equals && primaryKeyIterator.hasNext()) {
			equals &= foreignKey.hasLocalColumn(primaryKeyIterator.next());
		}
		return equals;
	}

	public FieldMetadata getOneToOneField(JavaSymbolName fieldName, JavaType fieldType, ForeignKey foreignKey, Column column) {
		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		// Add @OneToOne annotation
		AnnotationMetadata oneToOneAnnotation = new DefaultAnnotationMetadata(ONE_TO_ONE, new ArrayList<AnnotationAttributeValue<?>>());
		annotations.add(oneToOneAnnotation);

		if (foreignKey.getReferenceCount() == 1) {
			// Add @JoinColumn annotation
			List<AnnotationAttributeValue<?>> joinColumnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			joinColumnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), column.getName()));
			AnnotationMetadata joinColumnAnnotation = new DefaultAnnotationMetadata(JOIN_COLUMN, joinColumnAttributes);
			annotations.add(joinColumnAnnotation);
		} else {
			// Add @JoinColumns annotations
			List<NestedAnnotationAttributeValue> joinColumnsArrayValues = new ArrayList<NestedAnnotationAttributeValue>();

			for (org.springframework.roo.addon.dbre.model.Reference reference : foreignKey.getReferences()) {
				List<AnnotationAttributeValue<?>> joinColumnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
				joinColumnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), reference.getLocalColumnName()));
				joinColumnAttributes.add(new StringAttributeValue(new JavaSymbolName("referencedColumnName"), reference.getForeignColumnName()));
				AnnotationMetadata joinColumnAnnotation = new DefaultAnnotationMetadata(JOIN_COLUMN, joinColumnAttributes);
				joinColumnsArrayValues.add(new NestedAnnotationAttributeValue(new JavaSymbolName("value"), joinColumnAnnotation));
			}

			List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
			attributes.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(new JavaSymbolName("value"), joinColumnsArrayValues));

			AnnotationMetadata joinColumnsAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.JoinColumns"), attributes);
			annotations.add(joinColumnsAnnotation);
		}

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldName, fieldType, null, annotations);
	}

	public FieldMetadata getField(JavaSymbolName fieldName, Column column) {
		JavaType fieldType = column.getType().getJavaType();

		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		// Add @NotNull if applicable
		if (column.isRequired()) {
			AnnotationMetadata notNullAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.NotNull"), new ArrayList<AnnotationAttributeValue<?>>());
			annotations.add(notNullAnnotation);
		}

		// Add @Column annotation
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(new JavaSymbolName("name"), column.getName()));

		// Add length attribute for Strings
		if (fieldType.equals(JavaType.STRING_OBJECT)) {
			attributes.add(new IntegerAttributeValue(new JavaSymbolName("length"), column.getSize()));
		}

		AnnotationMetadata annotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.Column"), attributes);
		annotations.add(annotation);

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldName, fieldType, null, annotations);
	}

	private void addToBuilder(FieldMetadata field) {
		if (field != null && !hasField(field)) {
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

	private JavaSymbolName getUniqueFieldName(JavaSymbolName fieldName) {
		int index = -1;
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

	public boolean hasField(JavaSymbolName fieldName, JavaType javaType) {
		// Check governor for field
		if (MemberFindingUtils.getField(governorTypeDetails, fieldName) != null) {
			return true;
		}

		// Check entity ITD for field
		List<? extends FieldMetadata> itdFields = entityMetadata.getItdTypeDetails().getDeclaredFields();
		for (FieldMetadata itdField : itdFields) {
			if (itdField.getFieldName().equals(fieldName)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasField(FieldMetadata field) {
		return hasField(field.getFieldName(), field.getFieldType());
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

	public String getInflectorPlural(String term) {
		try {
			return Noun.pluralOf(term, Locale.ENGLISH);
		} catch (RuntimeException re) {
			// Inflector failed (see for example ROO-305), so don't pluralize it
			return term;
		}
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
