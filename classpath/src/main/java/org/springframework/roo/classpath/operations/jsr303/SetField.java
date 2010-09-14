package org.springframework.roo.classpath.operations.jsr303;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.Fetch;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Properties used by the one side of a many-to-one relationship or an @ElementCollection of enums (called a "set").
 * 
 * <p>
 * For example, an Order-LineItem link would have the Order contain a "set" of Orders.
 *  
 * <p>
 * Limited support for collection mapping is provided. This reflects the pragmatic goals of the tool and the fact a user can
 * edit the generated files by hand anyway.
 * 
 * <p>
 * This field is intended for use with JSR 220 and will create a @OneToMany annotation or in the case of enums,
 * an @ElementCollection annotation will be created.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public class SetField extends CollectionField {
	
	/** Whether the JSR 220 @OneToMany.mappedBy annotation attribute will be added */
	private JavaSymbolName mappedBy = null;
	
	private Cardinality cardinality = null;
	private Fetch fetch = null;
	
	public SetField(String physicalTypeIdentifier, JavaType fieldType, JavaSymbolName fieldName, JavaType genericParameterTypeName, Cardinality cardinality) {
		super(physicalTypeIdentifier, fieldType, fieldName, genericParameterTypeName);
		this.cardinality = cardinality;
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

		if (cardinality == null) {
			// Assume set field is an enum
			annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.ElementCollection")));
		} else {
			attributes.add(new EnumAttributeValue(new JavaSymbolName("cascade"), new EnumDetails(new JavaType("javax.persistence.CascadeType"), new JavaSymbolName("ALL"))));
			if (fetch != null) {
				JavaSymbolName value = new JavaSymbolName("EAGER");
				if (fetch == Fetch.LAZY) {
					value = new JavaSymbolName("LAZY");
				}
				attributes.add(new EnumAttributeValue(new JavaSymbolName("fetch"), new EnumDetails(new JavaType("javax.persistence.FetchType"), value)));
			}
			if (mappedBy != null) {
				attributes.add(new StringAttributeValue(new JavaSymbolName("mappedBy"), mappedBy.getSymbolName()));
			}

			switch (cardinality) {
				case ONE_TO_MANY:
					annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.OneToMany"), attributes));
					break;
				case MANY_TO_ONE:
					annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.ManyToOne"), attributes));
					break;
				case ONE_TO_ONE:
					annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.OneToOne"), attributes));
					break;
				default:
					annotations.add(new AnnotationMetadataBuilder(new JavaType("javax.persistence.ManyToMany"), attributes));
					break;
			}
		}
	}

	public JavaType getInitializer() {
		List<JavaType> params = new ArrayList<JavaType>();
		params.add(getGenericParameterTypeName());
		return new JavaType("java.util.HashSet", 0, DataType.TYPE, null, params);
	}

	public JavaSymbolName getMappedBy() {
		return mappedBy;
	}

	public void setMappedBy(JavaSymbolName mappedBy) {
		this.mappedBy = mappedBy;
	}
}
