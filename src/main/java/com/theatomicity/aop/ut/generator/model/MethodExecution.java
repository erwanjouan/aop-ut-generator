package com.theatomicity.aop.ut.generator.model;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MethodExecution {

    private static final Logger log = LoggerFactory.getLogger(MethodExecution.class);

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
        methodExecution.setStartTime(System.currentTimeMillis());
        methodExecution.setHashCode(jointPoint.hashCode());
        return methodExecution;
    }

    public static MethodExecution from(final MethodInvocation invocation) {
        final Method method = invocation.getMethod();
        final String clazzName = method.getDeclaringClass().getName();
        final String simpleClazzName = method.getDeclaringClass().getSimpleName();
        final String methodName = method.getName();
        final Parameter[] parameters = method.getParameters();
        final Object[] args = invocation.getArguments();
        final List<InterceptedParam> inputParams = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            inputParams.add(new InterceptedParam(parameters[i].getName(), parameters[i].getType(), args[i]));
        }
        final MethodExecution methodExecution = new MethodExecution();
        methodExecution.setName(methodName);
        methodExecution.setClassName(clazzName);
        methodExecution.setSimpleClassName(simpleClazzName);
        methodExecution.setInputParams(inputParams);
        methodExecution.setStartTime(System.currentTimeMillis());
        methodExecution.setHashCode(invocation.hashCode());
        return methodExecution;
    }

    public void log() {
        log.debug("MethodExecution {}.{} {}->{} {}", this.getClassName(), this.getName(),
                this.displayTs(this.getStartTime()), this.displayTs(this.getEndTime()),
                this.getResult());
    }

    private String displayTs(final long timeStamp) {
        final Instant instant = Instant.ofEpochMilli(timeStamp);
        final LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public List<InterceptedParam> getInputParams() { return inputParams; }
    public void setInputParams(final List<InterceptedParam> inputParams) { this.inputParams = inputParams; }
    public String getName() { return name; }
    public void setName(final String name) { this.name = name; }
    public String getClassName() { return className; }
    public void setClassName(final String className) { this.className = className; }
    public String getSimpleClassName() { return simpleClassName; }
    public void setSimpleClassName(final String simpleClassName) { this.simpleClassName = simpleClassName; }
    public InterceptedParam getResult() { return result; }
    public void setResult(final InterceptedParam result) { this.result = result; }
    public int getHashCode() { return hashCode; }
    public void setHashCode(final int hashCode) { this.hashCode = hashCode; }
    public long getStartTime() { return startTime; }
    public void setStartTime(final long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(final long endTime) { this.endTime = endTime; }
}
