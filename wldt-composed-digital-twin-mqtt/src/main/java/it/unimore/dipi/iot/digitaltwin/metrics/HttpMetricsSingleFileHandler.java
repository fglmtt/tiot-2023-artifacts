package it.unimore.dipi.iot.digitaltwin.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class HttpMetricsSingleFileHandler implements HttpHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        String folderId = exchange.getQueryParameters().get("folderId").getFirst();
        String fileId = exchange.getQueryParameters().get("fileId").getFirst();

        if(folderId != null && fileId != null) {

            List<Map<?, ?>> fileContent = MetricsExporterUtils.readCsvFile(
                    Paths.get(
                            MetricsExporterUtils.METRICS_FOLDER,
                            folderId,
                            fileId).toString());

            if(fileContent != null) {
                exchange.setStatusCode(StatusCodes.OK);
                exchange.getResponseSender().send(objectMapper.writeValueAsString(fileContent));
            }
            else
                exchange.setStatusCode(StatusCodes.NOT_FOUND);
        }
        else
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
    }
}