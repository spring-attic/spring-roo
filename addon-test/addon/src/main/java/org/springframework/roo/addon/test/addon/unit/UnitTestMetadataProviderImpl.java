package org.springframework.roo.addon.test.addon.unit;

import static org.springframework.roo.model.RooJavaType.*;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.*;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.itd.*;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.*;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link AuditMetadataProvider}.
 * <p/>
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class UnitTestMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements UnitTestMetadataProvider {

  protected MetadataDependencyRegistryTracker registryTracker = null;
  private final JavaSymbolName TO_STRING = new JavaSymbolName("toString");
  private final JavaSymbolName HASH_CODE = new JavaSymbolName("hashCode");

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_UNIT_TEST} as additional 
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    context = cContext.getBundleContext();
    super.setDependsOnGovernorBeingAClass(true);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(context, this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_UNIT_TEST);
  }

  /**
   * This service is being deactivated so unregister upstream-downstream 
   * dependencies, triggers, matchers and listeners.
   * 
   * @param context
   */
  protected void deactivate(final ComponentContext context) {
    MetadataDependencyRegistry registry = this.registryTracker.getService();
    registry.removeNotificationListener(this);
    registry.deregisterDependency(PhysicalTypeIdentifier.getMetadataIdentiferType(),
        getProvidesType());
    this.registryTracker.close();

    removeMetadataTrigger(ROO_UNIT_TEST);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return UnitTestMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = UnitTestMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = UnitTestMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "UnitTest";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    final UnitTestAnnotationValues annotationValues =
        new UnitTestAnnotationValues(governorPhysicalTypeMetadata);

    JavaType targetType = annotationValues.getTargetClass();
    Validate.notNull(targetType,
        targetType.getSimpleTypeName().concat(" doesn't exist in the project."));

    // Obtain target type complete details
    final ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(targetType);
    final List<FieldMetadata> fieldDependencies = new ArrayList<FieldMetadata>();
    final MemberDetails targetTypeDetails =
        getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), cid);

    // Obtain all governor external field dependencies
    List<FieldMetadata> fields = targetTypeDetails.getFields();
    for (FieldMetadata field : fields) {
      if (field.getAnnotation(JpaJavaType.ONE_TO_ONE) != null
          || field.getAnnotation(JpaJavaType.MANY_TO_ONE) != null) {
        fieldDependencies.add(field);
      }
    }

    // Obtain all methods of target type
    final List<MethodMetadata> targetTypeMethods = targetTypeDetails.getMethods();
    final List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
    for (MethodMetadata method : targetTypeMethods) {

      // Check if method is an accesor or mutator
      boolean isAccesorOrMutator = false;
      for (FieldMetadata field : fields) {
        JavaSymbolName accesorName = BeanInfoUtils.getAccessorMethodName(field);
        JavaSymbolName mutatorName = BeanInfoUtils.getMutatorMethodName(field);
        if (method.getMethodName().equals(accesorName)
            || method.getMethodName().equals(mutatorName)) {
          isAccesorOrMutator = true;
        }
      }

      // Only add "custom" methods. Avoid adding accesors, mutators, toString and hashCode
      if (!method.getMethodName().equals(TO_STRING) && !method.getMethodName().equals(HASH_CODE)
          && !isAccesorOrMutator) {
        methods.add(method);
      }
    }

    return new UnitTestMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, fieldDependencies, methods, cid,
        getMemberDetails(cid));
  }

  public String getProvidesType() {
    return UnitTestMetadata.getMetadataIdentiferType();
  }

}
