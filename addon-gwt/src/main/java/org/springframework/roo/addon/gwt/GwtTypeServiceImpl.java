package org.springframework.roo.addon.gwt;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides a basic implementation of {@link GwtTypeService}.
 *
 * @author James Tyrrell
 * @since 1.1.2
 */
@Component
@Service
public class GwtTypeServiceImpl implements GwtTypeService {
	@Reference private MetadataService metadataService;

	/**
	 * Return the type arg for the client side method, given the domain method return type.
	 * if domainMethodReturnType is List<Integer> or Set<Integer>, returns the same.
	 * if domainMethodReturnType is List<Employee>, return List<EmployeeProxy>
	 *
	 * @param type
	 * @param projectMetadata
	 * @param governorType
	 * @return
	 */
	public JavaType getGwtSideLeafType(JavaType type, ProjectMetadata projectMetadata, JavaType governorType, boolean requestType) {
		if (type.isPrimitive()) {
			if (!requestType) {
				GwtUtils.checkPrimitive(type);
			}
			return GwtUtils.convertPrimitiveType(type);
		}

		if (GwtUtils.isCommonType(type)) {
			return type;
		}

		if (GwtUtils.isCollectionType(type)) {
			List<JavaType> args = type.getParameters();
			if (args != null && args.size() == 1) {
				JavaType elementType = args.get(0);
				return new JavaType(type.getFullyQualifiedTypeName(), 0, DataType.TYPE, null, Arrays.asList(getGwtSideLeafType(elementType, projectMetadata, governorType, requestType)));
			}
			return type;
		}

		PhysicalTypeMetadata ptmd = (PhysicalTypeMetadata) metadataService.get(PhysicalTypeIdentifier.createIdentifier(type, Path.SRC_MAIN_JAVA));
		if (GwtUtils.isDomainObject(type, ptmd)) {

			if (GwtUtils.isEmbeddable(ptmd)) {
				throw new IllegalStateException("GWT does not currently support embedding objects in entities, such as '" + type.getSimpleTypeName() + "' in '" + governorType.getSimpleTypeName() + "'.");
			}

			return GwtUtils.getDestinationJavaType(type, GwtType.PROXY, projectMetadata);
		}

		return type;
	}

	public List<MemberHoldingTypeDetails> getExtendsTypes(ClassOrInterfaceTypeDetails childType) {
		List<MemberHoldingTypeDetails> extendsTypes = new ArrayList<MemberHoldingTypeDetails>();
		if (childType != null) {
			for (JavaType javaType : childType.getExtendsTypes()) {
				String superTypeId = PhysicalTypeIdentifier.createIdentifier(javaType, Path.SRC_MAIN_JAVA);
				if (metadataService.get(superTypeId) == null) {
					continue;
				}
				MemberHoldingTypeDetails superType = ((PhysicalTypeMetadata) metadataService.get(superTypeId)).getMemberHoldingTypeDetails();
				extendsTypes.add(superType);
			}
		}
		return extendsTypes;
	}
}
