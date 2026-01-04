package com.theatomicity.aop.ut.generator.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InterceptedParam {
    private String name;
    private Class<?> type;
    private Object value;
}
