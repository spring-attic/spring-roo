package org.springframework.roo.classpath.operations;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.DefaultFieldMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.operations.jsr303.BooleanField;
import org.springframework.roo.classpath.operations.jsr303.CollectionField;
import org.springframework.roo.classpath.operations.jsr303.DateField;
import org.springframework.roo.classpath.operations.jsr303.DateFieldPersistenceType;
import org.springframework.roo.classpath.operations.jsr303.FieldDetails;
import org.springframework.roo.classpath.operations.jsr303.NumericField;
import org.springframework.roo.classpath.operations.jsr303.ReferenceField;
import org.springframework.roo.classpath.operations.jsr303.SetField;
import org.springframework.roo.classpath.operations.jsr303.StringField;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Additional shell commands for {@link ClasspathOperations}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class FieldCommands implements CommandMarker {
	private ClasspathOperations classpathOperations;

	public FieldCommands(ClasspathOperations classpathOperations) {
		Assert.notNull(classpathOperations, "Classpath operations required");
		this.classpathOperations = classpathOperations;
	}
	
	@CliAvailabilityIndicator({"insert field", "add field number", "add field string", "add field date jdk", "add field boolean"})
	public boolean isJdkFieldManagementAvailable() {
		return classpathOperations.isProjectAvailable();
	}

	@CliAvailabilityIndicator({"add field date jpa", "add field reference jpa", "add field set jpa"})
	public boolean isJpaFieldManagementAvailable() {
		// in a separate method in case we decide to check for JPA registration in the future
		return classpathOperations.isProjectAvailable();
	}

	@CliCommand(value="insert field", help="Inserts a private field into the specified file")
	public void insertField(
			@CliOption(key="class", mandatory=true, help="The class to receive the field (class must exist)") JavaType name, 
			@CliOption(key="path", mandatory=true, help="The path where the class can be found") Path path, 
			@CliOption(key="name", mandatory=true, help="The name of the field") JavaSymbolName fieldName,
			@CliOption(key="type", mandatory=true, help="The Java type of this field") JavaType fieldType,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			// no need to check the "name" as if the class exists it is assumed it is a legal name
			ReservedWords.verifyReservedWordsNotPresent(fieldName);
		}

		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);
		FieldMetadata fieldMetadata = new DefaultFieldMetadata(declaredByMetadataId, Modifier.PRIVATE, fieldName, fieldType, null, null);
		classpathOperations.addField(fieldMetadata);
	}

	private void insertField(FieldDetails fieldDetails, boolean permitReservedWords) {
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(fieldDetails.getFieldName());
		}
		
		List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
		fieldDetails.decorateAnnotationsList(annotations);
		JavaType initializer = null;
		if (fieldDetails instanceof CollectionField) {
			CollectionField collectionField = (CollectionField) fieldDetails;
			initializer = collectionField.getInitializer();
		}
		FieldMetadata fieldMetadata = new DefaultFieldMetadata(fieldDetails.getPhysicalTypeIdentifier(), Modifier.PRIVATE, fieldDetails.getFieldName(), fieldDetails.getFieldType(), initializer, annotations);
		classpathOperations.addField(fieldMetadata);
	}
	
	@CliCommand(value="add field number", help="Adds a private numeric field to an existing Java source file")
	public void addFieldNumber(
			@CliOption(key={"","fieldName"}, mandatory=true, help="The name of the field to add") JavaSymbolName fieldName,
			@CliOption(key="type", mandatory=true, optionContext="java-number", help="The Java type of the entity") JavaType fieldType,
			@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The name of the class to receive this field") JavaType typeName,
			@CliOption(key="notNull", mandatory=false, specifiedDefaultValue="true", help="Whether this value cannot be null") Boolean notNull,
			@CliOption(key="nullRequired", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be null") Boolean nullRequired,
			@CliOption(key="decimalMin", mandatory=false, help="The BigDecimal string-based representation of the minimum value") String decimalMin,
			@CliOption(key="decimalMax", mandatory=false, help="The BigDecimal string based representation of the maximum value") String decimalMax,
			@CliOption(key="min", mandatory=false, help="The minimum value") Long min,
			@CliOption(key="max", mandatory=false, help="The maximum value") Long max,
			@CliOption(key="column", mandatory=false, help="The JPA column name") String column,
			@CliOption(key="comment", mandatory=false, help="An optional comment for JavaDocs") String comment,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		NumericField fieldDetails = new NumericField(physicalTypeIdentifier, fieldType, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (decimalMin != null) fieldDetails.setDecimalMin(decimalMin);
		if (decimalMax != null) fieldDetails.setDecimalMax(decimalMax);
		if (min != null) fieldDetails.setMin(min);
		if (max != null) fieldDetails.setMax(max);
		if (column != null) fieldDetails.setColumn(column);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords);
	}

	@CliCommand(value="add field string", help="Adds a private string field to an existing Java source file")
	public void addFieldString(
			@CliOption(key={"","fieldName"}, mandatory=true, help="The name of the field to add") JavaSymbolName fieldName,
			@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The name of the class to receive this field") JavaType typeName,
			@CliOption(key="notNull", mandatory=false, specifiedDefaultValue="true", help="Whether this value cannot be null") Boolean notNull,
			@CliOption(key="nullRequired", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be null") Boolean nullRequired,
			@CliOption(key="decimalMin", mandatory=false, help="The BigDecimal string-based representation of the minimum value") String decimalMin,
			@CliOption(key="decimalMax", mandatory=false, help="The BigDecimal string based representation of the maximum value") String decimalMax,
			@CliOption(key="sizeMin", mandatory=false, help="The minimum string length") Integer sizeMin,
			@CliOption(key="sizeMax", mandatory=false, help="The maximum string length") Integer sizeMax,
			@CliOption(key="regexp", mandatory=false, help="The required regular expression pattern") String regexp,
			@CliOption(key="column", mandatory=false, help="The JPA column name") String column,
			@CliOption(key="comment", mandatory=false, help="An optional comment for JavaDocs") String comment,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
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
		insertField(fieldDetails, permitReservedWords);
	}

	@CliCommand(value="add field date jpa", help="Adds a private JPA-specific date field to an existing Java source file")
	public void addFieldDateJpa(
			@CliOption(key={"","fieldName"}, mandatory=true, help="The name of the field to add") JavaSymbolName fieldName,
			@CliOption(key="type", mandatory=true, optionContext="java-date", help="The Java type of the entity") JavaType fieldType,
			@CliOption(key="persistenceType", mandatory=false, help="The type of persistent storage to be used") DateFieldPersistenceType persistenceType,
			@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The name of the class to receive this field") JavaType typeName,
			@CliOption(key="notNull", mandatory=false, specifiedDefaultValue="true", help="Whether this value cannot be null") Boolean notNull,
			@CliOption(key="nullRequired", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be null") Boolean nullRequired,
			@CliOption(key="future", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be in the future") Boolean future,
			@CliOption(key="past", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be in the past") Boolean past,
			@CliOption(key="column", mandatory=false, help="The JPA column name") String column,
			@CliOption(key="comment", mandatory=false, help="An optional comment for JavaDocs") String comment,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
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
		insertField(fieldDetails, permitReservedWords);
	}

	@CliCommand(value="add field date jdk", help="Adds a private date field to an existing Java source file")
	public void addFieldDateJdk(
			@CliOption(key={"","fieldName"}, mandatory=true, help="The name of the field to add") JavaSymbolName fieldName,
			@CliOption(key="type", mandatory=true, optionContext="java-date", help="The Java type of the entity") JavaType fieldType,
			@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The name of the class to receive this field") JavaType typeName,
			@CliOption(key="notNull", mandatory=false, specifiedDefaultValue="true", help="Whether this value cannot be null") Boolean notNull,
			@CliOption(key="nullRequired", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be null") Boolean nullRequired,
			@CliOption(key="future", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be in the future") Boolean future,
			@CliOption(key="past", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be in the past") Boolean past,
			@CliOption(key="column", mandatory=false, help="The JPA column name") String column,
			@CliOption(key="comment", mandatory=false, help="An optional comment for JavaDocs") String comment,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		DateField fieldDetails = new DateField(physicalTypeIdentifier, fieldType, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (future != null) fieldDetails.setFuture(future);
		if (past != null) fieldDetails.setPast(past);
		if (column != null) fieldDetails.setColumn(column);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords);
	}

	@CliCommand(value="add field boolean", help="Adds a private boolean field to an existing Java source file")
	public void addFieldBoolean(
			@CliOption(key={"","fieldName"}, mandatory=true, help="The name of the field to add") JavaSymbolName fieldName,
			@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The name of the class to receive this field") JavaType typeName,
			@CliOption(key="notNull", mandatory=false, specifiedDefaultValue="true", help="Whether this value cannot be null") Boolean notNull,
			@CliOption(key="nullRequired", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be null") Boolean nullRequired,
			@CliOption(key="assertFalse", mandatory=false, specifiedDefaultValue="true", help="Whether this value must assert false") Boolean assertFalse,
			@CliOption(key="assertTrue", mandatory=false, specifiedDefaultValue="true", help="Whether this value must assert true") Boolean assertTrue,
			@CliOption(key="column", mandatory=false, help="The JPA column name") String column,
			@CliOption(key="comment", mandatory=false, help="An optional comment for JavaDocs") String comment,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		BooleanField fieldDetails = new BooleanField(physicalTypeIdentifier, new JavaType("java.lang.Boolean"), fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (assertFalse != null) fieldDetails.setAssertFalse(assertFalse);
		if (assertTrue != null) fieldDetails.setAssertTrue(assertTrue);
		if (column != null) fieldDetails.setColumn(column);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords);
	}

	@CliCommand(value="add field reference jpa", help="Adds a private reference field to an existing Java source file (ie the 'many' side of a many-to-one)")
	public void addFieldReferenceJpa(
			@CliOption(key={"","fieldName"}, mandatory=true, help="The name of the field to add") JavaSymbolName fieldName,
			@CliOption(key="type", mandatory=true, optionContext="project", help="The Java type of the entity to reference") JavaType fieldType,
			@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The name of the class to receive this field") JavaType typeName,
			@CliOption(key="notNull", mandatory=false, specifiedDefaultValue="true", help="Whether this value cannot be null") Boolean notNull,
			@CliOption(key="nullRequired", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be null") Boolean nullRequired,
			@CliOption(key="comment", mandatory=false, help="An optional comment for JavaDocs") String comment,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		ReferenceField fieldDetails = new ReferenceField(physicalTypeIdentifier, fieldType, fieldName);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords);
	}

	@CliCommand(value="add field set jpa", help="Adds a private Set field to an existing Java source file (ie the 'one' side of a many-to-one)")
	public void addFieldSetJpa(
			@CliOption(key={"","fieldName"}, mandatory=true, help="The name of the field to add") JavaSymbolName fieldName,
			@CliOption(key="element", mandatory=true, help="The entity which will be contained within the Set") JavaType element,
			@CliOption(key="class", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project", help="The name of the class to receive this field") JavaType typeName,
			@CliOption(key="mappedBy", mandatory=false, help="The field name on the referenced type which owns the relationship") JavaSymbolName mappedBy,
			@CliOption(key="notNull", mandatory=false, specifiedDefaultValue="true", help="Whether this value cannot be null") Boolean notNull,
			@CliOption(key="nullRequired", mandatory=false, specifiedDefaultValue="true", help="Whether this value must be null") Boolean nullRequired,
			@CliOption(key="sizeMin", mandatory=false, help="The minimum string length") Integer sizeMin,
			@CliOption(key="sizeMax", mandatory=false, help="The maximum string length") Integer sizeMax,
			@CliOption(key="comment", mandatory=false, help="An optional comment for JavaDocs") String comment,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		String physicalTypeIdentifier = PhysicalTypeIdentifier.createIdentifier(typeName, Path.SRC_MAIN_JAVA);
		List<JavaType> params = new ArrayList<JavaType>();
		params.add(element);
		SetField fieldDetails = new SetField(physicalTypeIdentifier, new JavaType("java.util.Set", 0, DataType.TYPE, null, params), fieldName, element);
		if (notNull != null) fieldDetails.setNotNull(notNull);
		if (nullRequired != null) fieldDetails.setNullRequired(nullRequired);
		if (sizeMin != null) fieldDetails.setSizeMin(sizeMin);
		if (sizeMax != null) fieldDetails.setSizeMax(sizeMax);
		if (mappedBy != null) fieldDetails.setMappedBy(mappedBy);
		if (comment != null) fieldDetails.setComment(comment);
		insertField(fieldDetails, permitReservedWords);
	}
}
