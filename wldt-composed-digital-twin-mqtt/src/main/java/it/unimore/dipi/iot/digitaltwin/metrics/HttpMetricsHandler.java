package it.unimore.dipi.iot.digitaltwin.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public class HttpMetricsHandler implements HttpHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        String[] directories = MetricsExporterUtils.loadAvailableMetricsFolder();

        if(directories != null) {
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().send(objectMapper.writeValueAsString(directories));
        }
        else
            exchange.setStatusCode(StatusCodes.NOT_FOUND);
    }
}