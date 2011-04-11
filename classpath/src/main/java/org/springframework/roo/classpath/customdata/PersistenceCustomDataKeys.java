package org.springframework.roo.classpath.customdata;

import org.springframework.roo.classpath.customdata.tagkeys.ConstructorMetadataCustomDataKey;
import org.springframework.roo.classpath.customdata.tagkeys.FieldMetadataCustomDataKey;
import org.springframework.roo.classpath.customdata.tagkeys.MemberHoldingTypeDetailsCustomDataKey;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataCustomDataKey;
import org.springframework.roo.model.CustomData;

/**
 * {@link CustomData} tag definitions for persistence-related functionality.
 * 
 * @author Stefan Schmidt
 * @author James Tyrrell
 * @since 1.1.3
 */
public class PersistenceCustomDataKeys {
	
	//TODO: Once CustomDataKey builders have been created they should be used here -JT

	public static final MemberHoldingTypeDetailsCustomDataKey IDENTIFIER_TYPE = new MemberHoldingTypeDetailsCustomDataKey("IDENTIFIER_TYPE");
	public static final MemberHoldingTypeDetailsCustomDataKey PERSISTENT_TYPE = new MemberHoldingTypeDetailsCustomDataKey("PERSISTENT_TYPE");

	public static final ConstructorMetadataCustomDataKey NO_ARG_CONSTRUCTOR = new ConstructorMetadataCustomDataKey("NO_ARG_CONSTRUCTOR");

	public static final FieldMetadataCustomDataKey IDENTIFIER_FIELD = new FieldMetadataCustomDataKey("IDENTIFIER_FIELD");
	public static final FieldMetadataCustomDataKey VERSION_FIELD = new FieldMetadataCustomDataKey("VERSION_FIELD");
	public static final FieldMetadataCustomDataKey TRANSIENT_FIELD = new FieldMetadataCustomDataKey("TRANSIENT_FIELD");
	public static final FieldMetadataCustomDataKey EMBEDDED_FIELD = new FieldMetadataCustomDataKey("EMBEDDED_FIELD");
	public static final FieldMetadataCustomDataKey EMBEDDED_ID_FIELD = new FieldMetadataCustomDataKey("EMBEDDED_ID_FIELD");
	public static final FieldMetadataCustomDataKey ENUMERATED_FIELD = new FieldMetadataCustomDataKey("ENUMERATED_FIELD");
	public static final FieldMetadataCustomDataKey MANY_TO_MANY_FIELD = new FieldMetadataCustomDataKey("MANY_TO_MANY_FIELD");
	public static final FieldMetadataCustomDataKey ONE_TO_MANY_FIELD = new FieldMetadataCustomDataKey("ONE_TO_MANY_FIELD");
	public static final FieldMetadataCustomDataKey MANY_TO_ONE_FIELD = new FieldMetadataCustomDataKey("MANY_TO_ONE_FIELD");
	public static final FieldMetadataCustomDataKey ONE_TO_ONE_FIELD = new FieldMetadataCustomDataKey("ONE_TO_ONE_FIELD");
	public static final FieldMetadataCustomDataKey LOB_FIELD = new FieldMetadataCustomDataKey("LOB_FIELD");
	public static final FieldMetadataCustomDataKey COLUMN_FIELD = new FieldMetadataCustomDataKey("COLUMN_FIELD");


	public static final MethodMetadataCustomDataKey IDENTIFIER_ACCESSOR_METHOD = new MethodMetadataCustomDataKey("IDENTIFIER_ACCESSOR_METHOD");
	public static final MethodMetadataCustomDataKey IDENTIFIER_MUTATOR_METHOD = new MethodMetadataCustomDataKey("IDENTIFIER_MUTATOR_METHOD");
	public static final MethodMetadataCustomDataKey VERSION_ACCESSOR_METHOD = new MethodMetadataCustomDataKey("VERSION_ACCESSOR_METHOD");
	public static final MethodMetadataCustomDataKey VERSION_MUTATOR_METHOD = new MethodMetadataCustomDataKey("VERSION_MUTATOR_METHOD");
	public static final MethodMetadataCustomDataKey PERSIST_METHOD = new MethodMetadataCustomDataKey("PERSIST_METHOD");
	public static final MethodMetadataCustomDataKey MERGE_METHOD = new MethodMetadataCustomDataKey("MERGE_METHOD");
	public static final MethodMetadataCustomDataKey REMOVE_METHOD = new MethodMetadataCustomDataKey("REMOVE_METHOD");
	public static final MethodMetadataCustomDataKey FLUSH_METHOD = new MethodMetadataCustomDataKey("FLUSH_METHOD");
	public static final MethodMetadataCustomDataKey CLEAR_METHOD = new MethodMetadataCustomDataKey("CLEAR_METHOD");
	public static final MethodMetadataCustomDataKey COUNT_ALL_METHOD = new MethodMetadataCustomDataKey("COUNT_ALL_METHOD");
	public static final MethodMetadataCustomDataKey FIND_ALL_METHOD = new MethodMetadataCustomDataKey("FIND_ALL_METHOD");
	public static final MethodMetadataCustomDataKey FIND_METHOD = new MethodMetadataCustomDataKey("FIND_METHOD");
	public static final MethodMetadataCustomDataKey FIND_ENTRIES_METHOD = new MethodMetadataCustomDataKey("FIND_ENTRIES_METHOD");

	//Dynamic finder method names; CustomData value expected to be a java.util.List<String> of finder names
	public static final MethodMetadataCustomDataKey DYNAMIC_FINDER_NAMES = new MethodMetadataCustomDataKey("DYNAMIC_FINDER_NAMES");
}
