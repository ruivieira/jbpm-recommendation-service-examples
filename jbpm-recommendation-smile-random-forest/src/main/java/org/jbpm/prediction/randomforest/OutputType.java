package org.jbpm.prediction.randomforest;

public class OutputType {

    private final String name;
    private final AttributeType type;

    private OutputType(String name, AttributeType type) {
        this.name = name;
        this.type = type;
    }

    public static OutputType create(String name, AttributeType type) {
        return new OutputType(name, type);
    }

    public String getName() {
        return name;
    }

    public AttributeType getType() {
        return type;
    }
}
