package org.springframework.roo.shell.json.model.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.roo.shell.model.CommandInfo;
import org.springframework.roo.shell.model.CommandOption;
import org.springframework.roo.shell.model.ModelSerializer;
import org.springframework.roo.shell.model.CommandInfo.CommandInfoBuilder;
import org.springframework.roo.support.util.Assert;

/**
 * Basic implementation of {@link ModelSerializer} that uses JSON-Simple.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
@Component
@Service
public class ModelSerializerImpl implements ModelSerializer {

	public CommandInfo deserialize(String json) {
		Assert.hasText(json, "Input JSON required");
		return fromJsonToCommandInfo(json);
	}

	public String serialize(CommandInfo commandInfo) {
		Assert.notNull(commandInfo, "A CommandInfo to convert is required");
		return toJSONObect(commandInfo).toJSONString();
	}

	public List<CommandInfo> deserializeList(String json) {
		Assert.hasText(json, "Input JSON required");
		List<CommandInfo> result = new ArrayList<CommandInfo>();
		JSONArray array = (JSONArray) JSONValue.parse(json);
		for (int i = 0; i < array.size(); i ++) {
			String jsonElement = array.get(i).toString();
			result.add(deserialize(jsonElement));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public String serializeList(List<CommandInfo> commandInfo) {
		Assert.notNull(commandInfo, "A list of CommandInfo instances to convert is required");
		JSONArray array = new JSONArray();
		for (CommandInfo info : commandInfo) {
			array.add(toJSONObect(info));
		}
		return array.toJSONString();
	}

	private CommandInfo fromJsonToCommandInfo(String json) {
		JSONObject o = (JSONObject) JSONValue.parse(json);
		JSONArray commandNames = (JSONArray) o.get("commandNames");
		JSONArray commandOptions = (JSONArray) o.get("commandOptions");
		String help = (String) o.get("help");

		CommandInfoBuilder builder = CommandInfo.builder();
		builder.setHelp(help);
			
		for (int i = 0; i < commandNames.size(); i++) {
			String value = (String) commandNames.get(i);
			builder.addCommandName(value);
		}

		for (int i = 0; i < commandOptions.size(); i++) {
			JSONObject value = (JSONObject) commandOptions.get(i);
			builder.addCommandOption(fromJsonToCommandOption(value.toJSONString()));
		}

		return builder.build();
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject toJSONObect(CommandInfo commandInfo) {
		JSONObject o = new JSONObject();
		o.put("commandNames", commandInfo.getCommandNames());
		JSONArray optionsArray = new JSONArray();
		for (CommandOption option : commandInfo.getCommandOptions()) {
			optionsArray.add(toJSONObject(option));
		}
		o.put("commandOptions", optionsArray);
		o.put("help", commandInfo.getHelp());
		return o;
	}

	@SuppressWarnings("unchecked")
	private JSONObject toJSONObject(CommandOption commandOption) {
		JSONObject jCmd = new JSONObject();
		jCmd.put("mandatory", commandOption.isMandatory());
		jCmd.put("help", commandOption.getHelp());
		jCmd.put("specifiedDefaultValue", commandOption.getSpecifiedDefaultValue());
		jCmd.put("unspecifiedDefaultValue", commandOption.getUnspecifiedDefaultValue());
		jCmd.put("optionNames", commandOption.getOptionNames());
		return jCmd;
	}

	private CommandOption fromJsonToCommandOption(String json) {
		JSONObject o = (JSONObject) JSONValue.parse(json);
		JSONArray optionNames = (JSONArray) o.get("optionNames");
		
		List<String> names = new ArrayList<String>();
		for (int i = 0; i < optionNames.size(); i++) {
			String value = (String) optionNames.get(i);
			names.add(value);
		}

		boolean mandatory = (Boolean) o.get("mandatory");
		String help = (String) o.get("help");
		String specifiedDefaultValue = (String) o.get("specifiedDefaultValue");
		String unspecifiedDefaultValue = (String) o.get("unspecifiedDefaultValue");
		
		String[] theNames = names.toArray(new String[] {});
		return new CommandOption(mandatory, specifiedDefaultValue, unspecifiedDefaultValue, help, theNames);
	}

}
