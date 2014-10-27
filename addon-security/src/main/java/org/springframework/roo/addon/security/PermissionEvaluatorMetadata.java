package org.springframework.roo.addon.security;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.model.SpringJavaType.AUTHENTICATION;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;

public class PermissionEvaluatorMetadata extends
        AbstractItdTypeDetailsProvidingMetadataItem {
    private static final String PROVIDES_TYPE_STRING = PermissionEvaluatorMetadata.class
            .getName();
    private static final String PROVIDES_TYPE = MetadataIdentificationUtils
            .create(PROVIDES_TYPE_STRING);

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

    protected PermissionEvaluatorMetadata(String identifier,
            final JavaType aspectName,
            final PhysicalTypeMetadata governorPhysicalTypeMetadata,
            final MemberDetails governorDetails,
            final PermissionEvaluatorAnnotationValues annotationValues,
            final Map<JavaType, String> domainTypeToPlurals) {
        super(identifier, aspectName, governorPhysicalTypeMetadata);
        
        //Creates method hasPermission(Authentication authentication, Object targetDomainObject, Object permission)
        List<JavaType> hasPermissionParameterTypes = new ArrayList<JavaType>();
        hasPermissionParameterTypes.add(AUTHENTICATION);
        hasPermissionParameterTypes.add(JavaType.OBJECT);
        hasPermissionParameterTypes.add(JavaType.OBJECT);

        JavaSymbolName hasPermissionMethodName = new JavaSymbolName("hasPermission");
        if (!governorDetails.isMethodDeclaredByAnother(hasPermissionMethodName,
                hasPermissionParameterTypes, getId())) {
        	
        	List<JavaSymbolName> hasPermissionParameterNames = new ArrayList<JavaSymbolName>();
            hasPermissionParameterNames.add(new JavaSymbolName("authentication"));
            hasPermissionParameterNames.add(new JavaSymbolName("targetObject"));
            hasPermissionParameterNames.add(new JavaSymbolName("permission"));
            
            final InvocableMemberBodyBuilder hasPermissionBodyBuilder = new InvocableMemberBodyBuilder();
            
            hasPermissionBodyBuilder.append("\n\t\treturn checkManagedPermissions(authentication, targetObject, permission);");

            MethodMetadataBuilder hasPermissionMethodMetadataBuilder = new MethodMetadataBuilder(
                    getId(), PUBLIC, hasPermissionMethodName, JavaType.BOOLEAN_PRIMITIVE,
                    AnnotatedJavaType
                            .convertFromJavaTypes(hasPermissionParameterTypes),
                    hasPermissionParameterNames, hasPermissionBodyBuilder);

            builder.addMethod(hasPermissionMethodMetadataBuilder.build());
        }
        
        //Creates method hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission)
        hasPermissionParameterTypes = new ArrayList<JavaType>();
        hasPermissionParameterTypes.add(AUTHENTICATION);
        hasPermissionParameterTypes.add(JavaType.SERIALIZABLE);
        hasPermissionParameterTypes.add(JavaType.STRING);
        hasPermissionParameterTypes.add(JavaType.OBJECT);

        if (!governorDetails.isMethodDeclaredByAnother(hasPermissionMethodName,
                hasPermissionParameterTypes, getId())) {
            final InvocableMemberBodyBuilder hasPermissionBodyBuilder2 = new InvocableMemberBodyBuilder();
            hasPermissionBodyBuilder2.append(String.format("\n\t\treturn %s;\n",annotationValues.getDefaultReturnValue()));

            List<JavaSymbolName> hasPermissionParameterNames2 = new ArrayList<JavaSymbolName>();
            hasPermissionParameterNames2.add(new JavaSymbolName(
                    "authentication"));
            hasPermissionParameterNames2.add(new JavaSymbolName("targetId"));
            hasPermissionParameterNames2.add(new JavaSymbolName("targetType"));
            hasPermissionParameterNames2.add(new JavaSymbolName("permission"));

            MethodMetadataBuilder hasPermissionMethodMetadataBuilder2 = new MethodMetadataBuilder(
                    getId(),
                    PUBLIC,
                    new JavaSymbolName("hasPermission"),
                    JavaType.BOOLEAN_PRIMITIVE,
                    AnnotatedJavaType
                            .convertFromJavaTypes(hasPermissionParameterTypes),
                    hasPermissionParameterNames2, hasPermissionBodyBuilder2);

            builder.addMethod(hasPermissionMethodMetadataBuilder2.build());
        }
        
        List<JavaType> checkManagedPermissionsParameterTypes = new ArrayList<JavaType>();
        checkManagedPermissionsParameterTypes.add(AUTHENTICATION);
        checkManagedPermissionsParameterTypes.add(JavaType.OBJECT);
        checkManagedPermissionsParameterTypes.add(JavaType.OBJECT);

        JavaSymbolName checkManagedPermissionsMethodName = new JavaSymbolName("checkManagedPermissions");
        
        if (!governorDetails.isMethodDeclaredByAnother(checkManagedPermissionsMethodName,
        		checkManagedPermissionsParameterTypes, getId())) {
        	
        	List<JavaSymbolName> checkManagedPermissionsParameterNames = new ArrayList<JavaSymbolName>();
        	checkManagedPermissionsParameterNames
                    .add(new JavaSymbolName("authentication"));
        	checkManagedPermissionsParameterNames.add(new JavaSymbolName("targetObject"));
        	checkManagedPermissionsParameterNames.add(new JavaSymbolName("permission"));
            
            final InvocableMemberBodyBuilder checkManagedPermissionsBodyBuilder = new InvocableMemberBodyBuilder();
            boolean firstPass = true;
            for (Entry<JavaType, String> entrySet : domainTypeToPlurals.entrySet()) {
            	for (Permission permission : Permission.values()){
    		    	String permissionName = permission.getName(entrySet.getKey(), entrySet.getValue());
    				if (permissionName == null) {
    					continue;
    				}
	            	checkManagedPermissionsBodyBuilder.append(String.format("\n\t\t%sif(permission.equals(\"%s\")){",firstPass ? "" : "else ", permissionName));
	            	checkManagedPermissionsBodyBuilder.append(String.format("\n\t\t\treturn %s(authentication, (%s)targetObject);", permissionName, entrySet.getKey().getFullyQualifiedTypeName()));
	            	checkManagedPermissionsBodyBuilder.append("\n\t\t}");
            	}
            	firstPass = false;
            }
            checkManagedPermissionsBodyBuilder.append(String.format("\n\t\treturn %s;\n",annotationValues.getDefaultReturnValue()));

            MethodMetadataBuilder hasPermissionMethodMetadataBuilder = new MethodMetadataBuilder(
                    getId(), PUBLIC, checkManagedPermissionsMethodName, JavaType.BOOLEAN_PRIMITIVE,
                    AnnotatedJavaType
                            .convertFromJavaTypes(checkManagedPermissionsParameterTypes),
                            checkManagedPermissionsParameterNames, checkManagedPermissionsBodyBuilder);

            builder.addMethod(hasPermissionMethodMetadataBuilder.build());
            
        }
        
        for (Entry<JavaType, String> entrySet : domainTypeToPlurals.entrySet()) {
        	for (Permission permission : Permission.values()){
		    	String permissionName = permission.getName(entrySet.getKey(), entrySet.getValue());
				if (permissionName == null) {
					continue;
				}
	        	JavaSymbolName isAllowedMethodName = new JavaSymbolName(permissionName);
	        	List<JavaType> isAllowedParameterTypes = new ArrayList<JavaType>();
	        	isAllowedParameterTypes.add(AUTHENTICATION);
	        	isAllowedParameterTypes.add(entrySet.getKey());
	        	if (!governorDetails.isMethodDeclaredByAnother(isAllowedMethodName, isAllowedParameterTypes, getId())) {
	        		List<JavaSymbolName> isAllowedParameterNames = new ArrayList<JavaSymbolName>();
	        		isAllowedParameterNames.add(new JavaSymbolName("authentication"));
	        		isAllowedParameterNames.add(JavaSymbolName.getReservedWordSafeName(entrySet.getKey()));
	                
	                final InvocableMemberBodyBuilder isAllowedBodyBuilder = new InvocableMemberBodyBuilder();
	                isAllowedBodyBuilder.append(String.format("\n\t\treturn %s;\n",annotationValues.getDefaultReturnValue()));
	
	                MethodMetadataBuilder isAllowedMethodMetadataBuilder = new MethodMetadataBuilder(
	                        getId(), PUBLIC, isAllowedMethodName, JavaType.BOOLEAN_PRIMITIVE,
	                        AnnotatedJavaType
	                                .convertFromJavaTypes(isAllowedParameterTypes),
	                                isAllowedParameterNames, isAllowedBodyBuilder);
	
	                builder.addMethod(isAllowedMethodMetadataBuilder.build());
	        	}
        	}
        }
        
        itdTypeDetails = builder.build();
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
