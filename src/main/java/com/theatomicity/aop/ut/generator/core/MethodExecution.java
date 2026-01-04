package com.theatomicity.aop.ut.generator.core;

import lombok.Data;
import lombok.ToString;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class MethodExecution {
    private List<InterceptedParam> inputParams;
    private String name;
    private String className;
    private String simpleClassName;
    private InterceptedParam result;
    private int hashCode;
    private long startTime;
    private long endTime;

    public static MethodExecution from(final ProceedingJoinPoint jointPoint) {
        final MethodSignature signature = (MethodSignature) jointPoint.getSignature();
        final Method method = signature.getMethod();
        final String clazzName = method.getDeclaringClass().getName();
        final String simpleClazzName = method.getDeclaringClass().getSimpleName();
        final String methodName = method.getName();
        final String[] parameterNames = ((MethodSignature) jointPoint.getSignature()).getParameterNames();
        final Class<?>[] parameterTypes = ((MethodSignature) jointPoint.getSignature()).getParameterTypes();
        final List<InterceptedParam> inputParams = new ArrayList<>();
        final Object[] args = jointPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            inputParams.add(new InterceptedParam(parameterNames[i], parameterTypes[i], args[i]));
        }
        final MethodExecution methodExecution = new MethodExecution();
        methodExecution.setName(methodName);
        methodExecution.setClassName(clazzName);
        methodExecution.setSimpleClassName(simpleClazzName);
        methodExecution.setInputParams(inputParams);
        methodExecution.setStartTime(System.nanoTime());
        methodExecution.setHashCode(jointPoint.hashCode());
        return methodExecution;
    }
}
