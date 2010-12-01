package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.Fetch;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Properties used by the many-to-one side of a relationship (called a "reference").
 * 
 * <p>
 * For example, an Order-LineItem link would have the LineItem contain a "reference" back to Order.
 *  
 * <p>
 * Limited support for collection mapping is provided. This reflects the pragmatic goals of ROO and the fact a user can
 * edit the generated files by hand anyway.
 * 
 * <p>
 * This field is intended for use with JSR 220 and will create a @ManyToOne and @JoinColumn annotation.
 *
 * @author Ben Alex
 * @since 1.0
 */
public class ReferenceField extends FieldDetails {
	private String joinColumnName;
	private String referencedColumnName;
	private Fetch fetch = null;
	private Cardinality cardinality = null;

	public ReferenceField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName, Cardinality cardinality) {		
		super(physicalTypeIdentifier, fieldType, fieldName);
		this.cardinality = cardinality;
	}

	public String getJoinColumnName() {
		return joinColumnName;
	}

	public void setJoinColumnName(String joinColumnName) {
		this.joinColumnName = joinColumnName;
	}

	public String getReferencedColumnName() {
		return referencedColumnName;
	}

	public void setReferencedColumnName(String referencedColumnName) {
		this.referencedColumnName = referencedColumnName;
	}

	public Fetch getFetch() {
		return fetch;
	}

	public void setFetch(Fetch fetch) {
		this.fetch = fetch;
	}

	public void decorateAnnotationsList(List<AnnotationMetadataBuilder> annotations) {
		super.decorateAnnotationsList(annotations);
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		
		if (fetch != null) {
			JavaSymbolName value = new JavaSymbolName("EAGER");
			if (fetch == Fetch.LAZY) {
				value = new JavaSymbolName("LAZY");
			}
			attributes.add(new EnumAttributeValue(new JavaSymbolName("fetch"), new EnumDetails(new JavaType("javax.persistence.FetchType"), value)));
		}
		
		switch (cardinality) {
			case ONE_TO_MANY:
				annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.OneToMany"), attributes));
				break;
			case MANY_TO_MANY:
				annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.ManyToMany"), attributes));
				break;
			case ONE_TO_ONE:
				annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.OneToOne"), attributes));
				break;
			default:
				annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.ManyToOne"), attributes));
				break;
		}
		
		if (joinColumnName != null) {
			List<AnnotationAttributeValue<?>> joinColumnAttrs = new ArrayList<AnnotationAttributeValue<?>>();
			joinColumnAttrs.add(new StringAttributeValue(new JavaSymbolName("name"), joinColumnName));

			if (referencedColumnName != null) {
				joinColumnAttrs.add(new StringAttributeValue(new JavaSymbolName("referencedColumnName"), referencedColumnName));
			}
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.JoinColumn"), joinColumnAttrs));
		}
	}
}
