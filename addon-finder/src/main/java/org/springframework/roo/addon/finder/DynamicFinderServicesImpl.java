package org.springframework.roo.addon.finder;

import static org.springframework.roo.model.JavaType.LONG_OBJECT;
import static org.springframework.roo.model.JdkJavaType.MAP;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.customdata.CustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;

/**
 * Default implementation of {@link DynamicFinderServices}.
 * 
 * @author Stefan Schmidt
 * @author Alan Stewart
 * @since 1.0
 */
@Component
@Service
public class DynamicFinderServicesImpl implements DynamicFinderServices {

    private Set<JavaSymbolName> createFinders(final FieldMetadata field,
            final Set<JavaSymbolName> finders, final String prepend,
            final boolean isFirst) {
        final Set<JavaSymbolName> tempFinders = new HashSet<JavaSymbolName>();

        if (isNumberOrDate(field.getFieldType())) {
            for (final ReservedToken keyWord : ReservedTokenHolder.NUMERIC_TOKENS) {
                tempFinders.addAll(populateFinders(finders, field, prepend,
                        isFirst, keyWord.getValue()));
            }
        }
        else if (field.getFieldType().equals(JavaType.STRING)) {
            for (final ReservedToken keyWord : ReservedTokenHolder.STRING_TOKENS) {
                tempFinders.addAll(populateFinders(finders, field, prepend,
                        isFirst, keyWord.getValue()));
            }
        }
        else if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT)
                || field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE)) {
            for (final ReservedToken keyWord : ReservedTokenHolder.BOOLEAN_TOKENS) {
                tempFinders.addAll(populateFinders(finders, field, prepend,
                        isFirst, keyWord.getValue()));
            }
        }
        else {
            tempFinders.addAll(populateFinders(finders, field, prepend,
                    isFirst, ""));
        }

        return tempFinders;
    }

    /**
     * Returns the {@link JavaType} from the specified {@link MemberDetails}
     * object;
     * <p>
     * If the found type is abstract the next {@link MemberHoldingTypeDetails}
     * is searched.
     * 
     * @param memberDetails the {@link MemberDetails} to search (required)
     * @return the first non-abstract JavaType, or null if not found
     */
    private JavaType getConcreteJavaType(final MemberDetails memberDetails) {
        Validate.notNull(memberDetails, "Member details required");
        JavaType javaType = null;
        for (final MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails
                .getDetails()) {
            if (Modifier.isAbstract(memberHoldingTypeDetails.getModifier())) {
                continue;
            }
            javaType = memberHoldingTypeDetails.getName();
        }
        return javaType;
    }

    public List<JavaSymbolName> getFinders(final MemberDetails memberDetails,
            final String plural, final int depth,
            final Set<JavaSymbolName> exclusions) {
        Validate.notNull(memberDetails, "Member details required");
        Validate.notBlank(plural, "Plural required");
        Validate.notNull(depth,
                "The depth of combinations used for finder signatures combinations required");
        Validate.notNull(exclusions, "Exclusions required");

        final SortedSet<JavaSymbolName> finders = new TreeSet<JavaSymbolName>();

        final List<FieldMetadata> fields = memberDetails.getFields();
        for (int i = 0; i < depth; i++) {
            final SortedSet<JavaSymbolName> tempFinders = new TreeSet<JavaSymbolName>();
            for (final FieldMetadata field : fields) {
                // Ignoring java.util.Map field types (see ROO-194)
                if (field == null || field.getFieldType().equals(MAP)) {
                    continue;
                }
                if (exclusions.contains(field.getFieldName())) {
                    continue;
                }
                if (i == 0) {
                    tempFinders.addAll(createFinders(field, finders, "find"
                            + plural + "By", true));
                }
                else {
                    tempFinders.addAll(createFinders(field, finders, "And",
                            false));
                    tempFinders.addAll(createFinders(field, finders, "Or",
                            false));
                }
            }
            finders.addAll(tempFinders);
        }

        return Collections.unmodifiableList(new ArrayList<JavaSymbolName>(
                finders));
    }

    private Token getFirstToken(final SortedSet<FieldToken> fieldTokens,
            final String finder, final String originalFinder,
            final String simpleTypeName) {
        for (final FieldToken fieldToken : fieldTokens) {
            if (finder.startsWith(fieldToken.getValue())) {
                return fieldToken;
            }
        }
        for (final ReservedToken reservedToken : ReservedTokenHolder.ALL_TOKENS) {
            if (finder.startsWith(reservedToken.getValue())) {
                return reservedToken;
            }
        }
        if (finder.length() > 0) {
            // TODO: Make this a FinderFieldTokenMissingException instead, to
            // make it easier to detect this
            throw new FinderFieldTokenMissingException(
                    "Dynamic finder is unable to match '" + finder
                            + "' token of '" + originalFinder
                            + "' finder definition in " + simpleTypeName
                            + ".java");
        }

        return null; // Finder does not start with reserved or field token
    }

    private String getJpaQuery(final List<Token> tokens,
            final String simpleTypeName, final JavaSymbolName finderName,
            final String plural, final String entityName) {
        final String typeName = StringUtils.defaultIfEmpty(entityName,
                simpleTypeName);
        final StringBuilder builder = new StringBuilder();
        builder.append("SELECT o FROM ").append(typeName);
        builder.append(" AS o WHERE ");

        FieldToken lastFieldToken = null;
        boolean isNewField = true;
        boolean isFieldApplied = false;

        for (final Token token : tokens) {
            if (token instanceof ReservedToken) {
                final String reservedToken = token.getValue();
                if (lastFieldToken == null) {
                    continue;
                }
                final String fieldName = lastFieldToken.getField()
                        .getFieldName().getSymbolName();
                boolean setField = true;

                if (!lastFieldToken.getField().getFieldType()
                        .isCommonCollectionType()) {
                    if (isNewField) {
                        if (reservedToken.equalsIgnoreCase("Like")) {
                            builder.append("LOWER(").append("o.")
                                    .append(fieldName).append(')');
                        }
                        else {
                            builder.append("o.").append(fieldName);
                        }
                        isNewField = false;
                        isFieldApplied = false;
                    }
                    if (reservedToken.equalsIgnoreCase("And")) {
                        if (!isFieldApplied) {
                            builder.append(" = :").append(fieldName);
                            isFieldApplied = true;
                        }
                        builder.append(" AND ");
                        setField = false;
                    }
                    else if (reservedToken.equalsIgnoreCase("Or")) {
                        if (!isFieldApplied) {
                            builder.append(" = :").append(fieldName);
                            isFieldApplied = true;
                        }
                        builder.append(" OR ");
                        setField = false;
                    }
                    else if (reservedToken.equalsIgnoreCase("Between")) {
                        builder.append(" BETWEEN ")
                                .append(":min")
                                .append(lastFieldToken.getField()
                                        .getFieldName()
                                        .getSymbolNameCapitalisedFirstLetter())
                                .append(" AND ")
                                .append(":max")
                                .append(lastFieldToken.getField()
                                        .getFieldName()
                                        .getSymbolNameCapitalisedFirstLetter())
                                .append(" ");
                        setField = false;
                        isFieldApplied = true;
                    }
                    else if (reservedToken.equalsIgnoreCase("Like")) {
                        builder.append(" LIKE ");
                        setField = true;
                    }
                    else if (reservedToken.equalsIgnoreCase("IsNotNull")) {
                        builder.append(" IS NOT NULL ");
                        setField = false;
                        isFieldApplied = true;
                    }
                    else if (reservedToken.equalsIgnoreCase("IsNull")) {
                        builder.append(" IS NULL ");
                        setField = false;
                        isFieldApplied = true;
                    }
                    else if (reservedToken.equalsIgnoreCase("Not")) {
                        builder.append(" IS NOT ");
                    }
                    else if (reservedToken.equalsIgnoreCase("NotEquals")) {
                        builder.append(" != ");
                    }
                    else if (reservedToken.equalsIgnoreCase("LessThan")) {
                        builder.append(" < ");
                    }
                    else if (reservedToken.equalsIgnoreCase("LessThanEquals")) {
                        builder.append(" <= ");
                    }
                    else if (reservedToken.equalsIgnoreCase("GreaterThan")) {
                        builder.append(" > ");
                    }
                    else if (reservedToken
                            .equalsIgnoreCase("GreaterThanEquals")) {
                        builder.append(" >= ");
                    }
                    else if (reservedToken.equalsIgnoreCase("Equals")) {
                        builder.append(" = ");
                    }
                    if (setField) {
                        if (builder.toString().endsWith("LIKE ")) {
                            builder.append("LOWER(:").append(fieldName)
                                    .append(") ");
                        }
                        else {
                            builder.append(':').append(fieldName).append(' ');
                        }
                        isFieldApplied = true;
                    }
                }
            }
            else {
                lastFieldToken = (FieldToken) token;
                isNewField = true;
            }
        }
        if (isNewField) {
            if (lastFieldToken != null
                    && !lastFieldToken.getField().getFieldType()
                            .isCommonCollectionType()) {
                builder.append("o.").append(
                        lastFieldToken.getField().getFieldName()
                                .getSymbolName());
            }
            isFieldApplied = false;
        }
        if (!isFieldApplied) {
            if (lastFieldToken != null
                    && !lastFieldToken.getField().getFieldType()
                            .isCommonCollectionType()) {
                builder.append(" = :").append(
                        lastFieldToken.getField().getFieldName()
                                .getSymbolName());
            }
        }
        return builder.toString().trim();
    }
    
    private String getJpaCountQuery(final List<Token> tokens,
            final String simpleTypeName, final JavaSymbolName finderName,
            final String plural, final String entityName) {
        String jpaQuery = this.getJpaQuery(tokens, simpleTypeName, finderName, plural, entityName);
        return jpaQuery.replaceFirst("SELECT o FROM ", "SELECT COUNT(o) FROM ");
    }

    private List<MethodMetadata> getLocatedMutators(
            final MemberDetails memberDetails) {
        final List<MethodMetadata> locatedMutators = new ArrayList<MethodMetadata>();
        for (final MethodMetadata method : memberDetails.getMethods()) {
            if (isMethodOfInterest(method)) {
                locatedMutators.add(method);
            }
        }
        return locatedMutators;
    }

    private List<JavaSymbolName> getParameterNames(final List<Token> tokens,
            final JavaSymbolName finderName, final String plural) {
        final List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

        for (int i = 0; i < tokens.size(); i++) {
            final Token token = tokens.get(i);
            if (token instanceof FieldToken) {
                final String fieldName = ((FieldToken) token).getField()
                        .getFieldName().getSymbolName();
                parameterNames.add(new JavaSymbolName(fieldName));
            }
            else {
                if ("Between".equals(token.getValue())) {
                    final Token field = tokens.get(i - 1);
                    if (field instanceof FieldToken) {
                        final JavaSymbolName fieldName = parameterNames
                                .get(parameterNames.size() - 1);
                        // Remove the last field token
                        parameterNames.remove(parameterNames.size() - 1);

                        // Replace by a min and a max value
                        parameterNames
                                .add(new JavaSymbolName(
                                        "min"
                                                + fieldName
                                                        .getSymbolNameCapitalisedFirstLetter()));
                        parameterNames
                                .add(new JavaSymbolName(
                                        "max"
                                                + fieldName
                                                        .getSymbolNameCapitalisedFirstLetter()));
                    }
                }
                else if ("IsNull".equals(token.getValue())
                        || "IsNotNull".equals(token.getValue())) {
                    final Token field = tokens.get(i - 1);
                    if (field instanceof FieldToken) {
                        parameterNames.remove(parameterNames.size() - 1);
                    }
                }
            }
        }

        return parameterNames;
    }

    private List<JavaType> getParameterTypes(final List<Token> tokens,
            final JavaSymbolName finderName, final String plural) {
        final List<JavaType> parameterTypes = new ArrayList<JavaType>();

        for (int i = 0; i < tokens.size(); i++) {
            final Token token = tokens.get(i);
            if (token instanceof FieldToken) {
                parameterTypes.add(((FieldToken) token).getField()
                        .getFieldType());
            }
            else {
                if ("Between".equals(token.getValue())) {
                    final Token field = tokens.get(i - 1);
                    if (field instanceof FieldToken) {
                        parameterTypes.add(parameterTypes.get(parameterTypes
                                .size() - 1));
                    }
                }
                else if ("IsNull".equals(token.getValue())
                        || "IsNotNull".equals(token.getValue())) {
                    final Token field = tokens.get(i - 1);
                    if (field instanceof FieldToken) {
                        parameterTypes.remove(parameterTypes.size() - 1);
                    }
                }
            }
        }
        return parameterTypes;
    }

    public QueryHolder getQueryHolder(final MemberDetails memberDetails,
            final JavaSymbolName finderName, final String plural,
            final String entityName) {
        Validate.notNull(memberDetails, "Member details required");
        Validate.notNull(finderName, "Finder name required");
        Validate.notBlank(plural, "Plural required");

        List<Token> tokens;
        try {
            tokens = tokenize(memberDetails, finderName, plural);
        }
        catch (final FinderFieldTokenMissingException e) {
            return null;
        }
        catch (final InvalidFinderException e) {
            return null;
        }

        final String simpleTypeName = getConcreteJavaType(memberDetails)
                .getSimpleTypeName();
        final String jpaQuery = getJpaQuery(tokens, simpleTypeName, finderName,
                plural, entityName);
        final List<JavaType> parameterTypes = getParameterTypes(tokens,
                finderName, plural);
        final List<JavaSymbolName> parameterNames = getParameterNames(tokens,
                finderName, plural);
        return new QueryHolder(jpaQuery, parameterTypes, parameterNames, tokens);
    }
    
    public QueryHolder getCountQueryHolder(final MemberDetails memberDetails,
            final JavaSymbolName finderName, final String plural,
            final String entityName) {
        Validate.notNull(memberDetails, "Member details required");
        Validate.notNull(finderName, "Finder name required");
        Validate.notBlank(plural, "Plural required");

        List<Token> tokens;
        try {
            tokens = tokenize(memberDetails, finderName, plural);
        }
        catch (final FinderFieldTokenMissingException e) {
            return null;
        }
        catch (final InvalidFinderException e) {
            return null;
        }

        final String simpleTypeName = getConcreteJavaType(memberDetails)
                .getSimpleTypeName();
        final String jpaQuery = getJpaCountQuery(tokens, simpleTypeName, finderName,
                plural, entityName);
        final List<JavaType> parameterTypes = getParameterTypes(tokens,
                finderName, plural);
        final List<JavaSymbolName> parameterNames = getParameterNames(tokens,
                finderName, plural);
        return new QueryHolder(jpaQuery, parameterTypes, parameterNames, tokens);
    }

    private boolean isMethodOfInterest(final MethodMetadata method) {
        return method.getMethodName().getSymbolName().startsWith("set")
                && method.getModifier() == Modifier.PUBLIC;
    }

    private boolean isNumberOrDate(final JavaType fieldType) {
        return fieldType.equals(JavaType.DOUBLE_OBJECT)
                || fieldType.equals(JavaType.FLOAT_OBJECT)
                || fieldType.equals(JavaType.INT_OBJECT)
                || fieldType.equals(LONG_OBJECT)
                || fieldType.equals(JavaType.SHORT_OBJECT)
                || fieldType.getFullyQualifiedTypeName().equals(
                        Date.class.getName())
                || fieldType.getFullyQualifiedTypeName().equals(
                        Calendar.class.getName());
    }

    private boolean isTransient(final FieldMetadata field) {
        return Modifier.isTransient(field.getModifier())
                || field.getCustomData().keySet()
                        .contains(CustomDataKeys.TRANSIENT_FIELD);
    }

    private Set<JavaSymbolName> populateFinders(
            final Set<JavaSymbolName> finders, final FieldMetadata field,
            final String prepend, final boolean isFirst, final String keyWord) {
        final Set<JavaSymbolName> tempFinders = new HashSet<JavaSymbolName>();

        if (isTransient(field)) {
            // No need to add transient fields
        }
        else if (isFirst) {
            final String finderName = prepend
                    + field.getFieldName()
                            .getSymbolNameCapitalisedFirstLetter() + keyWord;
            tempFinders.add(new JavaSymbolName(finderName));
        }
        else {
            for (final JavaSymbolName finder : finders) {
                final String finderName = finder.getSymbolName();
                if (!finderName.contains(field.getFieldName()
                        .getSymbolNameCapitalisedFirstLetter())) {
                    tempFinders.add(new JavaSymbolName(finderName
                            + prepend
                            + field.getFieldName()
                                    .getSymbolNameCapitalisedFirstLetter()
                            + keyWord));
                }
            }
        }

        return tempFinders;
    }

    private List<Token> tokenize(final MemberDetails memberDetails,
            final JavaSymbolName finderName, final String plural) {
        final String simpleTypeName = getConcreteJavaType(memberDetails)
                .getSimpleTypeName();
        String finder = finderName.getSymbolName();

        // Just in case it starts with findBy we can remove it here
        final String findBy = "find" + plural + "By";
        if (finder.startsWith(findBy)) {
            finder = finder.substring(findBy.length());
        }

        // If finder still contains the findBy sequence it is most likely a
        // wrong finder (ie someone pasted the finder string accidentally twice
        if (finder.contains(findBy)) {
            throw new InvalidFinderException("Dynamic finder definition for '"
                    + finderName.getSymbolName() + "' in " + simpleTypeName
                    + ".java is invalid");
        }

        final SortedSet<FieldToken> fieldTokens = new TreeSet<FieldToken>();
        for (final MethodMetadata method : getLocatedMutators(memberDetails)) {
            final FieldMetadata field = BeanInfoUtils.getFieldForPropertyName(
                    memberDetails, method.getParameterNames().get(0));

            // If we did find a field matching the first parameter name of the
            // mutator method we can add it to the finder ITD
            if (field != null) {
                fieldTokens.add(new FieldToken(field));
            }
        }

        final List<Token> tokens = new ArrayList<Token>();

        while (finder.length() > 0) {
            final Token token = getFirstToken(fieldTokens, finder,
                    finderName.getSymbolName(), simpleTypeName);
            if (token != null) {
                if (token instanceof FieldToken
                        || token instanceof ReservedToken) {
                    tokens.add(token);
                }
                finder = finder.substring(token.getValue().length());
            }
        }

        return tokens;
    }
}
