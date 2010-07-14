package org.springframework.roo.felix.pgp;

import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * {@link Converter} for {@link PgpKeyId}.
 *
 * @author Ben Alex
 * @since 1.1
 *
 */
@Component
@Service
public class PgpKeyIdConverter implements Converter {

	@Reference private PgpService pgpService;
	
	public Object convertFromText(String value, Class<?> requiredType, String optionContext) {
		return new PgpKeyId(value.trim());
	}
	
	public boolean getAllPossibleValues(List<String> completions, Class<?> requiredType, String originalUserInput, String optionContext, MethodTarget target) {
		for (PgpKeyId candidate : pgpService.getDiscoveredKeyIds()) {
			String id = candidate.getId();
			if (id.toUpperCase().startsWith(originalUserInput.toUpperCase())) {
				completions.add(id);
			}
		}

		return false;
	}

	public boolean supports(Class<?> requiredType, String optionContext) {
		return PgpKeyId.class.isAssignableFrom(requiredType);
	}

}