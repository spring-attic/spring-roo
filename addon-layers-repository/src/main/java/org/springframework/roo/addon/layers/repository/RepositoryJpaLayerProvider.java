package org.springframework.roo.addon.layers.repository;

import java.util.Arrays;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.layers.CoreLayerProvider;
import org.springframework.roo.project.layers.LayerType;
import org.springframework.roo.project.layers.MemberTypeAdditions;
import org.springframework.roo.support.util.Pair;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.uaa.client.util.Assert;

/**
 * A provider of the {@link LayerType#REPOSITORY} layer.
 *  
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2
 */
@Component
@Service
public class RepositoryJpaLayerProvider extends CoreLayerProvider {
	
	// Constants
	private static final JavaType ANNOTATION_TYPE = new JavaType(RooRepositoryJpa.class.getName());
	private static final JavaType AUTOWIRED = new JavaType("org.springframework.beans.factory.annotation.Autowired");
	
	// Fields
	@Reference private TypeLocationService typeLocationService;
	@Reference private MetadataService metadataService;
	
	public MemberTypeAdditions getMemberTypeAdditions(String metadataId, String methodIdentifier, JavaType targetEntity, Pair<JavaType, JavaSymbolName>... methodParameters) {
		Assert.isTrue(StringUtils.hasText(metadataId), "Metadata identifier required");
		Assert.notNull(methodIdentifier, "Method identifier required");
		Assert.notNull(targetEntity, "Target enitity type required");
		Assert.notNull(methodParameters, "Method param names and types required (may be empty)");
		
		TypeContainer typeContainer = findInterfaceType(targetEntity);
		if (typeContainer == null) {
			return null;
		}
		
		if (methodIdentifier.equals(PersistenceCustomDataKeys.FIND_ALL_METHOD.name())) {
			return getFindAllMethod(metadataId, typeContainer);
		} else if (methodIdentifier.equals(PersistenceCustomDataKeys.PERSIST_METHOD.name())) {
			return getPersistMethod(metadataId, targetEntity, typeContainer, methodParameters);
		} else if (methodIdentifier.equals(PersistenceCustomDataKeys.MERGE_METHOD.name())) {
			return getMergeMethod(metadataId, targetEntity, typeContainer, methodParameters);
		} else if (methodIdentifier.equals(PersistenceCustomDataKeys.REMOVE_METHOD.name())) {
			return getDeleteMethod(metadataId, targetEntity, typeContainer, methodParameters);
		}
		return null;
	}
	
	private MemberTypeAdditions getPersistMethod(final String metadataId, final JavaType entityType, final TypeContainer typeContainer, final Pair<JavaType, JavaSymbolName>... methodParameters) {
		if (methodParameters == null || methodParameters.length != 1 || !methodParameters[0].getKey().equals(entityType)) {
			return null;
		}
		ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(metadataId);
		AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(AUTOWIRED);
		String repoField = StringUtils.uncapitalize(typeContainer.getClassOrInterfaceTypeDetails().getName().getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(metadataId, 0, Arrays.asList(annotation), new JavaSymbolName(repoField), typeContainer.getClassOrInterfaceTypeDetails().getName()).build());
		String methodName = typeContainer.getRepositoryJpaAnnotationValues().getSaveMethod();
		return new MemberTypeAdditions(classBuilder, repoField, methodName, methodParameters[0].getValue());
	}
	
	private MemberTypeAdditions getMergeMethod(final String metadataId, final JavaType entityType, final TypeContainer typeContainer, final Pair<JavaType, JavaSymbolName>... methodParameters) {
		if (methodParameters == null || methodParameters.length != 1 || !methodParameters[0].getKey().equals(entityType)) {
			return null;
		}
		ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(metadataId);
		AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(AUTOWIRED);
		String repoField = StringUtils.uncapitalize(typeContainer.getClassOrInterfaceTypeDetails().getName().getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(metadataId, 0, Arrays.asList(annotation), new JavaSymbolName(repoField), typeContainer.getClassOrInterfaceTypeDetails().getName()).build());
		String methodName = typeContainer.getRepositoryJpaAnnotationValues().getUpdateMethod();
		return new MemberTypeAdditions(classBuilder, repoField, methodName, methodParameters[0].getValue());
	}

	private MemberTypeAdditions getFindAllMethod(final String metadataId, final TypeContainer typeContainer) {
		ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(metadataId);
		AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(AUTOWIRED);
		String repoField = StringUtils.uncapitalize(typeContainer.getClassOrInterfaceTypeDetails().getName().getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(metadataId, 0, Arrays.asList(annotation), new JavaSymbolName(repoField), typeContainer.getClassOrInterfaceTypeDetails().getName()).build());
		String methodName = typeContainer.getRepositoryJpaAnnotationValues().getFindAllMethod();
		return new MemberTypeAdditions(classBuilder, repoField, methodName);
	}
	
	private MemberTypeAdditions getDeleteMethod(final String metadataId, final JavaType entityType, final TypeContainer typeContainer, final Pair<JavaType, JavaSymbolName>... methodParameters) {
		if (methodParameters == null || methodParameters.length != 1 || !methodParameters[0].getKey().equals(entityType)) {
			return null;
		}
		ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(metadataId);
		AnnotationMetadataBuilder annotation = new AnnotationMetadataBuilder(AUTOWIRED);
		String repoField = StringUtils.uncapitalize(typeContainer.getClassOrInterfaceTypeDetails().getName().getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(metadataId, 0, Arrays.asList(annotation), new JavaSymbolName(repoField), typeContainer.getClassOrInterfaceTypeDetails().getName()).build());
		String methodName = typeContainer.getRepositoryJpaAnnotationValues().getDeleteMethod();
		return new MemberTypeAdditions(classBuilder, repoField, methodName, methodParameters[0].getValue());
	}

	private TypeContainer findInterfaceType(JavaType type) {
		for (ClassOrInterfaceTypeDetails coitd : typeLocationService.findClassesOrInterfaceDetailsWithAnnotation(ANNOTATION_TYPE)) {
			PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(coitd.getName(), Path.SRC_MAIN_JAVA));
			if (physicalTypeMetadata == null) {
				continue;
			}
			RepositoryJpaAnnotationValues repositoryJpaAnnotationValues = new RepositoryJpaAnnotationValues(physicalTypeMetadata);
			if (!repositoryJpaAnnotationValues.getDomainType().equals(type)) {
				continue;
			}
			return new TypeContainer(coitd, repositoryJpaAnnotationValues);
		}
		return null;
	}

	public int getLayerPosition() {
		return LayerType.REPOSITORY.getPosition();
	}
	
	/**
	 * Container to hold {@link ClassOrInterfaceTypeDetails} and {@link RepositoryJpaAnnotationValues} 
	 * for a given domain type;
	 * 
	 * @author Stefan Schmidt
	 * @since 1.2
	 */
	private class TypeContainer {
		private ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails;
		private RepositoryJpaAnnotationValues repositoryJpaAnnotationValues;
		public TypeContainer(ClassOrInterfaceTypeDetails classOrInterfaceTypeDetails, RepositoryJpaAnnotationValues repositoryJpaAnnotationValues) {
			this.classOrInterfaceTypeDetails = classOrInterfaceTypeDetails;
			this.repositoryJpaAnnotationValues = repositoryJpaAnnotationValues;
		}
		public ClassOrInterfaceTypeDetails getClassOrInterfaceTypeDetails() {
			return classOrInterfaceTypeDetails;
		}
		public RepositoryJpaAnnotationValues getRepositoryJpaAnnotationValues() {
			return repositoryJpaAnnotationValues;
		}
	}
}
