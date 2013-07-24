package org.springframework.roo.addon.layers.service;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.plural.PluralMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.CoreLayerProvider;
import org.springframework.roo.classpath.layers.LayerType;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.classpath.layers.MethodParameter;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.PairList;

/**
 * The {@link org.springframework.roo.classpath.layers.LayerProvider} that
 * provides an application's service layer.
 * 
 * @author Stefan Schmidt
 * @author Andrew Swan
 * @since 1.2.0
 */
@Component
@Service
public class ServiceLayerProvider extends CoreLayerProvider {

    @Reference private MetadataService metadataService;
    @Reference private ServiceAnnotationValuesFactory serviceAnnotationValuesFactory;
    @Reference private ServiceInterfaceLocator serviceInterfaceLocator;
    @Reference TypeLocationService typeLocationService;

    public int getLayerPosition() {
        return LayerType.SERVICE.getPosition();
    }

    public MemberTypeAdditions getMemberTypeAdditions(final String callerMID,
            final String methodIdentifier, final JavaType targetEntity,
            final JavaType idType, final MethodParameter... methodParameters) {
        return getMemberTypeAdditions(callerMID, methodIdentifier,
                targetEntity, idType, true, methodParameters);
    }

    public MemberTypeAdditions getMemberTypeAdditions(final String callerMID,
            final String methodIdentifier, final JavaType targetEntity,
            final JavaType idType, boolean autowire,
            final MethodParameter... methodParameters) {
        Validate.notBlank(callerMID, "Caller's metadata identifier required");
        Validate.notNull(methodIdentifier, "Method identifier required");
        Validate.notNull(targetEntity, "Target entity type required");
        Validate.notNull(methodParameters,
                "Method param names and types required (may be empty)");

        // Check whether this is even a known service layer method
        final List<JavaType> parameterTypes = new PairList<JavaType, JavaSymbolName>(
                methodParameters).getKeys();
        final ServiceLayerMethod method = ServiceLayerMethod.valueOf(
                methodIdentifier, parameterTypes, targetEntity, idType);
        if (method == null) {
            return null;
        }

        // Check the entity has a plural form
        final String pluralId = PluralMetadata.createIdentifier(targetEntity,
                typeLocationService.getTypePath(targetEntity));
        final PluralMetadata pluralMetadata = (PluralMetadata) metadataService
                .get(pluralId);
        if (pluralMetadata == null || pluralMetadata.getPlural() == null) {
            return null;
        }

        // Loop through the service interfaces that claim to support the given
        // target entity
        for (final ClassOrInterfaceTypeDetails serviceInterface : serviceInterfaceLocator
                .getServiceInterfaces(targetEntity)) {
            // Get the values of the @RooService annotation for this service
            // interface
            final ServiceAnnotationValues annotationValues = serviceAnnotationValuesFactory
                    .getInstance(serviceInterface);
            if (annotationValues != null) {

                // Check whether this method is implemented by the given service
                final String methodName = method.getName(annotationValues,
                        targetEntity, pluralMetadata.getPlural());
                if (StringUtils.isNotBlank(methodName)) {
                    // The service implements the method; get the additions to
                    // be made by the caller
                    final MemberTypeAdditions methodAdditions = getMethodAdditions(
                            callerMID, methodName, serviceInterface.getName(),
                            Arrays.asList(methodParameters), autowire);

                    // Return these additions
                    return methodAdditions;
                }
            }
        }
        // None of the services for this entity were able to provide the method
        return null;
    }

    /**
     * Returns the additions the caller should make in order to invoke the given
     * method for the given domain entity.
     * 
     * @param callerMID the caller's metadata ID (required)
     * @param methodName the name of the method being invoked (required)
     * @param serviceInterface the domain service type (required)
     * @param parameterNames the names of the parameters being passed by the
     *            caller to the method
     * @return a non-<code>null</code> set of additions
     */
    private MemberTypeAdditions getMethodAdditions(final String callerMID,
            final String methodName, final JavaType serviceInterface,
            final List<MethodParameter> parameters, boolean autowire) {
        // The method is supported by this service interface; make a builder
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                callerMID);

        final String fieldName = StringUtils.uncapitalize(serviceInterface
                .getSimpleTypeName());

        if (autowire) {
            // Add an autowired field of the type of this service
            cidBuilder.addField(new FieldMetadataBuilder(callerMID, 0, Arrays
                    .asList(new AnnotationMetadataBuilder(AUTOWIRED)),
                    new JavaSymbolName(fieldName), serviceInterface));
        }
        else {
            // Add a set method of the type of this service
            cidBuilder.addField(new FieldMetadataBuilder(callerMID, 0,
                    new JavaSymbolName(fieldName), serviceInterface, null));
            JavaSymbolName setMethodName = new JavaSymbolName("set"
                    + serviceInterface.getSimpleTypeName());
            List<JavaType> parameterTypes = new ArrayList<JavaType>();
            parameterTypes.add(serviceInterface);
            List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
            parameterNames.add(new JavaSymbolName(fieldName));
            final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
            bodyBuilder.append("\n\tthis." + fieldName + " = " + fieldName
                    + ";\n");

            MethodMetadataBuilder setSeviceMethod = new MethodMetadataBuilder(
                    callerMID, PUBLIC, setMethodName, JavaType.VOID_PRIMITIVE,
                    AnnotatedJavaType.convertFromJavaTypes(parameterTypes),
                    parameterNames, bodyBuilder);

            cidBuilder.addMethod(setSeviceMethod);
        }

        // Generate an additions object that includes a call to the method
        return MemberTypeAdditions.getInstance(cidBuilder, fieldName,
                methodName, false, parameters);
    }

    // -------------------- Setters for use by unit tests ----------------------

    void setMetadataService(final MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    void setServiceAnnotationValuesFactory(
            final ServiceAnnotationValuesFactory serviceAnnotationValuesFactory) {
        this.serviceAnnotationValuesFactory = serviceAnnotationValuesFactory;
    }

    void setServiceInterfaceLocator(
            final ServiceInterfaceLocator serviceInterfaceLocator) {
        this.serviceInterfaceLocator = serviceInterfaceLocator;
    }
}
