package org.springframework.roo.classpath.details;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.model.Builder;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;

/**
 * Assists in the creation of a {@link Builder} for types that eventually implement {@link IdentifiableAnnotatedJavaStructure}.
 * 
 * @author Ben Alex
 * @since 1.1
 */
public abstract class AbstractIdentifiableAnnotatedJavaStructureBuilder<T extends IdentifiableAnnotatedJavaStructure> extends AbstractIdentifiableJavaStructureBuilder<T> {
	private List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();

	protected AbstractIdentifiableAnnotatedJavaStructureBuilder(String declaredbyMetadataId) {
		super(declaredbyMetadataId);
	}

	protected AbstractIdentifiableAnnotatedJavaStructureBuilder(IdentifiableAnnotatedJavaStructure existing) {
		super(existing);
		init(existing);
	}
	
	protected AbstractIdentifiableAnnotatedJavaStructureBuilder(String declaredbyMetadataId, IdentifiableAnnotatedJavaStructure existing) {
		super(declaredbyMetadataId, existing);
		init(existing);
	}

	private void init(IdentifiableAnnotatedJavaStructure existing) {
		for (AnnotationMetadata element : existing.getAnnotations()) {
			this.annotations.add(new AnnotationMetadataBuilder(element));
		}
	}

	public final boolean addAnnotation(AnnotationMetadata annotationMetadata) {
		if (annotationMetadata == null) return false;
		return addAnnotation(new AnnotationMetadataBuilder(annotationMetadata));
	}

	public final boolean addAnnotation(AnnotationMetadataBuilder annotationMetadata) {
		if (annotationMetadata == null) return false;
		onAddAnnotation(annotationMetadata);
		return annotations.add(annotationMetadata);
	}

	protected void onAddAnnotation(AnnotationMetadataBuilder annotationMetadata) {}
	
	public final List<AnnotationMetadataBuilder> getAnnotations() {
		return annotations;
	}

	public final List<AnnotationMetadata> buildAnnotations() {
		List<AnnotationMetadata> result = new ArrayList<AnnotationMetadata>();
		for (AnnotationMetadataBuilder builder : annotations) {
			result.add(builder.build());
		}
		return result;
	}

	public final void setAnnotations(List<AnnotationMetadataBuilder> annotations) {
		this.annotations = annotations;
	}

	public final void setAnnotations(Collection<AnnotationMetadata> annotations) {
		List<AnnotationMetadataBuilder> annotationMetadataBuilders = new LinkedList<AnnotationMetadataBuilder>();
		for (AnnotationMetadata annotationMetadata : annotations) {
			annotationMetadataBuilders.add(new AnnotationMetadataBuilder(annotationMetadata));
		}
		setAnnotations(annotationMetadataBuilders);
	}

	public boolean updateTypeAnnotation(AnnotationMetadata annotation, Set<JavaSymbolName> attributesToDeleteIfPresent) {
		boolean hasChanged = false;

		// We are going to build a replacement AnnotationMetadata.
		// This variable tracks the new attribute values the replacement will hold.
		Map<JavaSymbolName, AnnotationAttributeValue<?>> replacementAttributeValues = new LinkedHashMap<JavaSymbolName, AnnotationAttributeValue<?>>();

		AnnotationMetadataBuilder existingBuilder = MemberFindingUtils.getDeclaredTypeAnnotation(this, annotation.getAnnotationType());
		AnnotationMetadata existing = existingBuilder.build();
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
			addAnnotation(replacement);
			return true;
		}

		// Copy the existing attributes into the new attributes
		for (JavaSymbolName existingAttributeName : existing.getAttributeNames()) {
			if (attributesToDeleteIfPresent != null && attributesToDeleteIfPresent.contains(existingAttributeName)) {
				hasChanged = true;
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
					hasChanged = true;
				}
			} else if (attributesToDeleteIfPresent != null && !attributesToDeleteIfPresent.contains(incomingAttributeName)) {
				// This is a new attribute that does not already exist, so add it to the end of the replacement attributes
				replacementAttributeValues.put(incomingAttributeName, incomingValue);
				hasChanged = true;
			}
		}

		// Were there any material changes?
		if (!hasChanged) {
			return false;
		}

		// Make a new AnnotationMetadata representing the replacement
		AnnotationMetadataBuilder replacement = new AnnotationMetadataBuilder(annotation.getAnnotationType(), new ArrayList<AnnotationAttributeValue<?>>(replacementAttributeValues.values()));
		annotations.remove(existingBuilder);
		addAnnotation(replacement);

		return true;
	}
}
