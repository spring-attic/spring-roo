package org.springframework.roo.addon.layers.repository.jpa;

import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.layers.CoreLayerProvider;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.Pair;
import org.springframework.roo.support.util.PairList;
import org.springframework.roo.support.util.StringUtils;
import org.springframework.uaa.client.util.Assert;

/**
 * A provider of the {@link LayerType#REPOSITORY} layer.
 *  
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class RepositoryJpaLayerProvider extends CoreLayerProvider {
	
	// Fields
	@Reference private RepositoryJpaLocator repositoryLocator;
	
	public MemberTypeAdditions getMemberTypeAdditions(final String callerMID, final String methodIdentifier, final JavaType targetEntity, final JavaType idType, final Pair<JavaType, JavaSymbolName>... callerParameters) {
		Assert.isTrue(StringUtils.hasText(callerMID), "Caller's metadata ID required");
		Assert.isTrue(StringUtils.hasText(methodIdentifier), "Method identifier required");
		Assert.notNull(targetEntity, "Target enitity type required");
		Assert.notNull(idType, "Enitity Id type required");
		
		// Look for a repository layer method with this ID and parameter types
		final PairList<JavaType, JavaSymbolName> parameterList = new PairList<JavaType, JavaSymbolName>(callerParameters);
		final List<JavaType> parameterTypes = parameterList.getKeys();
		final RepositoryJpaLayerMethod method = RepositoryJpaLayerMethod.valueOf(methodIdentifier, parameterTypes, targetEntity, idType);
		if (method == null) {
			return null;
		}
		
		// Look for repositories that support this domain type
		final Collection<ClassOrInterfaceTypeDetails> repositories = repositoryLocator.getRepositories(targetEntity);
		if (CollectionUtils.isEmpty(repositories)) {
			return null;
		}
		
		// Use the first such repository (could refine this later)
		final ClassOrInterfaceTypeDetails repository = repositories.iterator().next();
		
		// Return the additions the caller needs to make
		return getMethodAdditions(callerMID, method, repository.getName(), parameterList.getValues());
	}
	
	/**
	 * Returns the additions that the caller needs to make in order to invoke
	 * the given method
	 * 
	 * @param callerMID the caller's metadata ID (required)
	 * @param method the method being called (required)
	 * @param repositoryType the type of repository being called
	 * @param parameterNames the parameter names used by the caller
	 * @return a non-<code>null</code> set of additions
	 */
	private MemberTypeAdditions getMethodAdditions(final String callerMID, final RepositoryJpaLayerMethod method, final JavaType repositoryType, final List<JavaSymbolName> parameterNames) {
		// Create a builder to hold the repository field to be copied into the caller
		final ClassOrInterfaceTypeDetailsBuilder classBuilder = new ClassOrInterfaceTypeDetailsBuilder(callerMID);
		final AnnotationMetadataBuilder autowiredAnnotation = new AnnotationMetadataBuilder(AUTOWIRED);
		final String repositoryFieldName = StringUtils.uncapitalize(repositoryType.getSimpleTypeName());
		classBuilder.addField(new FieldMetadataBuilder(callerMID, 0, Arrays.asList(autowiredAnnotation), new JavaSymbolName(repositoryFieldName), repositoryType).build());
		
		// Create the additions to invoke the given method on this field
		final String methodCall = repositoryFieldName + "." + method.getCall(parameterNames);
		return new MemberTypeAdditions(classBuilder, method.getName(), methodCall);		
	}

	public int getLayerPosition() {
		return LayerType.REPOSITORY.getPosition();
	}
	
	// -------------------- Setters for use by unit tests ----------------------
	
	void setRepositoryLocator(final RepositoryJpaLocator repositoryLocator) {
		this.repositoryLocator = repositoryLocator;
	}
}
