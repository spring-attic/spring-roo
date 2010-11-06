package org.springframework.roo.addon.dbre.model;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;

import org.springframework.roo.model.DataType;
import org.springframework.roo.model.JavaType;

/**
 * Maps column types to Java types. 
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public enum ColumnType {
	BOOLEAN(Types.BOOLEAN, JavaType.BOOLEAN_OBJECT),
	BIT(Types.BIT, JavaType.BOOLEAN_PRIMITIVE), 
	TINYINT(Types.TINYINT, JavaType.SHORT_OBJECT), 
	SMALLINT(Types.SMALLINT, JavaType.SHORT_OBJECT), 
	INTEGER(Types.INTEGER, JavaType.INT_OBJECT), 
	BIGINT(Types.BIGINT, JavaType.LONG_OBJECT), 
	FLOAT(Types.FLOAT, JavaType.DOUBLE_OBJECT), 
	REAL(Types.REAL, JavaType.FLOAT_OBJECT), 
	NUMERIC(Types.NUMERIC, new JavaType(BigDecimal.class.getName())), 
	DECIMAL(Types.DECIMAL, new JavaType(BigDecimal.class.getName())), 
	CHAR(Types.CHAR, JavaType.CHAR_OBJECT), 
	VARCHAR(Types.VARCHAR, JavaType.STRING_OBJECT), 
	LONGVARCHAR(Types.LONGVARCHAR, JavaType.STRING_OBJECT), 
	DATE(Types.DATE, new JavaType(Date.class.getName())), 
	TIME(Types.TIME, new JavaType(Date.class.getName())), 
	TIMESTAMP(Types.TIMESTAMP, new JavaType(Date.class.getName())), 
	BINARY(Types.BINARY, new JavaType("java.lang.Byte", 1, DataType.PRIMITIVE, null, null)), 
	VARBINARY(Types.VARBINARY, new JavaType("java.lang.Byte", 1, DataType.PRIMITIVE, null, null)), 
	LONGVARBINARY(Types.LONGVARBINARY, new JavaType("java.lang.Byte", 1, DataType.PRIMITIVE, null, null)), 
	NULL(Types.NULL, null), 
	OTHER(Types.OTHER, JavaType.STRING_OBJECT), 
	JAVA_OBJECT(Types.JAVA_OBJECT, new JavaType("java.lang.Object")), 
	DISTINCT(Types.DISTINCT, new JavaType(HashMap.class.getName())), 
	STRUCT(Types.STRUCT, new JavaType(HashMap.class.getName())), 
	ARRAY(Types.ARRAY, new JavaType("java.sql.Array")), 
	BLOB(Types.BLOB, new JavaType("java.lang.Byte", 1, DataType.PRIMITIVE, null, null)), 
	CLOB(Types.CLOB, JavaType.STRING_OBJECT), 
	REF(Types.REF, new JavaType("java.sql.Ref")), 
	DOUBLE(Types.DOUBLE, JavaType.DOUBLE_OBJECT);

	private int typeCode;
	private JavaType javaType;

	private ColumnType(int typeCode, JavaType javaType) {
		this.typeCode = typeCode;
		this.javaType = javaType;
	}

	public int getTypeCode() {
		return typeCode;
	}

	public JavaType getJavaType() {
		return javaType;
	}

	public static ColumnType getColumnType(int typeCode) {
		for (ColumnType columnType : ColumnType.values()) {
			if (columnType.getTypeCode() == typeCode) {
				return columnType;
			}
		}
		return VARCHAR;
	}
}
