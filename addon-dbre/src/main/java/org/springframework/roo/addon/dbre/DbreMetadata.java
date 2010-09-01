package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;

import org.jvnet.inflector.Noun;
import org.springframework.roo.addon.dbre.model.Column;
import org.springframework.roo.addon.dbre.model.Database;
import org.springframework.roo.addon.dbre.model.ForeignKey;
import org.springframework.roo.addon.dbre.model.JoinTable;
import org.springframework.roo.addon.dbre.model.Reference;
import org.springframework.roo.addon.dbre.model.Table;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.IdentifierMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
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
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
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
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooDbManaged}.
 * 
 * <p>
 * Creates and manages entity relationships, such as many-valued and single-valued associations.
 * 
 * <p>
 * One-to-many and one-to-one associations are created based on the following laws:
 * <ul>
 * <li>Primary Key (PK) - Foreign Key (FK) LAW #1: If the foreign key column is part of the primary key (or part of an index) then the relationship between the tables will be one to many (1:M).
 * <li>Primary Key (PK) - Foreign Key (FK) LAW #2: If the foreign key column represents the entire primary key (or the entire index) then the relationship between the tables will be one to one (1:1).
 * </ul>
 * 
 * <p>
 * Many-to-many associations are created if a join table is detected. To be identified as a many-to-many join table, the table must have have exactly two primary keys and have exactly two foreign-keys
 * pointing to other entity tables and have no other columns.
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
	private static final JavaSymbolName NAME = new JavaSymbolName("name");
	private static final JavaSymbolName VALUE = new JavaSymbolName("value");
	private static final JavaSymbolName MAPPED_BY = new JavaSymbolName("mappedBy");
	private static final JavaSymbolName REFERENCED_COLUMN = new JavaSymbolName("referencedColumnName");

	private EntityMetadata entityMetadata;
	private MetadataService metadataService;
	private DbreTypeResolutionService dbreTypeResolutionService;

	public DbreMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, EntityMetadata entityMetadata, MetadataService metadataService, DbreTypeResolutionService dbreTypeResolutionService, Database database) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		this.entityMetadata = entityMetadata;
		this.metadataService = metadataService;
		this.dbreTypeResolutionService = dbreTypeResolutionService;
		
		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooDbManaged.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		JavaType javaType = governorPhysicalTypeMetadata.getPhysicalTypeDetails().getName();

		Table table = database.findTable(dbreTypeResolutionService.suggestTableNameForNewType(javaType));
		if (table == null) {
			return;
		}
		
		// Add fields for many-valued associations with many-to-many multiplicity
		addManyToManyFields(database, table);

		// Add fields for single-valued associations to other entities that have one-to-one multiplicity
		addOneToOneFields(database, table);

		// Add fields for many-valued associations with one-to-many multiplicity
		addOneToManyFields(database, table);

		// Add fields for single-valued associations to other entities that have many-to-one multiplicity
		addManyToOneFields(database, table);

		// Add remaining fields from columns
		addOtherFields(javaType, table);

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private void addManyToManyFields(Database database, Table table) {
		int manyToManyCount = database.getJoinTables().size() > 1 ? 1 : 0;
		for (JoinTable joinTable : database.getJoinTables()) {
			String fieldSuffix = manyToManyCount > 0 ? String.valueOf(manyToManyCount) : "";

			if (joinTable.getOwningSideTable().equals(table)) {
				JavaSymbolName fieldName = new JavaSymbolName(getInflectorPlural(dbreTypeResolutionService.suggestFieldName(joinTable.getInverseSideTable().getName())) + fieldSuffix);
				FieldMetadata field = getManyToManyOwningSideField(fieldName, joinTable, governorTypeDetails.getName().getPackage());
				addToBuilder(field);
			}

			if (joinTable.getInverseSideTable().equals(table)) {
				JavaSymbolName fieldName = new JavaSymbolName(getInflectorPlural(dbreTypeResolutionService.suggestFieldName(joinTable.getOwningSideTable().getName())) + fieldSuffix);
				JavaSymbolName mappedByFieldName = new JavaSymbolName(getInflectorPlural(dbreTypeResolutionService.suggestFieldName(joinTable.getInverseSideTable().getName())) + fieldSuffix);
				FieldMetadata field = getManyToManyInverseSideField(fieldName, mappedByFieldName, joinTable, governorTypeDetails.getName().getPackage());
				addToBuilder(field);
			}

			manyToManyCount++;
		}
	}

	private void addOneToOneFields(Database database, Table table) {
		// Add unique one-to-one fields
		Map<JavaSymbolName, FieldMetadata> uniqueFields = new LinkedHashMap<JavaSymbolName, FieldMetadata>();

		for (Column column : table.getColumns()) {
			ForeignKey foreignKey = table.findForeignKeyByLocalColumnName(column.getName());
			if (foreignKey != null && isOneToOne(table, foreignKey)) {
				String foreignTableName = foreignKey.getForeignTableName();
				Short keySequence = foreignKey.getKeySequence();
				String fieldSuffix = keySequence != null && keySequence > 0 ? String.valueOf(keySequence) : "";
				JavaSymbolName fieldName = new JavaSymbolName(dbreTypeResolutionService.suggestFieldName(foreignTableName) + fieldSuffix);
				JavaType fieldType = dbreTypeResolutionService.findTypeForTableName(foreignTableName, governorTypeDetails.getName().getPackage());
				Assert.notNull(fieldType, getErrorMsg(foreignTableName));

				// Fields are stored in a field-keyed map first before adding them to the builder.
				// This ensures the fields from foreign keys with multiple columns will only get created once.
				FieldMetadata field = getOneToOneField(fieldName, fieldType, foreignKey.getReferences(), column, isCompositeKeyColumn(column.getName(), fieldType));
				uniqueFields.put(fieldName, field);
			}
		}

		for (FieldMetadata field : uniqueFields.values()) {
			addToBuilder(field);
		}

		// Add one-to-one mapped-by fields
		if (!database.isJoinTable(table)) {
			for (ForeignKey exportedKey : table.getExportedKeys()) {
				if (!database.isJoinTable(exportedKey.getForeignTable())) {
					String foreignTableName = exportedKey.getForeignTableName();
					Table foreignTable = database.findTable(foreignTableName);
					Assert.notNull(foreignTable, "Related table '" + foreignTableName + "' could not be found but was referenced by table '" + table.getName() + "'");

					if (isOneToOne(foreignTable, foreignTable.getForeignKey(exportedKey.getName()))) {
						Short keySequence = exportedKey.getKeySequence();
						String fieldSuffix = keySequence != null && keySequence > 0 ? String.valueOf(keySequence) : "";
						JavaSymbolName fieldName = new JavaSymbolName(dbreTypeResolutionService.suggestFieldName(foreignTableName) + fieldSuffix);
						
						JavaType fieldType = dbreTypeResolutionService.findTypeForTableName(foreignTableName, governorTypeDetails.getName().getPackage());
						Assert.notNull(fieldType, getErrorMsg(foreignTableName));

						JavaSymbolName mappedByFieldName = new JavaSymbolName(dbreTypeResolutionService.suggestFieldName(table.getName()) + fieldSuffix);
						FieldMetadata field = getOneToOneMappedByField(fieldName, fieldType, mappedByFieldName);
						addToBuilder(field);
					}
				}
			}
		}
	}

	private void addOneToManyFields(Database database, Table table) {
		if (!database.isJoinTable(table)) {
			for (ForeignKey exportedKey : table.getExportedKeys()) {
				if (!database.isJoinTable(exportedKey.getForeignTable())) {
					String foreignTableName = exportedKey.getForeignTableName();
					Table foreignTable = database.findTable(foreignTableName);
					Assert.notNull(foreignTable, "Related table '" + foreignTableName + "' could not be found but was referenced by table '" + table.getName() + "'");

					if (!isOneToOne(foreignTable, foreignTable.getForeignKey(exportedKey.getName()))) {
						Short keySequence = exportedKey.getKeySequence();
						String fieldSuffix = keySequence != null  && keySequence > 0 ? String.valueOf(keySequence) : "";
						JavaSymbolName fieldName = new JavaSymbolName(getInflectorPlural(dbreTypeResolutionService.suggestFieldName(foreignTableName)) + fieldSuffix);
						JavaSymbolName mappedByFieldName = new JavaSymbolName(dbreTypeResolutionService.suggestFieldName(table.getName()) + fieldSuffix);
						FieldMetadata field = getOneToManyMappedByField(fieldName, mappedByFieldName, foreignTableName, governorTypeDetails.getName().getPackage());
						addToBuilder(field);
					}
				}
			}
		}
	}

	private void addManyToOneFields(Database database, Table table) {
		// Add unique many-to-one fields
		Map<JavaSymbolName, FieldMetadata> uniqueFields = new LinkedHashMap<JavaSymbolName, FieldMetadata>();

		for (Column column : table.getColumns()) {
			String columnName = column.getName();
			ForeignKey foreignKey = table.findForeignKeyByLocalColumnName(columnName);
			if (foreignKey != null && !isOneToOne(table, foreignKey)) {
				// Assume many-to-one multiplicity
				Short keySequence = foreignKey.getKeySequence();
				String fieldSuffix = keySequence != null && keySequence > 0 ? String.valueOf(keySequence) : "";
				String foreignTableName = foreignKey.getForeignTableName();
				JavaSymbolName fieldName = new JavaSymbolName(dbreTypeResolutionService.suggestFieldName(foreignTableName) + fieldSuffix);
				JavaType fieldType = dbreTypeResolutionService.findTypeForTableName(foreignTableName, governorTypeDetails.getName().getPackage());
				Assert.notNull(fieldType, getErrorMsg(foreignTableName));

				// Fields are stored in a field-keyed map first before adding them to the builder.
				// This ensures the fields from foreign keys with multiple columns will only get created once.
				FieldMetadata field = getManyToOneField(fieldName, fieldType, foreignKey.getReferences(), isCompositeKeyColumn(column.getName(), fieldType));
				uniqueFields.put(fieldName, field);
			}
		}

		for (FieldMetadata field : uniqueFields.values()) {
			addToBuilder(field);
		}
	}

	private FieldMetadata getManyToManyOwningSideField(JavaSymbolName fieldName, JoinTable joinTable, JavaPackage javaPackage) {
		List<JavaType> params = new ArrayList<JavaType>();
		JavaType element = dbreTypeResolutionService.findTypeForTableName(joinTable.getInverseSideTable().getName(), javaPackage);
		Assert.notNull(element, getErrorMsg(joinTable.getInverseSideTable().getName()));
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
		joinTableAnnotationAttributes.add(new StringAttributeValue(NAME, joinTable.getTable().getName()));

		// Add joinColumns attribute containing nested @JoinColumn annotation
		List<NestedAnnotationAttributeValue> joinColumnArrayValues = new ArrayList<NestedAnnotationAttributeValue>();
		List<AnnotationAttributeValue<?>> joinColumnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		joinColumnAttributes.add(new StringAttributeValue(NAME, joinTable.getPrimaryKeyOfOwningSideTable()));
		AnnotationMetadata joinColumnAnnotation = new DefaultAnnotationMetadata(JOIN_COLUMN, joinColumnAttributes);
		joinColumnArrayValues.add(new NestedAnnotationAttributeValue(VALUE, joinColumnAnnotation));
		joinTableAnnotationAttributes.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(new JavaSymbolName("joinColumns"), joinColumnArrayValues));

		// Add inverseJoinColumns attribute containing nested @JoinColumn annotation
		List<NestedAnnotationAttributeValue> inverseJoinColumnArrayValues = new ArrayList<NestedAnnotationAttributeValue>();
		List<AnnotationAttributeValue<?>> inverseJoinColumnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		inverseJoinColumnAttributes.add(new StringAttributeValue(NAME, joinTable.getPrimaryKeyOfInverseSideTable()));
		AnnotationMetadata inverseJoinColumnAnnotation = new DefaultAnnotationMetadata(JOIN_COLUMN, inverseJoinColumnAttributes);
		inverseJoinColumnArrayValues.add(new NestedAnnotationAttributeValue(VALUE, inverseJoinColumnAnnotation));
		joinTableAnnotationAttributes.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(new JavaSymbolName("inverseJoinColumns"), inverseJoinColumnArrayValues));

		AnnotationMetadata joinTableAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.JoinTable"), joinTableAnnotationAttributes);
		annotations.add(joinTableAnnotation);

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldDetails.getFieldName(), fieldDetails.getFieldType(), null, annotations);
	}

	private FieldMetadata getManyToManyInverseSideField(JavaSymbolName fieldName, JavaSymbolName mappedByFieldName, JoinTable joinTable, JavaPackage javaPackage) {
		List<JavaType> params = new ArrayList<JavaType>();
		JavaType element = dbreTypeResolutionService.findTypeForTableName(joinTable.getOwningSideTable().getName(), javaPackage);
		Assert.notNull(element, getErrorMsg(joinTable.getOwningSideTable().getName()));
		params.add(element);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(element, Path.SRC_MAIN_JAVA);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType("java.util.Set", 0, DataType.TYPE, null, params), fieldName, element, Cardinality.MANY_TO_MANY);

		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(MAPPED_BY, mappedByFieldName.getSymbolName()));
		AnnotationMetadata annotation = new DefaultAnnotationMetadata(MANY_TO_MANY, attributes);
		annotations.add(annotation);

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldDetails.getFieldName(), fieldDetails.getFieldType(), null, annotations);
	}

	private FieldMetadata getOneToOneField(JavaSymbolName fieldName, JavaType fieldType, SortedSet<Reference> references, Column column, boolean nullable) {
		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		// Add @OneToOne annotation
		AnnotationMetadata oneToOneAnnotation = new DefaultAnnotationMetadata(ONE_TO_ONE, new ArrayList<AnnotationAttributeValue<?>>());
		annotations.add(oneToOneAnnotation);

		if (references.size() == 1) {
			// Add @JoinColumn annotation
			List<AnnotationAttributeValue<?>> joinColumnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			joinColumnAttributes.add(new StringAttributeValue(NAME, column.getName()));
			addOtherJoinColumnAttributes(nullable, joinColumnAttributes);
			AnnotationMetadata joinColumnAnnotation = new DefaultAnnotationMetadata(JOIN_COLUMN, joinColumnAttributes);
			annotations.add(joinColumnAnnotation);
		} else {
			// Add @JoinColumns annotation
			annotations.add(getJoinColumnsAnnotation(references, nullable));
		}

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldName, fieldType, null, annotations);
	}

	private FieldMetadata getOneToOneMappedByField(JavaSymbolName fieldName, JavaType fieldType, JavaSymbolName mappedByFieldName) {
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(MAPPED_BY, mappedByFieldName.getSymbolName()));
		AnnotationMetadata oneToOneAnnotation = new DefaultAnnotationMetadata(ONE_TO_ONE, attributes);
		annotations.add(oneToOneAnnotation);

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldName, fieldType, null, annotations);
	}

	private FieldMetadata getOneToManyMappedByField(JavaSymbolName fieldName, JavaSymbolName mappedByFieldName, String foreignTableName, JavaPackage javaPackage) {
		List<JavaType> params = new ArrayList<JavaType>();

		JavaType element = dbreTypeResolutionService.findTypeForTableName(foreignTableName, javaPackage);
		Assert.notNull(element, getErrorMsg(foreignTableName));
		params.add(element);
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(element, Path.SRC_MAIN_JAVA);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType("java.util.Set", 0, DataType.TYPE, null, params), fieldName, element, Cardinality.ONE_TO_MANY);

		// Add @OneToMany annotation
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new StringAttributeValue(MAPPED_BY, mappedByFieldName.getSymbolName()));
		AnnotationMetadata annotation = new DefaultAnnotationMetadata(ONE_TO_MANY, attributes);
		annotations.add(annotation);

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldDetails.getFieldName(), fieldDetails.getFieldType(), null, annotations);
	}

	private FieldMetadata getManyToOneField(JavaSymbolName fieldName, JavaType fieldType, SortedSet<Reference> references, boolean nullable) {
		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		// Add @ManyToOne annotation
		AnnotationMetadata manyToOneAnnotation = new DefaultAnnotationMetadata(MANY_TO_ONE, new ArrayList<AnnotationAttributeValue<?>>());
		annotations.add(manyToOneAnnotation);

		if (references.size() == 1) {
			// Add @JoinColumn annotation
			Reference reference = references.first();
			annotations.add(getJoinColumnAnnotation(reference.getLocalColumnName(), reference.getForeignColumnName(), nullable));
		} else {
			// Add @JoinColumns annotation
			annotations.add(getJoinColumnsAnnotation(references, nullable));
		}

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldName, fieldType, null, annotations);
	}

	private AnnotationMetadata getJoinColumnAnnotation(String localColumnName, String foreignColumnName, boolean nullable) {
		List<AnnotationAttributeValue<?>> joinColumnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		joinColumnAttributes.add(new StringAttributeValue(NAME, localColumnName));
		joinColumnAttributes.add(new StringAttributeValue(REFERENCED_COLUMN, foreignColumnName));
		addOtherJoinColumnAttributes(nullable, joinColumnAttributes);
		return new DefaultAnnotationMetadata(JOIN_COLUMN, joinColumnAttributes);
	}

	private void addOtherJoinColumnAttributes(boolean nullable, List<AnnotationAttributeValue<?>> joinColumnAttributes) {
		if (nullable) {
			joinColumnAttributes.add(new BooleanAttributeValue(new JavaSymbolName("insertable"), false));
			joinColumnAttributes.add(new BooleanAttributeValue(new JavaSymbolName("updatable"), false));
		}
	}

	private AnnotationMetadata getJoinColumnsAnnotation(SortedSet<Reference> references, boolean nullable) {
		List<NestedAnnotationAttributeValue> arrayValues = new ArrayList<NestedAnnotationAttributeValue>();

		for (Reference reference : references) {
			AnnotationMetadata joinColumnAnnotation = getJoinColumnAnnotation(reference.getLocalColumnName(), reference.getForeignColumnName(), nullable);
			arrayValues.add(new NestedAnnotationAttributeValue(VALUE, joinColumnAnnotation));
		}

		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		attributes.add(new ArrayAttributeValue<NestedAnnotationAttributeValue>(VALUE, arrayValues));

		return new DefaultAnnotationMetadata(new JavaType("javax.persistence.JoinColumns"), attributes);
	}

	private boolean isOneToOne(Table table, ForeignKey foreignKey) {
		boolean equals = table.getPrimaryKeyCount() == foreignKey.getReferenceCount();
		Iterator<Column> primaryKeyIterator = table.getPrimaryKeys().iterator();
		while (equals && primaryKeyIterator.hasNext()) {
			equals &= foreignKey.hasLocalColumn(primaryKeyIterator.next());
		}
		return equals;
	}

	private void addOtherFields(JavaType javaType, Table table) {
		Map<JavaSymbolName, FieldMetadata> uniqueFields = new LinkedHashMap<JavaSymbolName, FieldMetadata>();

		for (Column column : table.getColumns()) {
			String columnName = column.getName();
			JavaSymbolName fieldName = new JavaSymbolName(dbreTypeResolutionService.suggestFieldName(column.getName()));
			boolean isCompositeKeyField = isCompositeKeyField(fieldName, javaType);
			FieldMetadata field = null;

			if ((isEmbeddedIdField(fieldName) && !isCompositeKeyField) || (isIdField(fieldName) && !column.isPrimaryKey())) {
				fieldName = getUniqueFieldName(fieldName);
				field = getField(fieldName, column);
				uniqueFields.put(fieldName, field);
			} else if ((getIdField() != null && column.isPrimaryKey()) || table.findForeignKeyByLocalColumnName(columnName) != null || (table.findExportedKeyByLocalColumnName(columnName) != null && table.findUniqueReference(columnName) != null)) {
				field = null;
			} else if (!isCompositeKeyField && !isVersionField(column.getName())) {	
				field = getField(fieldName, column);
				uniqueFields.put(fieldName, field);
			}
		}

		for (FieldMetadata field : uniqueFields.values()) {
			addToBuilder(field);
		}
	}

	private boolean isCompositeKeyField(JavaSymbolName fieldName, JavaType javaType) {
		List<FieldMetadata> identifierFields = getIdentifierFields(javaType);
		for (FieldMetadata field : identifierFields) {
			if (fieldName.equals(field.getFieldName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isCompositeKeyColumn(String columnName, JavaType javaType) {
		List<FieldMetadata> identifierFields = getIdentifierFields(javaType);
		for (FieldMetadata field : identifierFields) {
			for (AnnotationMetadata annotation : field.getAnnotations()) {
				if (annotation.getAnnotationType().equals(new JavaType("javax.persistence.Column"))) {
					AnnotationAttributeValue<?> nameAttribute = annotation.getAttribute(NAME);
					if (nameAttribute != null) {
						String name = (String) nameAttribute.getValue();
						if (columnName.equals(name)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	private List<FieldMetadata> getIdentifierFields(JavaType javaType) {
		List<FieldMetadata> identifierFields = new ArrayList<FieldMetadata>();
		// Check for identifier class and exclude fields that are part of the composite primary key
		AnnotationMetadata entityAnnotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooEntity.class.getName()));
		AnnotationAttributeValue<?> identifierTypeAttribute = entityAnnotation.getAttribute(new JavaSymbolName("identifierType"));
		if (identifierTypeAttribute != null) {
			// Attribute identifierType exists so get the value
			JavaType identifierType = (JavaType) identifierTypeAttribute.getValue();
			if (identifierType != null && !identifierType.getFullyQualifiedTypeName().startsWith("java.lang")) {
				// The identifierType is not a simple type, ie not of type 'java.lang', so find the type
				String identifierMetadataMid = IdentifierMetadata.createIdentifier(identifierType, Path.SRC_MAIN_JAVA);
				IdentifierMetadata identifierMetadata = (IdentifierMetadata) metadataService.get(identifierMetadataMid);
				if (identifierMetadata != null) {
					identifierFields.addAll(identifierMetadata.getFields());
				}
			}
		}
		return identifierFields;
	}

	private boolean isVersionField(String columnName) {
		FieldMetadata versionField = entityMetadata.getVersionField();
		return versionField != null && versionField.getFieldName().getSymbolName().equals(columnName);
	}

	private boolean isIdField(JavaSymbolName fieldName) {
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.Id"));
		if (idFields.size() > 0) {
			Assert.isTrue(idFields.size() == 1, "More than one field was annotated with @Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return idFields.get(0).getFieldName().equals(fieldName);
		}
		return false;
	}

	private JavaSymbolName getIdField() {
		List<FieldMetadata> idFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.Id"));
		if (idFields.size() > 0) {
			Assert.isTrue(idFields.size() == 1, "More than one field was annotated with @Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return idFields.get(0).getFieldName();
		}
		return null;
	}

	private boolean isEmbeddedIdField(JavaSymbolName fieldName) {
		List<FieldMetadata> embeddedIdFields = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, new JavaType("javax.persistence.EmbeddedId"));
		if (embeddedIdFields.size() > 0) {
			Assert.isTrue(embeddedIdFields.size() == 1, "More than one field was annotated with @EmbeddedId in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
			return embeddedIdFields.get(0).getFieldName().equals(fieldName);
		}
		return false;
	}

	private FieldMetadata getField(JavaSymbolName fieldName, Column column) {
		JavaType fieldType = column.getType().getJavaType();

		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		// Add @Column annotation
		List<AnnotationAttributeValue<?>> columnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		columnAttributes.add(new StringAttributeValue(NAME, column.getName()));

		// Add length attribute for Strings
		// if (fieldType.equals(JavaType.STRING_OBJECT)) {
		// columnAttributes.add(new IntegerAttributeValue(new JavaSymbolName("length"), column.getSize()));
		// }

		AnnotationMetadata columnAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.Column"), columnAttributes);
		annotations.add(columnAnnotation);

		// Add @NotNull if applicable
		if (column.isRequired()) {
			AnnotationMetadata notNullAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.NotNull"), new ArrayList<AnnotationAttributeValue<?>>());
			annotations.add(notNullAnnotation);
		}

		// Add JSR 220 @Temporal annotation to date fields
		if (fieldType.equals(new JavaType("java.util.Date"))) {
			List<AnnotationAttributeValue<?>> temporalAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			temporalAttributes.add(new EnumAttributeValue(VALUE, new EnumDetails(new JavaType("javax.persistence.TemporalType"), new JavaSymbolName(column.getType().name()))));
			annotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Temporal"), temporalAttributes));

			List<AnnotationAttributeValue<?>> dateTimeFormatAttributes = new ArrayList<AnnotationAttributeValue<?>>();
			dateTimeFormatAttributes.add(new StringAttributeValue(new JavaSymbolName("style"), "S-"));
			annotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.format.annotation.DateTimeFormat"), dateTimeFormatAttributes));
		}

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

	private boolean hasAccessor(FieldMetadata field) {
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

	private boolean hasField(FieldMetadata field) {
		// Check governor for field
		if (MemberFindingUtils.getField(governorTypeDetails, field.getFieldName()) != null) {
			return true;
		}

		// Check entity ITD for field
		List<? extends FieldMetadata> itdFields = entityMetadata.getItdTypeDetails().getDeclaredFields();
		for (FieldMetadata itdField : itdFields) {
			if (itdField.getFieldName().equals(field.getFieldName())) {
				return true;
			}
		}

		return false;
	}

	private MethodMetadata getAccessor(FieldMetadata field) {
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

	private boolean hasMutator(FieldMetadata field) {
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

	private MethodMetadata getMutator(FieldMetadata field) {
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

	private String getInflectorPlural(String term) {
		try {
			return Noun.pluralOf(term, Locale.ENGLISH);
		} catch (RuntimeException re) {
			// Inflector failed (see for example ROO-305), so don't pluralize it
			return term;
		}
	}

	private String getErrorMsg(String tableName) {
		return "Type for table '" + tableName + "' could not be found";
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
