package org.springframework.roo.addon.layers.repository.mongo;

import java.math.BigInteger;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * Custom id type converter for {@link MongoIdType} to limit options in {@link MongoCommands}
 * 
 * @author Stefan Schmidt
 * @since 1.2.0
 */
@Component
@Service
public class MongoIdTypeConverter implements Converter<MongoIdType> {

	public boolean supports(Class<?> type, String optionContext) {
		return MongoIdType.class.isAssignableFrom(type);
	}

	public MongoIdType convertFromText(String value, Class<?> targetType, String optionContext) {
		if (value == null || "".equals(value)) {
			return null;
		}
		return new MongoIdType(value);
	}

	public boolean getAllPossibleValues(List<String> completions, Class<?> targetType, String existingData, String optionContext, MethodTarget target) {
		SortedSet<String> types = new TreeSet<String>();
		types.add(BigInteger.class.getName());
		types.add("org.bson.types.ObjectId");
		
		for (String type : types) {
			if (type.startsWith(existingData) || existingData.startsWith(type)) {
				completions.add(type);
			}
		}
		return false;
	}
}
