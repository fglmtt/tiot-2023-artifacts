package it.unimore.dipi.iot.digitaltwin.conf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import it.unimore.dipi.iot.digitaltwin.metrics.MetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpConfigurationHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpConfigurationHandler.class);

    private ObjectMapper objectMapper;

    private ComposedDigitalTwinConfiguration digitalTwinConfiguration;

    public HttpConfigurationHandler(ComposedDigitalTwinConfiguration digitalTwinConfiguration) {
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

                        ComposedDigitalTwinConfiguration newDigitalTwinConfiguration = null;

                        //De-Serialize received body
                        try {
                            newDigitalTwinConfiguration = objectMapper.readValue(receivedString, ComposedDigitalTwinConfiguration.class);
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
                                if(newDigitalTwinConfiguration.getPrimeNumbersComputationCount() > 0)
                                    digitalTwinConfiguration.setPrimeNumbersComputationCount(newDigitalTwinConfiguration.getPrimeNumbersComputationCount());

                                //Update ODTE Configuration Parameters
                                if(newDigitalTwinConfiguration.getOdteSlidingWindowSec() > 0)
                                    digitalTwinConfiguration.setOdteSlidingWindowSec(newDigitalTwinConfiguration.getOdteSlidingWindowSec());

                                if(newDigitalTwinConfiguration.getOdteDesiredTimelinessSec() > 0)
                                    digitalTwinConfiguration.setOdteDesiredTimelinessSec(newDigitalTwinConfiguration.getOdteDesiredTimelinessSec());

                                if(newDigitalTwinConfiguration.getOdteExpectedMsgSec() > 0)
                                    digitalTwinConfiguration.setOdteExpectedMsgSec(newDigitalTwinConfiguration.getOdteExpectedMsgSec());

                                if(newDigitalTwinConfiguration.getOdteTargetPercentile() > 0)
                                    digitalTwinConfiguration.setOdteTargetPercentile(newDigitalTwinConfiguration.getOdteTargetPercentile());

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