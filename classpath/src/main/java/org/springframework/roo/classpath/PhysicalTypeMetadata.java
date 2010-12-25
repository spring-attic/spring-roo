package org.springframework.roo.classpath;

import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.itd.ItdMetadataProvider;
import org.springframework.roo.classpath.itd.MemberHoldingTypeDetailsMetadataItem;
import org.springframework.roo.classpath.scanner.MemberDetailsScanner;
import org.springframework.roo.model.JavaType;

/**
 * Represents metadata for a particular {@link PhysicalTypeIdentifierNamingUtils}, which is usually a class or
 * interface but may potentially represent an annotation or enum type.
 * 
 * <p>
 * Note that subclasses must support class or interface declarations (ie {@link ClassOrInterfaceTypeDetails}),
 * but they may support (at their option) annotation, enumeration and empty types.
 * 
 * <p>
 * {@link PhysicalTypeMetadata} may be parsed from source code, parsed from a .class bytecode,
 * or parsed from a .class bytecode from a JAR.
 * 
 * <p>
 * It is important to note a {@link PhysicalTypeMetadata} will only include those members explicitly
 * declared in the relevant source or bytecode. This explicitly excludes members introduced
 * via an inter-type declaration (ITD) or other special bytecode modification techniques.
 *  
 * @author Ben Alex
 * @since 1.0
 * @see ItdMetadataProvider
 * @see MemberDetailsScanner
 */
public interface PhysicalTypeMetadata extends MemberHoldingTypeDetailsMetadataItem<MemberHoldingTypeDetails> {

	/**
	 * @return the location of the disk file containing this resource, in canonical name format (never null) 
	 */
	String getPhysicalLocationCanonicalPath();
	
	/**
	 * Obtains the canonical file path to where an ITD can be emitted for this physical Java type.
	 * 
	 * @param metadataProvider so the {@link ItdMetadataProvider#getItdUniquenessFilenameSuffix()} can be queried (never null)
	 * @return a full file path that can be used to produce an ITD (never null)
	 */
	String getItdCanoncialPath(ItdMetadataProvider metadataProvider);
	
	/**
	 * Obtains the {@link JavaType} which represents an ITD for this physical Java type.
	 * 
	 * @param metadataProvider so the {@link ItdMetadataProvider#getItdUniquenessFilenameSuffix()} can be queried (never null)
	 * @return the {@link JavaType} applicable for this ITD (never null)
	 */
	JavaType getItdJavaType(ItdMetadataProvider metadataProvider);
}
