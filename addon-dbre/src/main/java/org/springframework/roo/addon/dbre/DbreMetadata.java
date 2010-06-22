package org.springframework.roo.addon.dbre;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.dbre.db.Column;
import org.springframework.roo.addon.dbre.db.DbModel;
import org.springframework.roo.addon.dbre.db.IdentifiableTable;
import org.springframework.roo.addon.dbre.db.Table;
import org.springframework.roo.addon.entity.EntityMetadata;
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
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.details.annotations.populator.AutoPopulationUtils;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
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
	private TableModelService tableModelService;

	public DbreMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, EntityMetadata entityMetadata, TableModelService tableModelService, DbModel dbModel) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");

		this.entityMetadata = entityMetadata;
		this.tableModelService = tableModelService;

		// Process values from the annotation, if present
		AnnotationMetadata annotation = MemberFindingUtils.getDeclaredTypeAnnotation(governorTypeDetails, new JavaType(RooDbManaged.class.getName()));
		if (annotation != null) {
			AutoPopulationUtils.populate(this, annotation);
		}

		JavaType javaType = governorPhysicalTypeMetadata.getPhysicalTypeDetails().getName();
		IdentifiableTable identifiableTable = tableModelService.findTableIdentity(javaType);
//		if (identifiableTable == null) {
//			identifiableTable = tableModelService.suggestTableNameForNewType(javaType);
//		}
		Table table = dbModel.getTable(identifiableTable);
		// System.out.println("table is null " + (table == null));
		if (table == null) {
			return;
		}
		// Add fields with their respective accessors and mutators
		for (Column column : table.getColumns()) {
			// Check for an existing declared field in the governor or in the entity metadata
			FieldMetadata field = null;
			if (!hasField(column, javaType)) {
				field = getField(column, javaType);
				builder.addField(field);
			}

			// Check for an existing accessor in the governor or in the entity metadata
			if (field != null && !hasAccessor(field)) {
				builder.addMethod(getAccessor(field));
			}

			// Check for an existing mutator in the governor or in the entity metadata
			if (field != null && !hasMutator(field)) {
				builder.addMethod(getMutator(field));
			}
		}

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	public boolean hasField(Column column, JavaType javaType) {
		JavaSymbolName fieldName = new JavaSymbolName(tableModelService.suggestFieldNameForColumn(column.getName()));
		// System.out.println("column name = " + columnElement.getAttribute("name") + ", field name = " + fieldName.getSymbolName());
		// Check governor for field
		if (MemberFindingUtils.getField(governorTypeDetails, fieldName) != null) {
			// System.out.println("found on governor " + fieldName + " - not adding to ITD");
			return true;
		}

		// Check entity ITD for field
		List<? extends FieldMetadata> itdFields = entityMetadata.getItdTypeDetails().getDeclaredFields();
		for (FieldMetadata field : itdFields) {
			if (field.getFieldName().equals(fieldName)) {
				// System.out.println("found on entity " + fieldName + " - not adding to ITD");
				return true;
			}
		}

		// Try to locate an existing field with @javax.persistence.Id
		// List<FieldMetadata> foundId = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, ID);
		// if (foundId.size() > 0) {
		// Assert.isTrue(foundId.size() == 1, "More than one field was annotated with @javax.persistence.Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
		// return true;
		// }
		// foundId = MemberFindingUtils.getFieldsWithAnnotation(entityMetadata., ID);
		// if (foundId.size() > 0) {
		// Assert.isTrue(foundId.size() == 1, "More than one field was annotated with @javax.persistence.Id in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
		// return true;
		// }

		// Try to locate an existing field with @javax.persistence.EmbeddedId
		// List<FieldMetadata> foundEmbeddedId = MemberFindingUtils.getFieldsWithAnnotation(governorTypeDetails, EMBEDDED_ID);
		// if (foundEmbeddedId.size() > 0) {
		// Assert.isTrue(foundEmbeddedId.size() == 1, "More than one field was annotated with @javax.persistence.EmbeddedId in '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "'");
		// return true;
		// }
		return false;
	}

	public FieldMetadata getField(Column column, JavaType javaType) {
		JavaSymbolName fieldName = new JavaSymbolName(tableModelService.suggestFieldNameForColumn(column.getName()));
		JavaType fieldType = column.getType();

		// Add annotations to field
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();

		// Add @Column annotation
		List<AnnotationAttributeValue<?>> columnAttributes = new ArrayList<AnnotationAttributeValue<?>>();
		columnAttributes.add(new StringAttributeValue(new JavaSymbolName("name"), column.getName()));

		// Add length attribute for Strings
		if (fieldType.equals(JavaType.STRING_OBJECT)) {
			columnAttributes.add(new IntegerAttributeValue(new JavaSymbolName("length"), column.getColumnSize()));
		}
		AnnotationMetadata columnAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.persistence.Column"), columnAttributes);
		annotations.add(columnAnnotation);

		// Add @NotNull if applicable
		if (!column.isNullable()) {
			AnnotationMetadata notNullAnnotation = new DefaultAnnotationMetadata(new JavaType("javax.validation.constraints.NotNull"), new ArrayList<AnnotationAttributeValue<?>>());
			annotations.add(notNullAnnotation);
		}

		return new DefaultFieldMetadata(getId(), Modifier.PRIVATE, fieldName, fieldType, null, annotations);
	}

	public boolean hasAccessor(FieldMetadata field) {
		String requiredAccessorName = getRequiredAccessorName(field);

		// Check governor for accessor method
		if (MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredAccessorName), new ArrayList<JavaType>()) != null) {
			// System.out.println("found on governor " + fieldName + " - not adding to ITD");
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
		String requiredAccessorName = getRequiredAccessorName(field);
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return this." + field.getFieldName().getSymbolName() + ";");
		return new DefaultMethodMetadata(getId(), Modifier.PUBLIC, new JavaSymbolName(requiredAccessorName), field.getFieldType(), new ArrayList<AnnotatedJavaType>(), new ArrayList<JavaSymbolName>(), new ArrayList<AnnotationMetadata>(), new ArrayList<JavaType>(), bodyBuilder.getOutput());
	}

	private String getRequiredAccessorName(FieldMetadata field) {
		return "get" + StringUtils.capitalize(field.getFieldName().getSymbolName());
	}

	public boolean hasMutator(FieldMetadata field) {
		String requiredMutatorName = getRequiredMutatorName(field);

		// Check governor for mutator method
		if (MemberFindingUtils.getMethod(governorTypeDetails, new JavaSymbolName(requiredMutatorName), new ArrayList<JavaType>()) != null) {
			// System.out.println("found on governor " + fieldName + " - not adding to ITD");
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
