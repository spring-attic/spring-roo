package org.springframework.roo.addon.gwt;

import hapax.Template;
import hapax.TemplateDataDictionary;
import hapax.TemplateDictionary;
import hapax.TemplateException;
import hapax.TemplateLoader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.ConstructorMetadataBuilder;
import org.springframework.roo.classpath.details.FieldMetadataBuilder;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.StringUtils;

/**
 * Listens for the creation and deletion of files by {@link GwtMetadata}.
 *
 * @author Ben Alex
 * @author Alan Stewart
 * @author Ray Cromwell
 * @author Amit Manjhi
 * @author Ray Ryan
 */
@Component
@Service
public class GwtFileListener implements FileEventListener {
    @Reference private FileManager fileManager;
    @Reference private MetadataService metadataService;
    @Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
    private ProjectMetadata projectMetadata;
    private boolean processedApplicationFiles = false;

    public void onFileEvent(FileEvent fileEvent) {
        projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
        if (projectMetadata == null) {
            return;
        }

        String eventPath = fileEvent.getFileDetails().getCanonicalPath();

        if (!eventPath.endsWith(".java")) {
            return;
        }
        boolean isMaintainedByRoo = eventPath.startsWith(GwtPath.MANAGED_REQUEST.canonicalFileSystemPath(projectMetadata)) || eventPath.startsWith(GwtPath.MANAGED.canonicalFileSystemPath(projectMetadata));
        if (!isMaintainedByRoo && (processedApplicationFiles || !eventPath.startsWith(GwtPath.SCAFFOLD.canonicalFileSystemPath(projectMetadata)))) {
            return;
        }

        //TODO: What does this even do? Is it still needed? - JT
        // Something happened with a GWT auto-generated *.java file (or we're starting monitoring)
       /* if (isMaintainedByRoo) {
            // First thing is for us to figure out the proxy file (or what it used to be called, if it has gone away)
            String proxyFile = null;
            if (eventPath.endsWith("Proxy.java")) {
                proxyFile = eventPath;
            } else {
                String name = fileEvent.getFileDetails().getFile().getName();
                name = name.substring(0, name.length() - 5); // Drop .java
                for (SharedType t : SharedType.values()) {
                    if (name.endsWith(t.getFullName()) || name.endsWith("_Roo_Gwt")) {
                        // This is just a shared type; we don't care about changes to them
                        return;
                    }
                }

                // A suffix could be inclusive of another suffix, so we need to find the best (longest) match,
                // not necessarily the first match.
                String bestMatch = "";
                for (MirrorType t : MirrorType.values()) {
                    String suffix = t.getSuffix();
                    if (name.endsWith(suffix) && suffix.length() > bestMatch.length()) {
                        // Drop the part of the filename with the suffix, as well as the extension
                        bestMatch = suffix;
                        String entityName = name.substring(0, name.lastIndexOf(suffix));
                        proxyFile = GwtPath.MANAGED_REQUEST.canonicalFileSystemPath(projectMetadata, entityName + "Proxy.java");
                    }
                }
            }
            Assert.hasText(proxyFile, "Proxy file not computed for input " + eventPath);

            // Calculate the name without the "Proxy.java" portion (simplifies working with it later)
            String simpleName = new File(proxyFile).getName();
            simpleName = simpleName.substring(0, simpleName.length() - 10); // Drop Proxy.java

            Assert.hasText(simpleName, "Simple name not computed for input " + eventPath);
        }*/

        // By this point the directory structure should correspond to files that should exist

        // Now we need to refresh all the application-wide files
        processedApplicationFiles = true;
        updateApplicationEntityTypesProcessor(fileManager, projectMetadata);
        updateApplicationRequestFactory(fileManager, projectMetadata);
        updateListPlaceRenderer(fileManager, projectMetadata);
        updateMasterActivities();
        updateDetailsActivities();
        updateMobileActivities();
    }

    public void updateApplicationEntityTypesProcessor(FileManager fileManager, ProjectMetadata projectMetadata) {
        SharedType type = SharedType.APP_ENTITY_TYPES_PROCESSOR;
        TemplateDataDictionary dataDictionary = buildDataDictionary(type);
        type.setOverwriteConcrete(true);
        MirrorType locate = MirrorType.PROXY;
        String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
        for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
            String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
            String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename

            dataDictionary.addSection("proxys").setVariable("proxy", fullPath);

            String entity1 = new StringBuilder("\t\tif (").append(fullPath).append(".class.equals(clazz)) {\n\t\t\tprocessor.handle").append(simpleName).append("((").append(fullPath).append(") null);\n\t\t\treturn;\n\t\t}").toString();
            dataDictionary.addSection("entities1").setVariable("entity", entity1);

            String entity2 = new StringBuilder("\t\tif (proxy instanceof ").append(fullPath).append(") {\n\t\t\tprocessor.handle").append(simpleName).append("((").append(fullPath).append(") proxy);\n\t\t\treturn;\n\t\t}").toString();
            dataDictionary.addSection("entities2").setVariable("entity", entity2);

            String entity3 = new StringBuilder("\tpublic abstract void handle").append(simpleName).append("(").append(fullPath).append(" proxy);").toString();
            dataDictionary.addSection("entities3").setVariable("entity", entity3);
        }

        try {
            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void buildType(SharedType destType, TemplateDataDictionary dataDictionary) {
        try {

            Assert.notNull(dataDictionary, "TemplateDataDictionary instance is required");

            JavaType childType = getDestinationJavaType(destType);

            if (dataDictionary == null) {
                dataDictionary = buildDataDictionary(destType);
            }

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

                String output = physicalTypeMetadataProvider.getCompilationUnitContents(abstractClassBuilder.build());
                output = "// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.\n\n" + output;

                String abstractDestFile = destType.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + abstractClassBuilder.getName().getSimpleTypeName() + ".java";

                write(abstractDestFile, output, fileManager);
            }

            if (!fileManager.exists(concreteDestFile) || destType.isOverwriteConcrete()) {
                String output = physicalTypeMetadataProvider.getCompilationUnitContents(templateClassBuilder.build());
                output = "// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.\n\n" + output;
                write(concreteDestFile, output, fileManager);
            }


        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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

    private PhysicalTypeMetadata getPhysicalTypeMetadata(String declaredByMetadataId) {
        return (PhysicalTypeMetadata) metadataService.get(declaredByMetadataId);
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

    private ClassOrInterfaceTypeDetails getTemplateDetails(TemplateDataDictionary dataDictionary, String templateFile, JavaType templateType) {

        try {
            TemplateLoader templateLoader = TemplateResourceLoader.create();
            Template template = templateLoader.getTemplate(templateFile);
            String templateContents = template.renderToString(dataDictionary);

            String templateId = PhysicalTypeIdentifier.createIdentifier(templateType, Path.SRC_MAIN_JAVA);

            return physicalTypeMetadataProvider.parse(templateContents, templateId, templateType);

        } catch (TemplateException e) {
            e.printStackTrace();
        }

        return null;

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

    public void updateApplicationRequestFactory(FileManager fileManager, ProjectMetadata projectMetadata) {
        SharedType type = SharedType.APP_REQUEST_FACTORY;
        type.setOverwriteConcrete(true);
        TemplateDataDictionary dataDictionary = buildDataDictionary(type);
        dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectMetadata));

        MirrorType locate = MirrorType.PROXY;
        String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
        for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
            String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
            String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
            String entity = new StringBuilder("\t").append(simpleName).append("Request ").append(StringUtils.uncapitalize(simpleName)).append("Request();").toString();
            dataDictionary.addSection("entities").setVariable("entity", entity);
        }

        if (projectMetadata.isGaeEnabled()) {
            dataDictionary.showSection("gae");
        }

        try {
            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String getQualifiedType(MirrorType type, ProjectMetadata projectMetadata, String clazz) {
        return type.getPath().packageName(projectMetadata) + "." + clazz + type.getSuffix();
    }

    public static String getQualifiedType(SharedType type, ProjectMetadata projectMetadata) {
        return type.getFullyQualifiedTypeName(projectMetadata);
    }

    public void updateListPlaceRenderer(FileManager fileManager, ProjectMetadata projectMetadata) {
        SharedType type = SharedType.LIST_PLACE_RENDERER;
        TemplateDataDictionary dataDictionary = buildDataDictionary(type);
        addReference(dataDictionary, SharedType.APP_ENTITY_TYPES_PROCESSOR);

        MirrorType locate = MirrorType.PROXY;
        String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
        for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
            String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
            String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
            TemplateDataDictionary section = dataDictionary.addSection("entities");
            section.setVariable("entitySimpleName", simpleName);
            section.setVariable("entityFullPath", fullPath);
            addImport(dataDictionary, MirrorType.PROXY.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.PROXY.getSuffix());
        }


        HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
        watchedMethods.put(new JavaSymbolName("render"), Collections.singletonList(new JavaType(projectMetadata.getTopLevelPackage().getFullyQualifiedPackageName() + ".client.scaffold.place.ProxyListPlace")));
        type.setWatchedMethods(watchedMethods);

        type.setCreateAbstract(true);

        try {
            buildType(type, dataDictionary);
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

    public void updateMasterActivities() {
        SharedType type = SharedType.MASTER_ACTIVITIES;
        TemplateDataDictionary dataDictionary = buildDataDictionary(type);
        addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
        addReference(dataDictionary, SharedType.APP_ENTITY_TYPES_PROCESSOR);
        addReference(dataDictionary, SharedType.SCAFFOLD_APP);

        MirrorType locate = MirrorType.PROXY;
        String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
        for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
            String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
            String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
            TemplateDataDictionary section = dataDictionary.addSection("entities");
            section.setVariable("entitySimpleName", simpleName);
            section.setVariable("entityFullPath", fullPath);
            addImport(dataDictionary, simpleName, MirrorType.LIST_ACTIVITY);
            addImport(dataDictionary, simpleName, MirrorType.PROXY);
            addImport(dataDictionary, simpleName, MirrorType.LIST_VIEW);
            addImport(dataDictionary, simpleName, MirrorType.MOBILE_LIST_VIEW);

            ArrayList<JavaSymbolName> watchFields = new ArrayList<JavaSymbolName>();
            watchFields.add(new JavaSymbolName("requests"));
            watchFields.add(new JavaSymbolName("placeController"));
            type.setWatchedFieldNames(watchFields);

            HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
            watchedMethods.put(new JavaSymbolName("getActivity"), Collections.singletonList(new JavaType("com.google.gwt.place.shared.Place")));
            type.setWatchedMethods(watchedMethods);

            type.setCreateAbstract(true);

        }

        try {
            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void addImport(TemplateDataDictionary dataDictionary,
                           String simpleName, MirrorType mirrorType) {
        addImport(dataDictionary, mirrorType.getPath().packageName(projectMetadata) + "." + simpleName + mirrorType.getSuffix());
    }

    public void updateDetailsActivities() {
        SharedType type = SharedType.DETAILS_ACTIVITIES;
        TemplateDataDictionary dataDictionary = buildDataDictionary(type);
        addReference(dataDictionary, SharedType.APP_REQUEST_FACTORY);
        addReference(dataDictionary, SharedType.APP_ENTITY_TYPES_PROCESSOR);

        MirrorType locate = MirrorType.PROXY;
        String antPath = locate.getPath().canonicalFileSystemPath(projectMetadata) + File.separatorChar + "**" + locate.getSuffix() + ".java";
        for (FileDetails fd : fileManager.findMatchingAntPath(antPath)) {
            String fullPath = fd.getFile().getName().substring(0, fd.getFile().getName().length() - 5); // Drop .java from filename
            String simpleName = fullPath.substring(0, fullPath.length() - locate.getSuffix().length()); // Drop "Proxy" suffix from filename
            String entity = new StringBuilder("\t\t\tpublic void handle").append(simpleName).append("(").append(fullPath).append(" proxy) {\n").append("\t\t\t\tsetResult(new ").append(simpleName).append("ActivitiesMapper(requests, placeController).getActivity(proxyPlace));\n\t\t\t}").toString();
            dataDictionary.addSection("entities").setVariable("entity", entity);
            addImport(dataDictionary, MirrorType.PROXY.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.PROXY.getSuffix());
            addImport(dataDictionary, MirrorType.ACTIVITIES_MAPPER.getPath().packageName(projectMetadata) + "." + simpleName + MirrorType.ACTIVITIES_MAPPER.getSuffix());
        }

        ArrayList<JavaSymbolName> watchFields = new ArrayList<JavaSymbolName>();
        watchFields.add(new JavaSymbolName("requests"));
        watchFields.add(new JavaSymbolName("placeController"));
        type.setWatchedFieldNames(watchFields);

        HashMap<JavaSymbolName, List<JavaType>> watchedMethods = new HashMap<JavaSymbolName, List<JavaType>>();
        watchedMethods.put(new JavaSymbolName("getActivity"), Collections.singletonList(new JavaType("com.google.gwt.place.shared.Place")));
        type.setWatchedMethods(watchedMethods);

        type.setCreateAbstract(true);

        try {
            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void updateMobileActivities() {
        SharedType type = SharedType.MOBILE_ACTIVITIES;
        TemplateDataDictionary dataDictionary = buildDataDictionary(type);

        try {
            buildType(type, dataDictionary);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private TemplateDataDictionary buildDataDictionary(SharedType destType) {
        JavaType javaType = getDestinationJavaType(destType);
        TemplateDataDictionary dataDictionary = TemplateDictionary.create();
        dataDictionary.setVariable("className", javaType.getSimpleTypeName());
        dataDictionary.setVariable("packageName", javaType.getPackage().getFullyQualifiedPackageName());
        dataDictionary.setVariable("placePackage", GwtPath.SCAFFOLD_PLACE.packageName(projectMetadata));
        dataDictionary.setVariable("sharedScaffoldPackage", GwtPath.SHARED_SCAFFOLD.packageName(projectMetadata));
        dataDictionary.setVariable("sharedGaePackage", GwtPath.SHARED_GAE.packageName(projectMetadata));
        return dataDictionary;
    }

    private JavaType getDestinationJavaType(SharedType destType) {
        return new JavaType(destType.getFullyQualifiedTypeName(projectMetadata));
    }

    private void addReference(TemplateDataDictionary dataDictionary, SharedType type) {
        addImport(dataDictionary, getDestinationJavaType(type).getFullyQualifiedTypeName());
        dataDictionary.setVariable(type.getName(), getDestinationJavaType(type).getSimpleTypeName());
    }

    private void addImport(TemplateDataDictionary dataDictionary, String importDeclaration) {
        dataDictionary.addSection("imports").setVariable("import", importDeclaration);
    }
}
