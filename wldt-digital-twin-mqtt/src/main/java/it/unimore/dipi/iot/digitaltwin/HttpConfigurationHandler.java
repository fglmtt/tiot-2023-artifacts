package it.unimore.dipi.iot.digitaltwin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpConfigurationHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpConfigurationHandler.class);

    private ObjectMapper objectMapper;

    private DigitalTwinConfiguration digitalTwinConfiguration;

    public HttpConfigurationHandler(DigitalTwinConfiguration digitalTwinConfiguration) {
        super();
        objectMapper = new ObjectMapper();
        this.digitalTwinConfiguration = digitalTwinConfiguration;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        try {

            logger.info("HTTP HANDLER -> CONFIGURATION REQUEST: {}", exchange.getRequestMethod());

            if(exchange.getRequestMethod().equals(Methods.GET)) {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                exchange.setStatusCode(StatusCodes.OK);
                exchange.getResponseSender().send(objectMapper.writeValueAsString(digitalTwinConfiguration));
            }
            else {
                exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                    @Override
                    public void handle(HttpServerExchange httpServerExchange, String receivedString) {

                        DigitalTwinConfiguration newDigitalTwinConfiguration = null;

                        //De-Serialize received body
                        try {
                            newDigitalTwinConfiguration = objectMapper.readValue(receivedString, DigitalTwinConfiguration.class);
                        } catch (JsonProcessingException e) {
                            logger.error("HTTP CONFIGURATION HANDLER Exception ! Msg: {}", e.getLocalizedMessage());
                            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                        }

                        if(newDigitalTwinConfiguration != null){

                            try{

                                //Update Expected MsgSec Value
                                if(newDigitalTwinConfiguration.getExpectedMsgSec() > 0){
                                    digitalTwinConfiguration.setExpectedMsgSec(newDigitalTwinConfiguration.getExpectedMsgSec());
                                    MetricsManager.getInstance().setExpectedMessageRateValue(digitalTwinConfiguration.getExpectedMsgSec());
                                }

                                //Update Emulated DT State Processing Load
                                if(digitalTwinConfiguration.getPrimeNumbersComputationCount() > 0)
                                    digitalTwinConfiguration.setPrimeNumbersComputationCount(newDigitalTwinConfiguration.getPrimeNumbersComputationCount());

                                exchange.setStatusCode(StatusCodes.NO_CONTENT);
                            }catch (Exception e){
                                logger.error("HTTP CONFIGURATION HANDLER Exception ! Msg: {}", e.getLocalizedMessage());
                                exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                            }
                        }
                        else {
                            logger.error("HTTP CONFIGURATION HANDLER WRONG RECEIVED CONFIGURATION ! Conf: {}", newDigitalTwinConfiguration);
                            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                        }
                    }
                });
            }
        }catch (Exception e){
            logger.error("HTTP CONFIGURATION HANDLER Exception ! Msg: {}", e.getLocalizedMessage());
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
        }

    }
}