package it.unimore.dipi.iot.digitaltwin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.smartobject.message.TelemetryMessage;
import it.unimore.dipi.iot.smartobject.resource.AggregatedResourcePayload;
import it.unimore.dipi.iot.wldt.processing.PipelineData;
import it.unimore.dipi.iot.wldt.processing.ProcessingStep;
import it.unimore.dipi.iot.wldt.processing.ProcessingStepListener;
import it.unimore.dipi.iot.wldt.processing.cache.PipelineCache;
import it.unimore.dipi.iot.wldt.worker.mqtt.MqttPipelineData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project edt-sdn-experiments
 * @created 10/11/2020 - 13:26
 */
public class DigitalStateProcessingStep implements ProcessingStep {

    private static final Logger logger = LoggerFactory.getLogger(DigitalStateProcessingStep.class);

    private DigitalTwinConfiguration digitalTwinConfiguration;

    private ObjectMapper objectMapper;

    public DigitalStateProcessingStep(DigitalTwinConfiguration digitalTwinConfiguration) {

        this.digitalTwinConfiguration = digitalTwinConfiguration;

        //Jackson Object Mapper + Ignore Null Fields in order to properly generate the SenML Payload
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    }

    @Override
    public void execute(PipelineCache pipelineCache, PipelineData pipelineData, ProcessingStepListener processingStepListener) {

        //Check & Set Digital Twin Life Cycle State Value
        if(MetricsManager.getInstance().getDigitalTwinLifeCycleStateGaugeValue() == DigitalTwinLifeCycleState.UN_BOUND.getValue())
            MetricsManager.getInstance().setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState.BOUND);

        try{

            //long currentTimestamp = System.currentTimeMillis();

            if(pipelineData instanceof MqttPipelineData){

                MqttPipelineData mqttPipelineData = (MqttPipelineData)pipelineData;

                logger.debug("Executing EntanglementMeasuringProcessingStep Step with data: {}", new String(mqttPipelineData.getPayload()));

                //Handle GPS and Battery Telemetry Messages and generate the associated SenML Payload
                if(mqttPipelineData.getTopic().contains("device_state")){

                    Optional<TelemetryMessage<AggregatedResourcePayload>> optionalTelemetryMessage = parseAggregatedStateTelemetryMessage(mqttPipelineData.getPayload());

                    if(optionalTelemetryMessage.isPresent()){

                        TelemetryMessage<AggregatedResourcePayload> telemetryMessage = optionalTelemetryMessage.get();

                        long physicalTimestamp = telemetryMessage.getTimestamp();

                        //Emulate State Computation
                        primeNumbersBruteForce(digitalTwinConfiguration.getPrimeNumbersComputationCount());

                        long entanglementObservation = System.currentTimeMillis() - physicalTimestamp;

                        double secDelay = (double)entanglementObservation/1000.0;
                        logger.info("Aggregated Physical State - Entanglement Observations Sec: {}", secDelay);

                        MetricsManager.getInstance().addObservationDelaySec(physicalTimestamp, secDelay);

//                        if(MetricsManager.getInstance().getDigitalTwinLifeCycleStateGaugeValue() == DigitalTwinLifeCycleState.BOUND.getValue()
//                                || MetricsManager.getInstance().getDigitalTwinLifeCycleStateGaugeValue() == DigitalTwinLifeCycleState.UN_SYNC.getValue())
//                            MetricsManager.getInstance().setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState.SHADOWED);

                        //Forward the original payload
                        mqttPipelineData.setPayload(mqttPipelineData.getPayload());
                        processingStepListener.onStepDone(this, Optional.of(mqttPipelineData));
                    }
                    else{
                        String errorMessage = "PipelineData Error ! Error parsing received AggregateStatePayload !";
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
    }

    public static List<Integer> primeNumbersBruteForce(int n) {
        List<Integer> primeNumbers = new LinkedList<>();
        for (int i = 2; i <= n; i++) {
            if (isPrimeBruteForce(i)) {
                primeNumbers.add(i);
            }
        }
        return primeNumbers;
    }
    public static boolean isPrimeBruteForce(int number) {
        for (int i = 2; i < number; i++) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }

    private Optional<TelemetryMessage<AggregatedResourcePayload>> parseAggregatedStateTelemetryMessage(byte[] payload){

        try{
            if(payload == null || payload.length == 0)
                return Optional.empty();
            return Optional.ofNullable(this.objectMapper.readValue(new String(payload),
                    new TypeReference<TelemetryMessage<AggregatedResourcePayload>>() {}));
        }catch (Exception e){
            return Optional.empty();
        }
    }

}
