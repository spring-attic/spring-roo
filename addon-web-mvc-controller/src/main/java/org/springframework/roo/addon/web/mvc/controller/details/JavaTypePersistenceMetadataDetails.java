package org.springframework.roo.addon.web.mvc.controller.details;

import java.util.List;

import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Aggregates persistence metadata for a given {@link JavaType} which is needed by Web scaffolding add-ons.
 * 
 * @author Stefan Schmidt
 * @since 1.1.2
 */
public class JavaTypePersistenceMetadataDetails {
	
	private FieldMetadata identifierField;
	private MethodMetadata identifierAccessorMethod;
	private MethodMetadata versionAccessorMethod;
	private MethodMetadata persistMethod;
	private MethodMetadata mergeMethod;
	private MethodMetadata removeMethod;
	private MethodMetadata findAllMethod;
	private MethodMetadata findMethod;
	private MethodMetadata countMethod;
	private MethodMetadata findEntriesMethod;
	private List<String> finderNames;
	private boolean isRooIdentifier;
	private List<FieldMetadata> rooIdentifierFields;
	
	/**
	 * Constructor for JavaTypePersistenceMetadataDetails
	 * 
	 * @param identifierField (must not be null)
	 * @param identifierAccessorMethod (must not be null)
	 * @param versionAccessorMethod (may be null if no version accessor is present)
	 * @param persistMethod (may be null if no persist method is present)
	 * @param mergeMethod (may be null if no merge method is present)
	 * @param removeMethod (may be null if no remove method is present)
	 * @param findAllMethod (may be null if no findAll method is present)
	 * @param findMethod (may be null if no find method is present)
	 * @param countMethod (may be null if no count method is present)
	 * @param findEntriesMethod (may be null if no findEntries method is present)
	 * @param finderNames (must not be null, but may be empty of no finders are defined)
	 * @param isRooIdentifier
	 * @param rooIdentifierFields (must not be null, but may be empty of no finders are defined)
	 */
	public JavaTypePersistenceMetadataDetails(FieldMetadata identifierField, MethodMetadata identifierAccessorMethod, MethodMetadata versionAccessorMethod, MethodMetadata persistMethod, MethodMetadata mergeMethod, MethodMetadata removeMethod, MethodMetadata findAllMethod, MethodMetadata findMethod, MethodMetadata countMethod, MethodMetadata findEntriesMethod, List<String> finderNames, boolean isRooIdentifier, List<FieldMetadata> rooIdentifierFields) {
		Assert.notNull(identifierField, "Indentifier field required");
		Assert.notNull(identifierAccessorMethod, "Indentifier accessor method required");
		Assert.notNull(finderNames, "List of finder Names required");
		Assert.notNull(rooIdentifierFields, "List of fields for Roo identifier required (may be empty)");
		
		this.identifierField = identifierField;
		this.identifierAccessorMethod = identifierAccessorMethod;
		this.versionAccessorMethod = versionAccessorMethod;
		this.persistMethod = persistMethod;
		this.mergeMethod = mergeMethod;
		this.removeMethod = removeMethod;
		this.findAllMethod = findAllMethod;
		this.findMethod = findMethod;
		this.finderNames = finderNames;
		this.countMethod = countMethod;
		this.findEntriesMethod = findEntriesMethod;
		this.isRooIdentifier = isRooIdentifier;
		this.rooIdentifierFields = rooIdentifierFields;
	}
	
	/**
	 * Field metadata for identifier
	 * 
	 * @return the {@link FieldMetadata} for the identifier field presented by the persistence MD (never null)
	 */
	public FieldMetadata getIdentifierField() {
		return identifierField;
	}

	/**
	 * Accessor for persistence identifier
	 * 
	 * @return the {@link MethodMetadata} for the identifier accessor method presented by the persistence MD (never null)
	 */
	public MethodMetadata getIdentifierAccessorMethod() {
		return identifierAccessorMethod;
	}

	/**
	 * Accessor for persistence version
	 * 
	 * @return the {@link MethodMetadata} for the version accessor method presented by the persistence MD (null if not defined)
	 */
	public MethodMetadata getVersionAccessorMethod() {
		return versionAccessorMethod;
	}

	/**
	 * Accessor for persistence persist method
	 * 
	 * @return the {@link MethodMetadata} for the persist method presented by the persistence MD (null if not defined)
	 */
	public MethodMetadata getPersistMethod() {
		return persistMethod;
	}

	/**
	 * Accessor for persistence merge method
	 * 
	 * @return the {@link MethodMetadata} for the merge method presented by the persistence MD (null if not defined)
	 */
	public MethodMetadata getMergeMethod() {
		return mergeMethod;
	}

	/**
	 * Accessor for persistence remove method
	 * 
	 * @return the {@link MethodMetadata} for the remove method presented by the persistence MD (null if not defined)
	 */
	public MethodMetadata getRemoveMethod() {
		return removeMethod;
	}

	/**
	 * Accessor for persistence findAll method
	 * 
	 * @return the {@link MethodMetadata} for the findAll method presented by the persistence MD (null if not defined)
	 */
	public MethodMetadata getFindAllMethod() {
		return findAllMethod;
	}

	/**
	 * Accessor for persistence find method
	 * 
	 * @return the {@link MethodMetadata} for the find method presented by the persistence MD (null if not defined)
	 */
	public MethodMetadata getFindMethod() {
		return findMethod;
	}
	
	/**
	 * Accessor for persistence count method
	 * 
	 * @return the {@link MethodMetadata} for the count method presented by the persistence MD (null if not defined)
	 */
	public MethodMetadata getCountMethod() {
		return countMethod;
	}
	
	/**
	 * Accessor for persistence findEntries method
	 * 
	 * @return the {@link MethodMetadata} for the findEntries method presented by the persistence MD (null if not defined)
	 */
	public MethodMetadata getFindEntriesMethod() {
		return findEntriesMethod;
	}
	
	/**
	 * Accessor for finder names
	 * 
	 * @return list of finder names (may be empty)
	 */
	public List<String> getFinderNames() {
		return finderNames;
	}
	
	/**
	 * Indicate if this type is annotated with @RooIdentifier
	 * 
	 * @return true if annotation is present
	 */
	public boolean isRooIdentifier() {
		return isRooIdentifier;
	}
	
	/**
	 * Accessor for identifier field collection in cases where the identifier type is annotated with @RooIdentifier.
	 * 
	 * @return list of Field metadata for fields defined in the type annotated with @RooIdentifier
	 */
	public List<FieldMetadata> getRooIdentifierFields() {
		return rooIdentifierFields;
	}
}
