package it.unimore.dipi.iot.digitaltwin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.nio.file.Paths;

class HttpMetricsFolderHandler implements HttpHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        String folderId = exchange.getQueryParameters().get("folderId").getFirst();

        if(folderId != null) {
            String[] files = MetricsExporterUtils.listAvailableFiles(Paths.get(MetricsExporterUtils.METRICS_FOLDER, folderId).toString());

            if(files != null) {
                exchange.setStatusCode(StatusCodes.OK);
                exchange.getResponseSender().send(objectMapper.writeValueAsString(files));
            }
            else
                exchange.setStatusCode(StatusCodes.NOT_FOUND);
        }
        else
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
    }
}