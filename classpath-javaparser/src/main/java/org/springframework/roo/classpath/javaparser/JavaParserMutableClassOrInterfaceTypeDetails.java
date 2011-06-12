package org.springframework.roo.classpath.javaparser;

import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.expr.AnnotationExpr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.javaparser.details.JavaParserAnnotationMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserFieldMetadata;
import org.springframework.roo.classpath.javaparser.details.JavaParserMethodMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;

/**
 * Java Parser implementation of {@link MutableClassOrInterfaceTypeDetails}.
 * 
 * <p>
 * This class is immutable once constructed.
 * 
 * @author Ben Alex
 * @author James Tyrrell
 * @since 1.0
 */
public class JavaParserMutableClassOrInterfaceTypeDetails extends JavaParserClassOrInterfaceTypeDetails implements MutableClassOrInterfaceTypeDetails {
	private FileManager fileManager;
	private String fileIdentifier;
	
	public JavaParserMutableClassOrInterfaceTypeDetails(CompilationUnit compilationUnit, TypeDeclaration typeDeclaration, String declaredByMetadataId, JavaType typeName, MetadataService metadataService, PhysicalTypeMetadataProvider physicalTypeMetadataProvider, FileManager fileManager, String fileIdentifier) throws ParseException, CloneNotSupportedException, IOException {
		super(compilationUnit, typeDeclaration, declaredByMetadataId, typeName, metadataService, physicalTypeMetadataProvider);
		Assert.notNull(fileManager, "File manager required");
		Assert.hasText(fileIdentifier, "File identifier required");
		this.fileManager = fileManager;
		this.fileIdentifier = fileIdentifier;
	}
	
	public void addTypeAnnotation(AnnotationMetadata annotation) {
		doAddTypeAnnotation(annotation);
		flush();
	}
	
	private void doAddTypeAnnotation(AnnotationMetadata annotation) {
		List<AnnotationExpr> annotations = clazz == null ? enumClazz.getAnnotations() : clazz.getAnnotations();
		if (annotations == null) {
			annotations = new ArrayList<AnnotationExpr>();
			if (clazz == null) {
				enumClazz.setAnnotations(annotations);
			} else {
				clazz.setAnnotations(annotations);
			}
		}
		JavaParserAnnotationMetadata.addAnnotationToList(this, annotations, annotation);
	}

	public boolean updateTypeAnnotation(AnnotationMetadata annotation, Set<JavaSymbolName> attributesToDeleteIfPresent) {
		boolean writeChangesToDisk = false;

		// We are going to build a replacement AnnotationMetadata.
		// This variable tracks the new attribute values the replacement will hold.
		Map<JavaSymbolName, AnnotationAttributeValue<?>> replacementAttributeValues = new LinkedHashMap<JavaSymbolName, AnnotationAttributeValue<?>>();

		AnnotationMetadata existing = MemberFindingUtils.getTypeAnnotation(this, annotation.getAnnotationType());
		if (existing == null) {
			// Not already present, so just go and add it
			for (JavaSymbolName incomingAttributeName : annotation.getAttributeNames()) {
				// Do not copy incoming attributes which exist in the attributesToDeleteIfPresent Set
				if (attributesToDeleteIfPresent == null || !attributesToDeleteIfPresent.contains(incomingAttributeName)) {
					AnnotationAttributeValue<?> incomingValue = annotation.getAttribute(incomingAttributeName);
					replacementAttributeValues.put(incomingAttributeName, incomingValue);
				}
			}

			AnnotationMetadataBuilder replacement = new AnnotationMetadataBuilder(annotation.getAnnotationType(), new ArrayList<AnnotationAttributeValue<?>>(replacementAttributeValues.values()));
			addTypeAnnotation(replacement.build());
			return true;
		}
		
		// Copy the existing attributes into the new attributes
		for (JavaSymbolName existingAttributeName : existing.getAttributeNames()) {
			if (attributesToDeleteIfPresent != null && attributesToDeleteIfPresent.contains(existingAttributeName)) {
				writeChangesToDisk = true;
			} else {
				AnnotationAttributeValue<?> existingValue = existing.getAttribute(existingAttributeName);
				replacementAttributeValues.put(existingAttributeName, existingValue);
			}
		}

		// Now we ensure every incoming attribute replaces the existing
		for (JavaSymbolName incomingAttributeName : annotation.getAttributeNames()) {
			AnnotationAttributeValue<?> incomingValue = annotation.getAttribute(incomingAttributeName);
			
			// Add this attribute to the end of the list if the attribute is not already present
			if (replacementAttributeValues.keySet().contains(incomingAttributeName)) {
				// There was already an attribute. Need to determine if this new attribute value is materially different
				AnnotationAttributeValue<?> existingValue = replacementAttributeValues.get(incomingAttributeName);
				Assert.notNull(existingValue, "Existing value should have been provided by earlier loop");
				if (!existingValue.equals(incomingValue)) {
					replacementAttributeValues.put(incomingAttributeName, incomingValue);
					writeChangesToDisk = true;
				}
			} else if (attributesToDeleteIfPresent != null && !attributesToDeleteIfPresent.contains(incomingAttributeName)) {
				// This is a new attribute that does not already exist, so add it to the end of the replacement attributes
				replacementAttributeValues.put(incomingAttributeName, incomingValue);
				writeChangesToDisk = true;
			}
		}
		
		// Were there any material changes?
		if (!writeChangesToDisk) {
			return false;
		}
		
		// Make a new AnnotationMetadata representing the replacement
		AnnotationMetadataBuilder replacement = new AnnotationMetadataBuilder(annotation.getAnnotationType(), new ArrayList<AnnotationAttributeValue<?>>(replacementAttributeValues.values()));
		doRemoveTypeAnnotation(replacement.getAnnotationType());
		doAddTypeAnnotation(replacement.build());
		flush();
		
		return true;
	}
	
	public void removeTypeAnnotation(JavaType annotationType) {
		doRemoveTypeAnnotation(annotationType);
		flush();
	}
	
	private void doRemoveTypeAnnotation(JavaType annotationType) {
		List<AnnotationExpr> annotations = clazz.getAnnotations();
		if (annotations == null) {
			annotations = new ArrayList<AnnotationExpr>();
			clazz.setAnnotations(annotations);
		}
		JavaParserAnnotationMetadata.removeAnnotationFromList(this, annotations, annotationType);
	}

	public void addField(FieldMetadata fieldMetadata) {
		List<BodyDeclaration> members = clazz == null ? enumClazz.getMembers() : clazz.getMembers();
		if (members == null) {
			members = new ArrayList<BodyDeclaration>();
			if (clazz == null) {
				enumClazz.setMembers(members);
			} else {
				clazz.setMembers(members);
			}
		}
		JavaParserFieldMetadata.addField(this, members, fieldMetadata);
		flush();
	}

	public void addEnumConstant(JavaSymbolName name) {
		Assert.notNull(name, "Name required");
		Assert.isTrue(this.enumClazz != null, "Enum constants can only be added to an enum class");
		List<EnumConstantDeclaration> constants = this.enumClazz.getEntries();
		if (constants == null) {
			constants = new ArrayList<EnumConstantDeclaration>();
			this.enumClazz.setEntries(constants);
		}
		addEnumConstant(this, constants, name);
		flush();
	}

	public void removeField(JavaSymbolName fieldName) {
		List<BodyDeclaration> members = clazz == null ? enumClazz.getMembers() : clazz.getMembers();
		if (members == null) {
			members = new ArrayList<BodyDeclaration>();
			if (clazz == null) {
				enumClazz.setMembers(members);
			} else {
				clazz.setMembers(members);
			}
		}
		JavaParserFieldMetadata.removeField(this, members, fieldName);
		flush();
	}

	public void addMethod(MethodMetadata methodMetadata) {
		List<BodyDeclaration> members = clazz == null ? enumClazz.getMembers() : clazz.getMembers();
		if (members == null) {
			members = new ArrayList<BodyDeclaration>();
			if (clazz == null) {
				enumClazz.setMembers(members);
			} else {
				clazz.setMembers(members);
			}
		}
		JavaParserMethodMetadata.addMethod(this, members, methodMetadata, typeParameterNames);
		flush();
	}

	private void flush() {
		Reader compilationUnitInputStream = new StringReader(compilationUnit.toString());
		MutableFile mutableFile = fileManager.updateFile(fileIdentifier);
		try {
			FileCopyUtils.copy(compilationUnitInputStream, new OutputStreamWriter(mutableFile.getOutputStream()));
		} catch (IOException ioe) {
			throw new IllegalStateException("Could not update '" + fileIdentifier + "'", ioe);
		}
	}
}
