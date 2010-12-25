package org.springframework.roo.addon.gwt;

import hapax.*;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.classpath.*;
import org.springframework.roo.classpath.details.*;
import org.springframework.roo.classpath.details.annotations.*;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.javaparser.JavaParserMutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.operations.ClasspathOperations;
import org.springframework.roo.metadata.AbstractMetadataItem;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Metadata for GWT.
 *
 * @author Ben Alex
 * @author Alan Stewart
 * @author Ray Cromwell
 * @author Amit Manjhi
 * @since 1.1
 */
public class GwtMetadata extends AbstractMetadataItem {
    private static final String PROVIDES_TYPE_STRING = GwtMetadata.class.getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

    private FileManager fileManager;
    private MetadataService metadataService;
    private BeanInfoMetadata beanInfoMetadata;
    private EntityMetadata entityMetadata;
    private MethodMetadata findAllMethod;
    private MethodMetadata findMethod;
    private MethodMetadata countMethod;
    private MethodMetadata findEntriesMethod;

    private MirrorTypeNamingStrategy mirrorTypeNamingStrategy;
    private ProjectMetadata projectMetadata;
    private ClassOrInterfaceTypeDetails governorTypeDetails;
    private Path mirrorTypePath;
    private ClassOrInterfaceTypeDetails request;

    private ClassOrInterfaceTypeDetails proxy;
    private JavaSymbolName idPropertyName;
    private JavaSymbolName versionPropertyName;

    private LinkedHashMap<JavaSymbolName, JavaType> orderedProxyFields;
    private MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider;
    private ClasspathOperations classpathOperations;


    public GwtMetadata(String identifier, MirrorTypeNamingStrategy mirrorTypeNamingStrategy, ProjectMetadata projectMetadata, ClassOrInterfaceTypeDetails governorTypeDetails, Path mirrorTypePath, BeanInfoMetadata beanInfoMetadata, EntityMetadata entityMetadata, FileManager fileManager, MetadataService metadataService, MutablePhysicalTypeMetadataProvider physicalTypeMetadataProvider, ClasspathOperations classpathOperations) {
        super(identifier);
        this.mirrorTypeNamingStrategy = mirrorTypeNamingStrategy;
        this.projectMetadata = projectMetadata;
        this.governorTypeDetails = governorTypeDetails;
        this.mirrorTypePath = mirrorTypePath;
        this.beanInfoMetadata = beanInfoMetadata;
        this.entityMetadata = entityMetadata;
        this.fileManager = fileManager;
        this.metadataService = metadataService;
        this.physicalTypeMetadataProvider = physicalTypeMetadataProvider;
        this.classpathOperations = classpathOperations;

        if (beanInfoMetadata != null) {
            /*for (AnnotationMetadata annotation : entityMetadata.getItdTypeDetails().getAnnotations()) {
                if (annotation.getAnnotationType().equals(new JavaType("javax.persistence.MappedSuperclass"))) {
                    throw new IllegalStateException("GWT does not currently support inheritence in proxied objects. Please remove the 'javax.persistence.MappedSuperclass' annotation from '" + governorTypeDetails.getName().getFullyQualifiedTypeName() + "' in order to complete 'gwt setup'.");
                }
            }
*/
            for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors(false)) {
                JavaType returnType = accessor.getReturnType();
                checkPrimitive(returnType);

                PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(returnType, Path.SRC_MAIN_JAVA));
                if (isEmbeddable(ptmd)) {
                    throw new IllegalStateException("GWT does not currently support embedding objects in entities, such as '" + returnType.getSimpleTypeName() + "' in '" + beanInfoMetadata.getJavaBean().getSimpleTypeName() + "'.");
                }
            }


        }

        // We know GwtMetadataProvider already took care of all the necessary checks. So we can just re-create fresh representations of the types we're responsible for
        resolveEntityInformation();


        buildProxy();
        buildActivitiesMapper();

        buildEditActivityWrapper();
        buildDetailsActivity();
        buildListActivity();
        buildMobileListView();
        buildListView();
        buildListViewUiXml();
        buildDetailsView();
        buildDetailsViewUiXml();
        buildMobileDetailsView();
        buildMobileDetailsViewUiXml();
        buildEditView();
        buildEditViewUiXml();
        buildMobileEditView();
        buildMobileEditViewUiXml();
        buildEditRenderer();
        buildSetEditor();
        buildSetEditorUiXml();
        buildListEditor();
        buildListEditorUiXml();
        buildRequest();
    }

    public List<ClassOrInterfaceTypeDetails> getAllTypes() {
        List<ClassOrInterfaceTypeDetails> result = new ArrayList<ClassOrInterfaceTypeDetails>();
        result.add(proxy);
        result.add(request);
        return result;
    }

    private void resolveEntityInformation() {
        if (entityMetadata != null && entityMetadata.isValid()) {
            // Lookup special fields
            FieldMetadata versionField = entityMetadata.getVersionField();
            FieldMetadata idField = entityMetadata.getIdentifierField();
            Assert.notNull(versionField, "Version unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
            Assert.notNull(idField, "Id unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
            versionPropertyName = versionField.getFieldName();
            idPropertyName = idField.getFieldName();

            // Lookup the "find all" method and store it
            findAllMethod = entityMetadata.getFindAllMethod();
            findMethod = entityMetadata.getFindMethod();
            findEntriesMethod = entityMetadata.getFindEntriesMethod();
            countMethod = entityMetadata.getCountMethod();
            Assert.notNull(findAllMethod, "Find all method unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
            Assert.isTrue("id".equals(idPropertyName.getSymbolName()), "Id property must be named \"id\" (found \"" + idPropertyName + "\") for " + governorTypeDetails.getName() + " - required for GWT support");
            Assert.isTrue("version".equals(versionPropertyName.getSymbolName()), "Version property must be named \"version\" (found \"" + versionPropertyName + "\") for " + governorTypeDetails.getName() + " - required for GWT support");
            Assert.notNull(findAllMethod, "Find all method unavailable for " + governorTypeDetails.getName() + " - required for GWT support");
        }
    }

    private void buildActivitiesMapper() {
        try {
            MirrorType type = MirrorType.ACTIVITIES_MAPPER;
            TemplateDataDictionary dataDictionary = buildDataDictionary(type);
            addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
            addReference(dataDictionary, SharedType.SCAFFOLD_APP);
            addReference(dataDictionary, MirrorType.DETAIL_ACTIVITY);
            addReference(dataDictionary, MirrorType.EDIT_ACTIVITY_WRAPPER);
            addReference(dataDictionary, MirrorType.LIST_VIEW);
            addReference(dataDictionary, MirrorType.DETAILS_VIEW);
            addReference(dataDictionary, MirrorType.MOBILE_DETAILS_VIEW);
            addReference(dataDictionary, MirrorType.EDIT_VIEW);
            addReference(dataDictionary, MirrorType.MOBILE_EDIT_VIEW);
            addReference(dataDictionary, MirrorType.REQUEST);
            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    //TODO: this should probably be sitting (is already??) in a lower level util class
    private PhysicalTypeMetadata getPhysicalTypeMetadata(String declaredByMetadataId) {
        return (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
    }

    private void resolveProxyFields() {

        orderedProxyFields = new LinkedHashMap<JavaSymbolName, JavaType>();

        if (beanInfoMetadata != null) {
            for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors(false)) {
                JavaSymbolName propertyName = new JavaSymbolName(StringUtils.uncapitalize(BeanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor).getSymbolName()));
                JavaType returnType = accessor.getReturnType();
                PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(returnType, Path.SRC_MAIN_JAVA));
                JavaType gwtSideType = getGwtSideLeafType(returnType, ptmd);
                orderedProxyFields.put(propertyName, gwtSideType);
            }
        }
    }

    private void buildProxy() {

        resolveProxyFields();

        String destinationMetadataId = getDestinationMetadataId(MirrorType.PROXY);

        //Get the Proxy's PhysicalTypeMetaData representation of the on disk Proxy
        JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

        //Create a new ClassOrInterfaceTypeDetailsBuilder for the Proxy, will be overridden if the Proxy has already been created
        ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(destinationMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.INTERFACE);

        List<AnnotationMetadataBuilder> typeAnnotations = createAnnotations();

        // @ProxyFor(Employee.class)
        typeAnnotations.add(createAdditionalAnnotation(new JavaType("com.google.gwt.requestfactory.shared.ProxyForName")));

        //Only add annotations that don't already exist on the target
        for (AnnotationMetadataBuilder annotationBuilder : typeAnnotations) {
            boolean exists = false;
            for (AnnotationMetadataBuilder existingAnnotation : typeDetailsBuilder.getAnnotations()) {
                if (existingAnnotation.getAnnotationType().equals(annotationBuilder.getAnnotationType())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                typeDetailsBuilder.addAnnotation(annotationBuilder);
            }
        }


        // extends EntityProxy
        //Only inherit from EntityProxy if extension is not already defined
        if (!typeDetailsBuilder.getExtendsTypes().contains(new JavaType("com.google.gwt.requestfactory.shared.EntityProxy"))) {
            typeDetailsBuilder.addExtendsTypes(new JavaType("com.google.gwt.requestfactory.shared.EntityProxy"));
        }

        /*
           * Decide which fields we'll be mapping. Remember the natural ordering for
           * processing, but order proxy getters alphabetically by name.
           */


        // Getter methods for EmployeeProxy
        for (JavaSymbolName propertyName : orderedProxyFields.keySet()) {
            JavaType methodReturnType = orderedProxyFields.get(propertyName);
            JavaSymbolName methodName = new JavaSymbolName("get" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
            List<JavaType> methodParameterTypes = new ArrayList<JavaType>();
            List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>();
            MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, new InvocableMemberBodyBuilder());

            //Only add a method if it isn't already present, this leaves the user defined methods in play
            boolean match = false;
            for (MethodMetadataBuilder builder : typeDetailsBuilder.getDeclaredMethods()) {
                if (methodBuildersEqual(methodBuilder, builder)) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                typeDetailsBuilder.addMethod(methodBuilder);
            }
        }

        // Setter methods for EmployeeProxy
        /*
        * The methods in the proxy will be sorted alphabetically, which makes sense
        * for the Java type. However, we want to process them in the order the
        * fields are declared, such that the first database field is the first
        * field we add to the dataDictionary. This affects the order of the
        * properties in the desktop client, as well as the primary/secondary
        * properties in the mobile client.
        */
        for (JavaSymbolName propertyName : orderedProxyFields.keySet()) {
            JavaType methodReturnType = JavaType.VOID_PRIMITIVE;
            JavaSymbolName methodName = new JavaSymbolName("set" + new JavaSymbolName(propertyName.getSymbolNameCapitalisedFirstLetter()));
            List<JavaType> methodParameterTypes = Collections.singletonList(orderedProxyFields.get(propertyName));
            List<JavaSymbolName> methodParameterNames = Collections.singletonList(propertyName);
            MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, new InvocableMemberBodyBuilder());

            //Only add a method if it isn't already present, this leaves the user defined methods in play
            boolean match = false;
            for (MethodMetadataBuilder builder : typeDetailsBuilder.getDeclaredMethods()) {
                if (methodBuildersEqual(methodBuilder, builder)) {
                    match = true;
                    break;
                }

            }

            if (!match) {
                typeDetailsBuilder.addMethod(methodBuilder);
            }
        }

        this.proxy = typeDetailsBuilder.build();

        // Determine the canonical filename
        String physicalLocationCanonicalPath = classpathOperations.getPhysicalLocationCanonicalPath(typeDetailsBuilder.getDeclaredByMetadataId());
        String contents = JavaParserMutableClassOrInterfaceTypeDetails.getOutput(typeDetailsBuilder.build());
        contents = "// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.\n\n" + contents;

        write(physicalLocationCanonicalPath, contents, fileManager);
    }


    private boolean methodBuildersEqual(MethodMetadataBuilder m1, MethodMetadataBuilder m2) {
        boolean match = false;

        if (m1.getMethodName().equals(m2.getMethodName())) {
            match = true;
        }

        return match;
    }

    private void checkPrimitive(JavaType returnType) {
        if (returnType.isPrimitive()) {
            String to = "";
            String from = "";
            if (returnType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
                from = "boolean";
                to = "Boolean";
            }
            if (returnType.equals(JavaType.INT_PRIMITIVE)) {
                from = "int";
                to = "Integer";
            }
            if (returnType.equals(JavaType.BYTE_PRIMITIVE)) {
                from = "byte";
                to = "Byte";
            }
            if (returnType.equals(JavaType.SHORT_PRIMITIVE)) {
                from = "short";
                to = "Short";
            }
            if (returnType.equals(JavaType.FLOAT_PRIMITIVE)) {
                from = "float";
                to = "Float";
            }
            if (returnType.equals(JavaType.DOUBLE_PRIMITIVE)) {
                from = "double";
                to = "Double";
            }
            if (returnType.equals(JavaType.CHAR_PRIMITIVE)) {
                from = "char";
                to = "Character";
            }
            if (returnType.equals(JavaType.LONG_PRIMITIVE)) {
                from = "long";
                to = "Long";
            }

            throw new IllegalStateException("GWT does not currently support primitive types in an entity. Please change any '" + from + "' entity property types to 'java.lang." + to + "'.");
        }
    }

    private JavaType getGwtSideLeafType(JavaType returnType, PhysicalTypeMetadata ptmd) {
        boolean isDomainObject = isDomainObject(returnType, ptmd);
        if (isDomainObject) {
            return getDestinationJavaType(returnType, MirrorType.PROXY);
        }
        if (returnType.isPrimitive()) {
            if (returnType.equals(JavaType.BOOLEAN_PRIMITIVE)) {
                return JavaType.BOOLEAN_OBJECT;
            }
            if (returnType.equals(JavaType.INT_PRIMITIVE)) {
                return JavaType.INT_OBJECT;
            }
            if (returnType.equals(JavaType.BYTE_PRIMITIVE)) {
                return JavaType.BYTE_OBJECT;
            }
            if (returnType.equals(JavaType.SHORT_PRIMITIVE)) {
                return JavaType.SHORT_OBJECT;
            }
            if (returnType.equals(JavaType.FLOAT_PRIMITIVE)) {
                return JavaType.FLOAT_OBJECT;
            }
            if (returnType.equals(JavaType.DOUBLE_PRIMITIVE)) {
                return JavaType.DOUBLE_OBJECT;
            }
            if (returnType.equals(JavaType.CHAR_PRIMITIVE)) {
                return JavaType.CHAR_OBJECT;
            }
            if (returnType.equals(JavaType.LONG_PRIMITIVE)) {
                return JavaType.LONG_OBJECT;
            }
            return returnType;
        }

        if (isCollectionType(returnType)) {
            List<JavaType> args = returnType.getParameters();
            if (args != null && args.size() == 1) {
                JavaType elementType = args.get(0);
                if (isDomainObject(elementType, (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(elementType, Path.SRC_MAIN_JAVA)))) {
                    return new JavaType(returnType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(getDestinationJavaType(elementType, MirrorType.PROXY)));
                } else {
                    return new JavaType(returnType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(elementType));
                }

            }
            return returnType;

        }
        return returnType;
    }

    private boolean isDomainObject(JavaType returnType, PhysicalTypeMetadata ptmd) {
        boolean isEnum = ptmd != null
                && ptmd.getMemberHoldingTypeDetails() != null
                && ptmd.getMemberHoldingTypeDetails().getPhysicalTypeCategory() == PhysicalTypeCategory.ENUMERATION;

        boolean isDomainObject = !isEnum
                && !isShared(returnType)
                && !(isRequestFactoryPrimitive(returnType))
                && !(isCollectionType(returnType))
                && !isEmbeddable(ptmd);

        return isDomainObject;
    }

    private boolean isEmbeddable(PhysicalTypeMetadata ptmd) {
        if (ptmd != null && ptmd.getMemberHoldingTypeDetails() != null) {
            if (ptmd.getMemberHoldingTypeDetails() instanceof ClassOrInterfaceTypeDetails) {
                List<AnnotationMetadata> annotations = ((ClassOrInterfaceTypeDetails) ptmd.getMemberHoldingTypeDetails()).getAnnotations();
                for (AnnotationMetadata annotation : annotations) {
                    if (annotation.getAnnotationType().equals(new JavaType("javax.persistence.Embeddable"))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isCollectionType(JavaType returnType) {
        return returnType.equals(new JavaType("java.util.List"))
                || returnType.equals(new JavaType("java.util.Set"));
    }

    private boolean isRequestFactoryPrimitive(JavaType returnType) {
        return returnType.equals(JavaType.BOOLEAN_OBJECT)
                || returnType.equals(JavaType.INT_OBJECT)
                || returnType.isPrimitive()
                || returnType.equals(JavaType.LONG_OBJECT)
                || returnType.equals(JavaType.STRING_OBJECT)
                || returnType.equals(JavaType.DOUBLE_OBJECT)
                || returnType.equals(JavaType.FLOAT_OBJECT)
                || returnType.equals(JavaType.CHAR_OBJECT)
                || returnType.equals(JavaType.BYTE_OBJECT)
                || returnType.equals(JavaType.SHORT_OBJECT)
                || returnType.equals(new JavaType("java.util.Date"))
                || returnType.equals(new JavaType("java.math.BigDecimal"));
    }

    private void addReference(TemplateDataDictionary dataDictionary, MirrorType type) {
        addImport(dataDictionary, getDestinationJavaType(type).getFullyQualifiedTypeName());
        dataDictionary.setVariable(type.getName(), getDestinationJavaType(type).getSimpleTypeName());
    }

    private void addReference(TemplateDataDictionary dataDictionary, SharedType type) {
        addImport(dataDictionary, getDestinationJavaType(type).getFullyQualifiedTypeName());
        dataDictionary.setVariable(type.getName(), getDestinationJavaType(type).getSimpleTypeName());
    }

    private void addImport(TemplateDataDictionary dataDictionary, String importDeclaration) {
        dataDictionary.addSection("imports").setVariable("import", importDeclaration);
    }

    private void buildEditActivityWrapper() {
        try {
            MirrorType type = MirrorType.EDIT_ACTIVITY_WRAPPER;
            type.setCreateAbstract(true);
            TemplateDataDictionary dataDictionary = buildDataDictionary(type);
            addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
            addReference(dataDictionary, SharedType.IS_SCAFFOLD_MOBILE_ACTIVITY);

            ArrayList<JavaSymbolName> fieldsToWatch = new ArrayList<JavaSymbolName>();
            fieldsToWatch.add(new JavaSymbolName("wrapped"));
            fieldsToWatch.add(new JavaSymbolName("view"));
            fieldsToWatch.add(new JavaSymbolName("requests"));
            type.setWatchedFieldNames(fieldsToWatch);

            HashMap<JavaSymbolName, List<JavaType>> methodsToWatch = new HashMap<JavaSymbolName, List<JavaType>>();
            List<JavaType> params = new ArrayList<JavaType>();
            params.add(new JavaType("com.google.gwt.user.client.ui.AcceptsOneWidget"));
            params.add(new JavaType("com.google.gwt.event.shared.EventBus"));
            methodsToWatch.put(new JavaSymbolName("start"), params);
            type.setWatchedMethods(methodsToWatch);

            List<JavaType> innerTypesToWatch = new ArrayList<JavaType>();
            innerTypesToWatch.add(new JavaType("View"));
            type.setWatchedInnerTypes(innerTypesToWatch);

            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildDetailsActivity() {
        try {
            MirrorType type = MirrorType.DETAIL_ACTIVITY;
            type.setCreateAbstract(true);

            ArrayList<JavaSymbolName> fieldsToWatch = new ArrayList<JavaSymbolName>();
            fieldsToWatch.add(new JavaSymbolName("requests"));
            fieldsToWatch.add(new JavaSymbolName("proxyId"));
            type.setWatchedFieldNames(fieldsToWatch);

            HashMap<JavaSymbolName, List<JavaType>> methodsToWatch = new HashMap<JavaSymbolName, List<JavaType>>();
            List<JavaType> params = new ArrayList<JavaType>();
            params.add(new JavaType("com.google.gwt.requestfactory.shared.Receiver", 0, DataType.TYPE, null, Collections.singletonList(new JavaType("com.google.gwt.requestfactory.shared.EntityProxy"))));
            methodsToWatch.put(new JavaSymbolName("find"), params);
            type.setWatchedMethods(methodsToWatch);

            TemplateDataDictionary dataDictionary = buildDataDictionary(type);
            addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
            addReference(dataDictionary, SharedType.IS_SCAFFOLD_MOBILE_ACTIVITY);
            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildListActivity() {
        try {
            MirrorType type = MirrorType.LIST_ACTIVITY;
            type.setCreateAbstract(false);
            TemplateDataDictionary dataDictionary = buildDataDictionary(type);
            addReference(dataDictionary, SharedType.SCAFFOLD_MOBILE_APP);
            addReference(dataDictionary, SharedType.IS_SCAFFOLD_MOBILE_ACTIVITY);
            addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildMobileListView() {
        try {
            MirrorType type = MirrorType.MOBILE_LIST_VIEW;
            type.setCreateAbstract(true);
            TemplateDataDictionary dataDictionary = buildDataDictionary(type);
            addReference(dataDictionary, SharedType.MOBILE_PROXY_LIST_VIEW);
            addReference(dataDictionary, SharedType.SCAFFOLD_MOBILE_APP);

            ArrayList<JavaSymbolName> fieldsToWatch = new ArrayList<JavaSymbolName>();
            fieldsToWatch.add(new JavaSymbolName("paths"));
            type.setWatchedFieldNames(fieldsToWatch);

            HashMap<JavaSymbolName, List<JavaType>> methodsToWatch = new HashMap<JavaSymbolName, List<JavaType>>();
            methodsToWatch.put(new JavaSymbolName("init"), new ArrayList<JavaType>());
            type.setWatchedMethods(methodsToWatch);

            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildListView() {
        try {
            MirrorType destType = MirrorType.LIST_VIEW;
            destType.setCreateAbstract(true);
            ArrayList<JavaSymbolName> fieldsToWatch = new ArrayList<JavaSymbolName>();
            fieldsToWatch.add(new JavaSymbolName("table"));
            fieldsToWatch.add(new JavaSymbolName("paths"));
            destType.setWatchedFieldNames(fieldsToWatch);

            HashMap<JavaSymbolName, List<JavaType>> methodsToWatch = new HashMap<JavaSymbolName, List<JavaType>>();
            methodsToWatch.put(new JavaSymbolName("init"), new ArrayList<JavaType>());
            destType.setWatchedMethods(methodsToWatch);

            buildType(destType);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean isShared(JavaType type) {
        PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
        return ptmd != null && ptmd.getPhysicalLocationCanonicalPath().startsWith(GwtPath.SHARED.canonicalFileSystemPath(projectMetadata));
    }

    private TemplateDataDictionary buildDataDictionary(MirrorType destType) {
        JavaType javaType = getDestinationJavaType(destType);
        JavaType proxyType = getDestinationJavaType(MirrorType.PROXY);

        TemplateDataDictionary dataDictionary = TemplateDictionary.create();
        addImport(dataDictionary, proxyType.getFullyQualifiedTypeName());
        dataDictionary.setVariable("className", javaType.getSimpleTypeName());
        dataDictionary.setVariable("packageName", javaType.getPackage().getFullyQualifiedPackageName());
        dataDictionary.setVariable("placePackage", GwtPath.SCAFFOLD_PLACE.packageName(projectMetadata));
        dataDictionary.setVariable("scaffoldUiPackage", GwtPath.SCAFFOLD_UI.packageName(projectMetadata));
        dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectMetadata));
        dataDictionary.setVariable("uiPackage", GwtPath.MANAGED_UI.packageName(projectMetadata));
        dataDictionary.setVariable("name", governorTypeDetails.getName().getSimpleTypeName());
        dataDictionary.setVariable("pluralName", entityMetadata.getPlural());
        dataDictionary.setVariable("nameUncapitalized", StringUtils.uncapitalize(governorTypeDetails.getName().getSimpleTypeName()));
        dataDictionary.setVariable("proxy", proxyType.getSimpleTypeName());
        dataDictionary.setVariable("pluralName", entityMetadata.getPlural());
        dataDictionary.setVariable("proxyRenderer", GwtProxyProperty.getProxyRendererType(projectMetadata, proxyType));
        String proxyFields = null;
        GwtProxyProperty primaryProp = null;
        GwtProxyProperty secondaryProp = null;
        GwtProxyProperty dateProp = null;
        Set<String> importSet = new HashSet<String>();
        for (MethodMetadata method : proxy.getDeclaredMethods()) {

            if (!GwtProxyProperty.isAccessor(method)) {
                continue;
            }

            PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(method.getReturnType(), Path.SRC_MAIN_JAVA));
            GwtProxyProperty property = new GwtProxyProperty(projectMetadata, method, ptmd);

            // Determine if this is the primary property.
            if (primaryProp == null) {
                // Choose the first available field.
                primaryProp = property;
            } else if (property.isString() && !primaryProp.isString()) {
                // Favor String properties over other types.
                secondaryProp = primaryProp;
                primaryProp = property;
            } else if (secondaryProp == null) {
                // Choose the next available property.
                secondaryProp = property;
            } else if (property.isString() && !secondaryProp.isString()) {
                // Favor String properties over other types.
                secondaryProp = property;
            }

            // Determine if this is the first date property.
            if (dateProp == null && property.isDate()) {
                dateProp = property;
            }

            if (property.isProxy() || property.isCollectionOfProxy()) {
                if (proxyFields != null) {
                    proxyFields += ", ";
                } else {
                    proxyFields = "";
                }
                proxyFields += "\"" + property.getName() + "\"";
            }

            dataDictionary.addSection("fields").setVariable("field", property.getName());
            if (!isReadOnly(property.getName()))
                dataDictionary.addSection("editViewProps").setVariable("prop", property.forEditView());

            TemplateDataDictionary propertiesSection = dataDictionary.addSection("properties");
            propertiesSection.setVariable("prop", property.getName());
            propertiesSection.setVariable("propId", proxy.getName().getSimpleTypeName() + "_" + property.getName());
            propertiesSection.setVariable("propGetter", property.getGetter());
            propertiesSection.setVariable("propType", property.getType());
            propertiesSection.setVariable("propFormatter", property.getFormatter());
            propertiesSection.setVariable("propRenderer", property.getRenderer());
            propertiesSection.setVariable("propReadable", property.getReadableName());

            if (!isReadOnly(property.getName())) {
                TemplateDataDictionary editableSection = dataDictionary.addSection("editableProperties");
                editableSection.setVariable("prop", property.getName());
                editableSection.setVariable("propId", proxy.getName().getSimpleTypeName() + "_" + property.getName());
                editableSection.setVariable("propGetter", property.getGetter());
                editableSection.setVariable("propType", property.getType());
                editableSection.setVariable("propFormatter", property.getFormatter());
                editableSection.setVariable("propRenderer", property.getRenderer());
                editableSection.setVariable("propBinder", property.getBinder());
                editableSection.setVariable("propReadable", property.getReadableName());
            }

            dataDictionary.setVariable("proxyRendererType", MirrorType.EDIT_RENDERER.getPath().packageName(projectMetadata) + "." + proxy.getName().getSimpleTypeName() + "Renderer");

            if (property.isProxy() || property.isEnum() || property.isCollectionOfProxy()) {
                TemplateDataDictionary section = dataDictionary.addSection(property.isEnum() ? "setEnumValuePickers" : "setProxyValuePickers");
                section.setVariable("setValuePicker", property.getSetValuePickerMethod());
                section.setVariable("setValuePickerName", property.getSetValuePickerMethodName());
                section.setVariable("valueType", property.getValueType().getSimpleTypeName());
                section.setVariable("rendererType", property.getProxyRendererType());
                if (property.isProxy() || property.isCollectionOfProxy()) {
                    String propTypeName = StringUtils.uncapitalize(property.isCollectionOfProxy() ? method.getReturnType().getParameters().get(0).getSimpleTypeName() : method.getReturnType().getSimpleTypeName());
                    propTypeName = propTypeName.substring(0, propTypeName.indexOf("Proxy"));
                    section.setVariable("requestInterface", propTypeName + "Request");
                    section.setVariable("findMethod", "find" + StringUtils.capitalize(propTypeName) + "Entries(0, 50)");
                }
                maybeAddImport(dataDictionary, importSet, property.getPropertyType());
                if (property.isCollectionOfProxy()) {
                    maybeAddImport(dataDictionary, importSet,
                            property.getPropertyType().getParameters().get(0));
                    maybeAddImport(dataDictionary, importSet, property.getSetEditorType());
                }

            }

        }

        dataDictionary.setVariable("proxyFields", proxyFields);

        // Add a section for the mobile properties.
        if (primaryProp != null) {
            dataDictionary.setVariable("primaryProp", primaryProp.getName());
            dataDictionary.setVariable("primaryPropGetter", primaryProp.getGetter());
            dataDictionary.setVariable("primaryPropBuilder", primaryProp.forMobileListView("primaryRenderer"));
            TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
            section.setVariable("prop", primaryProp.getName());
            section.setVariable("propGetter", primaryProp.getGetter());
            section.setVariable("propType", primaryProp.getType());
            section.setVariable("propRenderer", primaryProp.getRenderer());
            section.setVariable("propRendererName", "primaryRenderer");
        } else {
            dataDictionary.setVariable("primaryProp", "id");
            dataDictionary.setVariable("primaryPropGetter", "getId");
            dataDictionary.setVariable("primaryPropBuilder", "");
        }
        if (secondaryProp != null) {
            dataDictionary.setVariable("secondaryPropBuilder", secondaryProp.forMobileListView("secondaryRenderer"));
            TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
            section.setVariable("prop", secondaryProp.getName());
            section.setVariable("propGetter", secondaryProp.getGetter());
            section.setVariable("propType", secondaryProp.getType());
            section.setVariable("propRenderer", secondaryProp.getRenderer());
            section.setVariable("propRendererName", "secondaryRenderer");
        } else {
            dataDictionary.setVariable("secondaryPropBuilder", "");
        }
        if (dateProp != null) {
            dataDictionary.setVariable("datePropBuilder", dateProp.forMobileListView("dateRenderer"));
            TemplateDataDictionary section = dataDictionary.addSection("mobileProperties");
            section.setVariable("prop", dateProp.getName());
            section.setVariable("propGetter", dateProp.getGetter());
            section.setVariable("propType", dateProp.getType());
            section.setVariable("propRenderer", dateProp.getRenderer());
            section.setVariable("propRendererName", "dateRenderer");
        } else {
            dataDictionary.setVariable("datePropBuilder", "");
        }
        return dataDictionary;
    }

    private void maybeAddImport(TemplateDataDictionary dataDictionary,
                                Set<String> importSet, JavaType type) {
        if (!importSet.contains(type.getFullyQualifiedTypeName())) {
            addImport(dataDictionary, type.getFullyQualifiedTypeName());
            importSet.add(type.getFullyQualifiedTypeName());
        }
    }

    private boolean isReadOnly(String name) {
        return name.equals(idPropertyName.getSymbolName()) || name.equals(versionPropertyName.getSymbolName());
    }

    private void buildListViewUiXml() {
        try {
            MirrorType destType = MirrorType.LIST_VIEW;
            String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";
            buildUiXml(getTemplateContents("ListViewUiXml", destType, null), destFile);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildMobileDetailsView() {
        try {
            MirrorType destType = MirrorType.MOBILE_DETAILS_VIEW;
            destType.setCreateAbstract(true);
            ArrayList<JavaSymbolName> watchFields = new ArrayList<JavaSymbolName>(orderedProxyFields.keySet());
            watchFields.add(new JavaSymbolName("proxy"));
            watchFields.add(new JavaSymbolName("displayRenderer"));
            destType.setWatchedFieldNames(watchFields);
            HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
            watchedMethods.put(new JavaSymbolName("setValue"), Collections.singletonList(proxy.getName()));
            destType.setWatchedMethods(watchedMethods);

            buildType(destType);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildMobileDetailsViewUiXml() {
        try {
            MirrorType destType = MirrorType.MOBILE_DETAILS_VIEW;
            String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";

            TemplateDataDictionary dataDictionary = buildDataDictionary(destType);
            TemplateLoader templateLoader = TemplateResourceLoader.create();
            Template template = templateLoader.getTemplate("MobileDetailsViewUiXml");
            String templateContents = template.renderToString(dataDictionary);

            buildUiXml(templateContents, destFile);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private MethodMetadataBuilder cloneMethod(MethodMetadataBuilder method, String metadataId) {
        MethodMetadataBuilder methodMetadataBuilder = new MethodMetadataBuilder(metadataId);
        methodMetadataBuilder.setMethodName(method.getMethodName());
        methodMetadataBuilder.setReturnType(method.getReturnType());
        methodMetadataBuilder.setBodyBuilder(method.getBodyBuilder());
        methodMetadataBuilder.setAnnotations(method.getAnnotations());
        if (method.getModifier() == Modifier.PRIVATE) {
            methodMetadataBuilder.setModifier(Modifier.PROTECTED);
        } else if (method.getModifier() == (Modifier.PRIVATE | Modifier.FINAL)) {
            methodMetadataBuilder.setModifier(Modifier.PROTECTED);
        } else {
            methodMetadataBuilder.setModifier(method.getModifier());
        }
        methodMetadataBuilder.setParameterNames(method.getParameterNames());
        methodMetadataBuilder.setParameterTypes(method.getParameterTypes());
        methodMetadataBuilder.setThrowsTypes(method.getThrowsTypes());
        methodMetadataBuilder.setCustomData(method.getCustomData());
        return methodMetadataBuilder;
    }

    private FieldMetadataBuilder cloneFieldBuilder(FieldMetadataBuilder field, String metadataId) {

        FieldMetadataBuilder fieldMetadataBuilder = new FieldMetadataBuilder(metadataId);
        fieldMetadataBuilder.setFieldName(field.getFieldName());
        fieldMetadataBuilder.setFieldType(field.getFieldType());
        if (field.getModifier() == Modifier.PRIVATE) {
            fieldMetadataBuilder.setModifier(Modifier.PROTECTED);
        } else if (field.getModifier() == (Modifier.PRIVATE | Modifier.FINAL)) {
            fieldMetadataBuilder.setModifier(Modifier.PROTECTED);
        } else {
            fieldMetadataBuilder.setModifier(field.getModifier());
        }
        fieldMetadataBuilder.setAnnotations(field.getAnnotations());
        fieldMetadataBuilder.setCustomData(field.getCustomData());
        fieldMetadataBuilder.setFieldInitializer(field.getFieldInitializer());

        return fieldMetadataBuilder;
    }

    private HashMap<JavaSymbolName, JavaType> resolveTypes(JavaType generic, JavaType typed) {
        HashMap<JavaSymbolName, JavaType> typeMap = new HashMap<JavaSymbolName, JavaType>();

        boolean typeCountMatch = generic.getParameters().size() == typed.getParameters().size();
        Assert.isTrue(typeCountMatch, "Type count must match.");

        int i = 0;
        for (JavaType genericParamType : generic.getParameters()) {
            typeMap.put(genericParamType.getArgName(), typed.getParameters().get(i));
            i++;
        }

        return typeMap;
    }

    private ClassOrInterfaceTypeDetailsBuilder createAbstractBuilder(ClassOrInterfaceTypeDetailsBuilder concreteClass) {

        JavaType concreteType = concreteClass.getName();
        String abstractName = concreteType.getSimpleTypeName() + "_Roo_Gwt";
        abstractName = concreteType.getPackage().getFullyQualifiedPackageName() + '.' + abstractName;
        JavaType abstractType = new JavaType(abstractName);
        String abstractId = PhysicalTypeIdentifier.createIdentifier(abstractType, Path.SRC_MAIN_JAVA);
        ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(abstractId);
        builder.setPhysicalTypeCategory(PhysicalTypeCategory.CLASS);
        builder.setName(abstractType);
        builder.setModifier(Modifier.ABSTRACT | Modifier.PUBLIC);
        builder.getExtendsTypes().addAll(concreteClass.getExtendsTypes());
        builder.getRegisteredImports().addAll(concreteClass.getRegisteredImports());


        for (JavaType extendedType : concreteClass.getExtendsTypes()) {
            String superTypeId = PhysicalTypeIdentifier.createIdentifier(extendedType, Path.SRC_MAIN_JAVA);
            if (getPhysicalTypeMetadata(superTypeId) == null) {
                continue;
            }
            ClassOrInterfaceTypeDetails superType = (ClassOrInterfaceTypeDetails) getPhysicalTypeMetadata(superTypeId).getMemberHoldingTypeDetails();

            for (ConstructorMetadata constructorMetadata : superType.getDeclaredConstructors()) {
                ConstructorMetadataBuilder abstractConstructor = new ConstructorMetadataBuilder(abstractId);
                abstractConstructor.setModifier(constructorMetadata.getModifier());

                HashMap<JavaSymbolName, JavaType> typeMap = resolveTypes(superType.getName(), extendedType);

                for (AnnotatedJavaType type : constructorMetadata.getParameterTypes()) {

                    JavaType newType = type.getJavaType();
                    if (type.getJavaType().getParameters().size() > 0) {
                        ArrayList<JavaType> paramTypes = new ArrayList<JavaType>();
                        for (JavaType typeType : type.getJavaType().getParameters()) {
                            JavaType typeParam = typeMap.get(new JavaSymbolName(typeType.toString()));
                            if (typeParam != null) {
                                paramTypes.add(typeParam);
                            }

                        }
                        newType = new JavaType(type.getJavaType().getFullyQualifiedTypeName(), type.getJavaType().getArray(), type.getJavaType().getDataType(), type.getJavaType().getArgName(), paramTypes);
                    }
                    abstractConstructor.getParameterTypes().add(new AnnotatedJavaType(newType, null));
                }
                abstractConstructor.setParameterNames(constructorMetadata.getParameterNames());

                InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
                bodyBuilder.newLine().indent().append("super(");

                int i = 0;
                for (JavaSymbolName paramName : abstractConstructor.getParameterNames()) {
                    bodyBuilder.append(" ").append(paramName.getSymbolName());
                    if (abstractConstructor.getParameterTypes().size() > i + 1) {
                        bodyBuilder.append(", ");
                    }
                    i++;
                }

                bodyBuilder.append(");");

                bodyBuilder.newLine().indentRemove();
                abstractConstructor.setBodyBuilder(bodyBuilder);
                builder.getDeclaredConstructors().add(abstractConstructor);
            }

        }

        return builder;
    }

    private void buildType(MirrorType destType) {
        buildType(destType, buildDataDictionary(destType));
    }

    private void buildType(MirrorType destType, TemplateDataDictionary dataDictionary) {
        try {

            Assert.notNull(dataDictionary, "TemplateDataDictionary instance is required");

            JavaType childType = getDestinationJavaType(destType);

            ClassOrInterfaceTypeDetails templateClass = getTemplateDetails(dataDictionary, destType.getTemplate(), childType);
            ClassOrInterfaceTypeDetailsBuilder templateClassBuilder = new ClassOrInterfaceTypeDetailsBuilder(templateClass);

            String concreteDestFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".java";

            if (destType.isCreateAbstract()) {
                ClassOrInterfaceTypeDetailsBuilder abstractClassBuilder = createAbstractBuilder(templateClassBuilder);


                ArrayList<FieldMetadataBuilder> fieldsToRemove = new ArrayList<FieldMetadataBuilder>();
                for (JavaSymbolName fieldName : destType.getWatchedFieldNames()) {

                    for (FieldMetadataBuilder fieldBuilder : templateClassBuilder.getDeclaredFields()) {
                        if (fieldBuilder.getFieldName().equals(fieldName)) {
                            abstractClassBuilder.addField(cloneFieldBuilder(new FieldMetadataBuilder(fieldBuilder.build()), abstractClassBuilder.getDeclaredByMetadataId()));
                            fieldsToRemove.add(fieldBuilder);
                            break;
                        }
                    }
                }

                templateClassBuilder.getDeclaredFields().removeAll(fieldsToRemove);

                ArrayList<MethodMetadataBuilder> methodsToRemove = new ArrayList<MethodMetadataBuilder>();
                for (JavaSymbolName methodName : destType.getWatchedMethods().keySet()) {
                    for (MethodMetadataBuilder methodBuilder : templateClassBuilder.getDeclaredMethods()) {
                        if (methodBuilder.getMethodName().equals(methodName)) {
                            if (destType.getWatchedMethods().get(methodName).equals(AnnotatedJavaType.convertFromAnnotatedJavaTypes(methodBuilder.getParameterTypes()))) {
                                abstractClassBuilder.addMethod(cloneMethod(methodBuilder, abstractClassBuilder.getDeclaredByMetadataId()));
                                methodsToRemove.add(methodBuilder);
                                break;
                            }
                        }
                    }
                }

                templateClassBuilder.getDeclaredMethods().removeAll(methodsToRemove);

                for (JavaType innerTypeName : destType.getWatchedInnerTypes()) {
                    for (ClassOrInterfaceTypeDetailsBuilder innerType : templateClassBuilder.getDeclaredInnerTypes()) {
                        if (innerType.getName().equals(innerTypeName)) {
                            ClassOrInterfaceTypeDetailsBuilder builder = new ClassOrInterfaceTypeDetailsBuilder(abstractClassBuilder.getDeclaredByMetadataId());
                            builder.setAnnotations(innerType.getAnnotations());
                            builder.setCustomData(innerType.getCustomData());
                            builder.setDeclaredConstructors(innerType.getDeclaredConstructors());
                            builder.setDeclaredFields(innerType.getDeclaredFields());
                            builder.setDeclaredInnerTypes(innerType.getDeclaredInnerTypes());
                            builder.setEnumConstants(innerType.getEnumConstants());
                            builder.setDeclaredInitializers(innerType.getDeclaredInitializers());
                            builder.setExtendsTypes(innerType.getExtendsTypes());
                            builder.setImplementsTypes(innerType.getImplementsTypes());
                            builder.setModifier(innerType.getModifier());
                            JavaType originalType = innerType.getName();
                            builder.setName(new JavaType(originalType.getSimpleTypeName() + "_Roo_Gwt", 0, DataType.TYPE, null, originalType.getParameters()));
                            builder.setPhysicalTypeCategory(innerType.getPhysicalTypeCategory());
                            builder.setRegisteredImports(innerType.getRegisteredImports());
                            builder.setSuperclass(innerType.getSuperclass());
                            builder.setDeclaredMethods(innerType.getDeclaredMethods());
                            abstractClassBuilder.addInnerType(builder);

                            templateClassBuilder.getDeclaredInnerTypes().remove(innerType);
                            if (innerType.getPhysicalTypeCategory().equals(PhysicalTypeCategory.INTERFACE)) {
                                ClassOrInterfaceTypeDetailsBuilder innerTypeBuilder = new ClassOrInterfaceTypeDetailsBuilder(innerType.build());

                                innerTypeBuilder.getDeclaredMethods().clear();
                                innerTypeBuilder.getDeclaredInnerTypes().clear();
                                innerTypeBuilder.getExtendsTypes().clear();
                                innerTypeBuilder.getExtendsTypes().add(new JavaType(builder.getName().getSimpleTypeName(), 0, DataType.TYPE, null, Collections.singletonList(new JavaType("V", 0, DataType.VARIABLE, null, new ArrayList<JavaType>()))));
                                templateClassBuilder.getDeclaredInnerTypes().add(innerTypeBuilder);
                            }

                            break;
                        }
                    }
                }

                abstractClassBuilder.setImplementsTypes(templateClass.getImplementsTypes());
                templateClassBuilder.getImplementsTypes().clear();

                templateClassBuilder.getExtendsTypes().clear();
                templateClassBuilder.getExtendsTypes().add(abstractClassBuilder.getName());

                String output = JavaParserMutableClassOrInterfaceTypeDetails.getOutput(abstractClassBuilder.build());
                output = "// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.\n\n" + output;

                String abstractDestFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + abstractClassBuilder.getName().getSimpleTypeName() + ".java";

                write(abstractDestFile, output, fileManager);
            }

            if (!fileManager.exists(concreteDestFile) || destType.isOverwriteConcrete()) {
                String output = JavaParserMutableClassOrInterfaceTypeDetails.getOutput(templateClassBuilder.build());
                output = "// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.\n\n" + output;
                write(concreteDestFile, output, fileManager);
            }


        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildDetailsView() {
        try {
            MirrorType destType = MirrorType.DETAILS_VIEW;
            destType.setCreateAbstract(true);
            ArrayList<JavaSymbolName> watchFields = new ArrayList<JavaSymbolName>(orderedProxyFields.keySet());
            watchFields.add(new JavaSymbolName("proxy"));
            watchFields.add(new JavaSymbolName("displayRenderer"));
            destType.setWatchedFieldNames(watchFields);
            HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
            watchedMethods.put(new JavaSymbolName("setValue"), Collections.singletonList(proxy.getName()));
            destType.setWatchedMethods(watchedMethods);

            buildType(destType);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ClassOrInterfaceTypeDetails getTemplateDetails(TemplateDataDictionary dataDictionary, String templateFile, JavaType templateType) {

        try {
            TemplateLoader templateLoader = TemplateResourceLoader.create();
            Template template = templateLoader.getTemplate(templateFile);
            String templateContents = template.renderToString(dataDictionary);

            String templateId = PhysicalTypeIdentifier.createIdentifier(templateType, Path.SRC_MAIN_JAVA);

            return new JavaParserMutableClassOrInterfaceTypeDetails(templateContents, templateId, templateType, metadataService, physicalTypeMetadataProvider);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildDetailsViewUiXml() {
        try {
            MirrorType destType = MirrorType.DETAILS_VIEW;
            String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";

            TemplateDataDictionary dataDictionary = buildDataDictionary(destType);
            TemplateLoader templateLoader = TemplateResourceLoader.create();
            Template template = templateLoader.getTemplate("DetailsViewUiXml");
            String templateContents = template.renderToString(dataDictionary);

            buildUiXml(templateContents, destFile);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildUiXml(String templateContents, String destFile) throws TransformerException, IOException, SAXException {

        Transformer transformer = XmlUtils.createIndentingTransformer();
        DocumentBuilder builder = XmlUtils.getDocumentBuilder();
        builder.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if (systemId.equals("http://dl.google.com/gwt/DTD/xhtml.ent")) {
                    return new InputSource(TemplateUtils.getTemplate(GwtMetadata.class, "templates/xhtml.ent"));
                } else {
                    // Use the default behaviour
                    return null;
                }
            }
        });

        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(templateContents));

        Document templateDocument = builder.parse(is);

        if (!fileManager.exists(destFile)) {


            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(templateDocument);
            transformer.transform(source, result);

            String xmlString = result.getWriter().toString();
            write(destFile, xmlString, fileManager);

            return;
        }

        is = new InputSource();
        is.setCharacterStream(new FileReader(destFile));
        Document existingDocument = builder.parse(is);

        Element existingHoldingElement = XmlUtils.findFirstElement("//*[@id='" + "boundElementHolder" + "']", existingDocument.getDocumentElement());
        Element templateHoldingElement = XmlUtils.findFirstElement("//*[@id='" + "boundElementHolder" + "']", templateDocument.getDocumentElement());


        if (existingHoldingElement != null) {

            HashMap<String, Element> templateElementMap = new LinkedHashMap<String, Element>();
            for (Element element : XmlUtils.findElements("//*[@id]", templateHoldingElement)) {
                templateElementMap.put(element.getAttribute("id"), element);
            }

            HashMap<String, Element> existingElementMap = new LinkedHashMap<String, Element>();
            for (Element element : XmlUtils.findElements("//*[@id]", existingHoldingElement)) {
                existingElementMap.put(element.getAttribute("id"), element);
            }

            ArrayList<Element> elementsToAdd = new ArrayList<Element>();
            for (String fieldName : templateElementMap.keySet()) {
                if (!existingElementMap.keySet().contains(fieldName)) {
                    elementsToAdd.add(templateElementMap.get(fieldName));
                }
            }

            ArrayList<Element> elementsToRemove = new ArrayList<Element>();
            for (String fieldName : existingElementMap.keySet()) {
                if (!templateElementMap.keySet().contains(fieldName)) {
                    elementsToRemove.add(existingElementMap.get(fieldName));
                }
            }

            for (Element element : elementsToAdd) {
                Node importedNode = existingDocument.importNode(element, true);
                existingHoldingElement.appendChild(importedNode);
            }

            for (Element element : elementsToRemove) {
                existingHoldingElement.removeChild(element);
            }

            if (elementsToAdd.size() > 0) {
                List<Element> sortedElements = new ArrayList<Element>();
                for (JavaSymbolName fieldName : orderedProxyFields.keySet()) {
                    Element element = XmlUtils.findFirstElement("//*[@id='" + fieldName.getSymbolName() + "']", existingHoldingElement);
                    if (element != null) {
                        sortedElements.add(element);
                    }
                }
                for (Element el : sortedElements) {
                    existingHoldingElement.removeChild(el);
                }

                for (Element el : sortedElements) {
                    existingHoldingElement.appendChild(el);
                }
            }

            if (elementsToAdd.size() > 0 || elementsToRemove.size() > 0) {
                StreamResult result = new StreamResult(new StringWriter());
                DOMSource source = new DOMSource(existingDocument);
                transformer.transform(source, result);

                String xmlString = result.getWriter().toString();
                write(destFile, xmlString, fileManager);
            }
        }

    }

    private void buildEditRenderer() {
        try {
            MirrorType destType = MirrorType.EDIT_RENDERER;
            destType.setCreateAbstract(false);
            HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
            for (MethodMetadata method : proxy.getDeclaredMethods()) {
                PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(method.getReturnType(), Path.SRC_MAIN_JAVA));
                GwtProxyProperty property = new GwtProxyProperty(projectMetadata, method, ptmd);
                if (property.isEnum() || property.isProxy() || property.isEmbeddable() || property.isCollectionOfProxy()) {
                    List<JavaType> params = new ArrayList<JavaType>();
                    JavaType param = new JavaType("java.util.Collection", 0, DataType.TYPE, null, Collections.singletonList(property.getPropertyType()));
                    params.add(param);
                    watchedMethods.put(new JavaSymbolName(property.getSetValuePickerMethodName()), params);
                }
            }
            watchedMethods.put(new JavaSymbolName("render"), Collections.singletonList(new JavaType(projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName() + ".client.scaffold.place.ProxyListPlace")));
            destType.setWatchedMethods(watchedMethods);
            buildType(destType);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildEditView() {
        try {
            MirrorType type = MirrorType.EDIT_VIEW;
            type.setCreateAbstract(true);
            TemplateDataDictionary dataDictionary = buildDataDictionary(type);
            addReference(dataDictionary, MirrorType.EDIT_ACTIVITY_WRAPPER);

            type.setWatchedFieldNames(new ArrayList<JavaSymbolName>(orderedProxyFields.keySet()));

            HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
            for (MethodMetadata method : proxy.getDeclaredMethods()) {
                PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(method.getReturnType(), Path.SRC_MAIN_JAVA));
                GwtProxyProperty property = new GwtProxyProperty(projectMetadata, method, ptmd);
                if (property.isEnum() || property.isProxy() || property.isEmbeddable() || property.isCollectionOfProxy()) {
                    List<JavaType> params = new ArrayList<JavaType>();
                    JavaType param = new JavaType("java.util.Collection", 0, DataType.TYPE, null, Collections.singletonList(property.getPropertyType()));
                    params.add(param);
                    watchedMethods.put(new JavaSymbolName(property.getSetValuePickerMethodName()), params);
                }
            }
            type.setWatchedMethods(watchedMethods);

            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildMobileEditView() {
        try {
            MirrorType type = MirrorType.MOBILE_EDIT_VIEW;
            type.setCreateAbstract(true);
            TemplateDataDictionary dataDictionary = buildDataDictionary(type);
            addReference(dataDictionary, MirrorType.EDIT_ACTIVITY_WRAPPER);

            type.setWatchedFieldNames(new ArrayList<JavaSymbolName>(orderedProxyFields.keySet()));

            HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
            for (MethodMetadata method : proxy.getDeclaredMethods()) {
                PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(method.getReturnType(), Path.SRC_MAIN_JAVA));
                GwtProxyProperty property = new GwtProxyProperty(projectMetadata, method, ptmd);
                if (property.isEnum() || property.isProxy() || property.isEmbeddable() || property.isCollectionOfProxy()) {
                    List<JavaType> params = new ArrayList<JavaType>();
                    JavaType param = new JavaType("java.util.Collection", 0, DataType.TYPE, null, Collections.singletonList(property.getPropertyType()));
                    params.add(param);
                    watchedMethods.put(new JavaSymbolName(property.getSetValuePickerMethodName()), params);
                }
            }
            type.setWatchedMethods(watchedMethods);

            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildEditViewUiXml() {
        try {
            MirrorType destType = MirrorType.EDIT_VIEW;
            String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";

            TemplateDataDictionary dataDictionary = buildDataDictionary(destType);
            TemplateLoader templateLoader = TemplateResourceLoader.create();
            Template template = templateLoader.getTemplate("EditViewUiXml");
            String templateContents = template.renderToString(dataDictionary);

            buildUiXml(templateContents, destFile);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildMobileEditViewUiXml() {
        try {
            MirrorType destType = MirrorType.MOBILE_EDIT_VIEW;
            String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";

            TemplateDataDictionary dataDictionary = buildDataDictionary(destType);
            TemplateLoader templateLoader = TemplateResourceLoader.create();
            Template template = templateLoader.getTemplate("MobileEditViewUiXml");
            String templateContents = template.renderToString(dataDictionary);

            buildUiXml(templateContents, destFile);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String getTemplateContents(String templateName, MirrorType destType, TemplateDataDictionary dataDictionary) throws TemplateException {
        if (dataDictionary == null) {
            dataDictionary = buildDataDictionary(destType);
        }
        TemplateLoader templateLoader = TemplateResourceLoader.create();
        Template template = templateLoader.getTemplate(templateName);
        return template.renderToString(dataDictionary);
    }

    private void buildSetEditor() {
        try {
            MirrorType type = MirrorType.SET_EDITOR;
            type.setCreateAbstract(true);
            buildType(type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildSetEditorUiXml() {
        try {
            MirrorType destType = MirrorType.SET_EDITOR;
            String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";
            buildUiXml(getTemplateContents("SetEditorUiXml", destType, null), destFile);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildListEditor() {
        try {
            MirrorType type = MirrorType.LIST_EDITOR;
            type.setCreateAbstract(true);
            buildType(type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildListEditorUiXml() {
        try {
            MirrorType destType = MirrorType.LIST_EDITOR;
            String destFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + getDestinationJavaType(destType).getSimpleTypeName() + ".ui.xml";
            buildUiXml(getTemplateContents("ListEditorUiXml", destType, null), destFile);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void write(String destFile, String newContents, FileManager fileManager) {


        // Write to disk, or update a file if it is already present
        MutableFile mutableFile = null;
        if (fileManager.exists(destFile)) {
            // First verify if the file has even changed
            File f = new File(destFile);
            String existing = null;
            try {
                existing = FileCopyUtils.copyToString(new FileReader(f));
            } catch (IOException ignoreAndJustOverwriteIt) {
            }

            if (!newContents.equals(existing)) {
                mutableFile = fileManager.updateFile(destFile);
            }
        } else {
            mutableFile = fileManager.createFile(destFile);
            Assert.notNull(mutableFile, "Could not create output file '" + destFile + "'");
        }

        try {
            if (mutableFile != null) {
                // If mutableFile was null, that means the source == destination content
                FileCopyUtils.copy(newContents.getBytes(), mutableFile.getOutputStream());
            }
        } catch (IOException ioe) {
            throw new IllegalStateException("Could not output '" + mutableFile.getCanonicalPath() + "'", ioe);
        }
    }

    private void buildRequest() {
        String destinationMetadataId = getDestinationMetadataId(MirrorType.REQUEST);
        JavaType name = PhysicalTypeIdentifier.getJavaType(destinationMetadataId);

        List<AnnotationMetadataBuilder> typeAnnotations = createAnnotations();
        // @Service(Employee.class)
        typeAnnotations.add(createAdditionalAnnotation(new JavaType("com.google.gwt.requestfactory.shared.ServiceName")));

        List<ConstructorMetadataBuilder> constructors = new ArrayList<ConstructorMetadataBuilder>();
        List<FieldMetadataBuilder> fields = new ArrayList<FieldMetadataBuilder>();
        List<MethodMetadataBuilder> methods = new ArrayList<MethodMetadataBuilder>();
        List<JavaType> extendsTypes = Collections.singletonList(new JavaType("com.google.gwt.requestfactory.shared.RequestContext"));
        List<JavaType> implementsTypes = new ArrayList<JavaType>();

        buildStaticRequestMethod(destinationMetadataId, methods, countMethod);
        buildStaticRequestMethod(destinationMetadataId, methods, findAllMethod);
        buildStaticRequestMethod(destinationMetadataId, methods, findEntriesMethod);
        buildStaticRequestMethod(destinationMetadataId, methods, findMethod);

        buildInstanceRequestMethod(destinationMetadataId, methods, entityMetadata.getRemoveMethod());
        buildInstanceRequestMethod(destinationMetadataId, methods, entityMetadata.getPersistMethod());

        ClassOrInterfaceTypeDetailsBuilder typeDetailsBuilder = new ClassOrInterfaceTypeDetailsBuilder(destinationMetadataId, Modifier.PUBLIC, name, PhysicalTypeCategory.INTERFACE);
        typeDetailsBuilder.setAnnotations(typeAnnotations);
        typeDetailsBuilder.setDeclaredConstructors(constructors);
        typeDetailsBuilder.setDeclaredFields(fields);
        typeDetailsBuilder.setDeclaredMethods(methods);
        typeDetailsBuilder.setExtendsTypes(extendsTypes);
        typeDetailsBuilder.setImplementsTypes(implementsTypes);

        String physicalLocationCanonicalPath = classpathOperations.getPhysicalLocationCanonicalPath(typeDetailsBuilder.getDeclaredByMetadataId());
        String contents = JavaParserMutableClassOrInterfaceTypeDetails.getOutput(typeDetailsBuilder.build());
        contents = "// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.\n\n" + contents;

        write(physicalLocationCanonicalPath, contents, fileManager);
    }

    private void buildInstanceRequestMethod(String destinationMetadataId,
                                            List<MethodMetadataBuilder> methods, MethodMetadata methodMetaData) {
        // com.google.gwt.requestfactory.shared.InstanceRequest remove()
        List<JavaType> methodReturnTypeArgs = Arrays.asList(new JavaType[]{getDestinationJavaType(MirrorType.PROXY), JavaType.VOID_OBJECT});
        JavaType methodReturnType = new JavaType("com.google.gwt.requestfactory.shared.InstanceRequest", 0, DataType.TYPE, null, methodReturnTypeArgs);

        buildRequestMethod(destinationMetadataId, methods, methodMetaData, methodReturnType);
    }

    private void buildStaticRequestMethod(String destinationMetadataId, List<MethodMetadataBuilder> methods, MethodMetadata methodMetaData) {
        // com.google.gwt.requestfactory.shared.Request<List<EmployeeProxy>> findAllEmployees();
        List<JavaType> methodReturnTypeArgs = Collections.singletonList(getGwtSideMethodType(methodMetaData.getReturnType()));
        JavaType methodReturnType = new JavaType("com.google.gwt.requestfactory.shared.Request", 0, DataType.TYPE, null, methodReturnTypeArgs);

        buildRequestMethod(destinationMetadataId, methods, methodMetaData, methodReturnType);
    }

    private void buildRequestMethod(String destinationMetadataId,
                                    List<MethodMetadataBuilder> methods, MethodMetadata methodMetaData,
                                    JavaType methodReturnType) {
        JavaSymbolName methodName = methodMetaData.getMethodName();

        List<JavaType> methodParameterTypes = new ArrayList<JavaType>();
        List<JavaSymbolName> methodParameterNames = new ArrayList<JavaSymbolName>(methodMetaData.getParameterNames());

        List<AnnotatedJavaType> paramTypes = methodMetaData.getParameterTypes();

        for (AnnotatedJavaType paramType : paramTypes) {
            JavaType jtype = paramType.getJavaType();
            if (methodName.equals(findMethod.getMethodName())) {
                jtype = entityMetadata.getIdentifierField().getFieldType();
            }
            methodParameterTypes.add(jtype);
        }

        methods.add(new MethodMetadataBuilder(destinationMetadataId, Modifier.ABSTRACT, methodName, methodReturnType, AnnotatedJavaType.convertFromJavaTypes(methodParameterTypes), methodParameterNames, new InvocableMemberBodyBuilder()));
    }

    /**
     * Return the type arg for the client side method, given the domain method return type.
     * if domainMethodReturnType is List<Integer> or Set<Integer>, returns the same.
     * if domainMethodReturnType is List<Employee>, return List<EmployeeProxy>
     *
     * @param domainMethodReturnType
     */
    private JavaType getGwtSideMethodType(JavaType domainMethodReturnType) {
        List<JavaType> typeParameters = domainMethodReturnType.getParameters();
        if (typeParameters == null || typeParameters.size() == 0) {
            PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(domainMethodReturnType, Path.SRC_MAIN_JAVA));
            return getGwtSideLeafType(domainMethodReturnType, ptmd);
        }
        List<JavaType> clientMethodTypeParameters = new ArrayList<JavaType>();
        for (JavaType domainSideType : typeParameters) {
            clientMethodTypeParameters.add(getGwtSideMethodType(domainSideType));
        }
        return new JavaType(domainMethodReturnType.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, clientMethodTypeParameters);
    }

    class ExportedMethod {
        JavaSymbolName operationName; // Mandatory
        JavaSymbolName methodName; // Mandatory
        JavaType returns; // Mandatory
        List<AnnotatedJavaType> args; // Mandatory, but can be empty
        boolean isList;
    }

    /**
     * @param mirrorType the mirror class we're producing (required)
     * @return the MID to the mirror class applicable for the current governor (never null)
     */
    private String getDestinationMetadataId(MirrorType mirrorType) {
        return PhysicalTypeIdentifier.createIdentifier(mirrorTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, governorTypeDetails.getName()), mirrorTypePath);
    }

    private JavaType getDestinationJavaType(JavaType physicalType, MirrorType mirrorType) {
        return mirrorTypeNamingStrategy.convertGovernorTypeNameIntoKeyTypeName(mirrorType, projectMetadata, physicalType);
    }

    /**
     * @param mirrorType the mirror class we're producing (required)
     * @return the Java type the mirror class applicable for the current governor (never null)
     */
    private JavaType getDestinationJavaType(MirrorType mirrorType) {
        return PhysicalTypeIdentifier.getJavaType(getDestinationMetadataId(mirrorType));
    }

    /**
     * @param sharedType the shared type to lookup(required)
     * @return the Java type the shared type applicable for the current project (never null)
     */
    private JavaType getDestinationJavaType(SharedType sharedType) {
        String packageName = sharedType.getPath().packageName(projectMetadata);
        String typeName = sharedType.getFullName();
        return new JavaType(packageName + "." + typeName);
    }

    private AnnotationMetadataBuilder createAdditionalAnnotation(JavaType serverType) {
        List<AnnotationAttributeValue<?>> serverTypeAttributes = new ArrayList<AnnotationAttributeValue<?>>();
        serverTypeAttributes.add(new StringAttributeValue(new JavaSymbolName("value"), governorTypeDetails.getName().getFullyQualifiedTypeName()));
        return new AnnotationMetadataBuilder(serverType, serverTypeAttributes);
    }

    /**
     * @return a newly-created type annotations list, complete with the @RooGwtMirroredFrom annotation properly setup
     */
    private List<AnnotationMetadataBuilder> createAnnotations() {
        List<AnnotationMetadataBuilder> annotations = new ArrayList<AnnotationMetadataBuilder>();
        List<AnnotationAttributeValue<?>> rooGwtMirroredFromConfig = new ArrayList<AnnotationAttributeValue<?>>();
        rooGwtMirroredFromConfig.add(new StringAttributeValue(new JavaSymbolName("value"), governorTypeDetails.getName().getFullyQualifiedTypeName()));
        annotations.add(new AnnotationMetadataBuilder(new JavaType(RooGwtMirroredFrom.class.getName()), rooGwtMirroredFromConfig));
        return annotations;
    }

    public static String getMetadataIdentifierType() {
        return PROVIDES_TYPE;
    }

    public static String createIdentifier(JavaType javaType, Path path) {
        return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
    }

    public static JavaType getJavaType(String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
    }

    public static Path getPath(String metadataIdentificationString) {
        return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
    }
}
