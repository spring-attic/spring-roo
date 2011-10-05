package org.springframework.roo.classpath.operations;

import static org.springframework.roo.model.JdkJavaType.CALENDAR;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.SET;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;
import static org.springframework.roo.model.JpaJavaType.ENTITY;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.jsr303.BooleanField;
import org.springframework.roo.classpath.operations.jsr303.CollectionField;
import org.springframework.roo.classpath.operations.jsr303.DateField;
import org.springframework.roo.classpath.operations.jsr303.DateFieldPersistenceType;
import org.springframework.roo.classpath.operations.jsr303.EmbeddedField;
import org.springframework.roo.classpath.operations.jsr303.EnumField;
import org.springframework.roo.classpath.operations.jsr303.FieldDetails;
import org.springframework.roo.classpath.operations.jsr303.NumericField;
import org.springframework.roo.classpath.operations.jsr303.ReferenceField;
import org.springframework.roo.classpath.operations.jsr303.SetField;
import org.springframework.roo.classpath.operations.jsr303.StringField;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.util.Assert;

/**
 * Additional shell commands for the purpose of creating fields.
 *
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class FieldCommands implements CommandMarker {

	// Fields
	@Reference private MemberDetailsScanner memberDetailsScanner;
	@Reference private MetadataService metadataService;
	@Reference private ProjectOperations projectOperations;
	@Reference private StaticFieldConverter staticFieldConverter;
	@Reference private TypeManagementService typeManagementService;

	private final Set<String> legalNumericPrimitives = new HashSet<String>();

	protected void activate(final ComponentContext context) {
		legalNumericPrimitives.add(Short.class.getName());
		legalNumericPrimitives.add(Byte.class.getName());
		legalNumericPrimitives.add(Integer.class.getName());
		legalNumericPrimitives.add(Long.class.getName());
		legalNumericPrimitives.add(Float.class.getName());
		legalNumericPrimitives.add(Double.class.getName());
		staticFieldConverter.add(Cardinality.class);
		staticFieldConverter.add(Fetch.class);
		staticFieldConverter.add(EnumType.class);
		staticFieldConverter.add(DateTime.class);
	}

	protected void deactivate(final ComponentContext context) {
		staticFieldConverter.remove(Cardinality.class);
		staticFieldConverter.remove(Fetch.class);
		staticFieldConverter.remove(EnumType.class);
		staticFieldConverter.remove(DateTime.class);
	}

	@CliAvailabilityIndicator({ "field other", "field number", "field string", "field date", "field boolean", "field enum", "field embedded" })
	public boolean isJdkFieldManagementAvailable() {
		return projectOperations.isProjectAvailable();
	}

	@CliAvailabilityIndicator({"field reference", "field set"})
	public boolean isJpaFieldManagementAvailable() {
		// In a separate method in case we decide to check for JPA registration in the future
		return projectOperations.isProjectAvailable();
	}

	@CliCommand(value = "field other", help = "Inserts a private field into the specified file")
	public void insertField(
		@CliOption(key = "fieldName", mandatory = true, help = "The name of the field") final JavaSymbolName fieldName,
		@CliOption(key = "type", mandatory = true, help = "The Java type of this field") final JavaType fieldType,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") final Boolean notNull,
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") final Boolean nullRequired,
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
		@CliOption(key = "column", mandatory = false, help = "The JPA @Column name") final String column,
		@CliOption(key = "value", mandatory = false, help = "Inserts an optional Spring @Value annotation with the given content") final String value,
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		FieldDetails fieldDetails = new FieldDetails(physicalTypeIdentifier, fieldType, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (comment != null) fieldDetails.setComment(comment);
		if (column != null) fieldDetails.setColumn(column);

		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	private void insertField(final FieldDetails fieldDetails, final boolean permitReservedWords, final boolean transientModifier) {
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getFieldName());
			if (fieldDetails.getColumn() != null) {
				ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getColumn());
			}
		}

		List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
		fieldDetails.decorateAnnotationsList(annotations);
		String initializer = null;
		if (fieldDetails instanceof CollectionField) {
			CollectionField collectionField = (CollectionField) fieldDetails;
			initializer = "new " + collectionField.getInitializer() + "()";
		}
		int modifier = Modifier.PRIVATE;
		if (transientModifier) modifier += Modifier.TRANSIENT;

		FieldMetadataBuilder fieldBuilder = new FieldMetadataBuilder(fieldDetails.getPhysicalTypeIdentifier(), modifier, annotations, fieldDetails.getFieldName(), fieldDetails.getFieldType());
		fieldBuilder.setFieldInitializer(initializer);

		typeManagementService.addField(fieldBuilder.build());
	}

	@CliCommand(value = "field number", help = "Adds a private numeric field to an existing Java source file")
	public void addFieldNumber(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
		@CliOption(key = "type", mandatory = true, optionContext = "java-number", help = "The Java type of the entity") JavaType fieldType,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") final Boolean notNull,
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") final Boolean nullRequired,
		@CliOption(key = "decimalMin", mandatory = false, help = "The BigDecimal string-based representation of the minimum value") final String decimalMin,
		@CliOption(key = "decimalMax", mandatory = false, help = "The BigDecimal string based representation of the maximum value") final String decimalMax,
		@CliOption(key = "digitsInteger", mandatory = false, help = "Maximum number of integral digits accepted for this number") final Integer digitsInteger,
		@CliOption(key = "digitsFraction", mandatory = false, help = "Maximum number of fractional digits accepted for this number") final Integer digitsFraction,
		@CliOption(key = "min", mandatory = false, help = "The minimum value") final Long min,
		@CliOption(key = "max", mandatory = false, help = "The maximum value") final Long max,
		@CliOption(key = "column", mandatory = false, help = "The JPA @Column name") final String column,
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
		@CliOption(key = "value", mandatory = false, help = "Inserts an optional Spring @Value annotation with the given content") final String value,
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
		@CliOption(key = "primitive", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to use a primitive type if possible") final boolean primitive,
		@CliOption(key = "unique", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether to mark the field with a unique constraint") final boolean unique,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		if (primitive && this.legalNumericPrimitives.contains(fieldType.getFullyQualifiedTypeName())) {
			fieldType = new JavaType(fieldType.getFullyQualifiedTypeName(), 0, DataType.PRIMITIVE, null, null);
		}
		NumericField fieldDetails = new NumericField(physicalTypeIdentifier, fieldType, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (decimalMin != null) fieldDetails.setDecimalMin(decimalMin);
		if (decimalMax != null) fieldDetails.setDecimalMax(decimalMax);
		if (digitsInteger != null) fieldDetails.setDigitsInteger(digitsInteger);
		if (digitsFraction != null) fieldDetails.setDigitsFraction(digitsFraction);
		if (min != null) fieldDetails.setMin(min);
		if (max != null) fieldDetails.setMax(max);
		if (column != null) fieldDetails.setColumn(column);
		if (comment != null) fieldDetails.setComment(comment);
		if (unique) fieldDetails.setUnique(true);
		if (value != null) fieldDetails.setValue(value);

		Assert.isTrue(fieldDetails.isDigitsSetCorrectly(), "Must specify both --digitsInteger and --digitsFractional for @Digits to be added");

		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value = "field string", help = "Adds a private string field to an existing Java source file")
	public void addFieldString(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") final Boolean notNull,
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") final Boolean nullRequired,
		@CliOption(key = "decimalMin", mandatory = false, help = "The BigDecimal string-based representation of the minimum value") final String decimalMin,
		@CliOption(key = "decimalMax", mandatory = false, help = "The BigDecimal string based representation of the maximum value") final String decimalMax,
		@CliOption(key = "sizeMin", mandatory = false, help = "The minimum string length") final Integer sizeMin,
		@CliOption(key = "sizeMax", mandatory = false, help = "The maximum string length") final Integer sizeMax,
		@CliOption(key = "regexp", mandatory = false, help = "The required regular expression pattern") final String regexp,
		@CliOption(key = "column", mandatory = false, help = "The JPA @Column name") final String column,
		@CliOption(key = "value", mandatory = false, help = "Inserts an optional Spring @Value annotation with the given content") final String value,
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
		@CliOption(key = "unique", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether to mark the field with a unique constraint") final boolean unique,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		StringField fieldDetails = new StringField(physicalTypeIdentifier, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (decimalMin != null) fieldDetails.setDecimalMin(decimalMin);
		if (decimalMax != null) fieldDetails.setDecimalMax(decimalMax);
		if (sizeMin != null) fieldDetails.setSizeMin(sizeMin);
		if (sizeMax != null) fieldDetails.setSizeMax(sizeMax);
		if (regexp != null) fieldDetails.setRegexp(regexp.replace("\\", "\\\\"));
		if (column != null) fieldDetails.setColumn(column);
		if (comment != null) fieldDetails.setComment(comment);
		if (unique) fieldDetails.setUnique(true);
		if (value != null) fieldDetails.setValue(value);

		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value = "field date", help = "Adds a private date field to an existing Java source file")
	public void addFieldDateJpa(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
		@CliOption(key = "type", mandatory = true, optionContext = "java-date", help = "The Java type of the entity") final JavaType fieldType,
		@CliOption(key = "persistenceType", mandatory = false, help = "The type of persistent storage to be used") final DateFieldPersistenceType persistenceType,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") final Boolean notNull,
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") final Boolean nullRequired,
		@CliOption(key = "future", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be in the future") final Boolean future,
		@CliOption(key = "past", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be in the past") final Boolean past,
		@CliOption(key = "column", mandatory = false, help = "The JPA @Column name") final String column,
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
		@CliOption(key = "value", mandatory = false, help = "Inserts an optional Spring @Value annotation with the given content") final String value,
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords,
		@CliOption(key = "dateFormat", mandatory = false, unspecifiedDefaultValue = "MEDIUM", specifiedDefaultValue = "MEDIUM", help = "Indicates the style of the date format (ignored if dateTimeFormatPattern is specified)") final DateTime dateFormat,
		@CliOption(key = "timeFormat", mandatory = false, unspecifiedDefaultValue = "NONE", specifiedDefaultValue = "NONE", help = "Indicates the style of the time format (ignored if dateTimeFormatPattern is specified)") final DateTime timeFormat,
		@CliOption(key = "dateTimeFormatPattern", mandatory = false, help = "Indicates a DateTime format pattern such as yyyy-MM-dd hh:mm:ss a") final String pattern) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		DateField fieldDetails = new DateField(physicalTypeIdentifier, fieldType, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (future != null) fieldDetails.setFuture(future);
		if (past != null) fieldDetails.setPast(past);
		if (isDateField(fieldType)) fieldDetails.setPersistenceType(persistenceType != null ? persistenceType : DateFieldPersistenceType.JPA_TIMESTAMP);
		if (column != null) fieldDetails.setColumn(column);
		if (comment != null) fieldDetails.setComment(comment);
		if (dateFormat != null) fieldDetails.setDateFormat(dateFormat);
		if (timeFormat != null) fieldDetails.setTimeFormat(timeFormat);
		if (pattern != null) fieldDetails.setPattern(pattern);
		if (value != null) fieldDetails.setValue(value);

		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value = "field boolean", help = "Adds a private boolean field to an existing Java source file")
	public void addFieldBoolean(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") final Boolean notNull,
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") final Boolean nullRequired,
		@CliOption(key = "assertFalse", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must assert false") final Boolean assertFalse,
		@CliOption(key = "assertTrue", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must assert true") final Boolean assertTrue,
		@CliOption(key = "column", mandatory = false, help = "The JPA @Column name") final String column,
		@CliOption(key = "value", mandatory = false, help = "Inserts an optional Spring @Value annotation with the given content") final String value,
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
		@CliOption(key = "primitive", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to use a primitive type") final boolean primitive,
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		BooleanField fieldDetails = new BooleanField(physicalTypeIdentifier, primitive ? JavaType.BOOLEAN_PRIMITIVE : JavaType.BOOLEAN_OBJECT, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (assertFalse != null) fieldDetails.setAssertFalse(assertFalse);
		if (assertTrue != null) fieldDetails.setAssertTrue(assertTrue);
		if (column != null) fieldDetails.setColumn(column);
		if (comment != null) fieldDetails.setComment(comment);
		if (value != null) fieldDetails.setValue(value);

		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value = "field reference", help = "Adds a private reference field to an existing Java source file (eg the 'many' side of a many-to-one)")
	public void addFieldReferenceJpa(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
		@CliOption(key = "type", mandatory = true, optionContext = "project", help = "The Java type of the entity to reference") final JavaType fieldType,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") final Boolean notNull,
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") final Boolean nullRequired,
		@CliOption(key = "joinColumnName", mandatory = false, help = "The JPA @JoinColumn name") final String joinColumnName,
		@CliOption(key = "referencedColumnName", mandatory = false, help = "The JPA @JoinColumn referencedColumnName") final String referencedColumnName,
		@CliOption(key = "cardinality", mandatory = false, unspecifiedDefaultValue = "MANY_TO_ONE", specifiedDefaultValue = "MANY_TO_ONE", help = "The relationship cardinality at a JPA level") final Cardinality cardinality,
		@CliOption(key = "fetch", mandatory = false, help = "The fetch semantics at a JPA level") final Fetch fetch,
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(fieldType, Path.SRC_MAIN_JAVA));
		Assert.notNull(physicalTypeMetadata, "The specified target '--type' does not exist or can not be found. Please create this type first.");
		PhysicalTypeDetails ptd = physicalTypeMetadata.getMemberHoldingTypeDetails();
		Assert.isInstanceOf(MemberHoldingTypeDetails.class, ptd);
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) ptd;

		// Check if the requested entity is a JPA @Entity
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(this.getClass().getName(), classOrInterfaceTypeDetails);
		AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
		Assert.notNull(entityAnnotation, "The field reference command is only applicable to JPA @Entity target types.");

		Assert.isTrue(cardinality == Cardinality.MANY_TO_ONE || cardinality == Cardinality.ONE_TO_ONE, "Cardinality must be MANY_TO_ONE or ONE_TO_ONE for the field reference command");

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		ReferenceField fieldDetails = new ReferenceField(physicalTypeIdentifier, fieldType, fieldName, cardinality);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (joinColumnName != null) fieldDetails.setJoinColumnName(joinColumnName);
		if (referencedColumnName != null) {
			Assert.notNull(joinColumnName, "@JoinColumn name is required if specifying a referencedColumnName");
			fieldDetails.setReferencedColumnName(referencedColumnName);
		}
		if (fetch != null) fieldDetails.setFetch(fetch);
		if (comment != null) fieldDetails.setComment(comment);

		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value = "field set", help = "Adds a private Set field to an existing Java source file (eg the 'one' side of a many-to-one)")
	public void addFieldSetJpa(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
		@CliOption(key = "type", mandatory = true, help = "The entity which will be contained within the Set") final JavaType fieldType,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
		@CliOption(key = "mappedBy", mandatory = false, help = "The field name on the referenced type which owns the relationship") final JavaSymbolName mappedBy,
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") final Boolean notNull,
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") final Boolean nullRequired,
		@CliOption(key = "sizeMin", mandatory = false, help = "The minimum number of elements in the collection") final Integer sizeMin,
		@CliOption(key = "sizeMax", mandatory = false, help = "The maximum number of elements in the collection") final Integer sizeMax,
		@CliOption(key = "cardinality", mandatory = false, unspecifiedDefaultValue = "MANY_TO_MANY", specifiedDefaultValue = "MANY_TO_MANY", help = "The relationship cardinality at a JPA level") Cardinality cardinality,
		@CliOption(key = "fetch", mandatory = false, help = "The fetch semantics at a JPA level") final Fetch fetch,
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(fieldType, Path.SRC_MAIN_JAVA));
		Assert.notNull(physicalTypeMetadata, "The specified target '--type' does not exist or can not be found. Please create this type first.");
		PhysicalTypeDetails ptd = physicalTypeMetadata.getMemberHoldingTypeDetails();
		Assert.isInstanceOf(MemberHoldingTypeDetails.class, ptd);
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) ptd;

		// Check if the requested entity is a JPA @Entity
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(this.getClass().getName(), classOrInterfaceTypeDetails);
		AnnotationMetadata entityAnnotation = memberDetails.getAnnotation(ENTITY);
		if (entityAnnotation != null) {
			Assert.isTrue(cardinality == Cardinality.ONE_TO_MANY || cardinality == Cardinality.MANY_TO_MANY, "Cardinality must be ONE_TO_MANY or MANY_TO_MANY for the field set command");
		} else if (ptd.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION) {
			cardinality = null;
		} else {
			throw new IllegalStateException("The field set command is only applicable to enum and JPA @Entity elements");
		}

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		List<JavaType> params = new ArrayList<JavaType>();
		params.add(fieldType);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType(SET.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, params), fieldName, fieldType, cardinality);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (sizeMin != null) fieldDetails.setSizeMin(sizeMin);
		if (sizeMax != null) fieldDetails.setSizeMax(sizeMax);
		if (mappedBy != null) fieldDetails.setMappedBy(mappedBy);
		if (fetch != null) fieldDetails.setFetch(fetch);
		if (comment != null) fieldDetails.setComment(comment);

		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value = "field enum", help = "Adds a private enum field to an existing Java source file")
	public void addFieldEnum(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
		@CliOption(key = "type", mandatory = true, help = "The enum type of this field") final JavaType fieldType,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") final JavaType typeName,
		@CliOption(key = "column", mandatory = false, help = "The JPA @Column name") final String column,
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") final Boolean notNull,
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") final Boolean nullRequired,
		@CliOption(key = "enumType", mandatory = false, help = "The fetch semantics at a JPA level") final EnumType enumType,
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") final String comment,
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") final boolean transientModifier,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		EnumField fieldDetails = new EnumField(physicalTypeIdentifier, fieldType, fieldName);
		if (column != null) fieldDetails.setColumn(column);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (enumType != null) fieldDetails.setEnumType(enumType);
		if (comment != null) fieldDetails.setComment(comment);

		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value = "field embedded", help = "Adds a private @Embedded field to an existing Java source file ")
	public void addFieldEmbeddedJpa(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") final JavaSymbolName fieldName,
		@CliOption(key = "type", mandatory = true, optionContext = "project", help = "The Java type of the @Embeddable class") final JavaType fieldType,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the @Entity class to receive this field") final JavaType typeName,
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") final boolean permitReservedWords) {

		// Check if the field type is a JPA @Embeddable class
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(fieldType, Path.SRC_MAIN_JAVA));
		Assert.notNull(physicalTypeMetadata, "The specified target '--type' does not exist or can not be found. Please create this type first.");
		PhysicalTypeDetails ptd = physicalTypeMetadata.getMemberHoldingTypeDetails();
		Assert.isInstanceOf(MemberHoldingTypeDetails.class, ptd);
		Assert.notNull(((IdentifiableAnnotatedJavaStructure) ptd).getAnnotation(EMBEDDABLE), "The field embedded command is only applicable to JPA @Embeddable field types.");

		// Check if the requested entity is a JPA @Entity
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		PhysicalTypeMetadata targetTypeMetadata = (PhysicalTypeMetadata) metadataService.get(physicalTypeIdentifier);
		Assert.notNull(targetTypeMetadata, "The specified target '--class' does not exist or can not be found. Please create this type first.");
		PhysicalTypeDetails targetPtd = targetTypeMetadata.getMemberHoldingTypeDetails();
		Assert.isInstanceOf(MemberHoldingTypeDetails.class, targetPtd);
		ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails = (ClassOrInterfaceTypeDetails) targetPtd;
		MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(this.getClass().getName(), classOrInterfaceTypeDetails);
		Assert.notNull(memberDetails.getAnnotation(ENTITY), "The field embedded command is only applicable to JPA @Entity target types.");

		EmbeddedField fieldDetails = new EmbeddedField(physicalTypeIdentifier, fieldType, fieldName);

		insertField(fieldDetails, permitReservedWords, false);
	}

	private boolean isDateField(final JavaType fieldType) {
		return fieldType.equals(DATE) || fieldType.equals(CALENDAR);
	}
}