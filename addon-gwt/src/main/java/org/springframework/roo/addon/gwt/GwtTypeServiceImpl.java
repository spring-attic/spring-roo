package org.springframework.roo.addon.gwt;

import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JdkJavaType.BIG_DECIMAL;
import static org.springframework.roo.model.JdkJavaType.DATE;
import static org.springframework.roo.model.JdkJavaType.LIST;
import static org.springframework.roo.model.JdkJavaType.SET;
import static org.springframework.roo.model.JpaJavaType.EMBEDDABLE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.gwt.scaffold.GwtScaffoldMetadata;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.AbstractIdentifiableAnnotatedJavaStructureBuilder;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.IdentifiableAnnotatedJavaStructure;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaPackage;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.RooJavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.FileUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Provides a basic implementation of {@link GwtTypeService}.
 * 
 * @author James Tyrrell
 * @since 1.1.2
 */
@Component
@Service
public class GwtTypeServiceImpl implements GwtTypeService {

    private static final Logger LOGGER = HandlerUtils
            .getLogger(GwtTypeServiceImpl.class);
    private static final String PATH = "path";

    @Reference private FileManager fileManager;
    @Reference private GwtFileManager gwtFileManager;
    @Reference private MemberDetailsScanner memberDetailsScanner;
    @Reference private MetadataService metadataService;
    @Reference private PersistenceMemberLocator persistenceMemberLocator;
    @Reference private ProjectOperations projectOperations;
    @Reference private TypeLocationService typeLocationService;

    private final Set<String> warnings = new LinkedHashSet<String>();
    private final Timer warningTimer = new Timer();

    public void addSourcePath(final String sourcePath, final String moduleName) {
        final String gwtXmlPath = getGwtModuleXml(moduleName);
        Validate.notBlank(gwtXmlPath, "gwt.xml could not be found for module '"
                + moduleName + "'");
        final Document gwtXmlDoc = getGwtXmlDocument(gwtXmlPath);
        final Element gwtXmlRoot = gwtXmlDoc.getDocumentElement();
        final List<Element> sourceElements = XmlUtils.findElements(
                "/module/source", gwtXmlRoot);
        if (!anyExistingSourcePathsIncludePath(sourcePath, sourceElements)) {
            final Element firstSourceElement = sourceElements.get(0);
            final Element newSourceElement = gwtXmlDoc.createElement("source");
            newSourceElement.setAttribute(PATH, sourcePath);
            gwtXmlRoot.insertBefore(newSourceElement, firstSourceElement);
            fileManager.createOrUpdateTextFileIfRequired(gwtXmlPath,
                    XmlUtils.nodeToString(gwtXmlDoc),
                    "Added source paths to gwt.xml file", true);
        }
    }

    private boolean anyExistingSourcePathsIncludePath(final String sourcePath,
            final Iterable<Element> sourceElements) {
        for (final Element sourceElement : sourceElements) {
            if (sourcePath.startsWith(sourceElement.getAttribute(PATH))) {
                return true;
            }
        }
        return false;
    }

    public List<ClassOrInterfaceTypeDetails> buildType(final GwtType destType,
            final ClassOrInterfaceTypeDetails templateClass,
            final List<MemberHoldingTypeDetails> extendsTypes,
            final String moduleName) {
        try {
            // A type may consist of a concrete type which depend on
            final List<ClassOrInterfaceTypeDetails> types = new ArrayList<ClassOrInterfaceTypeDetails>();
            final ClassOrInterfaceTypeDetailsBuilder templateClassBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                    templateClass);

            if (destType.isCreateAbstract()) {
                final ClassOrInterfaceTypeDetailsBuilder abstractClassBuilder = createAbstractBuilder(
                        templateClassBuilder, extendsTypes, moduleName);

                final ArrayList<FieldMetadataBuilder> fieldsToRemove = new ArrayList<FieldMetadataBuilder>();
                for (final JavaSymbolName fieldName : destType
                        .getWatchedFieldNames()) {
                    for (final FieldMetadataBuilder fieldBuilder : templateClassBuilder
                            .getDeclaredFields()) {
                        if (fieldBuilder.getFieldName().equals(fieldName)) {
                            final FieldMetadataBuilder abstractFieldBuilder = new FieldMetadataBuilder(
                                    abstractClassBuilder
                                            .getDeclaredByMetadataId(),
                                    fieldBuilder.build());
                            abstractClassBuilder
                                    .addField(convertModifier(abstractFieldBuilder));
                            fieldsToRemove.add(fieldBuilder);
                            break;
                        }
                    }
                }

                templateClassBuilder.getDeclaredFields().removeAll(
                        fieldsToRemove);

                final List<MethodMetadataBuilder> methodsToRemove = new ArrayList<MethodMetadataBuilder>();
                for (final JavaSymbolName methodName : destType
                        .getWatchedMethods().keySet()) {
                    for (final MethodMetadataBuilder methodBuilder : templateClassBuilder
                            .getDeclaredMethods()) {
                        final List<JavaType> params = new ArrayList<JavaType>();
                        for (final AnnotatedJavaType param : methodBuilder
                                .getParameterTypes()) {
                            params.add(new JavaType(param.getJavaType()
                                    .getFullyQualifiedTypeName()));
                        }
                        if (methodBuilder.getMethodName().equals(methodName)) {
                            if (destType.getWatchedMethods().get(methodName)
                                    .containsAll(params)) {
                                final MethodMetadataBuilder abstractMethodBuilder = new MethodMetadataBuilder(
                                        abstractClassBuilder
                                                .getDeclaredByMetadataId(),
                                        methodBuilder.build());
                                abstractClassBuilder
                                        .addMethod(convertModifier(abstractMethodBuilder));
                                methodsToRemove.add(methodBuilder);
                                break;
                            }
                        }
                    }
                }

                templateClassBuilder.removeAll(methodsToRemove);

                for (final JavaType innerTypeName : destType
                        .getWatchedInnerTypes()) {
                    for (final ClassOrInterfaceTypeDetailsBuilder innerTypeBuilder : templateClassBuilder
                            .getDeclaredInnerTypes()) {
                        if (innerTypeBuilder.getName().getSimpleTypeName()
                                .equals(innerTypeName.getSimpleTypeName())) {
                            final ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(
                                    abstractClassBuilder
                                            .getDeclaredByMetadataId(),
                                    innerTypeBuilder.build());
                            builder.setName(new JavaType(
                                    innerTypeBuilder.getName()
                                            .getSimpleTypeName() + "_Roo_Gwt",
                                    0, DataType.TYPE, null, innerTypeBuilder
                                            .getName().getParameters()));

                            templateClassBuilder.getDeclaredInnerTypes()
                                    .remove(innerTypeBuilder);
                            if (innerTypeBuilder.getPhysicalTypeCategory()
                                    .equals(PhysicalTypeCategory.INTERFACE)) {
                                final ClassOrInterfaceTypeDetailsBuilder interfaceInnerTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                                        innerTypeBuilder.build());
                                abstractClassBuilder.addInnerType(builder);
                                templateClassBuilder.getDeclaredInnerTypes()
                                        .remove(innerTypeBuilder);
                                interfaceInnerTypeBuilder
                                        .clearDeclaredMethods();
                                interfaceInnerTypeBuilder
                                        .getDeclaredInnerTypes().clear();
                                interfaceInnerTypeBuilder.getExtendsTypes()
                                        .clear();
                                interfaceInnerTypeBuilder
                                        .getExtendsTypes()
                                        .add(new JavaType(
                                                builder.getName()
                                                        .getSimpleTypeName(),
                                                0,
                                                DataType.TYPE,
                                                null,
                                                Collections
                                                        .singletonList(new JavaType(
                                                                "V",
                                                                0,
                                                                DataType.VARIABLE,
                                                                null,
                                                                new ArrayList<JavaType>()))));
                                templateClassBuilder.getDeclaredInnerTypes()
                                        .add(interfaceInnerTypeBuilder);
                            }
                            break;
                        }
                    }
                }

                abstractClassBuilder.setImplementsTypes(templateClass
                        .getImplementsTypes());
                templateClassBuilder.getImplementsTypes().clear();
                templateClassBuilder.getExtendsTypes().clear();
                templateClassBuilder.getExtendsTypes().add(
                        abstractClassBuilder.getName());
                types.add(abstractClassBuilder.build());
            }

            types.add(templateClassBuilder.build());

            return types;
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void buildType(final GwtType type,
            final List<ClassOrInterfaceTypeDetails> templateTypeDetails,
            final String moduleName) {
        if (GwtType.LIST_PLACE_RENDERER.equals(type)) {
            final Map<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
            watchedMethods.put(new JavaSymbolName("render"), Collections
                    .singletonList(new JavaType(projectOperations
                            .getTopLevelPackage(moduleName)
                            .getFullyQualifiedPackageName()
                            + ".client.scaffold.place.ProxyListPlace")));
            type.setWatchedMethods(watchedMethods);
        }
        else {
            type.resolveMethodsToWatch(type);
        }

        type.resolveWatchedFieldNames(type);
        final List<ClassOrInterfaceTypeDetails> typesToBeWritten = new ArrayList<ClassOrInterfaceTypeDetails>();
        for (final ClassOrInterfaceTypeDetails templateTypeDetail : templateTypeDetails) {
            typesToBeWritten.addAll(buildType(type, templateTypeDetail,
                    getExtendsTypes(templateTypeDetail), moduleName));
        }
        gwtFileManager.write(typesToBeWritten, type.isOverwriteConcrete());
    }

    private void checkPrimitive(final JavaType type) {
        if (type.isPrimitive() && !JavaType.VOID_PRIMITIVE.equals(type)) {
            final String to = type.getSimpleTypeName();
            final String from = to.toLowerCase();
            throw new IllegalStateException(
                    "GWT does not currently support primitive types in an entity. Please change any '"
                            + from
                            + "' entity property types to 'java.lang."
                            + to + "'.");
        }
    }

    private <T extends AbstractIdentifiableAnnotatedJavaStructureBuilder<? extends IdentifiableAnnotatedJavaStructure>> T convertModifier(
            final T builder) {
        if (Modifier.isPrivate(builder.getModifier())) {
            builder.setModifier(Modifier.PROTECTED);
        }
        return builder;
    }

    private ClassOrInterfaceTypeDetailsBuilder createAbstractBuilder(
            final ClassOrInterfaceTypeDetailsBuilder concreteClass,
            final List<MemberHoldingTypeDetails> extendsTypesDetails,
            final String moduleName) {
        final JavaType concreteType = concreteClass.getName();
        String abstractName = concreteType.getSimpleTypeName() + "_Roo_Gwt";
        abstractName = concreteType.getPackage().getFullyQualifiedPackageName()
                + '.' + abstractName;
        final JavaType abstractType = new JavaType(abstractName);
        final String abstractId = PhysicalTypeIdentifier.createIdentifier(
                abstractType,
                LogicalPath.getInstance(Path.SRC_MAIN_JAVA, moduleName));
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                abstractId);
        cidBuilder.setPhysicalTypeCategory(PhysicalTypeCategory.CLASS);
        cidBuilder.setName(abstractType);
        cidBuilder.setModifier(Modifier.ABSTRACT | Modifier.PUBLIC);
        cidBuilder.getExtendsTypes().addAll(concreteClass.getExtendsTypes());
        cidBuilder.add(concreteClass.getRegisteredImports());

        for (final MemberHoldingTypeDetails extendsTypeDetails : extendsTypesDetails) {
            for (final ConstructorMetadata constructor : extendsTypeDetails
                    .getDeclaredConstructors()) {
                final ConstructorMetadataBuilder abstractConstructor = new ConstructorMetadataBuilder(
                        abstractId);
                abstractConstructor.setModifier(constructor.getModifier());

                final Map<JavaSymbolName, JavaType> typeMap = resolveTypes(
                        extendsTypeDetails.getName(), concreteClass
                                .getExtendsTypes().get(0));
                for (final AnnotatedJavaType type : constructor
                        .getParameterTypes()) {
                    JavaType newType = type.getJavaType();
                    if (type.getJavaType().getParameters().size() > 0) {
                        final ArrayList<JavaType> parameterTypes = new ArrayList<JavaType>();
                        for (final JavaType typeType : type.getJavaType()
                                .getParameters()) {
                            final JavaType typeParam = typeMap
                                    .get(new JavaSymbolName(typeType.toString()));
                            if (typeParam != null) {
                                parameterTypes.add(typeParam);
                            }
                        }
                        newType = new JavaType(type.getJavaType()
                                .getFullyQualifiedTypeName(), type
                                .getJavaType().getArray(), type.getJavaType()
                                .getDataType(),
                                type.getJavaType().getArgName(), parameterTypes);
                    }
                    abstractConstructor.getParameterTypes().add(
                            new AnnotatedJavaType(newType));
                }
                abstractConstructor.setParameterNames(constructor
                        .getParameterNames());

                final InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
                bodyBuilder.newLine().indent().append("super(");

                int i = 0;
                for (final JavaSymbolName paramName : abstractConstructor
                        .getParameterNames()) {
                    bodyBuilder.append(" ").append(paramName.getSymbolName());
                    if (abstractConstructor.getParameterTypes().size() > i + 1) {
                        bodyBuilder.append(", ");
                    }
                    i++;
                }

                bodyBuilder.append(");");

                bodyBuilder.newLine().indentRemove();
                abstractConstructor.setBodyBuilder(bodyBuilder);
                cidBuilder.getDeclaredConstructors().add(abstractConstructor);
            }
        }
        return cidBuilder;
    }

    private void displayWarning(final String warning) {
        if (!warnings.contains(warning)) {
            warnings.add(warning);
            LOGGER.severe(warning);
            warningTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    warnings.clear();
                }
            }, 15000);
        }
    }

    public List<MemberHoldingTypeDetails> getExtendsTypes(
            final ClassOrInterfaceTypeDetails childType) {
        final List<MemberHoldingTypeDetails> extendsTypes = new ArrayList<MemberHoldingTypeDetails>();
        if (childType != null) {
            for (final JavaType javaType : childType.getExtendsTypes()) {
                final String superTypeId = typeLocationService
                        .getPhysicalTypeIdentifier(javaType);
                if (superTypeId == null
                        || metadataService.get(superTypeId) == null) {
                    continue;
                }
                final MemberHoldingTypeDetails superType = ((PhysicalTypeMetadata) metadataService
                        .get(superTypeId)).getMemberHoldingTypeDetails();
                extendsTypes.add(superType);
            }
        }
        return extendsTypes;
    }

    public String getGwtModuleXml(final String moduleName) {
        final LogicalPath logicalPath = LogicalPath.getInstance(
                Path.SRC_MAIN_JAVA, moduleName);
        final String gwtModuleXml = projectOperations.getPathResolver()
                .getRoot(logicalPath)
                + File.separatorChar
                + projectOperations.getTopLevelPackage(moduleName)
                        .getFullyQualifiedPackageName()
                        .replace('.', File.separatorChar)
                + File.separator
                + "*.gwt.xml";
        final Set<String> paths = new LinkedHashSet<String>();
        for (final FileDetails fileDetails : fileManager
                .findMatchingAntPath(gwtModuleXml)) {
            paths.add(fileDetails.getCanonicalPath());
        }
        if (paths.isEmpty()) {
            throw new IllegalStateException(
                    "Each module must have a gwt.xml file");
        }
        if (paths.size() > 1) {
            throw new IllegalStateException(
                    "Each module can only have only gwt.xml file: "
                            + paths.size());
        }
        return paths.iterator().next();
    }

    /**
     * Return the type arg for the client side method, given the domain method
     * return type. If domain method return type is List<Integer> or
     * Set<Integer>, returns the same. If domain method return type is
     * List<Employee>, return List<EmployeeProxy>
     * 
     * @param returnType
     * @param projectMetadata
     * @param governorType
     * @return the GWT side leaf type as a JavaType
     */

    public JavaType getGwtSideLeafType(final JavaType returnType,
            final JavaType governorType, final boolean requestType,
            final boolean convertPrimitive) {
        if (returnType.isPrimitive() && convertPrimitive) {
            if (!requestType) {
                checkPrimitive(returnType);
            }
            return GwtUtils.convertPrimitiveType(returnType, requestType);
        }

        if (isTypeCommon(returnType)) {
            return returnType;
        }

        if (isCollectionType(returnType)) {
            final List<JavaType> args = returnType.getParameters();
            if (args != null && args.size() == 1) {
                final JavaType elementType = args.get(0);
                final JavaType convertedJavaType = getGwtSideLeafType(
                        elementType, governorType, requestType,
                        convertPrimitive);
                if (convertedJavaType == null) {
                    return null;
                }
                return new JavaType(returnType.getFullyQualifiedTypeName(), 0,
                        DataType.TYPE, null, Arrays.asList(convertedJavaType));
            }
            return returnType;
        }

        final ClassOrInterfaceTypeDetails ptmd = typeLocationService
                .getTypeDetails(returnType);
        if (isDomainObject(returnType, ptmd)) {
            if (isEmbeddable(ptmd)) {
                throw new IllegalStateException(
                        "GWT does not currently support embedding objects in entities, such as '"
                                + returnType.getSimpleTypeName() + "' in '"
                                + governorType.getSimpleTypeName() + "'.");
            }
            final ClassOrInterfaceTypeDetails typeDetails = typeLocationService
                    .getTypeDetails(returnType);
            if (typeDetails == null) {
                return null;
            }
            final ClassOrInterfaceTypeDetails proxy = lookupProxyFromEntity(typeDetails);
            if (proxy == null) {
                return null;
            }
            return proxy.getName();
        }
        return returnType;
    }

    public Document getGwtXmlDocument(final String gwtModuleCanonicalPath) {
        final DocumentBuilder builder = XmlUtils.getDocumentBuilder();
        builder.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(final String publicId,
                    final String systemId) throws SAXException, IOException {
                if (systemId.endsWith("gwt-module.dtd")) {
                    return new InputSource(FileUtils.getInputStream(
                            GwtScaffoldMetadata.class,
                            "templates/gwt-module.dtd"));
                }
                // Use the default behaviour
                return null;
            }
        });

        InputStream inputStream = null;
        try {
            inputStream = fileManager.getInputStream(gwtModuleCanonicalPath);
            return builder.parse(inputStream);
        }
        catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public List<MethodMetadata> getProxyMethods(
            final ClassOrInterfaceTypeDetails governorTypeDetails) {
        final List<MethodMetadata> proxyMethods = new ArrayList<MethodMetadata>();
        final MemberDetails memberDetails = memberDetailsScanner
                .getMemberDetails(GwtTypeServiceImpl.class.getName(),
                        governorTypeDetails);
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails
                .getDetails()) {
            for (final MethodMetadata method : memberDetails.getMethods()) {
                if (!proxyMethods.contains(method)
                        && isPublicAccessor(method)
                        && isValidMethodReturnType(method,
                                memberHoldingTypeDetails)) {
                    if (method
                            .getCustomData()
                            .keySet()
                            .contains(CustomDataKeys.IDENTIFIER_ACCESSOR_METHOD)) {
                        proxyMethods.add(0, method);
                    }
                    else {
                        proxyMethods.add(method);
                    }
                }
            }
        }
        return proxyMethods;
    }

    public JavaType getServiceLocator(final String moduleName) {
        return new JavaType(projectOperations.getTopLevelPackage(moduleName)
                + ".server.locator.GwtServiceLocator");
    }

    public Collection<JavaPackage> getSourcePackages(final String moduleName) {
        final Document gwtXmlDoc = getGwtXmlDocument(getGwtModuleXml(moduleName));
        final Element gwtXmlRoot = gwtXmlDoc.getDocumentElement();
        final JavaPackage topLevelPackage = projectOperations
                .getTopLevelPackage(moduleName);
        final Collection<JavaPackage> sourcePackages = new HashSet<JavaPackage>();
        for (final Element sourcePathElement : XmlUtils.findElements(
                "/module/source", gwtXmlRoot)) {
            final String relativePackage = sourcePathElement.getAttribute(PATH)
                    .replace(GwtOperations.PATH_DELIMITER, ".");
            sourcePackages.add(new JavaPackage(topLevelPackage + "."
                    + relativePackage));
        }
        return sourcePackages;
    }

    private boolean isAllowableReturnType(final JavaType type) {
        return isCommonType(type) || isEntity(type) || isEnum(type);
    }

    private boolean isAllowableReturnType(final MethodMetadata method) {
        return isAllowableReturnType(method.getReturnType());
    }

    private boolean isCollectionType(final JavaType returnType) {
        return returnType.getFullyQualifiedTypeName().equals(
                LIST.getFullyQualifiedTypeName())
                || returnType.getFullyQualifiedTypeName().equals(
                        SET.getFullyQualifiedTypeName());
    }

    private boolean isCommonType(final JavaType type) {
        return isTypeCommon(type) || isCollectionType(type)
                && type.getParameters().size() == 1
                && isAllowableReturnType(type.getParameters().get(0));
    }

    public boolean isDomainObject(final JavaType type) {
        final ClassOrInterfaceTypeDetails ptmd = typeLocationService
                .getTypeDetails(type);
        return isDomainObject(type, ptmd);
    }

    private boolean isDomainObject(final JavaType returnType,
            final ClassOrInterfaceTypeDetails ptmd) {
        return !isEnum(ptmd) && isEntity(returnType)
                && !isRequestFactoryCompatible(returnType)
                && !isEmbeddable(ptmd);
    }

    private boolean isEmbeddable(final ClassOrInterfaceTypeDetails ptmd) {
        if (ptmd == null) {
            return false;
        }
        final AnnotationMetadata annotationMetadata = ptmd
                .getAnnotation(EMBEDDABLE);
        return annotationMetadata != null;
    }

    private boolean isEntity(final JavaType type) {
        return persistenceMemberLocator.getIdentifierFields(type).size() == 1;
    }

    private boolean isEnum(final ClassOrInterfaceTypeDetails ptmd) {
        return ptmd != null
                && ptmd.getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;
    }

    private boolean isEnum(final JavaType type) {
        return isEnum(typeLocationService.getTypeDetails(type));
    }

    public boolean isMethodReturnTypeInSourcePath(final MethodMetadata method,
            final MemberHoldingTypeDetails memberHoldingTypeDetail,
            final Iterable<JavaPackage> sourcePackages) {
        final JavaType returnType = method.getReturnType();
        final boolean inSourcePath = isTypeInAnySourcePackage(returnType,
                sourcePackages);
        if (!inSourcePath
                && !isCommonType(returnType)
                && !JavaType.VOID_PRIMITIVE.getFullyQualifiedTypeName().equals(
                        returnType.getFullyQualifiedTypeName())) {
            displayWarning("The path to type "
                    + returnType.getFullyQualifiedTypeName()
                    + " which is used in type "
                    + memberHoldingTypeDetail.getName()
                    + " by the field '"
                    + method.getMethodName().getSymbolName()
                    + "' needs to be added to the module's gwt.xml file in order to be used in a Proxy.");
            return false;
        }
        return true;
    }

    private boolean isPrimitive(final JavaType type) {
        return type.isPrimitive() || isCollectionType(type)
                && type.getParameters().size() == 1
                && isPrimitive(type.getParameters().get(0));
    }

    private boolean isPublicAccessor(final MethodMetadata method) {
        return Modifier.isPublic(method.getModifier())
                && !method.getReturnType().equals(JavaType.VOID_PRIMITIVE)
                && method.getParameterTypes().isEmpty()
                && method.getMethodName().getSymbolName().startsWith("get");
    }

    private boolean isRequestFactoryCompatible(final JavaType type) {
        return isCommonType(type) || isCollectionType(type);
    }

    private boolean isTypeCommon(final JavaType type) {
        return JavaType.BOOLEAN_OBJECT.equals(type)
                || JavaType.CHAR_OBJECT.equals(type)
                || JavaType.BYTE_OBJECT.equals(type)
                || JavaType.SHORT_OBJECT.equals(type)
                || JavaType.INT_OBJECT.equals(type)
                || LONG_OBJECT.equals(type)
                || JavaType.FLOAT_OBJECT.equals(type)
                || JavaType.DOUBLE_OBJECT.equals(type)
                || JavaType.STRING.equals(type)
                || DATE.equals(type)
                || BIG_DECIMAL.equals(type)
                || type.isPrimitive()
                && !JavaType.VOID_PRIMITIVE.getFullyQualifiedTypeName().equals(
                        type.getFullyQualifiedTypeName());
    }

    private boolean isTypeInAnySourcePackage(final JavaType type,
            final Iterable<JavaPackage> sourcePackages) {
        for (final JavaPackage sourcePackage : sourcePackages) {
            if (type.getPackage().isWithin(sourcePackage)) {
                return true; // It's a project type
            }
            if (isCollectionType(type)
                    && type.getParameters().size() == 1
                    && type.getParameters().get(0).getPackage()
                            .isWithin(sourcePackage)) {
                return true; // It's a collection of a project type
            }
        }
        return false;
    }

    private boolean isValidMethodReturnType(final MethodMetadata method,
            final MemberHoldingTypeDetails memberHoldingTypeDetail) {
        final JavaType returnType = method.getReturnType();
        if (isPrimitive(returnType)) {
            displayWarning("The primitive field type, "
                    + method.getReturnType().getSimpleTypeName().toLowerCase()
                    + " of '"
                    + method.getMethodName().getSymbolName()
                    + "' in type "
                    + memberHoldingTypeDetail.getName().getSimpleTypeName()
                    + " is not currently support by GWT and will not be added to the scaffolded application.");
            return false;
        }

        final JavaSymbolName propertyName = new JavaSymbolName(
                StringUtils.uncapitalize(BeanInfoUtils
                        .getPropertyNameForJavaBeanMethod(method)
                        .getSymbolName()));
        if (!isAllowableReturnType(method)) {
            displayWarning("The field type "
                    + method.getReturnType().getFullyQualifiedTypeName()
                    + " of '"
                    + method.getMethodName().getSymbolName()
                    + "' in type "
                    + memberHoldingTypeDetail.getName().getSimpleTypeName()
                    + " is not currently support by GWT and will not be added to the scaffolded application.");
            return false;
        }
        if (propertyName.getSymbolName().equals("owner")) {
            displayWarning("'owner' is not allowed to be used as field name as it is currently reserved by GWT. Please rename the field 'owner' in type "
                    + memberHoldingTypeDetail.getName().getSimpleTypeName()
                    + ".");
            return false;
        }

        return true;
    }

    public ClassOrInterfaceTypeDetails lookupEntityFromLocator(
            final ClassOrInterfaceTypeDetails locator) {
        Validate.notNull(locator, "Locator is required");
        return lookupTargetFromX(locator, RooJavaType.ROO_GWT_LOCATOR);
    }

    public ClassOrInterfaceTypeDetails lookupEntityFromProxy(
            final ClassOrInterfaceTypeDetails proxy) {
        Validate.notNull(proxy, "Proxy is required");
        return lookupTargetFromX(proxy, RooJavaType.ROO_GWT_PROXY);
    }

    public ClassOrInterfaceTypeDetails lookupEntityFromRequest(
            final ClassOrInterfaceTypeDetails request) {
        Validate.notNull(request, "Request is required");
        return lookupTargetFromX(request, RooJavaType.ROO_GWT_REQUEST);
    }

    public ClassOrInterfaceTypeDetails lookupProxyFromEntity(
            final ClassOrInterfaceTypeDetails entity) {
        return lookupXFromEntity(entity, RooJavaType.ROO_GWT_PROXY);
    }

    public ClassOrInterfaceTypeDetails lookupProxyFromRequest(
            final ClassOrInterfaceTypeDetails request) {
        final AnnotationMetadata annotation = GwtUtils.getFirstAnnotation(
                request, RooJavaType.ROO_GWT_REQUEST);
        Validate.notNull(annotation, "Request '" + request.getName()
                + "' isn't annotated with '" + RooJavaType.ROO_GWT_REQUEST
                + "'");
        final AnnotationAttributeValue<?> attributeValue = annotation
                .getAttribute("value");
        final JavaType proxyType = new JavaType(
                GwtUtils.getStringValue(attributeValue));
        return lookupProxyFromEntity(typeLocationService
                .getTypeDetails(proxyType));
    }

    public ClassOrInterfaceTypeDetails lookupRequestFromEntity(
            final ClassOrInterfaceTypeDetails entity) {
        return lookupXFromEntity(entity, RooJavaType.ROO_GWT_REQUEST);
    }

    public ClassOrInterfaceTypeDetails lookupRequestFromProxy(
            final ClassOrInterfaceTypeDetails proxy) {
        final AnnotationMetadata annotation = GwtUtils.getFirstAnnotation(
                proxy, RooJavaType.ROO_GWT_PROXY);
        Validate.notNull(annotation, "Proxy '" + proxy.getName()
                + "' isn't annotated with '" + RooJavaType.ROO_GWT_PROXY + "'");
        final AnnotationAttributeValue<?> attributeValue = annotation
                .getAttribute("value");
        final JavaType serviceNameType = new JavaType(
                GwtUtils.getStringValue(attributeValue));
        return lookupRequestFromEntity(typeLocationService
                .getTypeDetails(serviceNameType));
    }

    public ClassOrInterfaceTypeDetails lookupUnmanagedRequestFromProxy(
            final ClassOrInterfaceTypeDetails proxy) {
        final AnnotationMetadata annotation = GwtUtils.getFirstAnnotation(
                proxy, RooJavaType.ROO_GWT_PROXY);
        Validate.notNull(annotation, "Proxy '" + proxy.getName()
                + "' isn't annotated with '" + RooJavaType.ROO_GWT_PROXY + "'");
        final AnnotationAttributeValue<?> attributeValue = annotation
                .getAttribute("value");
        final JavaType serviceNameType = new JavaType(
                GwtUtils.getStringValue(attributeValue));
        return lookupUnmanagedRequestFromEntity(typeLocationService
                .getTypeDetails(serviceNameType));
    }

    public ClassOrInterfaceTypeDetails lookupUnmanagedRequestFromEntity(
            final ClassOrInterfaceTypeDetails entity) {
        return lookupXFromEntity(entity, RooJavaType.ROO_GWT_UNMANAGED_REQUEST);
    }

    public ClassOrInterfaceTypeDetails lookupTargetFromX(
            final ClassOrInterfaceTypeDetails annotatedType,
            final JavaType... annotations) {
        final AnnotationMetadata annotation = GwtUtils.getFirstAnnotation(
                annotatedType, annotations);
        Validate.notNull(annotation,
                "Type '" + annotatedType.getName() + "' isn't annotated with '"
                        + StringUtils.join(Arrays.asList(annotations), ",")
                        + "'");
        final AnnotationAttributeValue<?> attributeValue = annotation
                .getAttribute("value");
        final JavaType targetType = new JavaType(
                GwtUtils.getStringValue(attributeValue));
        return typeLocationService.getTypeDetails(targetType);
    }

    public ClassOrInterfaceTypeDetails lookupTargetServiceFromRequest(
            final ClassOrInterfaceTypeDetails request) {
        Validate.notNull(request, "Request is required");
        return lookupTargetFromX(request, GwtUtils.REQUEST_ANNOTATIONS);
    }

    public ClassOrInterfaceTypeDetails lookupXFromEntity(
            final ClassOrInterfaceTypeDetails entity,
            final JavaType... annotations) {
        Validate.notNull(entity, "Entity not found");
        for (final ClassOrInterfaceTypeDetails cid : typeLocationService
                .findClassesOrInterfaceDetailsWithAnnotation(annotations)) {
            final AnnotationMetadata annotationMetadata = GwtUtils
                    .getFirstAnnotation(cid, annotations);
            if (annotationMetadata != null) {
                final AnnotationAttributeValue<?> attributeValue = annotationMetadata
                        .getAttribute("value");
                final String value = GwtUtils.getStringValue(attributeValue);
                if (entity.getName().getFullyQualifiedTypeName().equals(value)) {
                    return cid;
                }
            }
        }
        return null;
    }

    private Map<JavaSymbolName, JavaType> resolveTypes(final JavaType generic,
            final JavaType typed) {
        final Map<JavaSymbolName, JavaType> typeMap = new LinkedHashMap<JavaSymbolName, JavaType>();
        final boolean typeCountMatch = generic.getParameters().size() == typed
                .getParameters().size();
        Validate.isTrue(typeCountMatch, "Type count must match.");

        int i = 0;
        for (final JavaType genericParamType : generic.getParameters()) {
            typeMap.put(genericParamType.getArgName(), typed.getParameters()
                    .get(i));
            i++;
        }
        return typeMap;
    }
}
