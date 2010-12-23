package org.springframework.roo.addon.roobot.client.model;

import org.springframework.roo.support.style.ToStringCreator;

/**
 * Star ratings for the "addon feedback bundle" command.
 * 
 * @author Ben Alex
 * @author Stefan Schmidt
 * @since 1.1.1
 */
public enum Rating {
	VERY_BAD(1),
	BAD(2),
	NEUTRAL(3),
	GOOD(4),
	VERY_GOOD(5);

	private Integer key;

	private Rating(Integer key) {
		this.key = key;
	}
	
	public static Rating fromInt(Integer rating) {
		switch (rating) {
		case 1: return VERY_BAD;
		case 2: return BAD;
		case 3: return NEUTRAL;
		case 4: return GOOD;
		case 5: return VERY_GOOD;
		default: return NEUTRAL;
		}
	}

	public Integer getKey() {
		return key;
	}
	
	public String toString() {
		ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("name", name());
		tsc.append("key", key);
		return tsc.toString();
	}
}
