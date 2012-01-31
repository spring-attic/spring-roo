package org.springframework.roo.addon.equals;

import static org.springframework.roo.model.RooJavaType.ROO_EQUALS;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.CollectionUtils;

/**
 * Implementation of {@link EqualsOperations}.
 * 
 * @author Alan Stewart
 * @since 1.2.0
 */
@Component
@Service
public class EqualsOperationsImpl implements EqualsOperations {

    @Reference private TypeLocationService typeLocationService;
    @Reference private TypeManagementService typeManagementService;

    public void addEqualsAndHashCodeMethods(final JavaType javaType,
            final boolean appendSuper, final Set<String> excludeFields) {
        // Add @RooEquals annotation to class if not yet present
        final ClassOrInterfaceTypeDetails cid = typeLocationService
                .getTypeDetails(javaType);
        if (cid == null || cid.getTypeAnnotation(ROO_EQUALS) != null) {
            return;
        }

        final AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(
                ROO_EQUALS);
        if (appendSuper) {
            annotationBuilder.addBooleanAttribute("appendSuper", appendSuper);
        }
        if (!CollectionUtils.isEmpty(excludeFields)) {
            final List<StringAttributeValue> attributes = new ArrayList<StringAttributeValue>();
            for (final String excludeField : excludeFields) {
                attributes.add(new StringAttributeValue(new JavaSymbolName(
                        "value"), excludeField));
            }
            annotationBuilder
                    .addAttribute(new ArrayAttributeValue<StringAttributeValue>(
                            new JavaSymbolName("excludeFields"), attributes));
        }

        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                cid);
        cidBuilder.addAnnotation(annotationBuilder.build());
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }
}
