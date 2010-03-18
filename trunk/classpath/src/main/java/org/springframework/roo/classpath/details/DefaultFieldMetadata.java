package org.springframework.roo.classpath.details;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link FieldMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
public class DefaultFieldMetadata implements FieldMetadata {

	private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
	private String fieldInitializer;
	private JavaSymbolName fieldName;
	private JavaType fieldType;
	private String declaredByMetadataId;
	private int modifier;
	
	public DefaultFieldMetadata(String declaredByMetadataId, int modifier, JavaSymbolName fieldName, JavaType fieldType, String fieldInitializer, List<AnnotationMetadata> annotations) {
		Assert.hasText(declaredByMetadataId, "Declared by metadata ID required");
		Assert.notNull(fieldName, "Field name required");
		Assert.notNull(fieldType, "Field type required");
		this.declaredByMetadataId = declaredByMetadataId;
		this.fieldName = fieldName;
		this.fieldType = fieldType;
		this.fieldInitializer = fieldInitializer;
		this.modifier = modifier;
		
		if (annotations != null) {
			this.annotations = annotations;
		}
	}
	
	public int getModifier() {
		return modifier;
	}

	public String getDeclaredByMetadataId() {
		return declaredByMetadataId;
	}

	public List<AnnotationMetadata> getAnnotations() {
		return annotations;
	}
	
	public String getFieldInitializer() {
		return fieldInitializer;
	}
	
	public JavaSymbolName getFieldName() {
		return fieldName;
	}
	
	public JavaType getFieldType() {
		return fieldType;
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("declaredByMetadataId", declaredByMetadataId);
		tsc.append("modifier", Modifier.toString(modifier));
		tsc.append("fieldType", fieldType);
		tsc.append("fieldName", fieldName);
		tsc.append("fieldInitializer", fieldInitializer);
		tsc.append("annotations", annotations);
		return tsc.toString();
	}

}
