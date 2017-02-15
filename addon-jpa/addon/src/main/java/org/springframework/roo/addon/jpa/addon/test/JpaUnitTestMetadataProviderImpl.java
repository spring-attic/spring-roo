package org.springframework.roo.addon.jpa.addon.test;

import static org.springframework.roo.model.RooJavaType.ROO_JPA_UNIT_TEST;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata;
import org.springframework.roo.addon.jpa.addon.entity.JpaEntityMetadata.RelationInfo;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.itd.AbstractMemberDiscoveringItdMetadataProvider;
import org.springframework.roo.classpath.itd.ItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.metadata.MetadataDependencyRegistry;
import org.springframework.roo.metadata.internal.MetadataDependencyRegistryTracker;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Implementation of {@link JpaUnitTestMetadataProvider}.
 * 
 * @author Sergio Clares
 * @since 2.0
 */
@Component
@Service
public class JpaUnitTestMetadataProviderImpl extends AbstractMemberDiscoveringItdMetadataProvider
    implements JpaUnitTestMetadataProvider {

  // TODO: This class is under construction. It can contain several commented code

  protected MetadataDependencyRegistryTracker registryTracker = null;

  /**
   * This service is being activated so setup it:
   * <ul>
   * <li>Create and open the {@link MetadataDependencyRegistryTracker}.</li>
   * <li>Registers {@link RooJavaType#ROO_JPA_UNIT_TEST} as additional 
   * JavaType that will trigger metadata registration.</li>
   * <li>Set ensure the governor type details represent a class.</li>
   * </ul>
   */
  @Override
  protected void activate(final ComponentContext cContext) {
    super.activate(cContext);
    this.registryTracker =
        new MetadataDependencyRegistryTracker(cContext.getBundleContext(), this,
            PhysicalTypeIdentifier.getMetadataIdentiferType(), getProvidesType());
    this.registryTracker.open();

    addMetadataTrigger(ROO_JPA_UNIT_TEST);
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

    removeMetadataTrigger(ROO_JPA_UNIT_TEST);
  }

  @Override
  protected String createLocalIdentifier(final JavaType javaType, final LogicalPath path) {
    return JpaUnitTestMetadata.createIdentifier(javaType, path);
  }

  @Override
  protected String getGovernorPhysicalTypeIdentifier(final String metadataIdentificationString) {
    final JavaType javaType = JpaUnitTestMetadata.getJavaType(metadataIdentificationString);
    final LogicalPath path = JpaUnitTestMetadata.getPath(metadataIdentificationString);
    return PhysicalTypeIdentifier.createIdentifier(javaType, path);
  }

  @Override
  protected String getLocalMidToRequest(final ItdTypeDetails itdTypeDetails) {
    return getLocalMid(itdTypeDetails);
  }

  @Override
  public String getItdUniquenessFilenameSuffix() {
    return "JpaUnitTest";
  }

  @Override
  protected ItdTypeDetailsProvidingMetadataItem getMetadata(
      final String metadataIdentificationString, final JavaType aspectName,
      final PhysicalTypeMetadata governorPhysicalTypeMetadata, final String itdFilename) {

    final JpaUnitTestAnnotationValues annotationValues =
        new JpaUnitTestAnnotationValues(governorPhysicalTypeMetadata);

    JavaType targetType = annotationValues.getTargetClass();
    Validate.notNull(targetType,
        targetType.getSimpleTypeName().concat(" doesn't exist in the project."));

    // Obtain target type child related entities
    final ClassOrInterfaceTypeDetails cid = getTypeLocationService().getTypeDetails(targetType);
    JpaEntityMetadata entityMetadata =
        getMetadataService().get(JpaEntityMetadata.createIdentifier(cid));
    List<JavaType> relatedEntities = new ArrayList<JavaType>();

    // Add the current entity first
    relatedEntities.add(targetType);
    Map<String, RelationInfo> relationInfos = entityMetadata.getRelationInfos();
    for (RelationInfo relation : relationInfos.values()) {

      // Get child entity type
      JavaType childType = relation.childType;
      if (!relatedEntities.contains(childType)) {
        relatedEntities.add(childType);
      }
    }

    // Get all entity factories for this entity and child related entities
    Set<ClassOrInterfaceTypeDetails> entityFactories =
        getTypeLocationService().findClassesOrInterfaceDetailsWithAnnotation(
            RooJavaType.ROO_JPA_ENTITY_FACTORY);
    Map<JavaType, JavaType> entityAndItsFactoryMap = new TreeMap<JavaType, JavaType>();
    for (ClassOrInterfaceTypeDetails entityFactory : entityFactories) {
      JavaType entity =
          (JavaType) entityFactory.getAnnotation(RooJavaType.ROO_JPA_ENTITY_FACTORY)
              .getAttribute("entity").getValue();
      if (entity != null && relatedEntities.contains(entity)) {
        entityAndItsFactoryMap.put(entity, entityFactory.getType());
      }
    }

    //    final List<FieldMetadata> fieldDependencies = new ArrayList<FieldMetadata>();
    //    final MemberDetails targetTypeDetails =
    //        getMemberDetailsScanner().getMemberDetails(this.getClass().getName(), cid);
    //
    //    // Obtain all governor external field dependencies
    //    List<FieldMetadata> fields = targetTypeDetails.getFields();
    //    for (FieldMetadata field : fields) {
    //      if (field.getAnnotation(JpaJavaType.ONE_TO_ONE) != null
    //          || field.getAnnotation(JpaJavaType.MANY_TO_ONE) != null) {
    //        fieldDependencies.add(field);
    //      }
    //    }
    //
    //    // Obtain all methods of target type
    //    final List<MethodMetadata> targetTypeMethods = targetTypeDetails.getMethods();
    //    final List<MethodMetadata> methods = new ArrayList<MethodMetadata>();
    //    for (MethodMetadata method : targetTypeMethods) {
    //
    //      // Check if method is an accesor or mutator
    //      boolean isAccesorOrMutator = false;
    //      for (FieldMetadata field : fields) {
    //        JavaSymbolName accesorName = BeanInfoUtils.getAccessorMethodName(field);
    //        JavaSymbolName mutatorName = BeanInfoUtils.getMutatorMethodName(field);
    //        if (method.getMethodName().equals(accesorName)
    //            || method.getMethodName().equals(mutatorName)) {
    //          isAccesorOrMutator = true;
    //        }
    //      }
    //
    //      // Only add "custom" methods. Avoid adding accesors, mutators, toString and hashCode
    //      if (!method.getMethodName().equals(TO_STRING) && !method.getMethodName().equals(HASH_CODE)
    //          && !isAccesorOrMutator) {
    //        methods.add(method);
    //      }
    //    }

    return new JpaUnitTestMetadata(metadataIdentificationString, aspectName,
        governorPhysicalTypeMetadata, annotationValues, relationInfos.values(),
        entityAndItsFactoryMap);
  }

  public String getProvidesType() {
    return JpaUnitTestMetadata.getMetadataIdentiferType();
  }

}
