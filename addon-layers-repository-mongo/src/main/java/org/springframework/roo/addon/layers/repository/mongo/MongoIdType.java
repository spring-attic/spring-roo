package org.springframework.roo.addon.layers.repository.mongo;

import org.springframework.roo.model.JavaType;

/**
 * Custom id type to limit options in {@link MongoCommands}
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
public class MongoIdType {
	private JavaType javaType;

	public MongoIdType(String type) {
		this.javaType = new JavaType(type);
	}

	public JavaType getJavaType() {
		return javaType;
	}
}
