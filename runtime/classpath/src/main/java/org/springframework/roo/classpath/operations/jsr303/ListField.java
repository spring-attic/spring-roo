package org.springframework.roo.classpath.operations.jsr303;

import static org.springframework.roo.model.JdkJavaType.ARRAY_LIST;

import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.jsr303.SetField;
import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

public class ListField extends SetField {

    public ListField(final String physicalTypeIdentifier,
            final JavaType fieldType, final JavaSymbolName fieldName,
            final JavaType genericParameterTypeName,
            final Cardinality cardinality) {
        super(physicalTypeIdentifier, fieldType, fieldName,
                genericParameterTypeName, cardinality);
    }

    @Override
    public JavaType getInitializer() {
        final List<JavaType> params = new ArrayList<JavaType>();
        params.add(getGenericParameterTypeName());
        return new JavaType(ARRAY_LIST.getFullyQualifiedTypeName(), 0,
                DataType.TYPE, null, params);
    }
}
