package org.springframework.roo.addon.web.mvc.controller.finder;

import static org.springframework.roo.model.RooJavaType.ROO_WEB_FINDER;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Logger;

import org.apache.commons.lang3.Validate;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.web.mvc.controller.details.DateTimeFormatDetails;
import org.springframework.roo.addon.web.mvc.controller.details.FinderMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.JavaTypeMetadataDetails;
import org.springframework.roo.addon.web.mvc.controller.details.WebMetadataService;
import org.springframework.roo.addon.web.mvc.controller.scaffold.WebScaffoldAnnotationValues;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.itd.AbstractItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.springframework.roo.support.logging.HandlerUtils;


/**
 * Implementation of {@link WebFinderMetadataProvider}.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
@Component
@Service
public class WebFinderMetadataProviderImpl extends AbstractItdMetadataProvider
        implements WebFinderMetadataProvider {
	
	protected final static Logger LOGGER = HandlerUtils.getLogger(WebFinderMetadataProviderImpl.class);
	
    private WebMetadataService webMetadataService;

    protected void activate(final ComponentContext cContext) {
    	context = cContext.getBundleContext();
        getMetadataDependencyRegistry().registerDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        addMetadataTrigger(ROO_WEB_FINDER);
    }

    @Override
    protected String createLocalIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return WebFinderMetadata.createIdentifier(javaType, path);
    }

    protected void deactivate(final ComponentContext context) {
        getMetadataDependencyRegistry().deregisterDependency(
                PhysicalTypeIdentifier.getMetadataIdentiferType(),
                getProvidesType());
        removeMetadataTrigger(ROO_WEB_FINDER);
    }

    @Override
    protected String getGovernorPhysicalTypeIdentifier(
            final String metadataIdentificationString) {
        final JavaType javaType = WebFinderMetadata
                .getJavaType(metadataIdentificationString);
        final LogicalPath path = WebFinderMetadata
                .getPath(metadataIdentificationString);
        return PhysicalTypeIdentifier.createIdentifier(javaType, path);
    }

    public String getItdUniquenessFilenameSuffix() {
        return "Controller_Finder";
    }

    @Override
    protected ItdTypeDetailsProvidingMetadataItem getMetadata(
            final String metadataIdentificationString,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final String itdFilename) {
    	
    	if(webMetadataService == null){
    		webMetadataService = getWebMetadataService();
    	}
    	Validate.notNull(webMetadataService, "WebMetadataService is required");
    	
        // We need to parse the annotation, which we expect to be present
        final WebScaffoldAnnotationValues annotationValues = new WebScaffoldAnnotationValues(
                governorPhysicalTypeMetadata);
        if (!annotationValues.isAnnotationFound()
                || !annotationValues.isExposeFinders()
                || annotationValues.getFormBackingObject() == null
                || governorPhysicalTypeMetadata.getMemberHoldingTypeDetails() == null) {
            return null;
        }

        // Lookup the form backing object's metadata
        final JavaType formBackingType = annotationValues
                .getFormBackingObject();
        final ClassOrInterfaceTypeDetails formBackingTypeDetails = getTypeLocationService()
                .getTypeDetails(formBackingType);
        if (formBackingTypeDetails == null
                || !formBackingTypeDetails.getCustomData().keySet()
                        .contains(CustomDataKeys.PERSISTENT_TYPE)) {
            return null;
        }

        // We need to be informed if our dependent metadata changes
        getMetadataDependencyRegistry().registerDependency(
                formBackingTypeDetails.getDeclaredByMetadataId(),
                metadataIdentificationString);

        final MemberDetails formBackingObjectMemberDetails = getMemberDetails(formBackingTypeDetails);
        final Set<FinderMetadataDetails> dynamicFinderMethods = webMetadataService
                .getDynamicFinderMethodsAndFields(formBackingType,
                        formBackingObjectMemberDetails,
                        metadataIdentificationString);
        if (dynamicFinderMethods == null) {
            return null;
        }

        
        final SortedMap<JavaType, JavaTypeMetadataDetails> relatedApplicationTypeMetadata = webMetadataService
                .getRelatedApplicationTypeMetadata(formBackingType,
                        formBackingObjectMemberDetails,
                        metadataIdentificationString);
        
        final Map<JavaSymbolName, DateTimeFormatDetails> datePatterns = webMetadataService
                .getDatePatterns(formBackingType,
                        formBackingObjectMemberDetails,
                        metadataIdentificationString);

        return new WebFinderMetadata(metadataIdentificationString, aspectName,
                governorPhysicalTypeMetadata, annotationValues,
                relatedApplicationTypeMetadata, dynamicFinderMethods, datePatterns);
    }

    public String getProvidesType() {
        return WebFinderMetadata.getMetadataIdentiferType();
    }
    
    public WebMetadataService getWebMetadataService(){
    	// Get all Services implement WebMetadataService interface
		try {
			ServiceReference<?>[] references = context.getAllServiceReferences(WebMetadataService.class.getName(), null);
			
			for(ServiceReference<?> ref : references){
				return (WebMetadataService) context.getService(ref);
			}
			
			return null;
			
		} catch (InvalidSyntaxException e) {
			LOGGER.warning("Cannot load WebMetadataService on WebFinderMetadataProviderImpl.");
			return null;
		}
    }
}