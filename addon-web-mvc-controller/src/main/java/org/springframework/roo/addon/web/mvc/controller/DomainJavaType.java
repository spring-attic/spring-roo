package org.springframework.roo.addon.web.mvc.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.Assert;

/**
 * Encapsulates a {@link JavaType} and provides access to related {@link BeanInfoMetadata} and {@link EntityMetadata}
 * obtained from the {@link MetadataService}.
 * 
 * @author Rossen Stoyanchev
 * @since 1.1.1
 */
public class DomainJavaType {
	
	private final JavaType javaType;
	private final MetadataService metadataService;
	private final MemberDetailsScanner memberDetailsScanner;

	private BeanInfoMetadata beanInfoMetadata;
	private EntityMetadata entityMetadata;
	private PhysicalTypeMetadata physicalTypeMetadata;
	private LinkedHashSet<DomainJavaType> relatedDomainTypes;
	
	/**
	 * Default constructor.
	 * 
	 * @param javaType the Java type of the domain object.
	 * @param metadataService the MetataService to use to obtain {@link BeanInfoMetadata} and {@link EntityMetadata}.
	 */
	public DomainJavaType(JavaType javaType, MetadataService metadataService, MemberDetailsScanner memberDetailsScanner) {
		Assert.notNull(javaType, "JavaType is required");
		this.javaType = javaType;
		this.metadataService = metadataService;
		this.memberDetailsScanner = memberDetailsScanner;
	}

	/**
	 * @return true if the bean info and entity metadata obtained from the metadata service are both valid
	 */
	public boolean isValidMetadata() {
		return ((getBeanInfoMetadata() != null) && getBeanInfoMetadata().isValid() && (getEntityMetadata() != null) && getEntityMetadata().isValid());
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
		return (isValidMetadata()) ? getBeanInfoMetadata().getId() : null;
	}

	/**
	 * @return the BeanInfoMetadata for the domain object type
	 */
	public BeanInfoMetadata getBeanInfoMetadata() {
		if (beanInfoMetadata == null) {
			String id = BeanInfoMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
			beanInfoMetadata = (BeanInfoMetadata) metadataService.get(id);
		}
		return beanInfoMetadata;
	}

	/**
	 * @return the metadata identifier for the EntityMetadata or null
	 */
	public String getEntityMetadataId() {
		return (isValidMetadata()) ? getEntityMetadata().getId() : null;
	}

	/**
	 * @return the EntityMetadata for the domain object type
	 */
	public EntityMetadata getEntityMetadata() {
		if (entityMetadata == null) {
			String id = EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
			entityMetadata = (EntityMetadata) metadataService.get(id);
		}
		return entityMetadata;
	}

	/**
	 * @return the PhysicalTypeMetadata for the domain object type
	 */
	public PhysicalTypeMetadata getPhysicalTypeMetadata() {
		if (physicalTypeMetadata == null) {
			String id = PhysicalTypeIdentifierNamingUtils.createIdentifier(PhysicalTypeIdentifier.class.getName(), javaType, Path.SRC_MAIN_JAVA);
			physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(id);
		}
		return physicalTypeMetadata;
	}

	/**
	 * Provides access to all associated {@link DomainJavaType} types.  
	 * 
	 * @return all associated types or an empty set.
	 */
	public LinkedHashSet<DomainJavaType> getRelatedDomainTypes() {
		if (relatedDomainTypes == null) {
			this.relatedDomainTypes = findRelatedDomainTypes();
		}
		return relatedDomainTypes;
	}
	
	/**
	 * @return true if the given Java type matches to one of the associated {@link DomainJavaType} types.
	 */
	public boolean isRelatedDomainType(JavaType javaType) {
		for (DomainJavaType domainJavaType : getRelatedDomainTypes()) {
			if (domainJavaType.getJavaType().equals(javaType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Selects up to 3 methods that can be used to build up a label that represents the 
	 * domain object. Only methods returning non-domain types, non-collections, and non-arrays
	 * are considered. Accessors for the id and the version fields are also excluded.
	 * 
	 * @return a list containing between 1 and 3 methods. 
	 *		If no methods could be selected the toString() method is added.
	 */
	public List<MethodMetadata> getMethodsForLabel() {
		int fieldCount = 0;
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		for (MethodMetadata accessor : getBeanInfoMetadata().getPublicAccessors(false)) {
			if (accessor.getMethodName().equals(entityMetadata.getIdentifierAccessor().getMethodName())) {
				continue;
			}
			MethodMetadata versionAccessor = entityMetadata.getVersionAccessor();
			if ((versionAccessor != null) && accessor.getMethodName().equals(versionAccessor.getMethodName())) {
				continue;
			}
			FieldMetadata field = getBeanInfoMetadata().getFieldForPropertyName(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor));
			if (field != null // Should not happen
					&& ! field.getFieldType().isCommonCollectionType() && !field.getFieldType().isArray() // Exclude collections and arrays
					&& ! isRelatedDomainType(field.getFieldType()) // Exclude references to other domain objects as they are too verbose
					&& ! field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE) && !field.getFieldType().equals(JavaType.BOOLEAN_OBJECT) /* Exclude boolean values as they would not be meaningful in this presentation */ ) {

				methods.add(accessor);
				fieldCount++;
				if (fieldCount == 3) {
					break;
				}
			}
		}
		if (methods.size() == 0) {
			ClassOrInterfaceTypeDetails typeDetails = (ClassOrInterfaceTypeDetails) getPhysicalTypeMetadata().getPhysicalTypeDetails();
			MemberDetails memberDetails = memberDetailsScanner.getMemberDetails(getClass().getName(), typeDetails);
			List<AnnotatedJavaType> parameters = Collections.emptyList();
			MethodMetadata method = MemberFindingUtils.getMethod(memberDetails, new JavaSymbolName("toString"), AnnotatedJavaType.convertFromAnnotatedJavaTypes(parameters));
			methods.add(method);
		}
		return methods;
	}
	
	@Override
	public int hashCode() {
		return javaType.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return ((obj != null) && (obj instanceof DomainJavaType) && this.javaType.equals(((DomainJavaType) obj).javaType));
	}

	@Override
	public String toString() {
		return "DomainJavaType [javaType=" + javaType + "]";
	}

	/* Private helper methods */
	
	LinkedHashSet<DomainJavaType> findRelatedDomainTypes() {
		LinkedHashSet<DomainJavaType> relatedDomainTypes = new LinkedHashSet<DomainJavaType>();
		for (MethodMetadata accessor : getBeanInfoMetadata().getPublicAccessors(false)) {
			// Not interested in identifiers and version fields
			if (accessor.equals(entityMetadata.getIdentifierAccessor()) || accessor.equals(entityMetadata.getVersionAccessor())) {
				continue;
			}
			// Not interested in fields that are not exposed via a mutator
			FieldMetadata fieldMetadata = getBeanInfoMetadata().getFieldForPropertyName(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor));
			if (fieldMetadata == null || !hasMutator(fieldMetadata, beanInfoMetadata)) {
				continue;
			}
			JavaType type = accessor.getReturnType();
			if (type.isCommonCollectionType()) {
				for (JavaType genericType : type.getParameters()) {
					if (isApplicationType(genericType)) {
						relatedDomainTypes.add(new DomainJavaType(genericType, metadataService, memberDetailsScanner));
					}
				}
			} else {
				if (isApplicationType(type) && (!isEmbeddedFieldType(fieldMetadata))) {
					relatedDomainTypes.add(new DomainJavaType(type, metadataService, memberDetailsScanner));
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
