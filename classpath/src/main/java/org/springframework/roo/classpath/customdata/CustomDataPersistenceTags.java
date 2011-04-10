package org.springframework.roo.classpath.customdata;

import org.springframework.roo.classpath.customdata.tagkeys.ConstructorMetadataTagKey;
import org.springframework.roo.classpath.customdata.tagkeys.FieldMetadataTagKey;
import org.springframework.roo.classpath.customdata.tagkeys.MemberHoldingTypeDetailsTagKey;
import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataTagKey;
import org.springframework.roo.model.CustomData;

/**
 * {@link CustomData} tag definitions for persistence-related functionality.
 * 
 * @author Stefan Schmidt
 * @author James Tyrrell
 * @since 1.1.3
 */
public class CustomDataPersistenceTags {
	
	//TODO: Once TagKey builders have been created they should be used here -JT

	public static final MemberHoldingTypeDetailsTagKey IDENTIFIER_TYPE = new MemberHoldingTypeDetailsTagKey("IDENTIFIER_TYPE");
	public static final MemberHoldingTypeDetailsTagKey PERSISTENT_TYPE = new MemberHoldingTypeDetailsTagKey("PERSISTENT_TYPE");

	public static final ConstructorMetadataTagKey NO_ARG_CONSTRUCTOR = new ConstructorMetadataTagKey("NO_ARG_CONSTRUCTOR");

	public static final FieldMetadataTagKey IDENTIFIER_FIELD = new FieldMetadataTagKey("IDENTIFIER_FIELD");
	public static final FieldMetadataTagKey VERSION_FIELD = new FieldMetadataTagKey("VERSION_FIELD");
	public static final FieldMetadataTagKey TRANSIENT_FIELD = new FieldMetadataTagKey("TRANSIENT_FIELD");
	public static final FieldMetadataTagKey EMBEDDED_FIELD = new FieldMetadataTagKey("EMBEDDED_FIELD");
	public static final FieldMetadataTagKey EMBEDDED_ID_FIELD = new FieldMetadataTagKey("EMBEDDED_ID_FIELD");
	public static final FieldMetadataTagKey ENUMERATED_FIELD = new FieldMetadataTagKey("ENUMERATED_FIELD");
	public static final FieldMetadataTagKey MANY_TO_MANY_FIELD = new FieldMetadataTagKey("MANY_TO_MANY_FIELD");
	public static final FieldMetadataTagKey ONE_TO_MANY_FIELD = new FieldMetadataTagKey("ONE_TO_MANY_FIELD");
	public static final FieldMetadataTagKey MANY_TO_ONE_FIELD = new FieldMetadataTagKey("MANY_TO_ONE_FIELD");
	public static final FieldMetadataTagKey ONE_TO_ONE_FIELD = new FieldMetadataTagKey("ONE_TO_ONE_FIELD");
	public static final FieldMetadataTagKey LOB_FIELD = new FieldMetadataTagKey("LOB_FIELD");
	public static final FieldMetadataTagKey COLUMN_FIELD = new FieldMetadataTagKey("COLUMN_FIELD");


	public static final MethodMetadataTagKey IDENTIFIER_ACCESSOR_METHOD = new MethodMetadataTagKey("IDENTIFIER_ACCESSOR_METHOD");
	public static final MethodMetadataTagKey IDENTIFIER_MUTATOR_METHOD = new MethodMetadataTagKey("IDENTIFIER_MUTATOR_METHOD");
	public static final MethodMetadataTagKey VERSION_ACCESSOR_METHOD = new MethodMetadataTagKey("VERSION_ACCESSOR_METHOD");
	public static final MethodMetadataTagKey VERSION_MUTATOR_METHOD = new MethodMetadataTagKey("VERSION_MUTATOR_METHOD");
	public static final MethodMetadataTagKey PERSIST_METHOD = new MethodMetadataTagKey("PERSIST_METHOD");
	public static final MethodMetadataTagKey MERGE_METHOD = new MethodMetadataTagKey("MERGE_METHOD");
	public static final MethodMetadataTagKey REMOVE_METHOD = new MethodMetadataTagKey("REMOVE_METHOD");
	public static final MethodMetadataTagKey FLUSH_METHOD = new MethodMetadataTagKey("FLUSH_METHOD");
	public static final MethodMetadataTagKey CLEAR_METHOD = new MethodMetadataTagKey("CLEAR_METHOD");
	public static final MethodMetadataTagKey COUNT_ALL_METHOD = new MethodMetadataTagKey("COUNT_ALL_METHOD");
	public static final MethodMetadataTagKey FIND_ALL_METHOD = new MethodMetadataTagKey("FIND_ALL_METHOD");
	public static final MethodMetadataTagKey FIND_METHOD = new MethodMetadataTagKey("FIND_METHOD");
	public static final MethodMetadataTagKey FIND_ENTRIES_METHOD = new MethodMetadataTagKey("FIND_ENTRIES_METHOD");

	//Dynamic finder method names; CustomData value expected to be a java.util.List<String> of finder names
	public static final MethodMetadataTagKey DYNAMIC_FINDER_NAMES = new MethodMetadataTagKey("DYNAMIC_FINDER_NAMES");
}
