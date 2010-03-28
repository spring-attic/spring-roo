package org.springframework.roo.addon.finder;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.EntityMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.support.logging.HandlerUtils;
import org.springframework.roo.support.util.Assert;

/**
 * Provides Finder addon operations. 
 * 
 * @author Stefan Schmidt
 * @since 1.0
 *
 */
@Component
@Service
public class FinderOperationsImpl implements FinderOperations {
	
	private static final Logger logger = HandlerUtils.getLogger(FinderOperationsImpl.class);
	
	@Reference private FileManager fileManager;
	@Reference private PathResolver pathResolver;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private MetadataService metadataService;
	
	public boolean isFinderCommandAvailable() {
		return fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}
	
	public SortedSet<String> listFindersFor(JavaType typeName, Integer depth) {
		Assert.notNull(typeName, "Java type required");
		
		String id = physicalTypeMetadataProvider.findIdentifier(typeName);
		if (id == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + typeName.getFullyQualifiedTypeName() + "'");
		}

		// Go and get the entity metadata, as any type with finders has to be an entity
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(id);
		Path path = PhysicalTypeIdentifier.getPath(id);
		String entityMid = EntityMetadata.createIdentifier(javaType, path);
		
		// Get the entity metadata
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMid);
		if (entityMetadata == null) {
			throw new IllegalArgumentException("Cannot provide finders because '" + typeName.getFullyQualifiedTypeName() + "' is not a entity");
		}
		
		// We also need the Bean Info metadata
		String beanInfoMid = BeanInfoMetadata.createIdentifier(javaType, path);
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMid);
		Assert.notNull(beanInfoMetadata, "Bean info ('" + beanInfoMid +"') was unexpectedly unavailable when entity metadata was available for '" + entityMetadata + "'");
		
		// Compute the finders
		DynamicFinderServices finderService = new DynamicFinderServicesImpl();
		List<JavaSymbolName> finders = finderService.getFindersFor(beanInfoMetadata, entityMetadata.getPlural(), depth);

		SortedSet<String> result = new TreeSet<String>();
		for (JavaSymbolName finder : finders) {
			// Avoid displaying problematic finders
			try {
				List<JavaSymbolName> paramNames = finderService.getParameterNames(finder, entityMetadata.getPlural(), beanInfoMetadata);
				List<JavaType> paramTypes = finderService.getParameterTypes(finder, entityMetadata.getPlural(), beanInfoMetadata);
				StringBuilder signature = new StringBuilder();
				int x = -1;
				for (JavaType param : paramTypes) {
					x++;
					if (x > 0) {
						signature.append(", ");
					}
					signature.append(param.getSimpleTypeName()).append(" ").append(paramNames.get(x).getSymbolName());
				}
				result.add(finder.getSymbolName() + "(" + signature + ")" /* query: '" + query + "'"*/);
			} catch (RuntimeException ex) {
				logger.warning("failure");
				result.add(finder.getSymbolName() + " - failure");
			}
		}
		return result;
	}
	
 	public void installFinder(JavaType typeName, JavaSymbolName finderName) {
 		Assert.notNull(typeName, "Java type required");
 		Assert.notNull(finderName, "Finer name required");
 		
		String id = physicalTypeMetadataProvider.findIdentifier(typeName);
		if (id == null) {
			logger.warning("Cannot locate source for '" + typeName.getFullyQualifiedTypeName() + "'");
			return;
		}
		
		// Go and get the entity metadata, as any type with finders has to be an entity
		JavaType javaType = PhysicalTypeIdentifier.getJavaType(id);
		Path path = PhysicalTypeIdentifier.getPath(id);
		String entityMid = EntityMetadata.createIdentifier(javaType, path);
		
		// Get the entity metadata
		EntityMetadata entityMetadata = (EntityMetadata) metadataService.get(entityMid);
		if (entityMetadata == null) {
			logger.warning("Cannot provide finders because '" + typeName.getFullyQualifiedTypeName() + "' is not a entity");
			return;
		}
		
		// We also need the Bean Info metadata
		String beanInfoMid = BeanInfoMetadata.createIdentifier(javaType, path);
		BeanInfoMetadata beanInfoMetadata = (BeanInfoMetadata) metadataService.get(beanInfoMid);
		Assert.notNull(beanInfoMetadata, "Bean info ('" + beanInfoMid +"') was unexpectedly unavailable when entity metadata was available for '" + entityMetadata + "'");

		// We know the file exists, as there's already entity metadata for it		
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(id);
		if (physicalTypeMetadata == null) {
			// For some reason we found the source file a few lines ago, but suddenly it has gone away
			logger.warning("Cannot provide finders because '" + typeName.getFullyQualifiedTypeName() + "' is unavailable");
			return;
		}
		PhysicalTypeDetails ptd = physicalTypeMetadata.getPhysicalTypeDetails();
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd);
		MutableClassOrInterfaceTypeDetails mutable = (MutableClassOrInterfaceTypeDetails) ptd;
		
		// We know there should be an existing Entity annotation
		List<? extends AnnotationMetadata> annotations = mutable.getTypeAnnotations();
		AnnotationMetadata found = MemberFindingUtils.getAnnotationOfType(annotations, new JavaType(RooEntity.class.getName()));
		if (found == null) {
			logger.warning("Unable to find the entity annotation on '" + typeName.getFullyQualifiedTypeName() + "'");
			return;
		}

		// Confirm they typed a valid finder name
		DynamicFinderServices finderService = new DynamicFinderServicesImpl();
		try {
			finderService.getJpaQueryFor(finderName, entityMetadata.getPlural(), beanInfoMetadata);
		} catch (RuntimeException ex) {
			// TODO: Improve detection of invalid finder names
			logger.warning("The finder name '" + finderName.getSymbolName() + "' contains an error");
			throw ex;
		}
		
		// Make a destination list to store our final attributes
		List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
		List<StringAttributeValue> desiredFinders = new ArrayList<StringAttributeValue>();
		
		// Copy the existing attributes, excluding the "finder" attribute
		boolean alreadyAdded = false;
		for (JavaSymbolName attributeName : found.getAttributeNames()) {
			AnnotationAttributeValue<?> val = found.getAttribute(attributeName);

			if ("finders".equals(attributeName.getSymbolName())) {
				// Ensure we have an array of strings
				if (!(val instanceof ArrayAttributeValue<?>)) {
					logger.warning("Annotation " + RooEntity.class.getSimpleName() + " attribute 'finders' must be an array of strings");
					return;
				}
				ArrayAttributeValue<?> arrayVal = (ArrayAttributeValue<?>) val;
				for (Object o : arrayVal.getValue()) {
					if (!(o instanceof StringAttributeValue)) {
						logger.warning("Annotation " + RooEntity.class.getSimpleName() + " attribute 'finders' must be an array of strings");
						return;
					}
					StringAttributeValue sv = (StringAttributeValue) o;
					if (sv.getValue().equals(finderName.getSymbolName())) {
						alreadyAdded = true;
					}
					desiredFinders.add(sv);
				}
				continue;
			}
			
			attributes.add(val);
		}

		// Add the desired finder to the end
		if (!alreadyAdded) {
			desiredFinders.add(new StringAttributeValue(new JavaSymbolName("ignored"), finderName.getSymbolName()));
		}

		// Now let's add the "finder" attribute
		attributes.add(new ArrayAttributeValue<StringAttributeValue>(new JavaSymbolName("finders"), desiredFinders));
		
		AnnotationMetadata annotation = new DefaultAnnotationMetadata(new JavaType(RooEntity.class.getName()), attributes);
		mutable.removeTypeAnnotation(new JavaType(RooEntity.class.getName()));
		mutable.addTypeAnnotation(annotation);
 	}
	
}
