package org.springframework.roo.shell.json.model.internal;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.roo.shell.model.CommandInfo;
import org.springframework.roo.shell.model.CommandOption;

/**
 * Tests {@link ModelSerializerImpl}.
 * 
 * @author Ben Alex
 * @since 1.1
 *
 */
public class ModelSerializerImplTests {
	ModelSerializerImpl serializer = new ModelSerializerImpl();

	@Test
	public void testSingle() throws Exception {
		CommandInfo info = getData("sample1");
		String serializedSingle = serializer.serialize(info);
		CommandInfo deserialized = serializer.deserialize(serializedSingle);
		Assert.assertEquals(info, deserialized);
	}
	
	@Test
	public void testCollection() throws Exception {
		List<CommandInfo> commandInfo = new ArrayList<CommandInfo>();
		commandInfo.add(getData("sample1"));
		commandInfo.add(getData("sample2"));
		String serializedList = serializer.serializeList(commandInfo);
		List<CommandInfo> deserialized = serializer.deserializeList(serializedList);
		Assert.assertEquals(commandInfo, deserialized);
	}

	private CommandInfo getData(String commandName) {
		CommandOption option1 = new CommandOption(true, "true", "false", "the confirmation", "doIt", "iAmSure");
		CommandOption option2 = new CommandOption(false, "C:\\", "/home", "where to format", "drive", "mountPoint");
		return CommandInfo.builder().addCommandName(commandName).addCommandName("format").addCommandName("repartition").addCommandOption(option1).addCommandOption(option2).setHelp("Some help").build();
	}
	
}