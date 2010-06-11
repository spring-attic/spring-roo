package org.springframework.roo.shell.model;

import java.util.List;


/**
 * Serializes and deserializes between JSON and lists of {@link CommandInfo} objects.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public interface ModelSerializer {
	/**
	 * Converts a JSON string into a list of {@link CommandInfo} instances.
	 * Intended to be used with JSON returned by {@link #serializeList(List)}.
	 * 
	 * @param json the input JSON (required, should be in the expected form)
	 * @return the list (never null, but may be empty if the input JSON was an empty list)
	 */
	List<CommandInfo> deserializeList(String json);
	
	/**
	 * Converts a list of {@link CommandInfo} instances into a JSON string. The
	 * returned JSON string will be as a JSON list. The JSON can be converted back
	 * into the original list by invoking {@link #deserializeList(String)}.
	 * 
	 * @param commandInfo the list to convert (may be empty, but never null)
	 * @return a string (never null, will at least contain "[]" if the input was empty)
	 */
	String serializeList(List<CommandInfo> commandInfo);
	
	/**
	 * Converts a JSON string into a single {@link CommandInfo} instance. Intended to 
	 * be used with JSON returned by {@link #serialize(CommandInfo)}.
	 * 
	 * @param json the input JSON (required, should be in the expected form)
	 * @return the object (never null or an empty object)
	 */
	CommandInfo deserialize(String json);
	
	/**
	 * Converts a {@link CommandInfo} instance into a JSON string. The returned
	 * JSON string will be as a JSON object. The JSON can be converted back into
	 * the oriinal object by invoking {@link #deserialize(String)}.
	 * 
	 * @param commandInfo the object to convert (never null)
	 * @return the JSON (never null or empty)
	 */
	String serialize(CommandInfo commandInfo);
}
