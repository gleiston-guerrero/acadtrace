package ec.uteq.sga.secretaria.infrastructure.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Layout personalizado de Logback para estructurar los logs en formato JSON estándar.
 * Facilita la ingesta en plataformas de observabilidad como Prometheus, Loki, ElasticSearch y Grafana.
 */
public class JsonStructuredLayout extends LayoutBase<ILoggingEvent> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private String serviceName = "microservicio-secretaria";

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        json.put("level", event.getLevel().toString());
        json.put("service", serviceName);

        // Extraer traceId del MDC o de propiedades de contexto
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && mdc.containsKey("traceId")) {
            json.put("traceId", mdc.get("traceId"));
        } else {
            json.put("traceId", null);
        }

        json.put("thread", event.getThreadName());
        json.put("logger", event.getLoggerName());
        json.put("message", event.getFormattedMessage());

        if (mdc != null && !mdc.isEmpty()) {
            Map<String, String> customMdc = new LinkedHashMap<>(mdc);
            customMdc.remove("traceId");
            customMdc.remove("service");
            if (!customMdc.isEmpty()) {
                json.put("context", customMdc);
            }
        }

        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            Map<String, Object> errorInfo = new LinkedHashMap<>();
            errorInfo.put("className", throwableProxy.getClassName());
            errorInfo.put("message", throwableProxy.getMessage());

            StackTraceElementProxy[] stackTraceElements = throwableProxy.getStackTraceElementProxyArray();
            if (stackTraceElements != null && stackTraceElements.length > 0) {
                List<String> frames = new ArrayList<>(Math.min(stackTraceElements.length, 15));
                for (int i = 0; i < Math.min(stackTraceElements.length, 15); i++) {
                    frames.add(stackTraceElements[i].getSTEAsString());
                }
                errorInfo.put("stackTrace", frames);
            }
            json.put("exception", errorInfo);
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(json) + System.lineSeparator();
        } catch (Exception e) {
            return "{\"error\":\"Error serializing log to JSON: " + e.getMessage() + "\"}" + System.lineSeparator();
        }
    }
}
