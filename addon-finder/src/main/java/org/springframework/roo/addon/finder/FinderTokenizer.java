package org.springframework.roo.addon.finder;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.support.util.Assert;


/**
 * Tokenizer used for custom find methods. Splits the presented String up by capital letters.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 * 
 */
public final class FinderTokenizer {
	
	private SortedSet<FieldToken> fieldTokens;
	
	private BeanInfoMetadata beanInfoMetadata;
	
	private String completeFinder;

	/**
	 * Use this method to tokenize the finder method signature based on capital letters. This method asserts 
	 * that the signature provided is valid (i.e. the fields in the String match fields in the {@link RooEntity} 
	 * or are of type {@link ReservedToken}. 
	 * 
	 * @param finder The string to parse. (required)
	 * @param beanInfoMetadata The metadata for the {@link RooEntity} for which the finder String applies. (required)
	 * @return The list of tokens (could be of type {@link FieldToken} or {@link ReservedToken} as a result of the tokenizing.
	 */
	public List<Token> tokenize(String finder, String plural, BeanInfoMetadata beanInfoMetadata) {
		Assert.notNull(finder, "finder method signature required");
		Assert.hasText(finder, "Empty finder Strings not allowd");
		Assert.hasText(plural, "Plural required");
		Assert.notNull(beanInfoMetadata, "BeanInfoMetadata required");
		
		this.beanInfoMetadata = beanInfoMetadata;		
		this.completeFinder = finder;
		
		//just in case it starts with findBy we can remove it here
		finder = finder.replace("find" + plural +"By", "");

		fieldTokens = new TreeSet<FieldToken>();
		for(MethodMetadata metadata : beanInfoMetadata.getPublicMutators()) {
			fieldTokens.add(new FieldToken(beanInfoMetadata.getFieldForPropertyName(metadata.getParameterNames().get(0))));
		}
		
		List<Token> tokens = new ArrayList<Token>();
		
		while(finder.length() > 0) {
			Token token = getFirstToken(finder);
			if(token != null) {
				if(token instanceof FieldToken) {
					tokens.add((FieldToken)token);					
				}
				if(token instanceof ReservedToken) {
					tokens.add((ReservedToken)token);
				}
				finder = finder.substring(token.getValue().length());
			}
		}		
		
		return tokens;
	}
	
	private Token getFirstToken(String finder) {
		for(FieldToken fieldToken: fieldTokens) {
			if (finder.startsWith(fieldToken.getValue())) {
				return fieldToken;								
			}
		}	
		for(ReservedToken reservedToken: ReservedTokenHolder.getAllTokens()) {
			if (finder.startsWith(reservedToken.getValue())) {
				return reservedToken;								
			}
		}	
		if (finder.length() > 0) {
			throw new IllegalStateException("Dynamic finder is unable to match '" + finder + "' token of '" + completeFinder + "' finder definition in " + beanInfoMetadata.getJavaBean().getSimpleTypeName() + ".java");
		}
		return null; //finder does not start with reserved or field token
	}	
}
