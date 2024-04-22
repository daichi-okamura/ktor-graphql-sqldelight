package com.example

import io.ktor.server.application.*
import io.ktor.utils.io.errors.*
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.instrumentation.ktor.v2_0.server.KtorServerTracing
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.io.FileWriter
import java.time.Duration

fun Application.setOpenTelemetry(): OpenTelemetry {
    val propagators = ContextPropagators.create(
        TextMapPropagator.composite(
            W3CTraceContextPropagator.getInstance(),
        )
    )

    val spanExporter = FileSpanExporter("traces.txt")
    val spanProcessor = BatchSpanProcessor.builder(spanExporter)
        .setMaxQueueSize(2048)
        .setScheduleDelay(Duration.ofSeconds(5))
        .build()

    val tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(spanProcessor)
        .build()

    val loggerProvider = SdkLoggerProvider.builder()
        .build()

    val openTelemetry = OpenTelemetrySdk.builder()
        .setPropagators(propagators)
        .setTracerProvider(tracerProvider)
        .setLoggerProvider(loggerProvider)
        .buildAndRegisterGlobal()

    install(KtorServerTracing) {
        setOpenTelemetry(openTelemetry)
    }

    return openTelemetry
}

class FileSpanExporter(private val filename: String) : SpanExporter {
    override fun export(spans: Collection<SpanData?>): CompletableResultCode {
        try {
            FileWriter(filename, true).use { writer ->
                for (span in spans) {
                    writer.write(span.toString())
                    writer.write(System.lineSeparator())
                }
            }
        } catch (e: IOException) {
            return CompletableResultCode.ofFailure()
        }
        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode {
        // No-op
        return CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode? {
        // No-op
        return CompletableResultCode.ofSuccess()
    }
}

inline fun <T> withNewSpan(
    scopeName: String,
    spanName: String,
    body: () -> T
): T {
    val newSpan = GlobalOpenTelemetry
        .getTracer(scopeName)
        .spanBuilder(spanName)
        .startSpan()
    try {
        val result = body()
        newSpan.setStatus(StatusCode.OK)
        return result
    } catch (e: Exception) {
        newSpan.setStatus(StatusCode.ERROR)
        throw e
    } finally {
        newSpan.end()
    }
}
