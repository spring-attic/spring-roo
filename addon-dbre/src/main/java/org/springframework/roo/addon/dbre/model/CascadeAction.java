package org.springframework.roo.addon.dbre.model;

/**
 * Represents the different cascade actions for the <code>onDelete</code> and
 * <code>onUpdate</code> properties of {@link ForeignKey}.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public enum CascadeAction {
    CASCADE("cascade"), NONE("none"), RESTRICT("restrict"), SET_DEFAULT(
            "setdefault"), SET_NULL("setnull");
    public static CascadeAction getCascadeAction(final String code) {
        for (final CascadeAction cascadeAction : CascadeAction.values()) {
            if (cascadeAction.getCode().equals(code)) {
                return cascadeAction;
            }
        }
        return NONE;
    }

    private String code;

    private CascadeAction(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
