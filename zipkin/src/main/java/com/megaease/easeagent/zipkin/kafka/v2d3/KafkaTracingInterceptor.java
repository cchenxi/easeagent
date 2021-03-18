package com.megaease.easeagent.zipkin.kafka.v2d3;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import com.megaease.easeagent.core.interceptor.AgentInterceptor;
import com.megaease.easeagent.core.interceptor.AgentInterceptorChain;
import com.megaease.easeagent.core.interceptor.MethodInfo;
import com.megaease.easeagent.core.utils.ContextUtils;
import com.megaease.easeagent.zipkin.kafka.brave.KafkaTracing;
import com.megaease.easeagent.zipkin.kafka.brave.MultiData;
import com.megaease.easeagent.zipkin.kafka.brave.TracingCallback;
import com.megaease.easeagent.zipkin.kafka.brave.TracingProducer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Map;

public class KafkaTracingInterceptor implements AgentInterceptor {

    private final KafkaTracing kafkaTracing;

    public KafkaTracingInterceptor() {
        kafkaTracing = KafkaTracing.newBuilder(Tracing.current()).remoteServiceName("my-broker").build();
    }

    @Override
    public void before(MethodInfo methodInfo, Map<Object, Object> context, AgentInterceptorChain chain) {
        KafkaProducer<?, ?> producer = (KafkaProducer<?, ?>) methodInfo.getInvoker();
        TracingProducer<?, ?> tracingProducer = kafkaTracing.producer(producer);
        ProducerRecord<?, ?> record = (ProducerRecord<?, ?>) methodInfo.getArgs()[0];
        MultiData<Span, Tracer.SpanInScope> multiData = tracingProducer.beforeSend(record);
        Callback callback = (Callback) methodInfo.getArgs()[1];
        Callback tracingCallback = TracingCallback.create(callback, multiData.data0, tracingProducer.currentTraceContext);
        methodInfo.getArgs()[1] = tracingCallback;
        context.put(MultiData.class, multiData);
        context.put(TracingProducer.class, tracingProducer);
        chain.doBefore(methodInfo, context);
    }

    @Override
    public Object after(MethodInfo methodInfo, Map<Object, Object> context, AgentInterceptorChain chain) {
        TracingProducer<?, ?> tracingProducer = ContextUtils.getFromContext(context, TracingProducer.class);
        MultiData<Span, Tracer.SpanInScope> multiData = ContextUtils.getFromContext(context, TracingProducer.class);
        tracingProducer.afterSend(multiData.data1, multiData.data0, methodInfo.getThrowable());
        return chain.doAfter(methodInfo, context);
    }

}
