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
import japa.parser.ast.expr.UnaryExpr;
import japa.parser.ast.expr.UnaryExpr.Operator;
import japa.parser.ast.type.Type;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
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
import org.springframework.roo.model.Builder;
import org.springframework.roo.model.EnumDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Java Parser implementation of {@link AnnotationMetadata}.
 * 
 * @author Ben Alex
 * @author Andrew Swan
 * @since 1.0
 */
public class JavaParserAnnotationMetadataBuilder implements
        Builder<AnnotationMetadata> {

    /**
     * Facilitates the addition of the annotation to the presented type.
     * 
     * @param compilationUnitServices to use (required)
     * @param annotations to add to the end of (required)
     * @param annotation to add (required)
     */
    public static void addAnnotationToList(
            final CompilationUnitServices compilationUnitServices,
            final List<AnnotationExpr> annotations,
            final AnnotationMetadata annotation) {
        Validate.notNull(compilationUnitServices,
                "Compilation unit services required");
        Validate.notNull(annotations, "Annotations required");
        Validate.notNull(annotation, "Annotation metadata required");

        // Create a holder for the annotation we're going to create
        boolean foundExisting = false;

        // Search for an existing annotation of this type
        for (final AnnotationExpr candidate : annotations) {
            NameExpr existingName = null;
            if (candidate instanceof NormalAnnotationExpr) {
                existingName = ((NormalAnnotationExpr) candidate).getName();
            }
            else if (candidate instanceof MarkerAnnotationExpr) {
                existingName = ((MarkerAnnotationExpr) candidate).getName();
            }
            else if (candidate instanceof SingleMemberAnnotationExpr) {
                existingName = ((SingleMemberAnnotationExpr) candidate)
                        .getName();
            }

            // Convert the candidate annotation type's into a JavaType
            final JavaType javaType = JavaParserUtils.getJavaType(
                    compilationUnitServices, existingName, null);
            if (annotation.getAnnotationType().equals(javaType)) {
                foundExisting = true;
                break;
            }
        }
        Validate.isTrue(
                !foundExisting,
                "Found an existing annotation for type '"
                        + annotation.getAnnotationType() + "'");

        // Import the annotation type, if needed
        final NameExpr nameToUse = JavaParserUtils.importTypeIfRequired(
                compilationUnitServices.getEnclosingTypeName(),
                compilationUnitServices.getImports(),
                annotation.getAnnotationType());

        // Create member-value pairs in accordance with Java Parser requirements
        final List<MemberValuePair> memberValuePairs = new ArrayList<MemberValuePair>();
        for (final JavaSymbolName attributeName : annotation
                .getAttributeNames()) {
            final AnnotationAttributeValue<?> value = annotation
                    .getAttribute(attributeName);
            Validate.notNull(value, "Unable to acquire value '" + attributeName
                    + "' from annotation");
            final MemberValuePair memberValuePair = convert(value);
            // Validate.notNull(memberValuePair,
            // "Member value pair should have been set");
            if (memberValuePair != null) {
                memberValuePairs.add(memberValuePair);
            }
        }

        // Create the AnnotationExpr; it varies depending on how many
        // member-value pairs we need to present
        AnnotationExpr annotationExpression = null;
        if (memberValuePairs.isEmpty()) {
            annotationExpression = new MarkerAnnotationExpr(nameToUse);
        }
        else if (memberValuePairs.size() == 1
                && (memberValuePairs.get(0).getName() == null || "value"
                        .equals(memberValuePairs.get(0).getName()))) {
            final Expression toUse = JavaParserUtils
                    .importExpressionIfRequired(
                            compilationUnitServices.getEnclosingTypeName(),
                            compilationUnitServices.getImports(),
                            memberValuePairs.get(0).getValue());
            annotationExpression = new SingleMemberAnnotationExpr(nameToUse,
                    toUse);
        }
        else {
            // We have a number of pairs being presented
            annotationExpression = new NormalAnnotationExpr(nameToUse,
                    new ArrayList<MemberValuePair>());
        }

        // Add our AnnotationExpr to the actual annotations that will eventually
        // be flushed through to the compilation unit
        annotations.add(annotationExpression);

        // Add member-value pairs to our AnnotationExpr
        if (!memberValuePairs.isEmpty()) {
            // Have to check here for cases where we need to change an existing
            // MarkerAnnotationExpr to a NormalAnnotationExpr or
            // SingleMemberAnnotationExpr
            if (annotationExpression instanceof MarkerAnnotationExpr) {
                final MarkerAnnotationExpr mae = (MarkerAnnotationExpr) annotationExpression;

                annotations.remove(mae);

                if (memberValuePairs.size() == 1
                        && (memberValuePairs.get(0).getName() == null || "value"
                                .equals(memberValuePairs.get(0).getName()))) {
                    final Expression toUse = JavaParserUtils
                            .importExpressionIfRequired(compilationUnitServices
                                    .getEnclosingTypeName(),
                                    compilationUnitServices.getImports(),
                                    memberValuePairs.get(0).getValue());
                    annotationExpression = new SingleMemberAnnotationExpr(
                            nameToUse, toUse);
                    annotations.add(annotationExpression);
                }
                else {
                    // We have a number of pairs being presented
                    annotationExpression = new NormalAnnotationExpr(nameToUse,
                            new ArrayList<MemberValuePair>());
                    annotations.add(annotationExpression);
                }
            }
            if (annotationExpression instanceof SingleMemberAnnotationExpr) {
                // Potentially upgrade this expression to a NormalAnnotationExpr
                final SingleMemberAnnotationExpr smae = (SingleMemberAnnotationExpr) annotationExpression;
                if (memberValuePairs.size() == 1
                        && memberValuePairs.get(0).getName() == null
                        || memberValuePairs.get(0).getName().equals("value")
                        || memberValuePairs.get(0).getName().equals("")) {
                    // They specified only a single member-value pair, and it is
                    // the default anyway, so we need not do anything except
                    // update the value
                    final Expression toUse = JavaParserUtils
                            .importExpressionIfRequired(compilationUnitServices
                                    .getEnclosingTypeName(),
                                    compilationUnitServices.getImports(),
                                    memberValuePairs.get(0).getValue());
                    smae.setMemberValue(toUse);
                    return;
                }

                // There is > 1 expression, or they have provided some sort of
                // non-default value, so it's time to upgrade the expression
                // (whilst retaining any potentially existing expression values)
                final Expression existingValue = smae.getMemberValue();
                annotationExpression = new NormalAnnotationExpr(smae.getName(),
                        new ArrayList<MemberValuePair>());
                ((NormalAnnotationExpr) annotationExpression).getPairs().add(
                        new MemberValuePair("value", existingValue));
            }
            Validate.isInstanceOf(
                    NormalAnnotationExpr.class,
                    annotationExpression,
                    "Attempting to add >1 annotation member-value pair requires an existing normal annotation expression");
            final List<MemberValuePair> annotationPairs = ((NormalAnnotationExpr) annotationExpression)
                    .getPairs();
            annotationPairs.clear();
            for (final MemberValuePair pair : memberValuePairs) {
                final Expression toUse = JavaParserUtils
                        .importExpressionIfRequired(
                                compilationUnitServices.getEnclosingTypeName(),
                                compilationUnitServices.getImports(),
                                pair.getValue());
                pair.setValue(toUse);
                annotationPairs.add(pair);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static MemberValuePair convert(
            final AnnotationAttributeValue<?> value) {
        if (value instanceof NestedAnnotationAttributeValue) {
            final NestedAnnotationAttributeValue castValue = (NestedAnnotationAttributeValue) value;
            AnnotationExpr annotationExpr;
            final AnnotationMetadata nestedAnnotation = castValue.getValue();
            if (castValue.getValue().getAttributeNames().size() == 0) {
                annotationExpr = new MarkerAnnotationExpr(
                        JavaParserUtils.getNameExpr(nestedAnnotation
                                .getAnnotationType()
                                .getFullyQualifiedTypeName()));
            }
            else if (castValue.getValue().getAttributeNames().size() == 1) {
                annotationExpr = new SingleMemberAnnotationExpr(
                        JavaParserUtils.getNameExpr(nestedAnnotation
                                .getAnnotationType()
                                .getFullyQualifiedTypeName()), convert(
                                nestedAnnotation.getAttribute(nestedAnnotation
                                        .getAttributeNames().get(0)))
                                .getValue());
            }
            else {
                final List<MemberValuePair> memberValuePairs = new ArrayList<MemberValuePair>();
                for (final JavaSymbolName attributeName : nestedAnnotation
                        .getAttributeNames()) {
                    memberValuePairs.add(convert(nestedAnnotation
                            .getAttribute(attributeName)));
                }
                annotationExpr = new NormalAnnotationExpr(
                        JavaParserUtils.getNameExpr(nestedAnnotation
                                .getAnnotationType()
                                .getFullyQualifiedTypeName()), memberValuePairs);
            }
            // Rely on the nested instance to know its member value pairs
            return new MemberValuePair(value.getName().getSymbolName(),
                    annotationExpr);
        }

        if (value instanceof BooleanAttributeValue) {
            final boolean castValue = ((BooleanAttributeValue) value)
                    .getValue();
            final BooleanLiteralExpr convertedValue = new BooleanLiteralExpr(
                    castValue);
            return new MemberValuePair(value.getName().getSymbolName(),
                    convertedValue);
        }

        if (value instanceof CharAttributeValue) {
            final char castValue = ((CharAttributeValue) value).getValue();
            final CharLiteralExpr convertedValue = new CharLiteralExpr(
                    new String(new char[] { castValue }));
            return new MemberValuePair(value.getName().getSymbolName(),
                    convertedValue);
        }

        if (value instanceof LongAttributeValue) {
            final Long castValue = ((LongAttributeValue) value).getValue();
            final LongLiteralExpr convertedValue = new LongLiteralExpr(
                    castValue.toString() + "L");
            return new MemberValuePair(value.getName().getSymbolName(),
                    convertedValue);
        }

        if (value instanceof IntegerAttributeValue) {
            final Integer castValue = ((IntegerAttributeValue) value)
                    .getValue();
            final IntegerLiteralExpr convertedValue = new IntegerLiteralExpr(
                    castValue.toString());
            return new MemberValuePair(value.getName().getSymbolName(),
                    convertedValue);
        }

        if (value instanceof DoubleAttributeValue) {
            final DoubleAttributeValue doubleAttributeValue = (DoubleAttributeValue) value;
            final Double castValue = doubleAttributeValue.getValue();
            DoubleLiteralExpr convertedValue;
            if (doubleAttributeValue.isFloatingPrecisionOnly()) {
                convertedValue = new DoubleLiteralExpr(castValue.toString()
                        + "F");
            }
            else {
                convertedValue = new DoubleLiteralExpr(castValue.toString()
                        + "D");
            }
            return new MemberValuePair(value.getName().getSymbolName(),
                    convertedValue);
        }

        if (value instanceof StringAttributeValue) {
            final String castValue = ((StringAttributeValue) value).getValue();
            final StringLiteralExpr convertedValue = new StringLiteralExpr(
                    castValue.toString());
            return new MemberValuePair(value.getName().getSymbolName(),
                    convertedValue);
        }

        if (value instanceof EnumAttributeValue) {
            final EnumDetails castValue = ((EnumAttributeValue) value)
                    .getValue();
            // This isn't as elegant as it could be (ie loss of type
            // parameters), but it will do for now
            final FieldAccessExpr convertedValue = new FieldAccessExpr(
                    JavaParserUtils.getNameExpr(castValue.getType()
                            .getFullyQualifiedTypeName()), castValue.getField()
                            .getSymbolName());
            return new MemberValuePair(value.getName().getSymbolName(),
                    convertedValue);
        }

        if (value instanceof ClassAttributeValue) {
            final JavaType castValue = ((ClassAttributeValue) value).getValue();
            // This doesn't preserve type parameters
            final NameExpr nameExpr = JavaParserUtils.getNameExpr(castValue
                    .getFullyQualifiedTypeName());
            final ClassExpr convertedValue = new ClassExpr(
                    JavaParserUtils.getReferenceType(nameExpr));
            return new MemberValuePair(value.getName().getSymbolName(),
                    convertedValue);
        }

        if (value instanceof ArrayAttributeValue) {
            final ArrayAttributeValue<AnnotationAttributeValue<?>> castValue = (ArrayAttributeValue<AnnotationAttributeValue<?>>) value;

            final List<Expression> arrayElements = new ArrayList<Expression>();
            for (final AnnotationAttributeValue<?> v : castValue.getValue()) {
                MemberValuePair converted = convert(v);
                if (converted != null) {
                    arrayElements.add(converted.getValue());
                }
            }
            return new MemberValuePair(value.getName().getSymbolName(),
                    new ArrayInitializerExpr(arrayElements));
        }

        throw new UnsupportedOperationException("Unsupported attribute value '"
                + value.getName() + "' of type '" + value.getClass().getName()
                + "'");
    }

    public static JavaParserAnnotationMetadataBuilder getInstance(
            final AnnotationExpr annotationExpr,
            final CompilationUnitServices compilationUnitServices) {
        return new JavaParserAnnotationMetadataBuilder(annotationExpr,
                compilationUnitServices);
    }

    private final JavaType annotationType;

    private final List<AnnotationAttributeValue<?>> attributeValues;

    /**
     * Factory method
     * 
     * @param annotationExpr
     * @param compilationUnitServices
     * @return a non-<code>null</code> instance
     * @since 1.2.0
     */
    private JavaParserAnnotationMetadataBuilder(
            final AnnotationExpr annotationExpr,
            final CompilationUnitServices compilationUnitServices) {
        Validate.notNull(annotationExpr, "Annotation expression required");
        Validate.notNull(compilationUnitServices,
                "Compilation unit services required");

        // Obtain the annotation type name from the assorted types of
        // annotations we might have received (ie marker annotations, single
        // member annotations, normal annotations etc)
        final NameExpr nameToFind = JavaParserUtils.getNameExpr(annotationExpr);

        // Compute the actual annotation type, having regard to the compilation
        // unit package and imports
        annotationType = JavaParserUtils.getJavaType(compilationUnitServices,
                nameToFind, null);

        // Generate some member-value pairs for subsequent parsing
        List<MemberValuePair> annotationPairs = new ArrayList<MemberValuePair>();
        if (annotationExpr instanceof MarkerAnnotationExpr) {
            // A marker annotation has no values, so we can have no pairs to add
        }
        else if (annotationExpr instanceof SingleMemberAnnotationExpr) {
            final SingleMemberAnnotationExpr a = (SingleMemberAnnotationExpr) annotationExpr;
            // Add the "value=" member-value pair.
            if (a.getMemberValue() != null) {
                annotationPairs.add(new MemberValuePair("value", a
                        .getMemberValue()));
            }
        }
        else if (annotationExpr instanceof NormalAnnotationExpr) {
            final NormalAnnotationExpr a = (NormalAnnotationExpr) annotationExpr;
            // Must iterate over the expressions
            if (a.getPairs() != null) {
                annotationPairs = a.getPairs();
            }
        }

        // Iterate over the annotation attributes, creating our parsed
        // attributes map
        final List<AnnotationAttributeValue<?>> attributeValues = new ArrayList<AnnotationAttributeValue<?>>();
        for (final MemberValuePair p : annotationPairs) {
            final JavaSymbolName annotationName = new JavaSymbolName(
                    p.getName());
            final AnnotationAttributeValue<?> value = convert(annotationName,
                    p.getValue(), compilationUnitServices);
            attributeValues.add(value);
        }
        this.attributeValues = attributeValues;
    }

    public AnnotationMetadata build() {
        final AnnotationMetadataBuilder annotationMetadataBuilder = new AnnotationMetadataBuilder(
                annotationType, attributeValues);
        return annotationMetadataBuilder.build();
    }

    private AnnotationAttributeValue<?> convert(JavaSymbolName annotationName,
            final Expression expression,
            final CompilationUnitServices compilationUnitServices) {
        if (annotationName == null) {
            annotationName = new JavaSymbolName("__ARRAY_ELEMENT__");
        }

        if (expression instanceof AnnotationExpr) {
            final AnnotationExpr annotationExpr = (AnnotationExpr) expression;
            final AnnotationMetadata value = getInstance(annotationExpr,
                    compilationUnitServices).build();
            return new NestedAnnotationAttributeValue(annotationName, value);
        }

        if (expression instanceof BooleanLiteralExpr) {
            final boolean value = ((BooleanLiteralExpr) expression).getValue();
            return new BooleanAttributeValue(annotationName, value);
        }

        if (expression instanceof CharLiteralExpr) {
            final String value = ((CharLiteralExpr) expression).getValue();
            Validate.isTrue(value.length() == 1,
                    "Expected a char expression, but instead received '"
                            + value + "' for attribute '" + annotationName
                            + "'");
            final char c = value.charAt(0);
            return new CharAttributeValue(annotationName, c);
        }

        if (expression instanceof LongLiteralExpr) {
            String value = ((LongLiteralExpr) expression).getValue();
            Validate.isTrue(value.toUpperCase().endsWith("L"),
                    "Expected long literal expression '" + value
                            + "' to end in 'l' or 'L'");
            value = value.substring(0, value.length() - 1);
            final long l = new Long(value);
            return new LongAttributeValue(annotationName, l);
        }

        if (expression instanceof IntegerLiteralExpr) {
            final String value = ((IntegerLiteralExpr) expression).getValue();
            final int i = new Integer(value);
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
            final double d = new Double(value);
            return new DoubleAttributeValue(annotationName, d,
                    floatingPrecisionOnly);
        }

        if (expression instanceof BinaryExpr) {
            String result = "";
            BinaryExpr current = (BinaryExpr) expression;
            while (current != null) {
                String right = "";
                if (current.getRight() instanceof StringLiteralExpr) {
                    right = ((StringLiteralExpr) current.getRight()).getValue();
                }
                else if (current.getRight() instanceof NameExpr) {
                    right = ((NameExpr) current.getRight()).getName();
                }

                result = right + result;
                if (current.getLeft() instanceof StringLiteralExpr) {
                    final String left = ((StringLiteralExpr) current.getLeft())
                            .getValue();
                    result = left + result;
                }
                if (current.getLeft() instanceof BinaryExpr) {
                    current = (BinaryExpr) current.getLeft();
                }
                else {
                    current = null;
                }
            }
            return new StringAttributeValue(annotationName, result);
        }

        if (expression instanceof StringLiteralExpr) {
            final String value = ((StringLiteralExpr) expression).getValue();
            return new StringAttributeValue(annotationName, value);
        }

        if (expression instanceof FieldAccessExpr) {
            final FieldAccessExpr field = (FieldAccessExpr) expression;
            final String fieldName = field.getField();

            // Determine the type
            final Expression scope = field.getScope();
            NameExpr nameToFind = null;
            if (scope instanceof FieldAccessExpr) {
                final FieldAccessExpr fScope = (FieldAccessExpr) scope;
                nameToFind = JavaParserUtils.getNameExpr(fScope.toString());
            }
            else if (scope instanceof NameExpr) {
                nameToFind = (NameExpr) scope;
            }
            else {
                throw new UnsupportedOperationException(
                        "A FieldAccessExpr for '"
                                + field.getScope()
                                + "' should return a NameExpr or FieldAccessExpr (was "
                                + field.getScope().getClass().getName() + ")");
            }
            final JavaType fieldType = JavaParserUtils.getJavaType(
                    compilationUnitServices, nameToFind, null);

            final EnumDetails enumDetails = new EnumDetails(fieldType,
                    new JavaSymbolName(fieldName));
            return new EnumAttributeValue(annotationName, enumDetails);
        }

        if (expression instanceof NameExpr) {
            final NameExpr field = (NameExpr) expression;
            final String name = field.getName();
            // As we have no way of finding out the real type
            final JavaType fieldType = new JavaType("unknown.Object");
            final EnumDetails enumDetails = new EnumDetails(fieldType,
                    new JavaSymbolName(name));
            return new EnumAttributeValue(annotationName, enumDetails);
        }

        if (expression instanceof ClassExpr) {
            final ClassExpr clazz = (ClassExpr) expression;
            final Type nameToFind = clazz.getType();
            final JavaType javaType = JavaParserUtils.getJavaType(
                    compilationUnitServices, nameToFind, null);
            return new ClassAttributeValue(annotationName, javaType);
        }

        if (expression instanceof ArrayInitializerExpr) {
            final ArrayInitializerExpr castExp = (ArrayInitializerExpr) expression;
            final List<AnnotationAttributeValue<?>> arrayElements = new ArrayList<AnnotationAttributeValue<?>>();
            for (final Expression e : castExp.getValues()) {
                arrayElements.add(convert(null, e, compilationUnitServices));
            }
            return new ArrayAttributeValue<AnnotationAttributeValue<?>>(
                    annotationName, arrayElements);
        }

        if (expression instanceof UnaryExpr) {
            final UnaryExpr castExp = (UnaryExpr) expression;
            if (castExp.getOperator() == Operator.negative) {
                String value = castExp.toString();
                value = value.toUpperCase().endsWith("L") ? value.substring(0,
                        value.length() - 1) : value;
                final long l = new Long(value);
                return new LongAttributeValue(annotationName, l);
            }
            else {
                throw new UnsupportedOperationException(
                        "Only negative operator in UnaryExpr is supported");
            }
        }

        throw new UnsupportedOperationException(
                "Unable to parse annotation attribute '" + annotationName
                        + "' due to unsupported annotation expression '"
                        + expression.getClass().getName() + "'");
    }
}
