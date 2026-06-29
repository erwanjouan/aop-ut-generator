package com.theatomicity.aop.ut.generator.model;

public class InterceptedParam {
    private String name;
    private Class<?> type;
    private Object value;

    public InterceptedParam(final String name, final Class<?> type, final Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    public String getName() { return name; }
    public void setName(final String name) { this.name = name; }
    public Class<?> getType() { return type; }
    public void setType(final Class<?> type) { this.type = type; }
    public Object getValue() { return value; }
    public void setValue(final Object value) { this.value = value; }
}
