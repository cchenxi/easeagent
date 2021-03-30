package com.megaease.easeagent.core.interceptor;

import com.megaease.easeagent.core.utils.ContextUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class AgentInterceptorChainInvoker {

    private static final String BEFORE_ELAPSED_TIME_KEY = AgentInterceptorChainInvoker.class.getName() + "-BEFORE_ELAPSED_TIME_KEY";

    public static final AgentInterceptorChainInvoker instance = new AgentInterceptorChainInvoker();

    public static AgentInterceptorChainInvoker getInstance() {
        return instance;
    }

    private boolean logElapsedTime;

    public AgentInterceptorChainInvoker setLogElapsedTime(boolean logElapsedTime) {
        this.logElapsedTime = logElapsedTime;
        return this;
    }

    public void doBefore(AgentInterceptorChain.Builder builder, MethodInfo methodInfo, Map<Object, Object> context) {
        long beginTime = System.currentTimeMillis();
        AgentInterceptorChain interceptorChain = this.prepare(builder, context);
        interceptorChain.doBefore(methodInfo, context);
        long elapsed = System.currentTimeMillis() - beginTime;
        context.put(BEFORE_ELAPSED_TIME_KEY, elapsed);
    }

    public Object doAfter(AgentInterceptorChain.Builder builder, MethodInfo methodInfo, Map<Object, Object> context) {
        return doAfter(builder, methodInfo, context, false);
    }

    public Object doAfter(AgentInterceptorChain.Builder builder, MethodInfo methodInfo, Map<Object, Object> context, boolean newInterceptorChain) {
        long beginTime = System.currentTimeMillis();
        if (newInterceptorChain) {
            context.remove(AgentInterceptorChain.class);
        }
        AgentInterceptorChain interceptorChain = ContextUtils.getFromContext(context, AgentInterceptorChain.class);
        if (interceptorChain == null) {
            interceptorChain = this.prepare(builder, context);
            if (interceptorChain == null) {
                return methodInfo.getRetValue();
            }
            interceptorChain.skipBegin();
        }
        Object result = interceptorChain.doAfter(methodInfo, context);
        StringBuilder sb = new StringBuilder();
        if (methodInfo != null) {
            if (methodInfo.getInvoker() != null) {
                sb.append(methodInfo.getInvoker().getClass().getName());
            }
            if (methodInfo.getMethod() != null) {
                sb.append("#").append(methodInfo.getMethod());
            }
        }
        Long elapsed4Before = ContextUtils.getFromContext(context, BEFORE_ELAPSED_TIME_KEY);
        long elapsed4After = System.currentTimeMillis() - beginTime;
        if (logElapsedTime) {
            log.info("===== elapsedTime advice:{} before invoke:{}ms, after invoke:{}ms ======", sb.toString(), elapsed4Before, elapsed4After);
        }
        return result;
    }

    private AgentInterceptorChain prepare(AgentInterceptorChain.Builder builder, Map<Object, Object> context) {
        if (builder == null) {
            return null;
        }
        AgentInterceptorChain interceptorChain = builder.build();
        context.put(AgentInterceptorChain.class, interceptorChain);
        return interceptorChain;
    }
}
