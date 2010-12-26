package org.springframework.roo.addon.web.mvc.controller;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.beaninfo.BeanInfoUtils;
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
public class JavaTypeWrapper {
	
	private final JavaType javaType;
	private final MetadataService metadataService;

	private BeanInfoMetadata beanInfoMetadata;
	private EntityMetadata entityMetadata;
	private Set<JavaTypeWrapper> relatedDomainTypes;
	private PhysicalTypeMetadata physicalTypeMetadata = null;
	private ClassOrInterfaceTypeDetails details = null;
	private static ThreadLocal<Set<JavaType>> alreadyProcessed = new ThreadLocal<Set<JavaType>>();

	/**
	 * Default constructor.
	 * 
	 * @param javaType the Java type of the domain object.
	 * @param metadataService the MetataService to use to obtain {@link BeanInfoMetadata} and {@link EntityMetadata}.
	 */
	public JavaTypeWrapper(JavaType javaType, MetadataService metadataService) {
		Assert.notNull(javaType, "JavaType is required");
		Assert.notNull(javaType, "MetadataService is required");
		this.javaType = javaType;
		this.metadataService = metadataService;
		this.beanInfoMetadata = (BeanInfoMetadata) metadataService.get(getBeanInfoMetadataId());
		this.entityMetadata = (EntityMetadata) metadataService.get(getEntityMetadataId());
		this.physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(getPhysicalTypeMetadataId());
		
		try {
			if (alreadyProcessed.get() == null) {
				alreadyProcessed.set(new HashSet<JavaType>());
			}
			alreadyProcessed.get().add(javaType);
			this.relatedDomainTypes = findRelatedDomainTypes();
		} finally {
			alreadyProcessed.get().remove(javaType);
		}
		
		if (this.physicalTypeMetadata != null) {
			this.details = (ClassOrInterfaceTypeDetails) physicalTypeMetadata.getMemberHoldingTypeDetails();
		}
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
	 * @return the metadata identifier for the EntityMetadata or null
	 */
	public String getEntityMetadataId() {
		return EntityMetadata.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
	}

	public String getPhysicalTypeMetadataId() {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PhysicalTypeIdentifier.class.getName(), javaType, Path.SRC_MAIN_JAVA);
	}
	
	/**
	 * @return the MetadataService instance provided to the constructor
	 */
	MetadataService getMetadataService() {
		return metadataService;
	}
	
	public Set<JavaTypeWrapper> getRelatedDomainTypes() {
		if (relatedDomainTypes == null) {
			return null;
		}
		return Collections.unmodifiableSet(relatedDomainTypes);
	}
	
	public BeanInfoMetadata getBeanInfoMetadata() {
		return beanInfoMetadata;
	}

	/**
	 * Finds and returns a type-level annotation on the underlying JavaType.
	 * @param annotation the annotation to find
	 * @return the annotation or null if not found
	 */
	public AnnotationMetadata getTypeAnnotation(JavaType annotation) {
		Assert.notNull(physicalTypeMetadata, "Java source code unavailable for type " + javaType);
		Assert.notNull(details, "Java source code details unavailable for type " + javaType);
		return MemberFindingUtils.getTypeAnnotation(details, annotation);
	}
	
	/**
	 * @return true if the bean info and entity metadata obtained from the metadata service are both valid
	 */
	public boolean isValidMetadata() {
		return ((beanInfoMetadata != null) && beanInfoMetadata.isValid() && entityMetadata != null && entityMetadata.isValid());
	}
	
	/**
	 * @return true if the given Java type matches to one of the associated {@link JavaTypeWrapper} types.
	 */
	public boolean isRelatedDomainType(JavaType javaType) {
		for (JavaTypeWrapper domainJavaType : relatedDomainTypes) {
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
		PhysicalTypeMetadata ptm = physicalTypeMetadata;
		if (ptm != null) {
			PhysicalTypeDetails ptmDetails = ptm.getMemberHoldingTypeDetails();
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
		Assert.notNull(beanInfoMetadata, "BeanInfo metadata is required.");
		int fieldCount = 0;
		List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
		for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors(false)) {
			if (accessor.getMethodName().equals(entityMetadata.getIdentifierAccessor().getMethodName())) {
				continue;
			}
			MethodMetadata versionAccessor = entityMetadata.getVersionAccessor();
			if ((versionAccessor != null) && accessor.getMethodName().equals(versionAccessor.getMethodName())) {
				continue;
			}
			FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor));
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
		return Collections.unmodifiableList(methods);
	}
	
	@Override
	public int hashCode() {
		return javaType.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return ((obj != null) && (obj instanceof JavaTypeWrapper) && this.javaType.equals(((JavaTypeWrapper) obj).javaType));
	}

	@Override
	public String toString() {
		return "DomainJavaType [javaType=" + javaType + "]";
	}

	private Set<JavaTypeWrapper> findRelatedDomainTypes() {
		LinkedHashSet<JavaTypeWrapper> relatedDomainTypes = new LinkedHashSet<JavaTypeWrapper>();
		if (beanInfoMetadata == null) {
			return null;
		}
		outer:
		for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors(false)) {
			// Not interested in identifiers and version fields
			if (accessor.equals(entityMetadata.getIdentifierAccessor()) || accessor.equals(entityMetadata.getVersionAccessor())) {
				continue;
			}
			// Not interested in fields that are not exposed via a mutator
			FieldMetadata fieldMetadata = beanInfoMetadata.getFieldForPropertyName(BeanInfoUtils.getPropertyNameForJavaBeanMethod(accessor));
			if (fieldMetadata == null || !hasMutator(fieldMetadata, beanInfoMetadata)) {
				continue;
			}
			JavaType type = accessor.getReturnType();
			if (type.isCommonCollectionType()) {
				for (JavaType genericType : type.getParameters()) {
					if (isApplicationType(genericType)) {
						if (alreadyProcessed.get().contains(genericType)) {
							continue outer;
						}
						relatedDomainTypes.add(new JavaTypeWrapper(genericType, metadataService));
					}
				}
			} else {
				if (isApplicationType(type) && (!isEmbeddedFieldType(fieldMetadata))) {
					if (alreadyProcessed.get().contains(type)) {
						continue outer;
					}
					relatedDomainTypes.add(new JavaTypeWrapper(type, metadataService));
				}
			}
		}
		return relatedDomainTypes;
	}

	private boolean isApplicationType(JavaType javaType) {
		return (metadataService.get(PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA)) != null);
	}
	
	private boolean isEmbeddedFieldType(FieldMetadata field) {
		return MemberFindingUtils.getAnnotationOfType(field.getAnnotations(), new JavaType("javax.persistence.Embedded")) != null;
	}

	private boolean hasMutator(FieldMetadata fieldMetadata, BeanInfoMetadata bim) {
		for (MethodMetadata mutator : bim.getPublicMutators()) {
			if (fieldMetadata.equals(bim.getFieldForPropertyName(BeanInfoUtils.getPropertyNameForJavaBeanMethod(mutator)))) {
				return true;
			}
		}
		return false;
	}
	
}