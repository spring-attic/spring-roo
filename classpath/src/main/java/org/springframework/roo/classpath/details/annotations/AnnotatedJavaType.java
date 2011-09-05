package org.springframework.roo.classpath.details.annotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
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
public class AnnotatedJavaType {

	/**
	 * Converts a non-null {@link List} of {@link JavaType}s into a {@link List} of equivalent {@link AnnotatedJavaType}s.
	 * Note that each returned {@link AnnotatedJavaType} will have no annotation metadata, as the input {@link JavaType}s
	 * cannot store any such metadata.
	 * 
	 * @param javaTypes to convert (required)
	 * @return the equivalent {@link AnnotatedJavaType}s (never returns null)
	 */
	public static List<AnnotatedJavaType> convertFromJavaTypes(final List<JavaType> javaTypes) {
		Assert.notNull(javaTypes, "Java types required");
		final List<AnnotatedJavaType> result = new ArrayList<AnnotatedJavaType>();
		for (final JavaType javaType : javaTypes) {
			result.add(convertFromJavaType(javaType));
		}
		return result;
	}

	/**
	 * Converts a {@link JavaType} into an equivalent {@link AnnotatedJavaType}.
	 * Note that each returned {@link AnnotatedJavaType}will have no annotation metadata, as the input {@link JavaType}
	 * cannot store any such metadata.
	 *
	 * @param javaType to convert (required)
	 * @return the equivalent {@link AnnotatedJavaType} (never returns null)
	 */
	public static AnnotatedJavaType convertFromJavaType(final JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		return new AnnotatedJavaType(javaType);
	}

	/**
	 * Converts a non-null {@link List} of {@link AnnotatedJavaType}s into a {@link List} of equivalent {@link JavaType}s.
	 * Note the annotation metadata will be discarded, as it cannot be stored inside a {@link JavaType}.
	 * 
	 * @param annotatedJavaTypes to convert (required)
	 * @return the equivalent {@link AnnotatedJavaType}s, but without any actual annotations (never returns null)
	 */
	public static List<JavaType> convertFromAnnotatedJavaTypes(final List<AnnotatedJavaType> annotatedJavaTypes) {
		Assert.notNull(annotatedJavaTypes, "Annotated Java types required");
		final List<JavaType> result = new LinkedList<JavaType>();
		for (final AnnotatedJavaType annotatedJavaType : annotatedJavaTypes) {
			result.add(convertFromAnnotatedJavaType(annotatedJavaType));
		}
		return result;
	}

	/**
	 * Converts a non-null {@link AnnotatedJavaType} into an equivalent {@link JavaType}.
	 * Note the annotation metadata will be discarded, as it cannot be stored inside a {@link JavaType}.
	 *
	 * @param annotatedJavaType to convert (required)
	 * @return the equivalent {@link AnnotatedJavaType}, but without any actual annotations (never returns null)
	 */
	public static JavaType convertFromAnnotatedJavaType(final AnnotatedJavaType annotatedJavaType) {
		Assert.notNull(annotatedJavaType, "Annotated Java types required");
		return annotatedJavaType.getJavaType();
	}
	
	// Fields
	private final JavaType javaType;
	private final List<AnnotationMetadata> annotations = new ArrayList<AnnotationMetadata>();
    private boolean isVarArgs;
    
    /**
     * Constructor that accepts a vararg array of annotations
     *
     * @param javaType the type (required)
     * @param annotations can be none
     * @since 1.2.0
     */
    public AnnotatedJavaType(final JavaType javaType, final AnnotationMetadata... annotations) {
    	this(javaType, Arrays.asList(annotations));
    }
	
	/**
	 * Constructor that accepts an optional list of annotations
	 * 
	 * @param javaType the type (required)
	 * @param annotations any annotations for the type (defensively copied,
	 * <code>null</code> is acceptable)
	 */
	public AnnotatedJavaType(final JavaType javaType, final Collection<AnnotationMetadata> annotations) {
		Assert.notNull(javaType, "Java type required");
		this.javaType = javaType;
		if (annotations != null) {
			this.annotations.addAll(annotations);
		}
	}
	
	/**
	 * @return the type (never returns null)
	 */
	public JavaType getJavaType() {
		return javaType;
	}

	/**
	 * Returns the annotations on this type
	 * 
	 * @return a copy of this list (never <code>null</code>, but may be empty)
	 */
	public List<AnnotationMetadata> getAnnotations() {
		return new ArrayList<AnnotationMetadata>(this.annotations);
	}

	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final AnnotationMetadata annotation : annotations) {
			sb.append(AnnotationMetadataUtils.toSourceForm(annotation));
			sb.append(" ");
		}
		sb.append(javaType.getNameIncludingTypeParameters());
		return sb.toString();
	}
	
    public boolean isVarArgs() {
        return isVarArgs;
    }

    public void setVarArgs(final boolean varArgs) {
        isVarArgs = varArgs;
    }
}
