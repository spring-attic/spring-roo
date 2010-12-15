package org.springframework.roo.addon.web.mvc.controller;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Encapsulates a {@link JavaType} and behavior relying on various kinds of metadata for the specific 
 * JavaType - e.g. {@link BeanInfoMetadata}, {@link EntityMetadata}, {@link PhysicalTypeMetadata}.
 * 
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
public class RooJavaType {
	
	private final JavaType javaType;
	private final MetadataService metadataService;

	private BeanInfoMetadata beanInfoMetadata;
	private EntityMetadata entityMetadata;
	private PhysicalTypeMetadata physicalTypeMetadata;
	private LinkedHashSet<RooJavaType> relatedDomainTypes;

	/**
	 * Default constructor.
	 * 
	 * @param javaType the Java type of the domain object.
	 * @param metadataService the MetataService to use to obtain {@link BeanInfoMetadata} and {@link EntityMetadata}.
	 */
	public RooJavaType(JavaType javaType, MetadataService metadataService) {
		Assert.notNull(javaType, "JavaType is required");
		Assert.notNull(javaType, "MetadataService is required");
		this.javaType = javaType;
		this.metadataService = metadataService;
	}

	/**
	 * @return the domain object's Java type
	 */
	public JavaType getJavaType() {
		return javaType;
	}
	
	/**
	 * @return the short name of the domain object's Java type
	 */
	public String getSimpleTypeName() {
		return javaType.getSimpleTypeName();
	}

	/**
	 * @return the metadata identifier for the BeanInfoMetadata or null
	 */
	public String getBeanInfoMetadataId() {
		return BeanInfoMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
	}

	/**
	 * @return the BeanInfoMetadata for the domain object type
	 */
	public BeanInfoMetadata getBeanInfoMetadata() {
		if (beanInfoMetadata == null) {
			beanInfoMetadata = (BeanInfoMetadata) metadataService.get(getBeanInfoMetadataId());
		}
		return beanInfoMetadata;
	}

	/**
	 * @return the metadata identifier for the EntityMetadata or null
	 */
	public String getEntityMetadataId() {
		return EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
	}

	/**
	 * @return the EntityMetadata for the domain object type
	 */
	public EntityMetadata getEntityMetadata() {
		if (entityMetadata == null) {
			entityMetadata = (EntityMetadata) metadataService.get(getEntityMetadataId());
		}
		return entityMetadata;
	}

	public String getPhysicalTypeMetadataId() {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PhysicalTypeIdentifier.class.getName(), javaType, Path.SRC_MAIN_JAVA);
	}
	
	/**
	 * @return the PhysicalTypeMetadata for the domain object type
	 */
	public PhysicalTypeMetadata getPhysicalTypeMetadata() {
		if (physicalTypeMetadata == null) {
			physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(getPhysicalTypeMetadataId());
		}
		return physicalTypeMetadata;
	}

	/**
	 * Provides access to all associated {@link RooJavaType} types.  
	 * 
	 * @return all associated types or an empty set.
	 */
	public LinkedHashSet<RooJavaType> getRelatedRooTypes() {
		if (relatedDomainTypes == null) {
			this.relatedDomainTypes = findRelatedDomainTypes();
		}
		return relatedDomainTypes;
	}

	/**
	 * @return the MetadataService instance provided to the constructor
	 */
	public MetadataService getMetadataService() {
		return metadataService;
	}

	/**
	 * Finds and returns a type-level annotation on the underlying JavaType.
	 * @param annotation the annotation to find
	 * @return the annotation or null if not found
	 */
	public AnnotationMetadata getTypeAnnotation(JavaType annotation) {
		Assert.notNull(getPhysicalTypeMetadata(), "Java source code unavailable for type " + javaType);
		ClassOrInterfaceTypeDetails details = (ClassOrInterfaceTypeDetails) getPhysicalTypeMetadata().getPhysicalTypeDetails();
		Assert.notNull(details, "Java source code details unavailable for type " + javaType);
		return MemberFindingUtils.getTypeAnnotation(details, annotation);
	}
	
	/**
	 * @return true if the bean info and entity metadata obtained from the metadata service are both valid
	 */
	public boolean isValidMetadata() {
		return ((getBeanInfoMetadata() != null) && getBeanInfoMetadata().isValid() && (getEntityMetadata() != null) && getEntityMetadata().isValid());
	}
	
	/**
	 * @return true if the given Java type matches to one of the associated {@link RooJavaType} types.
	 */
	public boolean isRelatedDomainType(JavaType javaType) {
		for (RooJavaType domainJavaType : getRelatedRooTypes()) {
			if (domainJavaType.getJavaType().equals(javaType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true if the given type is an enumeration.
	 */
	public boolean isEnumType() {
		PhysicalTypeMetadata ptm = getPhysicalTypeMetadata();
		if (ptm != null) {
			PhysicalTypeDetails ptmDetails = ptm.getPhysicalTypeDetails();
			if (ptmDetails != null) {
				if (PhysicalTypeCategory.ENUMERATION.equals(ptmDetails.getPhysicalTypeCategory())) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Selects up to 3 methods that can be used to build up a label that represents the 
	 * domain object. Only methods returning non-domain types, non-collections, and non-arrays
	 * are considered. Accessors for the id and the version fields are also excluded.
	 * @param memberDetailsScanner 
	 * 
	 * @return a list containing between 1 and 3 methods. 
	 *		If no methods could be selected the toString() method is added.
	 */
	public List<MethodMetadata> getMethodsForLabel() {
		Assert.notNull(getBeanInfoMetadata(), "BeanInfo metadata is required.");
		int fieldCount = 0;
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		for (MethodMetadata accessor : getBeanInfoMetadata().getPublicAccessors(false)) {
			if (accessor.getMethodName().equals(getEntityMetadata().getIdentifierAccessor().getMethodName())) {
				continue;
			}
			MethodMetadata versionAccessor = getEntityMetadata().getVersionAccessor();
			if ((versionAccessor != null) && accessor.getMethodName().equals(versionAccessor.getMethodName())) {
				continue;
			}
			FieldMetadata field = getBeanInfoMetadata().getFieldForPropertyName(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor));
			if (field != null // Should not happen
					&& !field.getFieldType().isCommonCollectionType() && !field.getFieldType().isArray() // Exclude collections and arrays
					&& !isRelatedDomainType(field.getFieldType()) // Exclude references to other domain objects as they are too verbose
					&& !field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE) 
					&& !field.getFieldType().equals(JavaType.BOOLEAN_OBJECT) /* Exclude boolean values as they would not be meaningful in this presentation */ ) {

				methods.add(accessor);
				fieldCount++;
				if (fieldCount == 3) {
					break;
				}
			}
		}
		if (methods.size() == 0) {
			methods.add(new MethodMetadataBuilder(getBeanInfoMetadataId(), Modifier.PUBLIC, new JavaSymbolName("toString"), 
					new JavaType("java.lang.String"), null, null, new InvocableMemberBodyBuilder()).build());
		}
		return methods;
	}
	
	@Override
	public int hashCode() {
		return javaType.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return ((obj != null) && (obj instanceof RooJavaType) && this.javaType.equals(((RooJavaType) obj).javaType));
	}

	@Override
	public String toString() {
		return "DomainJavaType [javaType=" + javaType + "]";
	}

	/* Private helper methods */
	
	LinkedHashSet<RooJavaType> findRelatedDomainTypes() {
		LinkedHashSet<RooJavaType> relatedDomainTypes = new LinkedHashSet<RooJavaType>();
		for (MethodMetadata accessor : getBeanInfoMetadata().getPublicAccessors(false)) {
			// Not interested in identifiers and version fields
			if (accessor.equals(getEntityMetadata().getIdentifierAccessor()) || accessor.equals(getEntityMetadata().getVersionAccessor())) {
				continue;
			}
			// Not interested in fields that are not exposed via a mutator
			FieldMetadata fieldMetadata = getBeanInfoMetadata().getFieldForPropertyName(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor));
			if (fieldMetadata == null || !hasMutator(fieldMetadata, getBeanInfoMetadata())) {
				continue;
			}
			JavaType type = accessor.getReturnType();
			if (type.isCommonCollectionType()) {
				for (JavaType genericType : type.getParameters()) {
					if (isApplicationType(genericType)) {
						relatedDomainTypes.add(new RooJavaType(genericType, metadataService));
					}
				}
			} else {
				if (isApplicationType(type) && (!isEmbeddedFieldType(fieldMetadata))) {
					relatedDomainTypes.add(new RooJavaType(type, metadataService));
				}
			}
		}
		return relatedDomainTypes;
	}

	boolean isApplicationType(JavaType javaType) {
		return (metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA)) != null);
	}
	
	boolean isEmbeddedFieldType(FieldMetadata field) {
		return MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Embedded")) != null;
	}

	boolean hasMutator(FieldMetadata fieldMetadata, BeanInfoMetadata bim) {
		for (MethodMetadata mutator : bim.getPublicMutators()) {
			if (fieldMetadata.equals(bim.getFieldForPropertyName(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(mutator)))) {
				return true;
			}
		}
		return false;
	}
	
}
