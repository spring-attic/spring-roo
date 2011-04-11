package org.springframework.roo.addon.json;

import org.springframework.roo.classpath.customdata.tagkeys.MethodMetadataTagKey;
import org.springframework.roo.model.CustomData;

/**
 * {@link CustomData} tag definitions for json-related functionality.
 * 
 * @author Stefan Schmidt
 * @since 1.1.3
 */
public class CustomDataJsonTags {
	
	public static MethodMetadataTagKey TO_JSON_METHOD = new MethodMetadataTagKey("TO_JSON_METHOD");
	public static MethodMetadataTagKey FROM_JSON_METHOD = new MethodMetadataTagKey("FROM_JSON_METHOD");
	public static MethodMetadataTagKey TO_JSON_ARRAY_METHOD = new MethodMetadataTagKey("TO_JSON_ARRAY_METHOD");
	public static MethodMetadataTagKey FROM_JSON_ARRAY_METHOD = new MethodMetadataTagKey("FROM_JSON_ARRAY_METHOD");

}
