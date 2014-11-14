package org.springframework.roo.addon.layers.service;

import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.model.SpringJavaType.AUTOWIRED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

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

import org.osgi.service.component.ComponentContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;

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
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(ServiceLayerProvider.class);
	
	// ------------ OSGi component attributes ----------------
   	private BundleContext context;

    private MetadataService metadataService;
    private ServiceAnnotationValuesFactory serviceAnnotationValuesFactory;
    private ServiceInterfaceLocator serviceInterfaceLocator;
    TypeLocationService typeLocationService;
    
    protected void activate(final ComponentContext context) {
    	this.context = context.getBundleContext();
    }

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
    	
    	if(metadataService == null){
    		metadataService = getMetadataService();
    	}
    	
    	Validate.notNull(metadataService, "MetadataService is required");
    	
    	if(serviceAnnotationValuesFactory == null){
    		serviceAnnotationValuesFactory = getServiceAnnotationValuesFactory();
    	}
    	
    	Validate.notNull(serviceAnnotationValuesFactory, "ServiceAnnotationValuesFactory is required");
    	
    	if(serviceInterfaceLocator == null){
    		serviceInterfaceLocator = getServiceInterfaceLocator();
    	}

    	Validate.notNull(serviceInterfaceLocator, "ServiceInterfaceLocator is required");
    	
    	if(typeLocationService == null){
    		typeLocationService = getTypeLocationService();
    	}
    	
    	Validate.notNull(typeLocationService, "TypeLocationService is required");
    	
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
    	if(metadataService == null){
    		this.metadataService = getMetadataService();
    		
    		Validate.notNull(metadataService, "MetadataService is required");
    		
    	}else{
    		this.metadataService = metadataService;
    	}
    }

    void setServiceAnnotationValuesFactory(
            final ServiceAnnotationValuesFactory serviceAnnotationValuesFactory) {
    	if(serviceAnnotationValuesFactory == null){
    		this.serviceAnnotationValuesFactory = getServiceAnnotationValuesFactory();
    		Validate.notNull(serviceAnnotationValuesFactory, "ServiceAnnotationValuesFactory is required");
    	}else{
    		this.serviceAnnotationValuesFactory = serviceAnnotationValuesFactory;
    	}
    	
    }

    void setServiceInterfaceLocator(
            final ServiceInterfaceLocator serviceInterfaceLocator) {
    	if(serviceInterfaceLocator == null){
    		this.serviceInterfaceLocator = getServiceInterfaceLocator();
    		Validate.notNull(serviceInterfaceLocator, "ServiceInterfaceLocator is required");
    	}else{
    		this.serviceInterfaceLocator = serviceInterfaceLocator;
    	}

    }
    
    public MetadataService getMetadataService(){
    	// Get all Services implement MetadataService interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(MetadataService.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (MetadataService) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load MetadataService on ServiceLayerProvider.");
			return null;
		}
    }
    
    public ServiceAnnotationValuesFactory getServiceAnnotationValuesFactory(){
    	// Get all Services implement ServiceAnnotationValuesFactory interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(ServiceAnnotationValuesFactory.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (ServiceAnnotationValuesFactory) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load ServiceAnnotationValuesFactory on ServiceLayerProvider.");
			return null;
		}
    }
    
    public ServiceInterfaceLocator getServiceInterfaceLocator(){
    	// Get all Services implement ServiceInterfaceLocator interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(ServiceInterfaceLocator.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (ServiceInterfaceLocator) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load ServiceInterfaceLocator on ServiceLayerProvider.");
			return null;
		}
    }
    
    public TypeLocationService getTypeLocationService(){
    	// Get all Services implement TypeLocationService interface
		try {
			ServiceReference<?>[] references = this.context.getAllServiceReferences(TypeLocationService.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (TypeLocationService) this.context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load TypeLocationService on ServiceLayerProvider.");
			return null;
		}
    }
    
    
}
