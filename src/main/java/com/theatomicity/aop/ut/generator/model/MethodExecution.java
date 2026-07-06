package com.theatomicity.aop.ut.generator.model;

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
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
import java.util.Optional;

public class MethodExecution {

    private static final Logger log = LoggerFactory.getLogger(MethodExecution.class);

    private List<InterceptedParam> inputParams;
    private String methodName;
    private String className;
    private String simpleClassName;
    private InterceptedParam result;
    private int hashCode;
    private long startTime;
    private long endTime;
    private Optional<StackTraceElement> caller;

    public static MethodExecution from(final ProceedingJoinPoint jointPoint,
                                       final Optional<StackTraceElement> caller) {
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
        methodExecution.setMethodName(methodName);
        methodExecution.setClassName(clazzName);
        methodExecution.setSimpleClassName(simpleClazzName);
        methodExecution.setInputParams(inputParams);
        methodExecution.setStartTime(System.currentTimeMillis());
        methodExecution.setHashCode(jointPoint.hashCode());
        methodExecution.setCaller(caller);
        return methodExecution;
    }

    public static MethodExecution from(final MethodInvocation invocation,
                                       final Optional<StackTraceElement> caller) {
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
        methodExecution.setMethodName(methodName);
        methodExecution.setClassName(clazzName);
        methodExecution.setSimpleClassName(simpleClazzName);
        methodExecution.setInputParams(inputParams);
        methodExecution.setStartTime(System.currentTimeMillis());
        methodExecution.setHashCode(invocation.hashCode());
        methodExecution.setCaller(caller);
        return methodExecution;
    }

    public void log() {
        log.debug("MethodExecution {}.{} (from) {} (to) {} (returns) {}", this.getClassName(), this.getMethodName(),
                this.displayTs(this.getStartTime()), this.displayTs(this.getEndTime()),
                this.getResult());
    }

    @Override
    public String toString() {
        return "MethodExecution{" +
                "inputParams=" + this.inputParams +
                ", name='" + this.methodName + '\'' +
                ", className='" + this.className + '\'' +
                ", simpleClassName='" + this.simpleClassName + '\'' +
                ", result=" + this.result +
                ", hashCode=" + this.hashCode +
                ", startTime=" + this.startTime +
                ", endTime=" + this.endTime +
                ", caller=" + this.caller +
                '}';
    }

    private String displayTs(final long timeStamp) {
        final Instant instant = Instant.ofEpochMilli(timeStamp);
        final LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public List<InterceptedParam> getInputParams() {
        return this.inputParams;
    }

    public void setInputParams(final List<InterceptedParam> inputParams) {
        this.inputParams = inputParams;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public void setMethodName(final String methodName) {
        this.methodName = methodName;
    }

    public String getClassName() {
        return this.className;
    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public String getSimpleClassName() {
        return this.simpleClassName;
    }

    public void setSimpleClassName(final String simpleClassName) {
        this.simpleClassName = simpleClassName;
    }

    public InterceptedParam getResult() {
        return this.result;
    }

    public void setResult(final InterceptedParam result) {
        this.result = result;
    }

    public int getHashCode() {
        return this.hashCode;
    }

    public void setHashCode(final int hashCode) {
        this.hashCode = hashCode;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(final long endTime) {
        this.endTime = endTime;
    }

    public Optional<StackTraceElement> getCaller() {
        return this.caller;
    }

    public void setCaller(final Optional<StackTraceElement> caller) {
        this.caller = caller;
    }
}
