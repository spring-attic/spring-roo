package org.springframework.roo.classpath.operations;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.operations.jsr303.BooleanField;
import org.springframework.roo.classpath.operations.jsr303.CollectionField;
import org.springframework.roo.classpath.operations.jsr303.DateField;
import org.springframework.roo.classpath.operations.jsr303.DateFieldPersistenceType;
import org.springframework.roo.classpath.operations.jsr303.EnumField;
import org.springframework.roo.classpath.operations.jsr303.FieldDetails;
import org.springframework.roo.classpath.operations.jsr303.NumericField;
import org.springframework.roo.classpath.operations.jsr303.ReferenceField;
import org.springframework.roo.classpath.operations.jsr303.SetField;
import org.springframework.roo.classpath.operations.jsr303.StringField;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.util.Assert;

/**
 * Additional shell commands for {@link ClasspathOperationsImpl}.
 * 
 * @author Ben Alex
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class FieldCommands implements CommandMarker {
	@Reference private ClasspathOperations classpathOperations;
	@Reference private MetadataService metadataService;
	@Reference private StaticFieldConverter staticFieldConverter;
	private final Set<String> legalNumericPrimitives = new HashSet<String>();

	protected void activate(ComponentContext context) {
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
	
	protected void deactivate(ComponentContext context) {
		staticFieldConverter.remove(Cardinality.class);
		staticFieldConverter.remove(Fetch.class);
		staticFieldConverter.remove(EnumType.class);
		staticFieldConverter.remove(DateTime.class);
	}

	@CliAvailabilityIndicator({"field other", "field number", "field string", "field date", "field boolean", "field enum"})
	public boolean isJdkFieldManagementAvailable() {
		return classpathOperations.isProjectAvailable();
	}

	@CliAvailabilityIndicator({"field reference", "field set"})
	public boolean isJpaFieldManagementAvailable() {
		// in a separate method in case we decide to check for JPA registration in the future
		return classpathOperations.isProjectAvailable();
	}

	@CliCommand(value="field other", help="Inserts a private field into the specified file")
	public void insertField(
		@CliOption(key = "fieldName", mandatory = true, help = "The name of the field") JavaSymbolName fieldName, 
		@CliOption(key = "type", mandatory = true, help = "The Java type of this field") JavaType fieldType, 
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") JavaType typeName, 
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") Boolean notNull, 
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") Boolean nullRequired, 
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") String comment, 
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") boolean transientModifier, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		JavaType useType = fieldType;
		FieldDetails fieldDetails = new FieldDetails(physicalTypeIdentifier, useType, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	private void insertField(FieldDetails fieldDetails, boolean permitReservedWords, boolean transientModifier) {
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getFieldName());
			if (fieldDetails.getColumn() != null) {
				ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getColumn());
			}
		}
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		fieldDetails.decorateAnnotationsList(annotations);
		String initializer = null;
		if (fieldDetails instanceof CollectionField) {
			CollectionField collectionField = (CollectionField) fieldDetails;
			initializer = "new " + collectionField.getInitializer() + "()";
		}
		int mod = Modifier.PRIVATE;
		if (transientModifier) mod += Modifier.TRANSIENT;
		FieldMetadata fieldMetadata = new DefaultFieldMetadata(fieldDetails.getPhysicalTypeIdentifier(), mod, fieldDetails.getFieldName(), fieldDetails.getFieldType(), initializer, annotations);
		classpathOperations.addField(fieldMetadata);
	}
	
	@CliCommand(value="field number", help="Adds a private numeric field to an existing Java source file")
	public void addFieldNumber(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") JavaSymbolName fieldName, 
		@CliOption(key = "type", mandatory = true, optionContext = "java-number", help = "The Java type of the entity") JavaType fieldType, 
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") JavaType typeName, 
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") Boolean notNull, 
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") Boolean nullRequired, 
		@CliOption(key = "decimalMin", mandatory = false, help = "The BigDecimal string-based representation of the minimum value") String decimalMin, 
		@CliOption(key = "decimalMax", mandatory = false, help = "The BigDecimal string based representation of the maximum value") String decimalMax, 
		@CliOption(key = "digitsInteger", mandatory = false, help = "Maximum number of integral digits accepted for this number") Integer digitsInteger, 
		@CliOption(key = "digitsFraction", mandatory = false, help = "Maximum number of fractional digits accepted for this number") Integer digitsFraction, 
		@CliOption(key = "min", mandatory = false, help = "The minimum value") Long min, 
		@CliOption(key = "max", mandatory = false, help = "The maximum value") Long max, 
		@CliOption(key = "column", mandatory = false, help = "The JPA column name") String column, 
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") String comment, 
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") boolean transientModifier, 
		@CliOption(key = "primitive", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to use a primitive type if possible") boolean primitive, 
			@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		JavaType useType = fieldType;
		if (primitive && this.legalNumericPrimitives.contains(fieldType.getFullyQualifiedTypeName())) {
			useType = new JavaType(fieldType.getFullyQualifiedTypeName(), 0, DataType.PRIMITIVE, null, null);
		}
		NumericField fieldDetails = new NumericField(physicalTypeIdentifier, useType, fieldName);
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
		Assert.isTrue(fieldDetails.isDigitsSetCorrectly(), "Must specify both --digitsInteger and --digitsFractional for @Digits to be added");
		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value="field string", help="Adds a private string field to an existing Java source file")
	public void addFieldString(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") JavaSymbolName fieldName, 
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") JavaType typeName, 
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") Boolean notNull, 
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") Boolean nullRequired, 
		@CliOption(key = "decimalMin", mandatory = false, help = "The BigDecimal string-based representation of the minimum value") String decimalMin, 
		@CliOption(key = "decimalMax", mandatory = false, help = "The BigDecimal string based representation of the maximum value") String decimalMax, 
		@CliOption(key = "sizeMin", mandatory = false, help = "The minimum string length") Integer sizeMin, 
		@CliOption(key = "sizeMax", mandatory = false, help = "The maximum string length") Integer sizeMax, 
		@CliOption(key = "regexp", mandatory = false, help = "The required regular expression pattern") String regexp, 
		@CliOption(key = "column", mandatory = false, help = "The JPA column name") String column, 
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") String comment, 
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") boolean transientModifier, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		StringField fieldDetails = new StringField(physicalTypeIdentifier, new JavaType("java.lang.String"), fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (decimalMin != null) fieldDetails.setDecimalMin(decimalMin);
		if (decimalMax != null) fieldDetails.setDecimalMax(decimalMax);
		if (sizeMin != null) fieldDetails.setSizeMin(sizeMin);
		if (sizeMax != null) fieldDetails.setSizeMax(sizeMax);
		if (regexp != null) fieldDetails.setRegexp(regexp.replace("\\", "\\\\"));
		if (column != null) fieldDetails.setColumn(column);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value="field date", help="Adds a private date field to an existing Java source file")
	public void addFieldDateJpa(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") JavaSymbolName fieldName, 
		@CliOption(key = "type", mandatory = true, optionContext = "java-date", help = "The Java type of the entity") JavaType fieldType, 
		@CliOption(key = "persistenceType", mandatory = false, help = "The type of persistent storage to be used") DateFieldPersistenceType persistenceType,
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") JavaType typeName, 
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") Boolean notNull, 
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") Boolean nullRequired, 
		@CliOption(key = "future", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be in the future") Boolean future, 
		@CliOption(key = "past", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be in the past") Boolean past, 
		@CliOption(key = "column", mandatory = false, help = "The JPA column name") String column, 
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") String comment, 
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") boolean transientModifier, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords, 
		@CliOption(key = "dateFormat", mandatory = false, unspecifiedDefaultValue = "SHORT", specifiedDefaultValue = "SHORT", help = "Indicates the style of the date format") DateTime dateFormat, 
		@CliOption(key = "timeFormat", mandatory = false, unspecifiedDefaultValue = "NONE", specifiedDefaultValue = "NONE", help = "Indicates the style of the time format") DateTime timeFormat) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		DateField fieldDetails = new DateField(physicalTypeIdentifier, fieldType, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (future != null) fieldDetails.setFuture(future);
		if (past != null) fieldDetails.setPast(past);
		if (persistenceType != null) fieldDetails.setPersistenceType(persistenceType);
		if (persistenceType == null) fieldDetails.setPersistenceType(DateFieldPersistenceType.JPA_TIMESTAMP);
		if (column != null) fieldDetails.setColumn(column);
		if (comment != null) fieldDetails.setComment(comment);
		if (dateFormat != null) fieldDetails.setDateFormat(dateFormat);
		if (timeFormat != null) fieldDetails.setTimeFormat(timeFormat);
		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value="field boolean", help="Adds a private boolean field to an existing Java source file")
	public void addFieldBoolean(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") JavaSymbolName fieldName, 
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") JavaType typeName, 
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") Boolean notNull, 
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") Boolean nullRequired, 
		@CliOption(key = "assertFalse", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must assert false") Boolean assertFalse, 
		@CliOption(key = "assertTrue", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must assert true") Boolean assertTrue, 
		@CliOption(key = "column", mandatory = false, help = "The JPA column name") String column, 
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") String comment, 
		@CliOption(key = "primitive", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to use a primitive type") boolean primitive, 
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") boolean transientModifier, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		BooleanField fieldDetails = new BooleanField(physicalTypeIdentifier, primitive ? JavaType.BOOLEAN_PRIMITIVE : JavaType.BOOLEAN_OBJECT, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (assertFalse != null) fieldDetails.setAssertFalse(assertFalse);
		if (assertTrue != null) fieldDetails.setAssertTrue(assertTrue);
		if (column != null) fieldDetails.setColumn(column);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value="field reference", help="Adds a private reference field to an existing Java source file (eg the 'many' side of a many-to-one)")
	public void addFieldReferenceJpa(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") JavaSymbolName fieldName, 
		@CliOption(key = "type", mandatory = true, optionContext = "project", help = "The Java type of the entity to reference") JavaType fieldType, 
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") JavaType typeName, 
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") Boolean notNull, 
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") Boolean nullRequired, 
		@CliOption(key = "joinColumnName", mandatory = false, help = "The JPA Join Column name") String joinColumnName, 
		@CliOption(key = "cardinality", mandatory = false, unspecifiedDefaultValue = "MANY_TO_ONE", specifiedDefaultValue = "MANY_TO_ONE", help = "The relationship cardinarily at a JPA level") Cardinality cardinality, 
		@CliOption(key = "fetch", mandatory = false, help = "The fetch semantics at a JPA level") Fetch fetch, 
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") String comment, 
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") boolean transientModifier, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		
		// Check if the target type is a JPA @Entity
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(fieldType, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata == null) {
			throw new IllegalStateException("The specified target '--type' does not exist or can not be found. Please create this type first.");
		}
		PhysicalTypeDetails ptd = physicalTypeMetadata.getPhysicalTypeDetails();
		Assert.isInstanceOf(MemberHoldingTypeDetails.class, ptd);
		if (null == MemberFindingUtils.getDeclaredTypeAnnotation((MemberHoldingTypeDetails)ptd, new JavaType("javax.persistence.Entity"))) {
			throw new IllegalStateException("The field reference command is only applicable to JPA @Entity target types.");
		}

		if (!(cardinality == Cardinality.MANY_TO_ONE || cardinality == Cardinality.ONE_TO_ONE)) {
			throw new IllegalStateException("Cardinality must be MANY_TO_ONE or ONE_TO_ONE for the field reference command");
		}

		ReferenceField fieldDetails = new ReferenceField(physicalTypeIdentifier, fieldType, fieldName, cardinality);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (joinColumnName != null) fieldDetails.setJoinColumnName(joinColumnName);
		if (fetch != null) fieldDetails.setFetch(fetch);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value="field set", help="Adds a private Set field to an existing Java source file (eg the 'one' side of a many-to-one)")
	public void addFieldSetJpa(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") JavaSymbolName fieldName, 
		@CliOption(key = "element", mandatory = true, help = "The entity which will be contained within the Set") JavaType element, 
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") JavaType typeName, 
		@CliOption(key = "mappedBy", mandatory = false, help = "The field name on the referenced type which owns the relationship") JavaSymbolName mappedBy, 
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") Boolean notNull, 
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") Boolean nullRequired, 
		@CliOption(key = "sizeMin", mandatory = false, help = "The minimum number of elements in the collection") Integer sizeMin, 
		@CliOption(key = "sizeMax", mandatory = false, help = "The maximum number of elements in the collection") Integer sizeMax, 
		@CliOption(key = "cardinality", mandatory = false, unspecifiedDefaultValue = "MANY_TO_MANY", specifiedDefaultValue = "MANY_TO_MANY", help = "The relationship cardinarily at a JPA level") Cardinality cardinality, 
		@CliOption(key = "fetch", mandatory = false, help = "The fetch semantics at a JPA level") Fetch fetch, 
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") String comment, 
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") boolean transientModifier, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		// Check if the target type is a JPA @Entity
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(element, Path.SRC_MAIN_JAVA));
		if (physicalTypeMetadata == null) {
			throw new IllegalStateException("The specified target '--element' does not exist or can not be found. Please create this type first.");
		}
		PhysicalTypeDetails ptd = physicalTypeMetadata.getPhysicalTypeDetails();
		Assert.isInstanceOf(MemberHoldingTypeDetails.class, ptd);
		if (null == MemberFindingUtils.getDeclaredTypeAnnotation((MemberHoldingTypeDetails)ptd, new JavaType("javax.persistence.Entity"))) {
			throw new IllegalStateException("The field set command is only applicable to JPA @Entity elements.");
		}	
		
		if (!(cardinality == Cardinality.ONE_TO_MANY || cardinality == Cardinality.MANY_TO_MANY)) {
			throw new IllegalStateException("Cardinality must be ONE_TO_MANY or MANY_TO_MANY for the field set command");
		}
		
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		List<JavaType> params = new ArrayList<JavaType>();
		params.add(element);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType("java.util.Set", 0, DataType.TYPE, null, params), fieldName, element, cardinality);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (sizeMin != null) fieldDetails.setSizeMin(sizeMin);
		if (sizeMax != null) fieldDetails.setSizeMax(sizeMax);
		if (mappedBy != null) fieldDetails.setMappedBy(mappedBy);
		if (fetch != null) fieldDetails.setFetch(fetch);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords, transientModifier);
	}

	@CliCommand(value="field enum", help="Adds a private enum field to an existing Java source file")
	public void addFieldEnum(
		@CliOption(key = { "", "fieldName" }, mandatory = true, help = "The name of the field to add") JavaSymbolName fieldName, 
		@CliOption(key = "type", mandatory = true, help = "The enum type of this field") JavaType fieldType, 
		@CliOption(key = "class", mandatory = false, unspecifiedDefaultValue = "*", optionContext = "update,project", help = "The name of the class to receive this field") JavaType typeName, 
		@CliOption(key = "notNull", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value cannot be null") Boolean notNull, 
		@CliOption(key = "nullRequired", mandatory = false, specifiedDefaultValue = "true", help = "Whether this value must be null") Boolean nullRequired, 
		@CliOption(key = "enumType", mandatory = false, help = "The fetch semantics at a JPA level") EnumType enumType, 
		@CliOption(key = "comment", mandatory = false, help = "An optional comment for JavaDocs") String comment, 
		@CliOption(key = "transient", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates to mark the field as transient") boolean transientModifier, 
		@CliOption(key = "permitReservedWords", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true", help = "Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {

		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		EnumField fieldDetails = new EnumField(physicalTypeIdentifier, fieldType, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (enumType != null) fieldDetails.setEnumType(enumType);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords, transientModifier);
	}
}