package it.unimore.dipi.iot.digitaltwin;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.smartobject.message.TelemetryMessage;
import it.unimore.dipi.iot.utils.SenMLPack;
import it.unimore.dipi.iot.utils.SenMLRecord;
import it.unimore.dipi.iot.wldt.metrics.WldtMetricsManager;
import it.unimore.dipi.iot.wldt.processing.PipelineData;
import it.unimore.dipi.iot.wldt.processing.ProcessingStep;
import it.unimore.dipi.iot.wldt.processing.ProcessingStepListener;
import it.unimore.dipi.iot.wldt.processing.cache.PipelineCache;
import it.unimore.dipi.iot.wldt.worker.mqtt.MqttPipelineData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project edt-sdn-experiments
 * @created 10/11/2020 - 13:26
 */
public class SenmlMqttProcessingStep implements ProcessingStep {

    private static final Logger logger = LoggerFactory.getLogger(SenmlMqttProcessingStep.class);

    private String deviceId;

    private ObjectMapper objectMapper;

    private static final String SENML_DATA_TYPE = "string_resource";

    private static final String DEMO_APP_NAME = "assembly";

    private static final long DELAYABLE_VALUE = 100;

    private static final int DROPPABLE_VALUE = 0;

    private static final String METRIC_BASE_IDENTIFIER = "mqtt_pp_senml";

    private static final String PROCESSING_PIPELINE_EXECUTION_TIME_METRICS_FIELD = "execution_time";

    public SenmlMqttProcessingStep(String deviceId) {

        this.deviceId = deviceId;

        //Jackson Object Mapper + Ignore Null Fields in order to properly generate the SenML Payload
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    }

    @Override
    public void execute(PipelineCache pipelineCache, PipelineData pipelineData, ProcessingStepListener processingStepListener) {

        Timer.Context metricsContext = WldtMetricsManager.getInstance().getTimer(String.format("%s.%s", METRIC_BASE_IDENTIFIER, this.deviceId), PROCESSING_PIPELINE_EXECUTION_TIME_METRICS_FIELD);

        try{

            if(pipelineData instanceof MqttPipelineData){

                MqttPipelineData mqttPipelineData = (MqttPipelineData)pipelineData;

                logger.debug("Executing SenmlMqttProcessingStep Step with data: {}", new String(mqttPipelineData.getPayload()));

                //Update payload with Senml
                Optional<String> newPayloadOptional = Optional.empty();

                //Handle GPS and Battery Telemetry Messages and generate the associated SenML Payload
                if(mqttPipelineData.getTopic().contains("temperature")){
                    Optional<TelemetryMessage<Double>> optionalTelemetryMessage = parseTelemetryMessage(mqttPipelineData.getPayload());

                    if(optionalTelemetryMessage.isPresent())
                        newPayloadOptional = buildSenmlPayload(optionalTelemetryMessage.get(), "Cel");
                }
                else if(mqttPipelineData.getTopic().contains("energy")){
                    Optional<TelemetryMessage<Double>> optionalTelemetryMessage = parseTelemetryMessage(mqttPipelineData.getPayload());

                    if(optionalTelemetryMessage.isPresent())
                        newPayloadOptional = buildSenmlPayload(optionalTelemetryMessage.get(), "kW");
                }

                if(newPayloadOptional.isPresent()){
                    mqttPipelineData.setPayload(newPayloadOptional.get().getBytes());
                    processingStepListener.onStepDone(this, Optional.of(mqttPipelineData));
                }
                else{
                    String errorMessage = "PipelineData Error ! Error creating the updated SenML Payload !";
                    logger.error(errorMessage);
                    processingStepListener.onStepError(this, pipelineData, errorMessage);
                }
            }
            else {

                if(processingStepListener != null) {
                    String errorMessage = "PipelineData Error !";
                    logger.error(errorMessage);
                    processingStepListener.onStepError(this, pipelineData, errorMessage);
                }
                else
                    logger.error("Processing Step Listener = Null ! Skipping processing step");
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if(metricsContext != null)
                metricsContext.stop();
        }
    }

    private Optional<String> buildSenmlPayload(TelemetryMessage<Double> telemetryMessage, String unit) {

        try {

            long originalTimestamp = telemetryMessage.getTimestamp();

            SenMLRecord senmlRecord = new SenMLRecord();
            senmlRecord.setT(originalTimestamp);
            senmlRecord.setN(telemetryMessage.getType());
            senmlRecord.setV(telemetryMessage.getDataValue());
            senmlRecord.setU(unit);

            SenMLPack senMLPack = new SenMLPack(){{ add(senmlRecord);}};

            return Optional.of(this.objectMapper.writeValueAsString(senMLPack));

        }catch (Exception e){
            logger.error("Error serializing Senml Packet ! Msg: {}", e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    private Optional<TelemetryMessage<Double>> parseTelemetryMessage(byte[] payload){

        try{

            if(payload == null || payload.length == 0)
                return Optional.empty();

            return Optional.ofNullable(this.objectMapper.readValue(new String(payload), new TypeReference<TelemetryMessage<Double>>() {}));

        }catch (Exception e){
            return Optional.empty();
        }
    }

}
