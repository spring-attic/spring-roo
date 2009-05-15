package org.springframework.roo.addon.finder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.support.util.Assert;

/**
 * Default implementation of {@link DynamicFinderServices}.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 */
public class DynamicFinderServicesImpl implements DynamicFinderServices {
	
	public List<JavaSymbolName> getFindersFor(BeanInfoMetadata beanInfoMetadata, String plural, int maxDepth) {
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.hasText(plural, "Plural required");
		Assert.notNull(maxDepth, "maxDepth required");

		SortedSet<JavaSymbolName> finders = new TreeSet<JavaSymbolName>();
		SortedSet<JavaSymbolName> tempFinders = new TreeSet<JavaSymbolName>();

		for (int i = 0; i < maxDepth; i++) {
			for (MethodMetadata accessor : beanInfoMetadata.getPublicAccessors()) {
				JavaSymbolName propertyName = beanInfoMetadata.getPropertyNameForJavaBeanMethod(accessor);
				FieldMetadata field = beanInfoMetadata.getFieldForPropertyName(propertyName);
				if (field == null) {
					continue;
				}
				if (!PhysicalTypeIdentifier.isValid(field.getDeclaredByMetadataId())) {
					// We are only interested in fields declared by physical types - not fields declared by ITDs
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
		
		return new ArrayList<JavaSymbolName>(finders);
	}

	public String getJpaQueryFor(JavaSymbolName finderName, String plural, BeanInfoMetadata beanInfoMetadata) {
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.hasText(plural, "Plural required");
		Assert.notNull(finderName, "JavaSymbolName required");

		String tablename = beanInfoMetadata.getJavaBean().getSimpleTypeName();
		
		StringBuilder builder = new StringBuilder();
		builder.append("FROM ").append(tablename);
		builder.append(" AS ").append(tablename.toLowerCase());
		builder.append(" WHERE ");

		FinderTokenizer tokenizer = new FinderTokenizer();

		List<Token> tokens = tokenizer.tokenize(finderName.getSymbolName(), plural, beanInfoMetadata);

		FieldToken lastFieldToken = null;
		boolean isNewField = true;
		boolean isFieldSet = false;
		for (Token token : tokens) {
			if (token instanceof ReservedToken) {
				String s = ((ReservedToken) token).getToken();
				String fieldName = lastFieldToken.getField().getFieldName().getSymbolName();
				boolean setField = true;

				if(isNewField){
					builder.append(tablename.toLowerCase()).append(".").append(fieldName);
					isNewField = false;
					isFieldSet = false;
				} 
				if (s.equalsIgnoreCase("And")) {
					if(!isFieldSet){
						builder.append(" = :").append(fieldName).append(" ");
						isFieldSet = true;
					}
					builder.append("AND ");
					setField = false;
				} else if (s.equalsIgnoreCase("Or")) {
					if(!isFieldSet){
						builder.append(" = :").append(fieldName).append(" ");
						isFieldSet = true;
					}					
					builder.append("OR ");
					setField = false;
				} else if (s.equalsIgnoreCase("Between")) {
					builder.append(" BETWEEN ").append(":min").append(lastFieldToken.getField().getFieldName().getSymbolNameCapitalisedFirstLetter()).append(" AND ").append(":max").append(lastFieldToken.getField().getFieldName().getSymbolNameCapitalisedFirstLetter()).append(" ");
					setField = false;
					isFieldSet = true;
				} else if (s.equalsIgnoreCase("Like")) {
					builder.append(" LIKE ");					
					setField = true;
				} else if (s.equalsIgnoreCase("IsNotNull")) {
					builder.append(" is not NULL ");
					setField = false;
					isFieldSet = true;
				} else if (s.equalsIgnoreCase("IsNull")) {
					builder.append(" is NULL ");
					setField = false;
					isFieldSet = true;
				} else if (s.equalsIgnoreCase("Not")) {
					builder.append(" NOT ");
				} else if (s.equalsIgnoreCase("NotEquals")) {
					builder.append(" != ");
				} else if (s.equalsIgnoreCase("LessThan")) {
					builder.append(" < ");
				} else if (s.equalsIgnoreCase("LessThanEquals")) {
					builder.append(" <= ");
				} else if (s.equalsIgnoreCase("GreaterThan")) {
					builder.append(" > ");
				} else if (s.equalsIgnoreCase("GreaterThanEquals")) {
					builder.append(" >= ");
				} else if (s.equalsIgnoreCase("Equals")) {
					builder.append(" = ");
				} 
				if(setField) {
					builder.append(":").append(fieldName).append(" ");
					isFieldSet = true;
				}			
			} else {
				lastFieldToken = (FieldToken) token;
				isNewField = true;
			}
		}
		if(isNewField){
			builder.append(tablename.toLowerCase()).append(".").append(lastFieldToken.getField().getFieldName().getSymbolName());
			isFieldSet = false;
		}
		if(!isFieldSet){
			builder.append(" = :").append(lastFieldToken.getField().getFieldName().getSymbolName());
		}
		return builder.toString().trim();
	}

	public List<JavaSymbolName> getParameterNames(JavaSymbolName finderName, String plural, BeanInfoMetadata beanInfoMetadata) {
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.hasText(plural, "Plural required");
		Assert.notNull(finderName, "JavaSymbolName required");

		List<JavaSymbolName> names = new ArrayList<JavaSymbolName>();

		FinderTokenizer tokenizer = new FinderTokenizer();
		List<Token> tokens = tokenizer.tokenize(finderName.getSymbolName(), plural, beanInfoMetadata);

		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			if (token instanceof FieldToken) {
				String fieldName = (((FieldToken) token).getField().getFieldName().getSymbolName());
				names.add(new JavaSymbolName(fieldName));
			} else {
				if("Between".equals(((ReservedToken)token).getToken())) {
					Token field = tokens.get(i-1);
					if(field instanceof FieldToken) {
						JavaSymbolName fieldName = names.get(names.size() - 1);
						//remove the last field token
						names.remove(names.size()-1);
						//and replace by a min and a max value
						names.add(new JavaSymbolName("min" + fieldName.getSymbolNameCapitalisedFirstLetter()));
						names.add(new JavaSymbolName("max" + fieldName.getSymbolNameCapitalisedFirstLetter()));
					}
				} else if("IsNull".equals(((ReservedToken)token).getToken()) || "IsNotNull".equals(((ReservedToken)token).getToken())) {
					Token field = tokens.get(i-1);
					if(field instanceof FieldToken) {
						names.remove(names.size()-1);
					}
				}
			}
		}

		return names;
	}

	public List<JavaType> getParameterTypes(JavaSymbolName finderName, String plural, BeanInfoMetadata beanInfoMetadata) {
		Assert.notNull(beanInfoMetadata, "Bean info metadata required");
		Assert.hasText(plural, "Plural required");
		Assert.notNull(finderName, "JavaSymbolName required");

		List<JavaType> types = new ArrayList<JavaType>();

		FinderTokenizer tokenizer = new FinderTokenizer();
		List<Token> tokens = tokenizer.tokenize(finderName.getSymbolName(), plural, beanInfoMetadata);

		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			if (token instanceof FieldToken) {
				types.add(((FieldToken) token).getField().getFieldType());
			} else {
				if("Between".equals(((ReservedToken)token).getToken())) {
					Token field = tokens.get(i-1);
					if(field instanceof FieldToken) {
						types.add(types.get(types.size() - 1));						
					}
				} else if("IsNull".equals(((ReservedToken)token).getToken()) || "IsNotNull".equals(((ReservedToken)token).getToken())) {
					Token field = tokens.get(i-1);
					if(field instanceof FieldToken) {
						types.remove(types.size()-1);
					}
				}
			}
		}
		return types;
	}

	private Set<JavaSymbolName> createFinders(FieldMetadata field, Set<JavaSymbolName> finders, String prepend, boolean isFirst) {
		Set<JavaSymbolName> tempFinders = new HashSet<JavaSymbolName>();

		if (isNumberOrDate(field.getFieldType().getFullyQualifiedTypeName())) {
			for (String keyWord : ReservedToken.getNumericReservedTokens()) {
				tempFinders.addAll(populateFinders(finders, field, prepend, isFirst, keyWord));
			}
		} else if (field.getFieldType().getFullyQualifiedTypeName().equals(String.class.getName())) {
			for (String keyWord : ReservedToken.getStringReservedTokens()) {
				tempFinders.addAll(populateFinders(finders, field, prepend, isFirst, keyWord));
			}
		} else if (field.getFieldType().getFullyQualifiedTypeName().equals(Boolean.class.getName()) || field.getFieldType().getFullyQualifiedTypeName().equals(boolean.class.getName())) {
			for (String keyWord : ReservedToken.getBooleanReservedTokens()) {
				tempFinders.addAll(populateFinders(finders, field, prepend, isFirst, keyWord));
			}
		}
		return tempFinders;
	}

	private Set<JavaSymbolName> populateFinders(Set<JavaSymbolName> finders, FieldMetadata field, String prepend, boolean isFirst, String keyWord) {
		Set<JavaSymbolName> tempFinders = new HashSet<JavaSymbolName>();

		if (isFirst) {
			tempFinders.add(new JavaSymbolName(prepend + field.getFieldName().getSymbolNameCapitalisedFirstLetter() + keyWord));
		} else {
			for (JavaSymbolName finder : finders) {
				if (!finder.getSymbolName().contains(field.getFieldName().getSymbolNameCapitalisedFirstLetter())) {
					tempFinders.add(new JavaSymbolName(finder.getSymbolName() + prepend + field.getFieldName().getSymbolNameCapitalisedFirstLetter() + keyWord));
				}
			}
		}
		return tempFinders;
	}

	private boolean isNumberOrDate(String fullyQualifiedTypeName) {
		if (fullyQualifiedTypeName.equals(Double.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(double.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(Float.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(float.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(Integer.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(int.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(Long.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(long.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(Short.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(short.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(Date.class.getName())) {
			return true;
		}
		if (fullyQualifiedTypeName.equals(Calendar.class.getName())) {
			return true;
		}
		return false;
	}
}
