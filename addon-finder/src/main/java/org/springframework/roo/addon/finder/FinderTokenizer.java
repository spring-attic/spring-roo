package org.springframework.roo.addon.finder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.roo.addon.beaninfo.BeanInfoMetadata;
import org.springframework.roo.classpath.details.FieldMetadata;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;


/**
 * Tokenizer used for custom find methods. Splits the presented String up by capital letters.
 * 
 * @author Stefan Schmidt
 * @since 1.0
 * 
 */
public final class FinderTokenizer {

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
		Assert.notNull(beanInfoMetadata, "RooJpaMetadata required");
		//just in case it starts with findBy we can remove it here
		finder = finder.replace("find" + plural +"By", "");
		
		FinderTokenIterator tokenizer = new FinderTokenIterator(finder);
		
		List<Token> tokens = new ArrayList<Token>();
		String partToken = null;
		ReservedToken previousToken = null;
		while (tokenizer.hasNext()) {
			FieldMetadata field = null;
			String singleToken = tokenizer.next();
			
			String token = singleToken;

			if (partToken != null) {
				token = partToken.concat(token);
			}
			if (ReservedToken.getAllReservedTokens().contains(token)) {
				if(previousToken != null && ReservedToken.getAllReservedTokens().contains(previousToken.getToken().concat(token))) {
					tokens.remove(previousToken);
					previousToken = new ReservedToken(previousToken.getToken().concat(token));	
				} else {
					previousToken = new ReservedToken(token);
				}				
//				System.out.println("reserved token "+previousToken.getToken());
				tokens.add(previousToken);		
				partToken = null; //new
			} else if ((field = beanInfoMetadata.getFieldForPropertyName(new JavaSymbolName(StringUtils.uncapitalize(token)))) != null) {
				tokens.add(new FieldToken(field));
				partToken = null;
				previousToken = null;
//				System.out.println("field token "+token);
			} else {
				partToken = token;
//				System.out.println("part token "+token);
			}
		}
		//test if there are leftovers, if so we cannot parse the presented finder signature completely
		if (partToken != null) {
			throw new IllegalArgumentException("Unable to match all fields of " + beanInfoMetadata.getJavaBean().getSimpleTypeName() + " for " + finder + " (remaining '" + partToken + "')");
		}		
		return tokens;
	}
	
	class FinderTokenIterator implements Iterator<String> {

		private String finder;

		private Matcher matcher;

		private String match;

		private int lastEnd = 0;

		FinderTokenIterator(String finder) {

			Assert.hasText(finder, "Non-empty finder String required");
			this.finder = finder;

			// Compile pattern and prepare input
			Pattern pattern = Pattern.compile("[A-Z][a-z]+");
			matcher = pattern.matcher(finder);
		}

		// Returns true if there are more tokens or delimiters.
		public boolean hasNext() {
			if (matcher == null) {
				return false;
			}
			if (match != null) {
				return true;
			}
			if (matcher.find()) {
				match = finder.subSequence(lastEnd, matcher.end()).toString();
				lastEnd = matcher.end();
			} else if (lastEnd < finder.length()) {
				lastEnd = finder.length();
				matcher = null;
			}
			return match != null;
		}

		public String next() {
			String result = null;

			if (match != null) {
				result = match;
				match = null;
			}
			return result;
		}

		public boolean isNextToken() {
			return match != null;
		}

		// Not supported.
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
