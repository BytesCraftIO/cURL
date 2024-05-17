package io.bytescraft.model;

/**
 * @author javaquery
 * @since 0.0.1
 */
public class QueryParam {
    private String name;
    private String defaultValue = "";
    boolean required;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
