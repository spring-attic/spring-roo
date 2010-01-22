package org.springframework.roo.model;

import java.util.Collections;
import java.util.Set;

import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Provides all reserved words.
 * 
 * @author Ben Alex
 *
 */
public abstract class ReservedWords {

	/**
	 * Represents an unmodifiable set of lowercase reserved words in Java.
	 */
	public static final Set<String> RESERVED_JAVA_KEYWORDS = Collections.unmodifiableSet(StringUtils.commaDelimitedListToSet("abstract,assert,boolean,break,byte,case,catch,char,class,const,continue,default,do,double,else,enum,extends,false,final,finally,float,for,goto,if,implements,import,instanceof,int,interface,long,native,new,null,package,private,protected,public,return,short,static,strictfp,super,switch,synchronized,this,throw,throws,transient,true,try,void,volatile,while"));

	/**
	 * Represents an unmodifiable set of lowercase reserved words.
	 */
	public static final Set<String> RESERVED_SQL_KEYWORDS = Collections.unmodifiableSet(StringUtils.commaDelimitedListToSet("ABSOLUTE,ACTION,ADD,AFTER,ALL,ALLOCATE,ALTER,AND,ANY,ARE,ARRAY,AS,ASC,ASENSITIVE,ASSERTION,ASYMMETRIC,AT,ATOMIC,AUTHORIZATION,AVG,BEFORE,BEGIN,BETWEEN,BIGINT,BINARY,BIT,BIT_LENGTH,BLOB,BOOLEAN,BOTH,BREADTH,BY,CALL,CALLED,CASCADE,CASCADED,CASE,CAST,CATALOG,CHAR,CHARACTER,CHARACTER_LENGTH,CHAR_LENGTH,CHECK,CLOB,CLOSE,COALESCE,COLLATE,COLLATION,COLUMN,COMMIT,CONDITION,CONNECT,CONNECTION,CONSTRAINT,CONSTRAINTS,CONSTRUCTOR,CONTAINS,CONTINUE,CONVERT,CORRESPONDING,COUNT,CREATE,CROSS,CUBE,CURRENT,CURRENT_DATE,CURRENT_DEFAULT_TRANSFORM_GROUP,CURRENT_PATH,CURRENT_ROLE,CURRENT_TIME,CURRENT_TIMESTAMP,CURRENT_TRANSFORM_GROUP_FOR_TYPE,CURRENT_USER,CURSOR,CYCLE,DATA,DATE,DAY,DEALLOCATE,DEC,DECIMAL,DECLARE,DEFAULT,DEFERRABLE,DEFERRED,DELETE,DEPTH,DEREF,DESC,DESCRIBE,DESCRIPTOR,DETERMINISTIC,DIAGNOSTICS,DISCONNECT,DISTINCT,DO,DOMAIN,DOUBLE,DROP,DYNAMIC,EACH,ELEMENT,ELSE,ELSEIF,END,EQUALS,ESCAPE,EXCEPT,EXCEPTION,EXEC,EXECUTE,EXISTS,EXIT,EXTERNAL,EXTRACT,FALSE,FETCH,FILTER,FIRST,FLOAT,FOR,FOREIGN,FOUND,FREE,FROM,FULL,FUNCTION,GENERAL,GET,GLOBAL,GO,GOTO,GRANT,GROUP,GROUPING,HANDLER,HAVING,HOLD,HOUR,IDENTITY,IF,IMMEDIATE,IN,INDEX,INDICATOR,INITIALLY,INNER,INOUT,INPUT,INSENSITIVE,INSERT,INT,INTEGER,INTERSECT,INTERVAL,INTO,IS,ISOLATION,ITERATE,JOIN,KEY,LANGUAGE,LARGE,LAST,LATERAL,LEADING,LEAVE,LEFT,LEVEL,LIKE,LOCAL,LOCALTIME,LOCALTIMESTAMP,LOCATOR,LOOP,LOWER,MAP,MATCH,MAX,MEMBER,MERGE,METHOD,MIN,MINUTE,MODIFIES,MODULE,MONTH,MULTISET,NAMES,NATIONAL,NATURAL,NCHAR,NCLOB,NEW,NEXT,NO,NONE,NOT,NULL,NULLIF,NUMBER,NUMERIC,OBJECT,OCTET_LENGTH,OF,OLD,ON,ONLY,OPEN,OPTION,OR,ORDER,ORDINALITY,OUT,OUTER,OUTPUT,OVER,OVERLAPS,PAD,PARAMETER,PARTIAL,PARTITION,PATH,POSITION,PRECISION,PREPARE,PRESERVE,PRIMARY,PRIOR,PRIVILEGES,PROCEDURE,PUBLIC,RANGE,READ,READS,REAL,RECURSIVE,REF,REFERENCES,REFERENCING,RELATIVE,RELEASE,REPEAT,RESIGNAL,RESTRICT,RESULT,RETURN,RETURNS,REVOKE,RIGHT,ROLE,ROLLBACK,ROLLUP,ROUTINE,ROW,ROWS,SAVEPOINT,SCHEMA,SCOPE,SCROLL,SEARCH,SECOND,SECTION,SELECT,SENSITIVE,SESSION,SESSION_USER,SET,SETS,SIGNAL,SIMILAR,SIZE,SMALLINT,SOME,SPACE,SPECIFIC,SPECIFICTYPE,SQL,SQLCODE,SQLERROR,SQLEXCEPTION,SQLSTATE,SQLWARNING,START,STATE,STATIC,SUBMULTISET,SUBSTRING,SUM,SYMMETRIC,SYSTEM,SYSTEM_USER,TABLE,TABLESAMPLE,TEMPORARY,THEN,TIME,TIMESTAMP,TIMEZONE_HOUR,TIMEZONE_MINUTE,TO,TRAILING,TRANSACTION,TRANSLATE,TRANSLATION,TREAT,TRIGGER,TRIM,TRUE,UNDER,UNDO,UNION,UNIQUE,UNKNOWN,UNNEST,UNTIL,UPDATE,UPPER,USAGE,USER,USING,VALUE,VALUES,VARCHAR,VARYING,VIEW,WHEN,WHENEVER,WHERE,WHILE,WINDOW,WITH,WITHIN,WITHOUT,WORK,WRITE,YEAR,ZONE".toLowerCase()));
	
	public static void verifyReservedWordsNotPresent(JavaType javaType) {
		verifyReservedJavaKeywordsNotPresent(javaType);
		verifyReservedSqlKeywordsNotPresent(javaType);
	}

	public static void verifyReservedWordsNotPresent(JavaSymbolName javaSymbolName) {
		verifyReservedJavaKeywordsNotPresent(javaSymbolName);
		verifyReservedSqlKeywordsNotPresent(javaSymbolName);
	}
	
	public static void verifyReservedWordsNotPresent(String string) {
		verifyReservedJavaKeywordsNotPresent(string);
		verifyReservedSqlKeywordsNotPresent(string);
	}

	public static void verifyReservedJavaKeywordsNotPresent(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		for (String s : javaType.getFullyQualifiedTypeName().split("\\.")) {
			if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(s)) {
				throw new IllegalStateException("Reserved Java keyword '" + s + "' is not permitted within fully qualified type name");
			}
		}	
	}
	
	public static void verifyReservedSqlKeywordsNotPresent(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");
		if (ReservedWords.RESERVED_SQL_KEYWORDS.contains(javaType.getSimpleTypeName().toLowerCase())) {
			throw new IllegalStateException("Reserved SQL keyword '" + javaType.getSimpleTypeName() + "' is not permitted as simple type name");
		}
	}

	public static void verifyReservedJavaKeywordsNotPresent(JavaSymbolName javaSymbolName) {
		Assert.notNull(javaSymbolName, "Java symbol required");
		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(javaSymbolName.getSymbolName())) {
			throw new IllegalStateException("Reserved Java keyword '" + javaSymbolName.getSymbolName() + "' is not permitted as symbol name");
		}
	}
	
	public static void verifyReservedSqlKeywordsNotPresent(JavaSymbolName javaSymbolName) {
		Assert.notNull(javaSymbolName, "Java symbol required");
		if (ReservedWords.RESERVED_SQL_KEYWORDS.contains(javaSymbolName.getSymbolName().toLowerCase())) {
			throw new IllegalStateException("Reserved SQL keyword '" + javaSymbolName.getSymbolName() + "' is not permitted as symbol name");
		}
	}

	public static void verifyReservedJavaKeywordsNotPresent(String string) {
		Assert.notNull(string, "String required");
		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(string.toLowerCase())) {
			throw new IllegalStateException("Reserved Java keyword '" + string.toLowerCase() + "' is not permitted");
		}
	}

	public static void verifyReservedSqlKeywordsNotPresent(String string) {
		Assert.notNull(string, "String required");
		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(string.toLowerCase())) {
			throw new IllegalStateException("Reserved SQL keyword '" + string.toLowerCase() + "' is not permitted");
		}
	}
}
