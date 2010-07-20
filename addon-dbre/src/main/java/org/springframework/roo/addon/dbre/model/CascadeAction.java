package org.springframework.roo.addon.dbre.model;

/**
 * Represents the different cascade actions for the <code>onDelete</code> and <code>onUpdate</code> properties of {@link ForeignKey}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public enum CascadeAction {
	CASCADE("cascade"), SET_NULL("setnull"), SET_DEFAULT("setdefault"), RESTRICT("restrict"), NONE("none");
	private String code;

	private CascadeAction(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static CascadeAction getCascadeAction(String code) {
		for (CascadeAction cascadeAction : CascadeAction.values()) {
			if (cascadeAction.getCode().equals(code)) {
				return cascadeAction;
			}
		}
		return NONE;
	}
}
