package org.springframework.roo.addon.layers.repository.jpa;

import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.CollectionUtils;
import org.springframework.roo.support.util.PairList;

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

    @Reference private RepositoryJpaLocator repositoryLocator;

    public int getLayerPosition() {
        return LayerType.REPOSITORY.getPosition();
    }

    public MemberTypeAdditions getMemberTypeAdditions(final String callerMID,
            final String methodIdentifier, final JavaType targetEntity,
            final JavaType idType, final MethodParameter... callerParameters) {
        return getMemberTypeAdditions(callerMID, methodIdentifier,
                targetEntity, idType, true, callerParameters);
    }

    public MemberTypeAdditions getMemberTypeAdditions(final String callerMID,
            final String methodIdentifier, final JavaType targetEntity,
            final JavaType idType, boolean autowire,
            final MethodParameter... callerParameters) {
        Validate.isTrue(StringUtils.isNotBlank(callerMID),
                "Caller's metadata ID required");
        Validate.notBlank(methodIdentifier, "Method identifier required");
        Validate.notNull(targetEntity, "Target enitity type required");
        Validate.notNull(idType, "Enitity Id type required");

        // Look for a repository layer method with this ID and parameter types
        final List<JavaType> parameterTypes = new PairList<JavaType, JavaSymbolName>(
                callerParameters).getKeys();
        final RepositoryJpaLayerMethod method = RepositoryJpaLayerMethod
                .valueOf(methodIdentifier, parameterTypes, targetEntity, idType);
        if (method == null) {
            return null;
        }

        // Look for repositories that support this domain type
        final Collection<ClassOrInterfaceTypeDetails> repositories = repositoryLocator
                .getRepositories(targetEntity);
        if (CollectionUtils.isEmpty(repositories)) {
            return null;
        }

        // Use the first such repository (could refine this later)
        final ClassOrInterfaceTypeDetails repository = repositories.iterator()
                .next();

        // Return the additions the caller needs to make
        return getMethodAdditions(callerMID, method, repository.getName(),
                Arrays.asList(callerParameters));
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
    private MemberTypeAdditions getMethodAdditions(final String callerMID,
            final RepositoryJpaLayerMethod method,
            final JavaType repositoryType,
            final List<MethodParameter> parameters) {
        // Create a builder to hold the repository field to be copied into the
        // caller
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                callerMID);
        final AnnotationMetadataBuilder autowiredAnnotation = new AnnotationMetadataBuilder(
                AUTOWIRED);
        final String repositoryFieldName = StringUtils
                .uncapitalize(repositoryType.getSimpleTypeName());
        cidBuilder.addField(new FieldMetadataBuilder(callerMID, 0, Arrays
                .asList(autowiredAnnotation), new JavaSymbolName(
                repositoryFieldName), repositoryType));

        // Create the additions to invoke the given method on this field
        final String methodCall = repositoryFieldName + "."
                + method.getCall(parameters);
        return new MemberTypeAdditions(cidBuilder, method.getName(),
                methodCall, false, parameters);
    }

    // -------------------- Setters for use by unit tests ----------------------

    void setRepositoryLocator(final RepositoryJpaLocator repositoryLocator) {
        this.repositoryLocator = repositoryLocator;
    }
}
