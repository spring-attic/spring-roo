package org.springframework.roo.classpath.scanner;

import java.util.List;
import java.util.Set;

import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.details.ConstructorMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.IdentifiableJavaStructure;
import org.springframework.roo.classpath.details.ItdTypeDetails;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.persistence.PersistenceMemberLocator;
import org.springframework.roo.metadata.MetadataItem;
import org.springframework.roo.metadata.MetadataProvider;
import org.springframework.roo.model.CustomData;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Immutable representation of member details scanned at a particular point in
 * time.
 * <p>
 * Please note that {@link MemberDetails} as well as all types it eventually
 * refers to are all immutable.
 * <p>
 * {@link MemberDetails} represents a convenient, customizable aggregation of
 * member information at a particular point in time and for a particular
 * {@link MetadataProvider}. It is distinct from other key types as follows:
 * <ul>
 * <li>{@link MetadataItem}s are specific to a given add-on. They also
 * frequently reflect a single AspectJ ITD.</li>
 * <li>{@link PhysicalTypeDetails} instances only contain details of a parsed
 * .java source file.</li>
 * <li>{@link ItdTypeDetails} instances only represent details of a created .aj
 * ITD source file.</li>
 * <li>{@link MemberHoldingTypeDetails}s represent a single compilation unit.</li>
 * <li>{@link IdentifiableJavaStructure}s represent a single member within a
 * {@link MemberHoldingTypeDetails} instance.</li>
 * <li>{@link MemberDetails}represent <em>all</em> available
 * {@link MemberHoldingTypeDetails} for a given type at this time and for this
 * metadata provider.</li>
 * </ul>
 * 
 * @author Ben Alex
 * @since 1.1
 */
public interface MemberDetails {

    /**
     * Locates the specified type-level annotation on any of the
     * {@link MemberHoldingTypeDetails} in this {@link MemberDetails}.
     * 
     * @param type the type of annotation to locate (required)
     * @return the annotation, or <code>null</code> if not found
     * @since 1.2.0
     */
    AnnotationMetadata getAnnotation(JavaType type);

    /**
     * Searches all {@link MemberHoldingTypeDetails} and returns all
     * constructors.
     * 
     * @return zero or more constructors (never null)
     * @since 1.2.0
     */
    List<ConstructorMetadata> getConstructors();

    /**
     * Returns an immutable representation of the member holders.
     * 
     * @return a List of immutable member holders (never null or empty)
     */
    List<MemberHoldingTypeDetails> getDetails();

    /**
     * Returns the names of this type's dynamic finders
     * 
     * @return a non-<code>null</code> list
     * @since 1.2.0
     */
    List<String> getDynamicFinderNames();

    /**
     * Searches all {@link MemberHoldingTypeDetails} and returns all fields.
     * 
     * @return zero or more fields (never null)
     */
    List<FieldMetadata> getFields();

    /**
     * Locates a method with the name presented. Searches all
     * {@link MemberDetails} until the first such method is located or none can
     * be found.
     * 
     * @param methodName the method name to locate (can be <code>null</code>)
     * @return the first located method, or <code>null</code> if the method name
     *         is <code>null</code> or such a method cannot be found
     * @since 1.2.0
     */
    MethodMetadata getMethod(JavaSymbolName methodName);

    /**
     * Locates a method with the name and parameter signature presented.
     * Searches all {@link MemberDetails} until the first such method is located
     * or none can be found.
     * 
     * @param methodName the method name to locate (can be <code>null</code>)
     * @param parameters the method parameter signature to locate (can be null
     *            if no parameters are required)
     * @return the first located method, or <code>null</code> if the method name
     *         is <code>null</code> or such a method cannot be found
     * @since 1.2.0
     */
    MethodMetadata getMethod(JavaSymbolName methodName,
            List<JavaType> parameters);

    /**
     * Locates a method with the name and parameter signature presented that is
     * not declared by the presented MID.
     * 
     * @param methodName the method name to locate (can be <code>null</code>)
     * @param parameters the method parameter signature to locate (can be null
     *            if no parameters are required)
     * @param excludingMid the MID that a found method cannot be declared by
     * @return the first located method, or <code>null</code> if the method name
     *         is <code>null</code> or such a method cannot be found
     * @since 1.2.0
     */
    MethodMetadata getMethod(JavaSymbolName methodName,
            List<JavaType> parameters, String excludingMid);

    /**
     * Searches all {@link MemberHoldingTypeDetails} and returns all methods.
     * 
     * @return zero or more methods (never null)
     * @since 1.2.0
     */
    List<MethodMetadata> getMethods();

    /**
     * Searches all {@link MemberDetails} and returns all methods which contain
     * a given {@link CustomData} tag.
     * 
     * @param memberDetails the {@link MemberDetails} to search (required)
     * @param tagKey the {@link CustomData} key to search for
     * @return zero or more methods (never null)
     * @since 1.2.0
     */
    List<MethodMetadata> getMethodsWithTag(Object tagKey);

    /**
     * Determines the most concrete {@link MemberHoldingTypeDetails} in cases
     * where multiple matches are found for a given tag.
     * 
     * @param tagKey the {@link CustomData} key to search for (required)
     * @return the most concrete tagged method or <code>null</code> if not found
     * @since 1.2.0
     */
    MethodMetadata getMostConcreteMethodWithTag(Object tagKey);

    /**
     * Returns the type of this class' persistent fields, including those in
     * collections, but excluding:
     * <ul>
     * <li>the ID field</li>
     * <li>the version field</li>
     * <li>JPA-transient fields</li>
     * <li>immutable fields (i.e. that don't have both a getter and a setter)</li>
     * <li>embedded ID fields</li>
     * <li>the collection types themselves</li>
     * </ul>
     * 
     * @param thisType the owning Java type (required)
     * @param persistenceMemberLocator for finding the ID and version fields
     *            (required)
     * @return a non-<code>null</code> set with stable iteration order
     * @since 1.2.0
     */
    Set<JavaType> getPersistentFieldTypes(JavaType thisType,
            PersistenceMemberLocator persistenceMemberLocator);

    /**
     * Indicates whether a method specified by the method attributes is present
     * and isn't declared by the passed in MID.
     * 
     * @param methodName the name of the method being searched for
     * @param parameterTypes the parameters of the method being searched for
     * @param declaredByMetadataId the MID to be used to see if a found method
     *            is declared by the MID
     * @return see above
     * @since 1.2.0
     */
    boolean isMethodDeclaredByAnother(JavaSymbolName methodName,
            List<JavaType> parameterTypes, String declaredByMetadataId);

    /**
     * Indicates whether the requesting MID is annotated with the specified
     * annotation.
     * 
     * @param annotationMetadata the annotation to look for
     * @param requestingMid the MID interested in
     * @return see above
     * @since 1.2.0
     */
    boolean isRequestingAnnotatedWith(AnnotationMetadata annotationMetadata,
            String requestingMid);
}