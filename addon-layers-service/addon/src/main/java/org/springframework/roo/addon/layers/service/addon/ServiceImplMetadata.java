package org.springframework.roo.addon.layers.service.addon;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.addon.layers.service.annotations.RooServiceImpl;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.SpringJavaType;
import org.springframework.roo.project.LogicalPath;

/**
 * Metadata for {@link RooServiceImpl}.
 * 
 * @author Juan Carlos García
 * @since 2.0
 */
public class ServiceImplMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {

    private static final String PROVIDES_TYPE_STRING = ServiceImplMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);
    
    private ImportRegistrationResolver importResolver;

    public static String createIdentifier(final JavaType javaType,
            final LogicalPath path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(
                PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(
                PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static String getMetadataIdentiferType() {
        return PROVIDES_TYPE;
    }

    public static LogicalPath getPath(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    public static boolean isValid(final String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING,
                metadataIdentificationString);
    }

    /**
     * Constructor
     * 
     * @param identifier the identifier for this item of metadata (required)
     * @param aspectName the Java type of the ITD (required)
     * @param governorPhysicalTypeMetadata the governor, which is expected to
     *            contain a {@link ClassOrInterfaceTypeDetails} (required)
     * @param serviceInterface the JavaType of service interface
     * @param repository the javatype of related repository
     * @param readOnly boolean that specifies if related entity is readOnly or not
     */
    public ServiceImplMetadata(final String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final JavaType serviceInterface,
            final ClassOrInterfaceTypeDetails repository,
            final JavaType entity,
            final JavaType identifierType,
            final boolean readOnly) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        
        Validate.notNull(serviceInterface, "ERROR: Service interface required");
        
        this.importResolver = builder.getImportRegistrationResolver();
        
        // Get service that needs to be implemented
        ensureGovernorImplements(serviceInterface);
        
        // All services should include @Service annotation
        AnnotationMetadataBuilder serviceAnnotation = new AnnotationMetadataBuilder(
                SpringJavaType.SERVICE);
        builder.addAnnotation(serviceAnnotation);
        
        // If exists a repository related with managed entity
        if(repository != null){
            // All service related with repository should be generated with
            // @Transactional(readOnly = true) annotation
            AnnotationMetadataBuilder transactionalAnnotation = new AnnotationMetadataBuilder(
                    SpringJavaType.TRANSACTIONAL);
            transactionalAnnotation.addBooleanAttribute("readOnly", true);
            builder.addAnnotation(transactionalAnnotation);
            
            // Services should include repository field if there's
            // a repository related with managed entity
            FieldMetadataBuilder repositoryFieldMetadata = new FieldMetadataBuilder(
                    getId(), Modifier.PUBLIC,
                    new ArrayList<AnnotationMetadataBuilder>(),
                    new JavaSymbolName("repository"), repository.getType());
            builder.addField(repositoryFieldMetadata);
        }
        
        // All services should include constructor
        builder.addConstructor(getServiceConstructor(repository));
        
        
        // Implements readOnly methods for every services
        builder.addMethod(getFindAllMethod(entity, repository.getType()));
        builder.addMethod(getFindAllIterableMethod(entity, identifierType, repository.getType()));
        builder.addMethod(getFindOneMethod(entity, identifierType, repository.getType()));
        
        // Generating persistent methods for not readOnly entities
        if(!readOnly){
            builder.addMethod(getSaveMethod(entity, repository.getType()));
            builder.addMethod(getDeleteMethod(identifierType, repository.getType()));
            builder.addMethod(getSaveBatchMethod(entity, repository.getType()));
            builder.addMethod(getDeleteBatchMethod(entity, identifierType, repository.getType()));
        }
        
        // Build the ITD
        itdTypeDetails = builder.build();
    }
    
    /**
     * Method that generates Service implementation constructor. If exists a
     * repository, it will be included as constructor parameter
     * 
     * @param repository
     * @return
     */
    private ConstructorMetadata getServiceConstructor(
            ClassOrInterfaceTypeDetails repository) {

        ConstructorMetadataBuilder constructorBuilder = new ConstructorMetadataBuilder(
                getId());
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();

        // Append repository parameter if needed
        if (repository != null) {
            constructorBuilder.addParameter("repository", repository.getType());
            bodyBuilder.appendFormalLine("this.repository = repository;");
        }

        constructorBuilder.setBodyBuilder(bodyBuilder);
        
        // Adding @Autowired annotation
        constructorBuilder.addAnnotation(
                new AnnotationMetadataBuilder(SpringJavaType.AUTOWIRED));

        return constructorBuilder.build();
    }

    /**
     * Method that generates method "findAll" method.
     * 
     * @param entity
     * @param repository
     * @return MethodMetadataBuilder with public List <Entity> findAll();
     *         structure
     */
    private MethodMetadataBuilder getFindAllMethod(JavaType entity, JavaType repository) {
        // Define method parameter types
        List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();

        // Define method parameter names
        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

        JavaType listEntityJavaType = new JavaType("java.util.List", 0,
                DataType.TYPE, null, Arrays.asList(entity));
        
        // Generate body
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        
        if(repository != null){
            bodyBuilder.appendFormalLine("return repository.findAll();");
        }else{
            bodyBuilder.appendFormalLine("// TO BE IMPLEMENTED BY DEVELOPER");
        }
        
        // Use the MethodMetadataBuilder for easy creation of MethodMetadata
        MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(),
                Modifier.PUBLIC, new JavaSymbolName("findAll"),
                listEntityJavaType, parameterTypes, parameterNames, bodyBuilder);

        return methodBuilder; // Build and return a MethodMetadata
        // instance
    }
    
    /**
     * Method that generates method "findAll" with iterable parameter.
     * 
     * @param entity
     * @param identifierType
     * @param repository
     * @return MethodMetadataBuilder with public List <Entity> findAll(Iterable
     *         <Long> ids) structure
     */
    private MethodMetadataBuilder getFindAllIterableMethod(JavaType entity,
            JavaType identifierType, JavaType repository) {
        // Define method parameter types
        List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        parameterTypes.add(AnnotatedJavaType
                .convertFromJavaType(new JavaType("java.lang.Iterable", 0,
                        DataType.TYPE, null, Arrays.asList(identifierType))));

        // Define method parameter names
        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        parameterNames.add(new JavaSymbolName("ids"));


        JavaType listEntityJavaType = new JavaType("java.util.List", 0,
                DataType.TYPE, null, Arrays.asList(entity));
        
        // Generate body
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        
        if(repository != null){
            bodyBuilder.appendFormalLine("return repository.findAll(ids);");
        }else{
            bodyBuilder.appendFormalLine("// TO BE IMPLEMENTED BY DEVELOPER");
        }

        // Use the MethodMetadataBuilder for easy creation of MethodMetadata
        MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(),
                Modifier.PUBLIC, new JavaSymbolName("findAll"),
                listEntityJavaType, parameterTypes, parameterNames,
                bodyBuilder);

        return methodBuilder; // Build and return a MethodMetadata
        // instance
    }
    
    
    /**
     * Method that generates method "findOne".
     * 
     * @param entity
     * @param identifierType
     * @param repository
     * @return MethodMetadataBuilder with public Entity findOne(Long id);
     *         structure
     */
    private MethodMetadataBuilder getFindOneMethod(JavaType entity,
            JavaType identifierType, JavaType repository) {
        // Define method parameter types
        List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        parameterTypes
                .add(AnnotatedJavaType.convertFromJavaType(identifierType));

        // Define method parameter names
        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        parameterNames.add(new JavaSymbolName("id"));
        
        // Generate body
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        
        if(repository != null){
            bodyBuilder.appendFormalLine("return repository.findOne(id);");
        }else{
            bodyBuilder.appendFormalLine("// TO BE IMPLEMENTED BY DEVELOPER");
        }

        // Use the MethodMetadataBuilder for easy creation of MethodMetadata
        MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(),
                Modifier.PUBLIC, new JavaSymbolName("findOne"), entity,
                parameterTypes, parameterNames, bodyBuilder);

        return methodBuilder; // Build and return a MethodMetadata
        // instance
    }
    
    /**
     * Method that generates "save" method.
     * 
     * @param entity
     * @param repository
     * @return MethodMetadataBuilder with public Entity save(Entity entity);
     *         structure
     */
    private MethodMetadataBuilder getSaveMethod(JavaType entity, JavaType repository) {
        // Define method parameter types
        List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        parameterTypes.add(AnnotatedJavaType.convertFromJavaType(entity));

        // Define method parameter names
        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        parameterNames.add(new JavaSymbolName("entity"));
        
        // Generate body
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        
        if(repository != null){
            bodyBuilder.appendFormalLine("return repository.save(entity);");
        }else{
            bodyBuilder.appendFormalLine("// TO BE IMPLEMENTED BY DEVELOPER");
        }

        // Use the MethodMetadataBuilder for easy creation of MethodMetadata
        MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(),
                Modifier.PUBLIC, new JavaSymbolName("save"), entity,
                parameterTypes, parameterNames, bodyBuilder);
        
        // save method should be defined with @Transactional(readOnly = false)
        AnnotationMetadataBuilder transactionalAnnotation = new AnnotationMetadataBuilder(
                SpringJavaType.TRANSACTIONAL);
        transactionalAnnotation.addBooleanAttribute("readOnly", false);
        methodBuilder.addAnnotation(transactionalAnnotation);
        
        return methodBuilder; // Build and return a MethodMetadata
        // instance
    }
    
    /**
     * Method that generates "delete" method.
     * 
     * @param identifierType
     * @param repository
     * @return MethodMetadataBuilder with public void delete(Long id); structure
     */
    private MethodMetadataBuilder getDeleteMethod(JavaType identifierType, JavaType repository) {
        // Define method parameter types
        List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        parameterTypes
                .add(AnnotatedJavaType.convertFromJavaType(identifierType));

        // Define method parameter names
        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        parameterNames.add(new JavaSymbolName("id"));
        
        // Generate body
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        
        if(repository != null){
            bodyBuilder.appendFormalLine("repository.delete(id);");
        }else{
            bodyBuilder.appendFormalLine("// TO BE IMPLEMENTED BY DEVELOPER");
        }

        // Use the MethodMetadataBuilder for easy creation of MethodMetadata
        MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(),
                Modifier.PUBLIC, new JavaSymbolName("delete"),
                JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, bodyBuilder);
        
        // delete method should be defined with @Transactional(readOnly = false)
        AnnotationMetadataBuilder transactionalAnnotation = new AnnotationMetadataBuilder(
                SpringJavaType.TRANSACTIONAL);
        transactionalAnnotation.addBooleanAttribute("readOnly", false);
        methodBuilder.addAnnotation(transactionalAnnotation);

        return methodBuilder; // Build and return a MethodMetadata
        // instance
    }
    
    /**
     * Method that generates "save" batch method.
     * 
     * @param entity
     * @param repository
     * @return MethodMetadataBuilder with public List<Entity> save(Iterable
     *         <Entity> entities); structure
     */
    private MethodMetadataBuilder getSaveBatchMethod(JavaType entity, JavaType repository) {
        // Define method parameter types
        List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        parameterTypes.add(AnnotatedJavaType
                .convertFromJavaType(new JavaType("java.lang.Iterable", 0,
                        DataType.TYPE, null, Arrays.asList(entity))));

        // Define method parameter names
        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        parameterNames.add(new JavaSymbolName("entities"));

        JavaType listEntityJavaType = new JavaType("java.util.List", 0,
                DataType.TYPE, null, Arrays.asList(entity));
        
        // Generate body
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        
        if(repository != null){
            bodyBuilder.appendFormalLine("return repository.save(entities);");
        }else{
            bodyBuilder.appendFormalLine("// TO BE IMPLEMENTED BY DEVELOPER");
        }

        // Use the MethodMetadataBuilder for easy creation of MethodMetadata
        MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(),
                Modifier.PUBLIC, new JavaSymbolName("save"), listEntityJavaType,
                parameterTypes, parameterNames, bodyBuilder);
        
        // save batch method should be defined with @Transactional(readOnly = false)
        AnnotationMetadataBuilder transactionalAnnotation = new AnnotationMetadataBuilder(
                SpringJavaType.TRANSACTIONAL);
        transactionalAnnotation.addBooleanAttribute("readOnly", false);
        methodBuilder.addAnnotation(transactionalAnnotation);

        return methodBuilder; // Build and return a MethodMetadata
        // instance
    }
    
    /**
     * Method that generates "delete" batch method
     * 
     * @param entityTyoe
     * @param identifierType
     * @param repository
     * @return MethodMetadataBuilder with public void delete(Iterable
     *         <Long> ids); structure
     */
    private MethodMetadataBuilder getDeleteBatchMethod(JavaType entityType,
            JavaType identifierType, JavaType repository) {
        // Define method parameter types
        List<AnnotatedJavaType> parameterTypes = new ArrayList<AnnotatedJavaType>();
        parameterTypes.add(AnnotatedJavaType
                .convertFromJavaType(new JavaType("java.lang.Iterable", 0,
                        DataType.TYPE, null, Arrays.asList(identifierType))));

        // Define method parameter names
        List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();
        parameterNames.add(new JavaSymbolName("ids"));
        
        // Generate body
        InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
        
        if(repository != null){
            bodyBuilder.appendFormalLine(String.format(
                    "List<%s> toDelete = repository.findAll(ids);",
                    entityType.getSimpleTypeName()));
            bodyBuilder.appendFormalLine("repository.deleteInBatch(toDelete);");
        }else{
            bodyBuilder.appendFormalLine("// TO BE IMPLEMENTED BY DEVELOPER");
        }

        // Use the MethodMetadataBuilder for easy creation of MethodMetadata
        MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(),
                Modifier.PUBLIC, new JavaSymbolName("delete"),
                JavaType.VOID_PRIMITIVE, parameterTypes, parameterNames, bodyBuilder);
        
        // delete batch method should be defined with @Transactional(readOnly = false)
        AnnotationMetadataBuilder transactionalAnnotation = new AnnotationMetadataBuilder(
                SpringJavaType.TRANSACTIONAL);
        transactionalAnnotation.addBooleanAttribute("readOnly", false);
        methodBuilder.addAnnotation(transactionalAnnotation);

        return methodBuilder; // Build and return a MethodMetadata
        // instance
    }
    
    @Override
    public String toString() {
        final ToStringBuilder builder = new ToStringBuilder(this);
        builder.append("identifier", getId());
        builder.append("valid", valid);
        builder.append("aspectName", aspectName);
        builder.append("destinationType", destination);
        builder.append("governor", governorPhysicalTypeMetadata.getId());
        builder.append("itdTypeDetails", itdTypeDetails);
        return builder.toString();
    }
}
