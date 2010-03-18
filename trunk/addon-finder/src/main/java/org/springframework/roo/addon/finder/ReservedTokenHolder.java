package org.springframework.roo.addon.finder;

import java.util.SortedSet;
import java.util.TreeSet;

public abstract class ReservedTokenHolder {
	
	private static SortedSet<ReservedToken> numericTokens;
	
	private static SortedSet<ReservedToken> booleanTokens;
	
	private static SortedSet<ReservedToken> stringTokens;
	
	private static SortedSet<ReservedToken> reservedTokens;

	public static SortedSet<ReservedToken> getNumericTokens(){
		if(numericTokens == null) {
			numericTokens =  new TreeSet<ReservedToken>();		
			numericTokens.add(new ReservedToken("Between"));
			numericTokens.add(new ReservedToken("LessThan"));
			numericTokens.add(new ReservedToken("LessThanEquals"));
			numericTokens.add(new ReservedToken("GreaterThan"));
			numericTokens.add(new ReservedToken("GreaterThanEquals"));
			numericTokens.add(new ReservedToken("IsNotNull"));
			numericTokens.add(new ReservedToken("IsNull"));
			numericTokens.add(new ReservedToken("NotEquals"));
			numericTokens.add(new ReservedToken("Equals"));
		}
		return numericTokens;
	}
	
	public static SortedSet<ReservedToken> getBooleanTokens() {
		if(booleanTokens == null) {
			booleanTokens = new TreeSet<ReservedToken>();
			booleanTokens.add(new ReservedToken("Not"));
		}
		return booleanTokens;
	}
	
	public static SortedSet<ReservedToken> getStringTokens() {
		if(stringTokens == null) {
			stringTokens = new TreeSet<ReservedToken>();
			//what would be the difference between findByName and findByNameEqual?
			stringTokens.add(new ReservedToken("Equals"));		
			//what would be the difference between findByNameNot and findByNameNotEqual?
			stringTokens.add(new ReservedToken("NotEquals"));		
			stringTokens.add(new ReservedToken("Like"));
	//		stringTokens.add("Ilike"); // ignore case like
			stringTokens.add(new ReservedToken("IsNotNull"));
			stringTokens.add(new ReservedToken("IsNull"));
		}
		return stringTokens;
	}
	
	public static SortedSet<ReservedToken> getAllTokens() {		
		if(reservedTokens == null) {
			reservedTokens = new TreeSet<ReservedToken>();
			reservedTokens.add(new ReservedToken("Or"));
			reservedTokens.add(new ReservedToken("And"));		
			reservedTokens.add(new ReservedToken("Not"));
			reservedTokens.add(new ReservedToken("Like"));
			reservedTokens.add(new ReservedToken("Ilike")); // ignore case like
			reservedTokens.add(new ReservedToken("LessThanEquals"));					
			reservedTokens.add(new ReservedToken("IsNull"));
			reservedTokens.add(new ReservedToken("Equals"));
			reservedTokens.add(new ReservedToken("Between"));
			reservedTokens.add(new ReservedToken("LessThan"));	
			reservedTokens.add(new ReservedToken("NotEquals"));
			reservedTokens.add(new ReservedToken("IsNotNull"));
			reservedTokens.add(new ReservedToken("GreaterThan"));			
			reservedTokens.add(new ReservedToken("GreaterThanEquals"));
			reservedTokens.add(new ReservedToken("Member"));
		}
		return reservedTokens;
	}
}
