package org.springframework.roo.addon.finder;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.customdata.PersistenceCustomDataKeys;
import org.springframework.roo.classpath.details.BeanInfoUtils;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MemberHoldingTypeDetails;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.scanner.MemberDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

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

	public List<JavaSymbolName> getFinders(MemberDetails memberDetails, String plural, int depth, Set<JavaSymbolName> exclusions) {
		Assert.notNull(memberDetails, "Member details required");
		Assert.hasText(plural, "Plural required");
		Assert.notNull(depth, "The depth of combinations used for finder signatures combinations required");
		Assert.notNull(exclusions, "Exclusions required");

		SortedSet<JavaSymbolName> finders = new TreeSet<JavaSymbolName>();

		List<FieldMetadata> fields = MemberFindingUtils.getFields(memberDetails);
		for (int i = 0; i < depth; i++) {
			SortedSet<JavaSymbolName> tempFinders = new TreeSet<JavaSymbolName>();
			for (FieldMetadata field : fields) {
				// Ignoring java.util.Map field types (see ROO-194)
				if (field == null || field.getFieldType().equals(new JavaType(Map.class.getName()))) {
					continue;
				}
				if (exclusions.contains(field.getFieldName())) {
					continue;
				}
				if (i == 0) {
					tempFinders.addAll(createFinders(field, finders, "find" + plural + "By", true));
				} else {
					tempFinders.addAll(createFinders(field, finders, "And", false));
					tempFinders.addAll(createFinders(field, finders, "Or", false));
				}
			}
			finders.addAll(tempFinders);
		}

		return Collections.unmodifiableList(new ArrayList<JavaSymbolName>(finders));
	}

	public QueryHolder getQueryHolder(MemberDetails memberDetails, JavaSymbolName finderName, String plural, String entityName) {
		Assert.notNull(memberDetails, "Member details required");
		Assert.notNull(finderName, "Finder name required");
		Assert.hasText(plural, "Plural required");

		List<Token> tokens;
		try {
			tokens = tokenize(memberDetails, finderName, plural);
		} catch (FinderFieldTokenMissingException e) {
			return null;
		} catch (InvalidFinderException e) {
			return null;
		}

		String simpleTypeName = getConcreteJavaType(memberDetails).getSimpleTypeName();
		String jpaQuery = getJpaQuery(tokens, simpleTypeName, finderName, plural, entityName);
		List<JavaType> parameterTypes = getParameterTypes(tokens, finderName, plural);
		List<JavaSymbolName> parameterNames = getParameterNames(tokens, finderName, plural);
		return new QueryHolder(jpaQuery, parameterTypes, parameterNames, tokens);
	}

	private String getJpaQuery(List<Token> tokens, String simpleTypeName, JavaSymbolName finderName, String plural, String entityName) {
		String typeName = StringUtils.hasText(entityName) ? entityName : simpleTypeName;
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT o FROM ").append(typeName);
		builder.append(" AS o WHERE ");
		
		FieldToken lastFieldToken = null;
		boolean isNewField = true;
		boolean isFieldApplied = false;

		for (Token token : tokens) {
			if (token instanceof ReservedToken) {
				String reservedToken = token.getValue();
				if (lastFieldToken == null) continue;
				String fieldName = lastFieldToken.getField().getFieldName().getSymbolName();
				boolean setField = true;

				if (!lastFieldToken.getField().getFieldType().isCommonCollectionType()) {
					if (isNewField) {
						if (reservedToken.equalsIgnoreCase("Like")) {
							builder.append("LOWER(").append("o.").append(fieldName).append(')');
						} else {
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
					} else if (reservedToken.equalsIgnoreCase("Or")) {
						if (!isFieldApplied) {
							builder.append(" = :").append(fieldName);
							isFieldApplied = true;
						}
						builder.append(" OR ");
						setField = false;
					} else if (reservedToken.equalsIgnoreCase("Between")) {
						builder.append(" BETWEEN ").append(":min").append(lastFieldToken.getField().getFieldName().getSymbolNameCapitalisedFirstLetter()).append(" AND ").append(":max").append(lastFieldToken.getField().getFieldName().getSymbolNameCapitalisedFirstLetter()).append(" ");
						setField = false;
						isFieldApplied = true;
					} else if (reservedToken.equalsIgnoreCase("Like")) {
						builder.append(" LIKE ");
						setField = true;
					} else if (reservedToken.equalsIgnoreCase("IsNotNull")) {
						builder.append(" IS NOT NULL ");
						setField = false;
						isFieldApplied = true;
					} else if (reservedToken.equalsIgnoreCase("IsNull")) {
						builder.append(" IS NULL ");
						setField = false;
						isFieldApplied = true;
					} else if (reservedToken.equalsIgnoreCase("Not")) {
						builder.append(" IS NOT ");
					} else if (reservedToken.equalsIgnoreCase("NotEquals")) {
						builder.append(" != ");
					} else if (reservedToken.equalsIgnoreCase("LessThan")) {
						builder.append(" < ");
					} else if (reservedToken.equalsIgnoreCase("LessThanEquals")) {
						builder.append(" <= ");
					} else if (reservedToken.equalsIgnoreCase("GreaterThan")) {
						builder.append(" > ");
					} else if (reservedToken.equalsIgnoreCase("GreaterThanEquals")) {
						builder.append(" >= ");
					} else if (reservedToken.equalsIgnoreCase("Equals")) {
						builder.append(" = ");
					}
					if (setField) {
						if (builder.toString().endsWith("LIKE ")) {
							builder.append("LOWER(:").append(fieldName).append(") ");
						} else {
							builder.append(':').append(fieldName).append(' ');
						}
						isFieldApplied = true;
					}
				}
			} else {
				lastFieldToken = (FieldToken) token;
				isNewField = true;
			}
		}
		if (isNewField) {
			if (lastFieldToken != null && !lastFieldToken.getField().getFieldType().isCommonCollectionType()) {
				builder.append("o.").append(lastFieldToken.getField().getFieldName().getSymbolName());
			}
			isFieldApplied = false;
		}
		if (!isFieldApplied) {
			if (lastFieldToken != null && !lastFieldToken.getField().getFieldType().isCommonCollectionType()) {
				builder.append(" = :").append(lastFieldToken.getField().getFieldName().getSymbolName());
			}
		}
		return builder.toString().trim();
	}

	private List<JavaSymbolName> getParameterNames(List<Token> tokens, JavaSymbolName finderName, String plural) {
		List<JavaSymbolName> parameterNames = new ArrayList<JavaSymbolName>();

		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			if (token instanceof FieldToken) {
				String fieldName = (((FieldToken) token).getField().getFieldName().getSymbolName());
				parameterNames.add(new JavaSymbolName(fieldName));
			} else {
				if ("Between".equals(token.getValue())) {
					Token field = tokens.get(i - 1);
					if (field instanceof FieldToken) {
						JavaSymbolName fieldName = parameterNames.get(parameterNames.size() - 1);
						// Remove the last field token
						parameterNames.remove(parameterNames.size() - 1);
						
						// Replace by a min and a max value
						parameterNames.add(new JavaSymbolName("min" + fieldName.getSymbolNameCapitalisedFirstLetter()));
						parameterNames.add(new JavaSymbolName("max" + fieldName.getSymbolNameCapitalisedFirstLetter()));
					}
				} else if ("IsNull".equals(token.getValue()) || "IsNotNull".equals(token.getValue())) {
					Token field = tokens.get(i - 1);
					if (field instanceof FieldToken) {
						parameterNames.remove(parameterNames.size() - 1);
					}
				}
			}
		}

		return parameterNames;
	}

	private List<JavaType> getParameterTypes(List<Token> tokens, JavaSymbolName finderName, String plural) {
		List<JavaType> parameterTypes = new ArrayList<JavaType>();

		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			if (token instanceof FieldToken) {
				parameterTypes.add(((FieldToken) token).getField().getFieldType());
			} else {
				if ("Between".equals(token.getValue())) {
					Token field = tokens.get(i - 1);
					if (field instanceof FieldToken) {
						parameterTypes.add(parameterTypes.get(parameterTypes.size() - 1));
					}
				} else if ("IsNull".equals(token.getValue()) || "IsNotNull".equals(token.getValue())) {
					Token field = tokens.get(i - 1);
					if (field instanceof FieldToken) {
						parameterTypes.remove(parameterTypes.size() - 1);
					}
				}
			}
		}
		return parameterTypes;
	}

	private List<MethodMetadata> getLocatedMutators(MemberDetails memberDetails) {
		List<MethodMetadata> locatedMutators = new ArrayList<MethodMetadata>();
		for (MethodMetadata methodMetadata : MemberFindingUtils.getMethods(memberDetails)) {
			if (isMethodOfInterest(methodMetadata)) {
				locatedMutators.add(methodMetadata);
			}
		}
		return locatedMutators;
	}

	private boolean isMethodOfInterest(MethodMetadata method) {
		return method.getMethodName().getSymbolName().startsWith("set") && method.getModifier() == Modifier.PUBLIC;
	}

	private List<Token> tokenize(MemberDetails memberDetails, JavaSymbolName finderName, String plural) {
		String simpleTypeName = getConcreteJavaType(memberDetails).getSimpleTypeName();
		String finder = finderName.getSymbolName();
		
		// Just in case it starts with findBy we can remove it here
		String findBy = "find" + plural + "By";
		if (finder.startsWith(findBy)) {
			finder = finder.substring(findBy.length());
		}
		
		// if finder still contains the findBy sequence it is most likely a wrong finder (ie someone pasted the finder string accidentally twice
		if (finder.contains(findBy)) {
			throw new InvalidFinderException("Dynamic finder definition for '" + finderName.getSymbolName() + "' in " + simpleTypeName + ".java is invalid");
		}
		
		SortedSet<FieldToken> fieldTokens = new TreeSet<FieldToken>();
		for (MethodMetadata methodMetadata : getLocatedMutators(memberDetails)) {
			FieldMetadata fieldMetadata = BeanInfoUtils.getFieldForPropertyName(memberDetails, methodMetadata.getParameterNames().get(0));
			
			// If we did find a field matching the first parameter name of the mutator method we can add it to the finder ITD
			if (fieldMetadata != null) {
				fieldTokens.add(new FieldToken(fieldMetadata));
			}
		}
		
		List<Token> tokens = new ArrayList<Token>();
		
		while (finder.length() > 0) {
			Token token = getFirstToken(fieldTokens, finder, finderName.getSymbolName(), simpleTypeName);
			if (token != null) {
				if (token instanceof FieldToken || token instanceof ReservedToken) {
					tokens.add(token);
				}
				finder = finder.substring(token.getValue().length());
			}
		}
		
		return tokens;
	}

	private Token getFirstToken(SortedSet<FieldToken> fieldTokens, String finder, String originalFinder, String simpleTypeName) {
		for (FieldToken fieldToken : fieldTokens) {
			if (finder.startsWith(fieldToken.getValue())) {
				return fieldToken;
			}
		}
		for (ReservedToken reservedToken : ReservedTokenHolder.ALL_TOKENS) {
			if (finder.startsWith(reservedToken.getValue())) {
				return reservedToken;
			}
		}
		if (finder.length() > 0) {
			// TODO: Make this a FinderFieldTokenMissingException instead, to make it easier to detect this
			throw new FinderFieldTokenMissingException("Dynamic finder is unable to match '" + finder + "' token of '" + originalFinder + "' finder definition in " + simpleTypeName + ".java");
		}

		return null; // Finder does not start with reserved or field token
	}

	private Set<JavaSymbolName> createFinders(FieldMetadata field, Set<JavaSymbolName> finders, String prepend, boolean isFirst) {
		Set<JavaSymbolName> tempFinders = new HashSet<JavaSymbolName>();

		if (isNumberOrDate(field.getFieldType())) {
			for (ReservedToken keyWord : ReservedTokenHolder.NUMERIC_TOKENS) {
				tempFinders.addAll(populateFinders(finders, field, prepend, isFirst, keyWord.getValue()));
			}
		} else if (field.getFieldType().equals(JavaType.STRING_OBJECT)) {
			for (ReservedToken keyWord : ReservedTokenHolder.STRING_TOKENS) {
				tempFinders.addAll(populateFinders(finders, field, prepend, isFirst, keyWord.getValue()));
			}
		} else if (field.getFieldType().equals(JavaType.BOOLEAN_OBJECT) || field.getFieldType().equals(JavaType.BOOLEAN_PRIMITIVE)) {
			for (ReservedToken keyWord : ReservedTokenHolder.BOOLEAN_TOKENS) {
				tempFinders.addAll(populateFinders(finders, field, prepend, isFirst, keyWord.getValue()));
			}
		} else {
			tempFinders.addAll(populateFinders(finders, field, prepend, isFirst, ""));
		}

		return tempFinders;
	}

	private Set<JavaSymbolName> populateFinders(Set<JavaSymbolName> finders, FieldMetadata field, String prepend, boolean isFirst, String keyWord) {
		Set<JavaSymbolName> tempFinders = new HashSet<JavaSymbolName>();
	
		if (isTransient(field)) {
			// No need to add transient fields
		} else if (isFirst) {
			String finderName = prepend + field.getFieldName().getSymbolNameCapitalisedFirstLetter() + keyWord;
			tempFinders.add(new JavaSymbolName(finderName));
		} else {
			for (JavaSymbolName finder : finders) {
				String finderName = finder.getSymbolName();
				if (!finderName.contains(field.getFieldName().getSymbolNameCapitalisedFirstLetter())) {
					tempFinders.add(new JavaSymbolName(finderName + prepend + field.getFieldName().getSymbolNameCapitalisedFirstLetter() + keyWord));
				}
			}
		}

		return tempFinders;
	}
	
	private boolean isTransient(FieldMetadata field) {
		return Modifier.isTransient(field.getModifier()) || field.getCustomData().keySet().contains(PersistenceCustomDataKeys.TRANSIENT_FIELD);
	}

	private boolean isNumberOrDate(JavaType fieldType) {
		return fieldType.equals(JavaType.DOUBLE_OBJECT) ||
				fieldType.equals(JavaType.FLOAT_OBJECT) ||
				fieldType.equals(JavaType.INT_OBJECT) ||
				fieldType.equals(JavaType.LONG_OBJECT) ||
				fieldType.equals(JavaType.SHORT_OBJECT) ||
				fieldType.getFullyQualifiedTypeName().equals(Date.class.getName()) ||
				fieldType.getFullyQualifiedTypeName().equals(Calendar.class.getName());
	}
	
	/**
	 * Returns the {@link JavaType} from the specified {@link MemberDetails} object;
	 * 
	 * <p>
	 * If the found type is abstract the next {@link MemberHoldingTypeDetails} is searched.
	 * 
	 * @param memberDetails the {@link MemberDetails} to search (required)
	 * @return the first non-abstract JavaType, or null if not found
	 */
	private JavaType getConcreteJavaType(MemberDetails memberDetails) {
		Assert.notNull(memberDetails, "Member details required");
		JavaType javaType = null;
		for (MemberHoldingTypeDetails memberHoldingTypeDetails : memberDetails.getDetails()) {
			if (Modifier.isAbstract(memberHoldingTypeDetails.getModifier())) {
				continue;
			}
			javaType = memberHoldingTypeDetails.getName();
		}
		return javaType;
	}
}
