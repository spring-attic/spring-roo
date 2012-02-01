package org.springframework.roo.shell.converters;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

/**
 * A simple {@link Converter} for those classes which provide public static
 * fields to represent possible textual values.
 * 
 * @author Stefan Schmidt
 * @author Ben Alex
 * @since 1.0
 */
public class StaticFieldConverterImpl implements StaticFieldConverter {

    private final Map<Class<?>, Map<String, Field>> fields = new HashMap<Class<?>, Map<String, Field>>();

    public void add(final Class<?> clazz) {
        Validate.notNull(clazz,
                "A class to provide conversion services is required");
        Validate.isTrue(fields.get(clazz) == null, "Class '" + clazz
                + "' is already registered for completion services");
        final Map<String, Field> ffields = new HashMap<String, Field>();
        for (final Field field : clazz.getFields()) {
            final int modifier = field.getModifiers();
            if (Modifier.isStatic(modifier) && Modifier.isPublic(modifier)) {
                ffields.put(field.getName(), field);
            }
        }
        Validate.notEmpty(ffields, "Zero public static fields accessible in '"
                + clazz + "'");
        fields.put(clazz, ffields);
    }

    public Object convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        final Map<String, Field> ffields = fields.get(requiredType);
        if (ffields == null) {
            return null;
        }
        Field f = ffields.get(value);
        if (f == null) {
            // Fallback to case insensitive search
            for (final Field candidate : ffields.values()) {
                if (candidate.getName().equalsIgnoreCase(value)) {
                    f = candidate;
                    break;
                }
            }
            if (f == null) {
                // Still not found, despite a case-insensitive search
                return null;
            }
        }
        try {
            return f.get(null);
        }
        catch (final Exception ex) {
            throw new IllegalStateException("Unable to acquire field '" + value
                    + "' from '" + requiredType.getName() + "'", ex);
        }
    }

    public boolean getAllPossibleValues(final List<Completion> completions,
            final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
        final Map<String, Field> ffields = fields.get(requiredType);
        if (ffields == null) {
            return true;
        }
        for (final String field : ffields.keySet()) {
            completions.add(new Completion(field));
        }
        return true;
    }

    public void remove(final Class<?> clazz) {
        Validate.notNull(clazz,
                "A class that was providing conversion services is required");
        fields.remove(clazz);
    }

    public boolean supports(final Class<?> requiredType,
            final String optionContext) {
        return fields.get(requiredType) != null;
    }
}
