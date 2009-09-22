package org.springframework.roo.addon.finder;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.DefaultMethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Metadata for {@link RooEntity}.
 * 
 * <p>
 * Any getter produced by this metadata is automatically included in the {@link BeanInfoMetadata}.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 *
 */
public class FinderMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	private static final String PROVIDES_TYPE_STRING = FinderMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	private BeanInfoMetadata beanInfoMetadata;
	private EntityMetadata entityMetadata;
	
	public FinderMetadata(String identifier, JavaType aspectName, PhysicalTypeMetadata governorPhysicalTypeMetadata, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' does not appear to be a valid");
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.notNull(entityMetadata, "Entity metadata required");
		
		if (!isValid()) {
			return;
		}
		
		this.beanInfoMetadata = beanInfoMetadata;
		this.entityMetadata = entityMetadata;
		
		for (String method : entityMetadata.getDynamicFinders()) {
			builder.addMethod(getDynamicFinderMethod(method));
		}
		
		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}
	
	/**
	 * Locates a dynamic finder method of the specified name, or creates one on demand if not present.
	 * 
	 * <p>
	 * It is required that the requested name was defined in the {@link RooEntity#finders()}. If it is not
	 * present, an exception is thrown.
	 * 
	 * @return the user-defined method, or an ITD-generated method (never returns null)
	 */
	public MethodMetadata getDynamicFinderMethod(String dynamicFinderMethodName) {
		Assert.hasText(dynamicFinderMethodName, "Dynamic finder method name is required");
		Assert.isTrue(entityMetadata.getDynamicFinders().contains(dynamicFinderMethodName), "Undefined method name '" + dynamicFinderMethodName + "'");
		
		JavaSymbolName methodName = new JavaSymbolName(dynamicFinderMethodName);
		
		// We have no access to method parameter information, so we scan by name alone and treat any match as authoritative
		// We do not scan the superclass, as the caller is expected to know we'll only scan the current class
		for (MethodMetadata method : governorTypeDetails.getDeclaredMethods()) {
			if (method.getMethodName().equals(methodName)) {
				// Found a method of the expected name; we won't check method parameters though
				return method;
			}
		}
		
		// To get this far we need to create the method...
		DynamicFinderServices dynamicFinderServices = new DynamicFinderServicesImpl();
		String jpaQuery = dynamicFinderServices.getJpaQueryFor(methodName, entityMetadata.getPlural(), beanInfoMetadata);
		List<JavaSymbolName> paramNames = dynamicFinderServices.getParameterNames(methodName, entityMetadata.getPlural(), beanInfoMetadata);
		List<JavaType> paramTypes = dynamicFinderServices.getParameterTypes(methodName, entityMetadata.getPlural(), beanInfoMetadata);
		
		// We declared the field in this ITD, so produce a public accessor for it
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

		for (int i = 0; i < paramTypes.size(); i++) {
			String name = paramNames.get(i).getSymbolName();
			
			StringBuilder length = new StringBuilder();
			if (paramTypes.get(i).equals(new JavaType("java.lang.String"))) {
				length.append(" || ").append(paramNames.get(i)).append(".length() == 0");
			}
			
			if (!paramTypes.get(i).isPrimitive()) {
				bodyBuilder.appendFormalLine("if (" + name + " == null" + length.toString() + ") throw new IllegalArgumentException(\"The " + name + " argument is required\");");
			}
			
			if (length.length() > 0 && dynamicFinderMethodName.substring(dynamicFinderMethodName.indexOf(paramNames.get(i).getSymbolNameCapitalisedFirstLetter()) + name.length()).startsWith("Like")){
				bodyBuilder.appendFormalLine(name + " = " + name + ".replace('*', '%');");
				bodyBuilder.appendFormalLine("if (" + name + ".charAt(0) != '%') {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(name + " = \"%\" + " + name + ";");
				bodyBuilder.indentRemove();
				bodyBuilder.appendFormalLine("}");
				bodyBuilder.appendFormalLine("if (" + name + ".charAt(" + name + ".length() -1) != '%') {");
				bodyBuilder.indent();
				bodyBuilder.appendFormalLine(name + " = " + name + " + \"%\";");
				bodyBuilder.indentRemove();
				bodyBuilder.appendFormalLine("}");
			}
		}
		
		bodyBuilder.appendFormalLine("javax.persistence.EntityManager em = new " + governorTypeDetails.getName().getSimpleTypeName() + "()." + entityMetadata.getEntityManagerField().getFieldName().getSymbolName() + ";");
		bodyBuilder.appendFormalLine("if (em == null) throw new IllegalStateException(\"Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)\");");

		bodyBuilder.appendFormalLine("javax.persistence.Query q = em.createQuery(\"" + jpaQuery + "\");");
		
		for (JavaSymbolName name : paramNames) {
			bodyBuilder.appendFormalLine("q.setParameter(\"" + name + "\", " + name + ");");
		}
		
		bodyBuilder.appendFormalLine("return q;");
		
		int modifier = Modifier.PUBLIC;
		modifier = modifier |= Modifier.STATIC;
		return new DefaultMethodMetadata(getId(), modifier, methodName, new JavaType("javax.persistence.Query"), AnnotatedJavaType.convertFromJavaTypes(paramTypes), paramNames, new ArrayList<AnnotationMetadata>(), bodyBuilder.getOutput());
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static final String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}
	
	public static final String createIdentifier(JavaType javaType, Path path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static final JavaType getJavaType(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static final Path getPath(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

}
