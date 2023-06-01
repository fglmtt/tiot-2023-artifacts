package it.unimore.dipi.iot.digitaltwin.behaviour;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.ArrayList;
import java.util.Optional;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project edt-sdn-experiments
 * @created 10/11/2020 - 13:26
 */
public class ComposedValuesProcessingStep implements ProcessingStep {

    private static final Logger logger = LoggerFactory.getLogger(ComposedValuesProcessingStep.class);

    private String deviceId;

    private ObjectMapper objectMapper;

    private static final String METRIC_BASE_IDENTIFIER = "mqtt_pp_compose";

    private static final String PROCESSING_PIPELINE_EXECUTION_TIME_METRICS_FIELD = "execution_time";

    private final static String PIPELINE_TEMPERATURE_CACHE_VALUE_LIST = "temperature_value_list";

    private final static String PIPELINE_ENERGY_CACHE_VALUE_LIST = "energy_value_list";

    public ComposedValuesProcessingStep(String deviceId) {

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

                Optional<SenMLPack> optionalSenMlMessage = parseSenMlMessage(mqttPipelineData.getPayload());

                if(optionalSenMlMessage.isPresent() && optionalSenMlMessage.get().size() > 0){

                    double value = optionalSenMlMessage.get().get(0).getV().doubleValue();
                    logger.info("Value: {}", value);

                    //Handle GPS and Battery Telemetry Messages and generate the associated SenML Payload
                    if(mqttPipelineData.getTopic().contains("temperature")){

                        if(pipelineCache.getData(this, PIPELINE_TEMPERATURE_CACHE_VALUE_LIST) == null)
                            pipelineCache.addData(this, PIPELINE_TEMPERATURE_CACHE_VALUE_LIST, new ArrayList<Double>());

                        ArrayList<Double> temperatureValueList = (ArrayList<Double>) pipelineCache.getData(this, PIPELINE_TEMPERATURE_CACHE_VALUE_LIST);

                        temperatureValueList.add(value);
                        logger.info("Temperature Cached list size: {}", temperatureValueList.size());

                        if(temperatureValueList.size() >= 10){

                            double sum = temperatureValueList.stream().mapToDouble(tempValue -> tempValue).sum();
                            double average = sum / (double)temperatureValueList.size();
                            temperatureValueList.clear();
                            pipelineCache.addData(this, PIPELINE_TEMPERATURE_CACHE_VALUE_LIST, temperatureValueList);

                            processingStepListener.onStepDone(this, Optional.of(
                                    new MqttPipelineData(String.format("%s/%s", "device", "average"),
                                            mqttPipelineData.getMqttTopicDescriptor(),
                                            buildSenmlPayload("iot.temperature.composed", value, "Cel").get().getBytes(),
                                            mqttPipelineData.isRetained())));
                        }
                        else{
                            pipelineCache.addData(this, PIPELINE_TEMPERATURE_CACHE_VALUE_LIST, temperatureValueList);
                            processingStepListener.onStepDone(this, Optional.empty());
                        }

                    }
                    else if(mqttPipelineData.getTopic().contains("energy")){

                        if(pipelineCache.getData(this, PIPELINE_ENERGY_CACHE_VALUE_LIST) == null)
                            pipelineCache.addData(this, PIPELINE_ENERGY_CACHE_VALUE_LIST, new ArrayList<Double>());

                        ArrayList<Double> energyValueList = (ArrayList<Double>) pipelineCache.getData(this, PIPELINE_ENERGY_CACHE_VALUE_LIST);

                        energyValueList.add(value);
                        logger.info("Energy Cached list size: {}", energyValueList.size());

                        if(energyValueList.size() >= 10){

                            double sum = energyValueList.stream().mapToDouble(enValue -> enValue).sum();
                            double average = sum / (double)energyValueList.size();
                            energyValueList.clear();
                            pipelineCache.addData(this, PIPELINE_ENERGY_CACHE_VALUE_LIST, energyValueList);

                            processingStepListener.onStepDone(this, Optional.of(
                                    new MqttPipelineData(String.format("%s/%s", "device", "average"),
                                            mqttPipelineData.getMqttTopicDescriptor(),
                                            buildSenmlPayload("iot.energy.composed", value, "kW").get().getBytes(),
                                            mqttPipelineData.isRetained())));
                        }
                        else{
                            pipelineCache.addData(this, PIPELINE_ENERGY_CACHE_VALUE_LIST, energyValueList);
                            processingStepListener.onStepDone(this, Optional.empty());
                        }

                    }
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


    private Optional<String> buildSenmlPayload(String type, double value, String unit) {

        try {

            long originalTimestamp = System.currentTimeMillis();

            SenMLRecord senmlRecord = new SenMLRecord();
            senmlRecord.setT(originalTimestamp);
            senmlRecord.setN(type);
            senmlRecord.setV(value);
            senmlRecord.setU(unit);

            SenMLPack senMLPack = new SenMLPack(){{ add(senmlRecord);}};

            return Optional.of(this.objectMapper.writeValueAsString(senMLPack));

        }catch (Exception e){
            logger.error("Error serializing Senml Packet ! Msg: {}", e.getLocalizedMessage());
            return Optional.empty();
        }
    }


    private Optional<SenMLPack> parseSenMlMessage(byte[] payload){

        try{

            if(payload == null || payload.length == 0)
                return Optional.empty();

            return Optional.ofNullable(this.objectMapper.readValue(new String(payload), new TypeReference<SenMLPack>() {}));

        }catch (Exception e){
            return Optional.empty();
        }
    }

}
