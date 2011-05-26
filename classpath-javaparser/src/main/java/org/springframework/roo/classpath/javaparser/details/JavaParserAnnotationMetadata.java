package org.springframework.roo.classpath.javaparser.details;

import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.ArrayInitializerExpr;
import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.CharLiteralExpr;
import japa.parser.ast.expr.ClassExpr;
import japa.parser.ast.expr.DoubleLiteralExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.LongLiteralExpr;
import japa.parser.ast.expr.MarkerAnnotationExpr;
import japa.parser.ast.expr.MemberValuePair;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NormalAnnotationExpr;
import japa.parser.ast.expr.SingleMemberAnnotationExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.type.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.ArrayAttributeValue;
import org.springframework.roo.classpath.details.annotations.BooleanAttributeValue;
import org.springframework.roo.classpath.details.annotations.CharAttributeValue;
import org.springframework.roo.classpath.details.annotations.ClassAttributeValue;
import org.springframework.roo.classpath.details.annotations.DoubleAttributeValue;
import org.springframework.roo.classpath.details.annotations.EnumAttributeValue;
import org.springframework.roo.classpath.details.annotations.IntegerAttributeValue;
import org.springframework.roo.classpath.details.annotations.LongAttributeValue;
import org.springframework.roo.classpath.details.annotations.NestedAnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.classpath.javaparser.CompilationUnitServices;
import org.springframework.roo.classpath.javaparser.JavaParserUtils;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;

/**
 * Java Parser implementation of {@link AnnotationMetadata}.
 * 
 * @author Ben Alex
 * @since 1.0
 */
public final class JavaParserAnnotationMetadata implements AnnotationMetadata {
	// Passed in
	private AnnotationExpr annotationExpr;
	private CompilationUnitServices compilationUnitServices;

	// Computed
	private JavaType annotationType;
	private List<AnnotationAttributeValue<?>> attributes = new ArrayList<AnnotationAttributeValue<?>>();
	private Map<JavaSymbolName, AnnotationAttributeValue<?>> attributeMap = new HashMap<JavaSymbolName, AnnotationAttributeValue<?>>();

	public JavaParserAnnotationMetadata(AnnotationExpr annotationExpr, CompilationUnitServices compilationUnitServices) {
		Assert.notNull(annotationExpr, "Annotation expression required");
		Assert.notNull(compilationUnitServices, "Compilation unit services required");

		// Store required source information for subsequent mutability support
		this.annotationExpr = annotationExpr;
		this.compilationUnitServices = compilationUnitServices;

		// Obtain the annotation type name from the assorted types of annotations we might have received (ie marker annotations, single member annotations, normal annotations etc)
		NameExpr nameToFind = JavaParserUtils.getNameExpr(annotationExpr);

		// Compute the actual annotation type, having regard to the compilation unit package and imports
		annotationType = JavaParserUtils.getJavaType(compilationUnitServices, nameToFind, null);

		// Generate some member-value pairs for subsequent parsing
		List<MemberValuePair> annotationPairs = new ArrayList<MemberValuePair>();
		if (annotationExpr instanceof MarkerAnnotationExpr) {
			// A marker annotation has no values, so we can have no pairs to add
		} else if (annotationExpr instanceof SingleMemberAnnotationExpr) {
			SingleMemberAnnotationExpr a = (SingleMemberAnnotationExpr) annotationExpr;
			// Add the "value=" member-value pair.
			if (a.getMemberValue() != null) {
				annotationPairs.add(new MemberValuePair("value", a.getMemberValue()));
			}
		} else if (annotationExpr instanceof NormalAnnotationExpr) {
			NormalAnnotationExpr a = (NormalAnnotationExpr) annotationExpr;
			// Must iterate over the expressions
			if (a.getPairs() != null) {
				annotationPairs = a.getPairs();
			}
		}

		// Iterate over the annotation attributes, creating our parsed attributes map
		for (MemberValuePair p : annotationPairs) {
			JavaSymbolName annotationName = new JavaSymbolName(p.getName());
			AnnotationAttributeValue<?> value = convert(annotationName, p.getValue());
			attributes.add(value);
			attributeMap.put(value.getName(), value);
		}
	}

	private AnnotationAttributeValue<?> convert(JavaSymbolName annotationName, Expression expression) {
		if (annotationName == null) {
			annotationName = new JavaSymbolName("__ARRAY_ELEMENT__");
		}

		if (expression instanceof AnnotationExpr) {
			AnnotationExpr annotationExpr = (AnnotationExpr) expression;
			AnnotationMetadata value = new JavaParserAnnotationMetadata(annotationExpr, compilationUnitServices);
			return new NestedAnnotationAttributeValue(annotationName, value);
		}

		if (expression instanceof BooleanLiteralExpr) {
			boolean value = ((BooleanLiteralExpr) expression).getValue();
			return new BooleanAttributeValue(annotationName, value);
		}

		if (expression instanceof CharLiteralExpr) {
			String value = ((CharLiteralExpr) expression).getValue();
			Assert.isTrue(value.length() == 1, "Expected a char expression, but instead received '" + value + "' for attribute '" + annotationName + "'");
			char c = value.charAt(0);
			return new CharAttributeValue(annotationName, c);
		}

		if (expression instanceof LongLiteralExpr) {
			String value = ((LongLiteralExpr) expression).getValue();
			Assert.isTrue(value.toUpperCase().endsWith("L"), "Expected long literal expression '" + value + "' to end in 'l' or 'L'");
			value = value.substring(0, value.length() - 1);
			long l = new Long(value);
			return new LongAttributeValue(annotationName, l);
		}

		if (expression instanceof IntegerLiteralExpr) {
			String value = ((IntegerLiteralExpr) expression).getValue();
			int i = new Integer(value);
			return new IntegerAttributeValue(annotationName, i);
		}

		if (expression instanceof DoubleLiteralExpr) {
			String value = ((DoubleLiteralExpr) expression).getValue();
			boolean floatingPrecisionOnly = false;
			if (value.toUpperCase().endsWith("F")) {
				value = value.substring(0, value.length() - 1);
				floatingPrecisionOnly = true;
			}
			if (value.toUpperCase().endsWith("D")) {
				value = value.substring(0, value.length() - 1);
			}
			double d = new Double(value);
			return new DoubleAttributeValue(annotationName, d, floatingPrecisionOnly);
		}

		if (expression instanceof BinaryExpr) {
			String result = "";
			BinaryExpr current = (BinaryExpr) expression;
			while (current != null) {
				String right = "";
				if (current.getRight() instanceof StringLiteralExpr) {
					right = ((StringLiteralExpr) current.getRight()).getValue();
				} else if (current.getRight() instanceof NameExpr) {
					right = ((NameExpr) current.getRight()).getName();
				}

				result = right + result;
				if (current.getLeft() instanceof StringLiteralExpr) {
					String left = ((StringLiteralExpr) current.getLeft()).getValue();
					result = left + result;
				}
				if (current.getLeft() instanceof BinaryExpr) {
					current = (BinaryExpr) current.getLeft();
				} else {
					current = null;
				}
			}
			return new StringAttributeValue(annotationName, result);
		}

		if (expression instanceof StringLiteralExpr) {
			String value = ((StringLiteralExpr) expression).getValue();
			return new StringAttributeValue(annotationName, value);
		}

		if (expression instanceof FieldAccessExpr) {
			FieldAccessExpr field = (FieldAccessExpr) expression;
			String fieldName = field.getField();

			// Determine the type
			Expression scope = field.getScope();
			NameExpr nameToFind = null;
			if (scope instanceof FieldAccessExpr) {
				FieldAccessExpr fScope = (FieldAccessExpr) scope;
				nameToFind = JavaParserUtils.getNameExpr(fScope.toString());
			} else if (scope instanceof NameExpr) {
				nameToFind = (NameExpr) scope;
			} else {
				throw new UnsupportedOperationException("A FieldAccessExpr for '" + field.getScope() + "' should return a NameExpr or FieldAccessExpr (was " + field.getScope().getClass().getName() + ")");
			}
			JavaType fieldType = JavaParserUtils.getJavaType(compilationUnitServices, nameToFind, null);

			EnumDetails enumDetails = new EnumDetails(fieldType, new JavaSymbolName(fieldName));
			return new EnumAttributeValue(annotationName, enumDetails);
		}

		if (expression instanceof NameExpr) {
			NameExpr field = (NameExpr) expression;
			String name = field.getName();

			JavaType fieldType = new JavaType("unknown.Object"); // As we have no way of finding out the real type

			EnumDetails enumDetails = new EnumDetails(fieldType, new JavaSymbolName(name));
			return new EnumAttributeValue(annotationName, enumDetails);
		}

		if (expression instanceof ClassExpr) {
			ClassExpr clazz = (ClassExpr) expression;
			Type nameToFind = clazz.getType();
			JavaType javaType = JavaParserUtils.getJavaType(compilationUnitServices, nameToFind, null);
			return new ClassAttributeValue(annotationName, javaType);
		}

		if (expression instanceof ArrayInitializerExpr) {
			ArrayInitializerExpr castExp = (ArrayInitializerExpr) expression;
			List<AnnotationAttributeValue<?>> arrayElements = new ArrayList<AnnotationAttributeValue<?>>();
			for (Expression e : castExp.getValues()) {
				arrayElements.add(convert(null, e));
			}
			return new ArrayAttributeValue<AnnotationAttributeValue<?>>(annotationName, arrayElements);
		}

		throw new UnsupportedOperationException("Unable to parse annotation attribute '" + annotationName + "' due to unsupported annotation expression '" + expression.getClass().getName() + "'");
	}

	public JavaType getAnnotationType() {
		return annotationType;
	}

	public AnnotationAttributeValue<?> getAttribute(JavaSymbolName attributeName) {
		Assert.notNull(attributeName, "Attribute name required");
		return attributeMap.get(attributeName);
	}

	public List<JavaSymbolName> getAttributeNames() {
		List<JavaSymbolName> result = new ArrayList<JavaSymbolName>();
		for (AnnotationAttributeValue<?> value : attributes) {
			result.add(value.getName());
		}
		return result;
	}

	/**
	 * Facilitates the addition of the annotation to the presented type.
	 * 
	 * @param compilationUnitServices to use (required)
	 * @param annotations to add to the end of (required)
	 * @param annotation to add (required)
	 */
	public static void addAnnotationToList(CompilationUnitServices compilationUnitServices, List<AnnotationExpr> annotations, AnnotationMetadata annotation) {
		Assert.notNull(compilationUnitServices, "Compilation unit services required");
		Assert.notNull(annotations, "Annotations required");
		Assert.notNull(annotation, "Annotation metadata required");

		// Create a holder for the annotation we're going to create
		boolean foundExisting = false;

		// Search for an existing annotation of this type
		for (AnnotationExpr candidate : annotations) {
			NameExpr existingName = null;
			if (candidate instanceof NormalAnnotationExpr) {
				existingName = ((NormalAnnotationExpr) candidate).getName();
			} else if (candidate instanceof MarkerAnnotationExpr) {
				existingName = ((MarkerAnnotationExpr) candidate).getName();
			} else if (candidate instanceof SingleMemberAnnotationExpr) {
				existingName = ((SingleMemberAnnotationExpr) candidate).getName();
			}

			// Convert the candidate annotation type's into a JavaType
			JavaType javaType = JavaParserUtils.getJavaType(compilationUnitServices, existingName, null);
			if (annotation.getAnnotationType().equals(javaType)) {
				foundExisting = true;
				break;
			}
		}
		Assert.isTrue(!foundExisting, "Found an existing annotation for type '" + annotation.getAnnotationType() + "'");

		// Import the annotation type, if needed
		NameExpr nameToUse = JavaParserUtils.importTypeIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), annotation.getAnnotationType());

		// Create member-value pairs in accordance with Java Parser requirements
		List<MemberValuePair> memberValuePairs = new ArrayList<MemberValuePair>();
		for (JavaSymbolName attributeName : annotation.getAttributeNames()) {
			AnnotationAttributeValue<?> value = annotation.getAttribute(attributeName);
			Assert.notNull(value, "Unable to acquire value '" + attributeName + "' from annotation");
			MemberValuePair memberValuePair = convert(value);
			Assert.notNull(memberValuePair, "Member value pair should have been set");
			memberValuePairs.add(memberValuePair);
		}

		// Create the AnnotationExpr; it varies depending on how many member-value pairs we need to present
		AnnotationExpr annotationExpression = null;
		if (memberValuePairs.isEmpty()) {
			annotationExpression = new MarkerAnnotationExpr(nameToUse);
		} else if (memberValuePairs.size() == 1 && (memberValuePairs.get(0).getName() == null || "value".equals(memberValuePairs.get(0).getName()))) {
			Expression toUse = JavaParserUtils.importExpressionIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), memberValuePairs.get(0).getValue());
			annotationExpression = new SingleMemberAnnotationExpr(nameToUse, toUse);
		} else {
			// We have a number of pairs being presented
			annotationExpression = new NormalAnnotationExpr(nameToUse, new ArrayList<MemberValuePair>());
		}
		
		// Add our AnnotationExpr to the actual annotations that will eventually be flushed through to the compilation unit
		annotations.add(annotationExpression);

		// Add member-value pairs to our AnnotationExpr
		if (memberValuePairs.size() > 0) {

			// Have to check here for cases where we need to change an existing MarkerAnnotationExpr to a NormalAnnotationExpr or SingleMemberAnnotationExpr
			if (annotationExpression instanceof MarkerAnnotationExpr) {
				MarkerAnnotationExpr mae = (MarkerAnnotationExpr) annotationExpression;

				annotations.remove(mae);

				if (memberValuePairs.size() == 1 && (memberValuePairs.get(0).getName() == null || "value".equals(memberValuePairs.get(0).getName()))) {
					Expression toUse = JavaParserUtils.importExpressionIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), memberValuePairs.get(0).getValue());
					annotationExpression = new SingleMemberAnnotationExpr(nameToUse, toUse);
					annotations.add(annotationExpression);
				} else {
					// We have a number of pairs being presented
					annotationExpression = new NormalAnnotationExpr(nameToUse, new ArrayList<MemberValuePair>());
					annotations.add(annotationExpression);
				}
			}
			if (annotationExpression instanceof SingleMemberAnnotationExpr) {
				// Potentially upgrade this expression to a NormalAnnotationExpr
				SingleMemberAnnotationExpr smae = (SingleMemberAnnotationExpr) annotationExpression;
				if (memberValuePairs.size() == 1 && memberValuePairs.get(0).getName() == null || memberValuePairs.get(0).getName().equals("value") || memberValuePairs.get(0).getName().equals("")) {
					// They specified only a single member-value pair, and it is the default anyway, so we need not do anything except update the value
					Expression toUse = JavaParserUtils.importExpressionIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), memberValuePairs.get(0).getValue());
					smae.setMemberValue(toUse);
					return;
				}
				
				// There is > 1 expression, or they have provided some sort of non-default value, so it's time to upgrade the expression
				// (whilst retaining any potentially existing expression values)
				Expression existingValue = smae.getMemberValue();
				annotationExpression = new NormalAnnotationExpr(smae.getName(), new ArrayList<MemberValuePair>());
				((NormalAnnotationExpr) annotationExpression).getPairs().add(new MemberValuePair("value", existingValue));
			}
			Assert.isInstanceOf(NormalAnnotationExpr.class, annotationExpression, "Attempting to add >1 annotation member-value pair requires an existing normal annotation expression");
			List<MemberValuePair> annotationPairs = ((NormalAnnotationExpr) annotationExpression).getPairs();
			annotationPairs.clear();
			for (MemberValuePair pair : memberValuePairs) {
				Expression toUse = JavaParserUtils.importExpressionIfRequired(compilationUnitServices.getEnclosingTypeName(), compilationUnitServices.getImports(), pair.getValue());
				pair.setValue(toUse);
				annotationPairs.add(pair);
			}
		}
	}

	/**
	 * Facilitates the removal of the annotation type indicated.
	 * 
	 * @param compilationUnitServices to use (required)
	 * @param annotations to remove the annotation from (required)
	 * @param annotation to remove (required)
	 */
	public static void removeAnnotationFromList(CompilationUnitServices compilationUnitServices, List<AnnotationExpr> annotations, JavaType annotation) {
		Assert.notNull(compilationUnitServices, "Compilation unit services required");
		Assert.notNull(annotations, "Annotations required");
		Assert.notNull(annotation, "Annotation metadata required");

		AnnotationExpr toRemove = null;
		for (AnnotationExpr candidate : annotations) {
			NameExpr existingName = null;
			if (candidate instanceof NormalAnnotationExpr) {
				existingName = ((NormalAnnotationExpr) candidate).getName();
			} else if (candidate instanceof MarkerAnnotationExpr) {
				existingName = ((MarkerAnnotationExpr) candidate).getName();
			} else if (candidate instanceof SingleMemberAnnotationExpr) {
				existingName = ((SingleMemberAnnotationExpr) candidate).getName();
			}

			JavaType javaType = JavaParserUtils.getJavaType(compilationUnitServices, existingName, null);
			if (annotation.equals(javaType)) {
				toRemove = candidate;
				break;
			}
		}

		Assert.notNull(toRemove, "Could not find annotation for type '" + annotation.getFullyQualifiedTypeName() + "' to remove");

		annotations.remove(toRemove);
	}

	@SuppressWarnings("unchecked")
	private static MemberValuePair convert(AnnotationAttributeValue<?> value) {
		if (value instanceof NestedAnnotationAttributeValue) {
			NestedAnnotationAttributeValue castValue = (NestedAnnotationAttributeValue) value;
			Assert.isInstanceOf(JavaParserAnnotationMetadata.class, castValue.getValue(), "Cannot present nested annotations unless created by this class");
			JavaParserAnnotationMetadata javaParserMutableAnnotationMetadata = (JavaParserAnnotationMetadata) castValue.getValue();

			// Rely on the nested instance to know its member value pairs
			return new MemberValuePair(value.getName().getSymbolName(), javaParserMutableAnnotationMetadata.annotationExpr);
		}

		if (value instanceof BooleanAttributeValue) {
			boolean castValue = ((BooleanAttributeValue) value).getValue();
			BooleanLiteralExpr convertedValue = new BooleanLiteralExpr(castValue);
			return new MemberValuePair(value.getName().getSymbolName(), convertedValue);
		}

		if (value instanceof CharAttributeValue) {
			char castValue = ((CharAttributeValue) value).getValue();
			CharLiteralExpr convertedValue = new CharLiteralExpr(new String(new char[] { castValue }));
			return new MemberValuePair(value.getName().getSymbolName(), convertedValue);
		}

		if (value instanceof LongAttributeValue) {
			Long castValue = ((LongAttributeValue) value).getValue();
			LongLiteralExpr convertedValue = new LongLiteralExpr(castValue.toString() + "L");
			return new MemberValuePair(value.getName().getSymbolName(), convertedValue);
		}

		if (value instanceof IntegerAttributeValue) {
			Integer castValue = ((IntegerAttributeValue) value).getValue();
			IntegerLiteralExpr convertedValue = new IntegerLiteralExpr(castValue.toString());
			return new MemberValuePair(value.getName().getSymbolName(), convertedValue);
		}

		if (value instanceof DoubleAttributeValue) {
			DoubleAttributeValue doubleAttributeValue = (DoubleAttributeValue) value;
			Double castValue = doubleAttributeValue.getValue();
			DoubleLiteralExpr convertedValue;
			if (doubleAttributeValue.isFloatingPrecisionOnly()) {
				convertedValue = new DoubleLiteralExpr(castValue.toString() + "F");
			} else {
				convertedValue = new DoubleLiteralExpr(castValue.toString() + "D");
			}
			return new MemberValuePair(value.getName().getSymbolName(), convertedValue);
		}

		if (value instanceof StringAttributeValue) {
			String castValue = ((StringAttributeValue) value).getValue();
			StringLiteralExpr convertedValue = new StringLiteralExpr(castValue.toString());
			return new MemberValuePair(value.getName().getSymbolName(), convertedValue);
		}

		if (value instanceof EnumAttributeValue) {
			EnumDetails castValue = ((EnumAttributeValue) value).getValue();
			// This isn't as elegant as it could be (ie loss of type parameters), but it will do for now
			FieldAccessExpr convertedValue = new FieldAccessExpr(JavaParserUtils.getNameExpr(castValue.getType().getFullyQualifiedTypeName()), castValue.getField().getSymbolName());
			return new MemberValuePair(value.getName().getSymbolName(), convertedValue);
		}

		if (value instanceof ClassAttributeValue) {
			JavaType castValue = ((ClassAttributeValue) value).getValue();
			// This doesn't preserve type parameters
			NameExpr nameExpr = JavaParserUtils.getNameExpr(castValue.getFullyQualifiedTypeName());
			ClassExpr convertedValue = new ClassExpr(JavaParserUtils.getReferenceType(nameExpr));
			return new MemberValuePair(value.getName().getSymbolName(), convertedValue);
		}

		if (value instanceof ArrayAttributeValue) {
			ArrayAttributeValue<AnnotationAttributeValue<?>> castValue = (ArrayAttributeValue<AnnotationAttributeValue<?>>) value;

			List<Expression> arrayElements = new ArrayList<Expression>();
			for (AnnotationAttributeValue<?> v : castValue.getValue()) {
				arrayElements.add(convert(v).getValue());
			}
			return new MemberValuePair(value.getName().getSymbolName(), new ArrayInitializerExpr(arrayElements));
		}

		throw new UnsupportedOperationException("Unsupported attribute value '" + value.getName() + "' of type '" + value.getClass().getName() + "'");
	}

	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("annotationType", annotationType);
		tsc.append("attributes", attributes);
		return tsc.toString();
	}
}
