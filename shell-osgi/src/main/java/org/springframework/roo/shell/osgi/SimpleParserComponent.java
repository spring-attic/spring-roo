package org.springframework.roo.shell.osgi;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.SimpleParser;

/**
 * OSGi component launcher for {@link SimpleParser}.
 *
 * @author Ben Alex
 * @since 1.1
 */
@Component
@Service
@References(value={
		@Reference(name="converter", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=Converter.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE),
		@Reference(name="command", strategy=ReferenceStrategy.EVENT, policy=ReferencePolicy.DYNAMIC, referenceInterface=CommandMarker.class, cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
		})
public class SimpleParserComponent extends SimpleParser {

	protected void bindConverter(Converter c) {
		add(c);
	}
	
	protected void unbindConverter(Converter c) {
		remove(c);
	}
	
	protected void bindCommand(CommandMarker c) {
		add(c);
	}
	
	protected void unbindCommand(CommandMarker c) {
		remove(c);
	}
	
}
