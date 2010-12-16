package org.springframework.roo.classpath.details.annotations;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.AnnotationMetadataUtils;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Represents a {@link JavaType} with zero or more annotations.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public final class AnnotatedJavaType {
	private JavaType javaType;
	private List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
    private boolean isVarArgs = false;
	
	/**
	 * Constructs an {@link AnnotatedJavaType}.
	 * 
	 * @param javaType the type (required)
	 * @param annotations any annotations for the type (null is acceptable)
	 */
	public AnnotatedJavaType(JavaType javaType, List<AnnotationMetadata> annotations) {
		Assert.notNull(javaType, "Java type required");
		this.javaType = javaType;
		if (annotations != null) {
			this.annotations = annotations;
		}
	}
	
	/**
	 * @return the type (never returns null)
	 */
	public JavaType getJavaType() {
		return javaType;
	}

	/**
	 * @return the annotations (never returns null, but may return an empty list)
	 */
	public List<AnnotationMetadata> getAnnotations() {
		return annotations;
	}

	public final String toString() {
		StringBuilder sb = new StringBuilder();
		for (AnnotationMetadata annotation : annotations) {
			sb.append(AnnotationMetadataUtils.toSourceForm(annotation));
			sb.append(" ");
		}
		sb.append(javaType.getNameIncludingTypeParameters());
		return sb.toString();
	}
	
	/**
	 * Converts a non-null {@link List} of {@link JavaType}s into a {@link List} of equivalent {@link AnnotatedJavaType}s.
	 * Note that each returned {@link AnnotatedJavaType} will have no annotation metadata, as the input {@link JavaType}s
	 * cannot store any such metadata.
	 * 
	 * @param javaTypes to convert (required)
	 * @return the equivalent {@link AnnotatedJavaType}s (never returns null)
	 */
	public static List<AnnotatedJavaType> convertFromJavaTypes(List<JavaType> javaTypes) {
		Assert.notNull(javaTypes, "Java types required");
		List<AnnotatedJavaType> result = new ArrayList<AnnotatedJavaType>();
		for (JavaType javaType : javaTypes) {
			result.add(new AnnotatedJavaType(javaType, null));
		}
		return result;
	}

	/**
	 * Converts a non-null {@link List} of {@link AnnotatedJavaType}s into a {@link List} of equivalent {@link JavaType}s.
	 * Note the annotation metadata will be discarded, as it cannot be stored inside a {@link JavaType}.
	 * 
	 * @param annotatedJavaTypes to convert (required)
	 * @return the equivalent {@link AnnotatedJavaType}s, but without any actual annotations (never returns null)
	 */
	public static List<JavaType> convertFromAnnotatedJavaTypes(List<AnnotatedJavaType> annotatedJavaTypes) {
		Assert.notNull(annotatedJavaTypes, "Annotated Java types required");
		List<JavaType> result = new ArrayList<JavaType>();
		for (AnnotatedJavaType annotatedJavaType : annotatedJavaTypes) {
			result.add(annotatedJavaType.getJavaType());
		}
		return result;
	}

    public boolean isVarArgs() {
        return isVarArgs;
    }

    public void setVarArgs(boolean varArgs) {
        isVarArgs = varArgs;
    }
}
