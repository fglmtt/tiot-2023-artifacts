package it.unimore.dipi.iot.digitaltwin.behaviour;

import com.codahale.metrics.Timer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.digitaltwin.conf.ComposedDigitalTwinConfiguration;
import it.unimore.dipi.iot.digitaltwin.metrics.MetricsManager;
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

import java.util.*;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project edt-sdn-experiments
 * @created 10/11/2020 - 13:26
 */
public class ComposedStateProcessingStep implements ProcessingStep {

    private static final Logger logger = LoggerFactory.getLogger(ComposedStateProcessingStep.class);

    private String deviceId;

    private ObjectMapper objectMapper;

    private static final String METRIC_BASE_IDENTIFIER = "mqtt_pp_compose_state";

    private static final String PROCESSING_PIPELINE_EXECUTION_TIME_METRICS_FIELD = "execution_time";

    private final static String STATE_CACHE_VALUE_LIST = "state_value_list";

    private ComposedDigitalTwinConfiguration composedDigitalTwinConfiguration;

    private List<String> targetResourceIdList;

    private List<SenMLPack> aggregationStateHistoryList;

    public ComposedStateProcessingStep(String deviceId, ComposedDigitalTwinConfiguration composedDigitalTwinConfiguration) {

        this.deviceId = deviceId;
        this.composedDigitalTwinConfiguration = composedDigitalTwinConfiguration;
        this.targetResourceIdList = new ArrayList<>(this.composedDigitalTwinConfiguration.getResourceMap().values());

        logger.info("ComposeStatesProcessingStep -> Composition Target Resource List: {}", this.targetResourceIdList);

        this.aggregationStateHistoryList = new ArrayList<>();

        //Jackson Object Mapper + Ignore Null Fields in order to properly generate the SenML Payload
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    }

    @Override
    public void execute(PipelineCache pipelineCache, PipelineData pipelineData, ProcessingStepListener processingStepListener) {

        //Check & Set Digital Twin Life Cycle State Value
        if(MetricsManager.getInstance().getDigitalTwinLifeCycleStateGaugeValue() == DigitalTwinLifeCycleState.UN_BOUND.getValue())
            MetricsManager.getInstance().setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState.BOUND);

        Timer.Context metricsContext = WldtMetricsManager.getInstance().getTimer(String.format("%s.%s", METRIC_BASE_IDENTIFIER, this.deviceId), PROCESSING_PIPELINE_EXECUTION_TIME_METRICS_FIELD);

        try{

            if(pipelineData instanceof MqttPipelineData){

                MqttPipelineData mqttPipelineData = (MqttPipelineData)pipelineData;

                logger.debug("Executing ComposeStatesProcessingStep Step with data: {}", new String(mqttPipelineData.getPayload()));

                Optional<SenMLPack> optionalSenMlMessage = parseSenMlMessage(mqttPipelineData.getPayload());

                if(optionalSenMlMessage.isPresent()){
                    Optional<SenMLPack> resultAggregatedSenmlPackOptional = processNewSenmlMessage(optionalSenMlMessage.get());

                    if(resultAggregatedSenmlPackOptional.isPresent()){

                        SenMLPack aggregatedSenmlPack = resultAggregatedSenmlPackOptional.get();

                        if(processingStepListener != null) {

                            processingStepListener.onStepDone(this, Optional.of(
                                    new MqttPipelineData(String.format("%s/%s", "device", "average"),
                                            mqttPipelineData.getMqttTopicDescriptor(),
                                            buildSenmlPayload(aggregatedSenmlPack).get().getBytes(),
                                            mqttPipelineData.isRetained())));

                            logger.info("ComposedStateProcessingStep -> Aggregated State Computed !");
                        }
                        else
                            logger.error("Processing Step Listener = Null ! Skipping processing step");
                    }
                    else {

                        if(processingStepListener != null) {
                            logger.debug("Skipping Step ! Aggregation list size: {} target: {}",
                                    this.aggregationStateHistoryList.size(),
                                    this.composedDigitalTwinConfiguration.getAggregationWindow());
                            processingStepListener.onStepSkip(this, pipelineData);
                        }
                        else
                            logger.error("Processing Step Listener = Null ! Skipping processing step");
                    }
                }

                if(optionalSenMlMessage.isPresent() && optionalSenMlMessage.get().size() > 0){

                    SenMLPack receivedSenmlPack = optionalSenMlMessage.get();
                    SenMLPack tempStateSenmlPack = new SenMLPack();

                    for(SenMLRecord senMLRecord : receivedSenmlPack)
                        if(senMLRecord != null && this.targetResourceIdList.contains(senMLRecord.getN()))
                            tempStateSenmlPack.add(senMLRecord);

                    if(tempStateSenmlPack.size() > 0) {
                        this.aggregationStateHistoryList.add(tempStateSenmlPack);
                        logger.debug("New SenMLPack added to the history ! New Size: {}", this.aggregationStateHistoryList.size());
                    }

                    //Check availability to handle aggregation and average of values
                    if(this.aggregationStateHistoryList.size() == this.composedDigitalTwinConfiguration.getAggregationWindow()){

                        logger.debug("Aggregation History Size: {} ! Starting aggregation ...", this.aggregationStateHistoryList.size());

                        // Aggregate Historical State Data
                        Optional<SenMLPack> aggregatedSenmlPackOptional = aggregateReceivedDigitalTwinStates(this.composedDigitalTwinConfiguration.getAggregationType());

                        //Clear Historical Data
                        this.aggregationStateHistoryList.clear();

                        if(aggregatedSenmlPackOptional.isPresent()){

                            SenMLPack aggregatedSenmlPack = aggregatedSenmlPackOptional.get();

                            processingStepListener.onStepDone(this, Optional.of(
                                    new MqttPipelineData(String.format("%s/%s", "device", "average"),
                                            mqttPipelineData.getMqttTopicDescriptor(),
                                            buildSenmlPayload(aggregatedSenmlPack).get().getBytes(),
                                            mqttPipelineData.isRetained())));
                        }
                        else{

                            if(processingStepListener != null) {
                                String errorMessage = "PipelineData Error ! Missing Aggregated SenML Pack";
                                logger.error(errorMessage);
                                processingStepListener.onStepError(this, pipelineData, errorMessage);
                            }
                            else
                                logger.error("Processing Step Listener = Null ! Skipping processing step");
                        }

                    }
                    else {
                        if(processingStepListener != null) {
                            logger.debug("Skipping Step ! Aggregation list size: {} target: {}",
                                    this.aggregationStateHistoryList.size(),
                                    this.composedDigitalTwinConfiguration.getAggregationWindow());
                            processingStepListener.onStepSkip(this, pipelineData);
                        }
                        else
                            logger.error("Processing Step Listener = Null ! Skipping processing step");
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

    private Optional<SenMLPack> processNewSenmlMessage(SenMLPack receivedSenmlPack){

        try{

            Optional<SenMLPack> resultAggregatedSenmlPackOptional = Optional.empty();
            long physicalTimestamp = -1;

            if(receivedSenmlPack.size() > 0){

                SenMLPack tempStateSenmlPack = new SenMLPack();

                for(SenMLRecord senMLRecord : receivedSenmlPack)
                    if(senMLRecord != null) {
                        if(senMLRecord.getT().longValue() > 0 && senMLRecord.getT().longValue() != physicalTimestamp)
                            physicalTimestamp = senMLRecord.getT().longValue();

                        if(this.targetResourceIdList.contains(senMLRecord.getN()))
                            tempStateSenmlPack.add(senMLRecord);
                    }

                //Emulate State Computation
                primeNumbersBruteForce(composedDigitalTwinConfiguration.getPrimeNumbersComputationCount());

                if(tempStateSenmlPack.size() > 0) {
                    this.aggregationStateHistoryList.add(tempStateSenmlPack);
                    logger.debug("New SenMLPack added to the history ! New Size: {}", this.aggregationStateHistoryList.size());
                }

                //Check availability to handle aggregation and average of values
                if(this.aggregationStateHistoryList.size() == this.composedDigitalTwinConfiguration.getAggregationWindow()){

                    logger.debug("Aggregation History Size: {} ! Starting aggregation ...", this.aggregationStateHistoryList.size());

                    // Aggregate Historical State Data
                    resultAggregatedSenmlPackOptional = aggregateReceivedDigitalTwinStates(this.composedDigitalTwinConfiguration.getAggregationType());

                    //Clear Historical Data
                    this.aggregationStateHistoryList.clear();
                }
            }

            // Compute Timeliness and save metric value
            if(physicalTimestamp >= 0){
                long entanglementObservation = System.currentTimeMillis() - physicalTimestamp;
                double secDelay = (double)entanglementObservation/1000.0;
                logger.info("Aggregated Physical State - Entanglement Observations Sec: {}", secDelay);
                MetricsManager.getInstance().addObservationDelaySec(physicalTimestamp, secDelay);
            }
            else
                logger.error("Error computing Timeliness ! physicalTimestamp value is not correct: {}", physicalTimestamp);

            return resultAggregatedSenmlPackOptional;

        }catch (Exception e){
            logger.error("Exception handling new SenMLMessage: {} ! Error: {}", receivedSenmlPack, e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    /**
     * TODO At the moment the aggregation is only the average value
     * @param aggregationType
     */
    private Optional<SenMLPack> aggregateReceivedDigitalTwinStates(String aggregationType){
        try{

            Map<String, List<SenMLRecord>> resultMap = new HashMap<>();

            // Init target ArrayList with reference resource type
            for(String resourceType : this.targetResourceIdList)
                resultMap.put(resourceType, new ArrayList<SenMLRecord>());

            //Save all the target values in the reference Result Map
            for(SenMLPack senMLPack : this.aggregationStateHistoryList){
                for(SenMLRecord senMLRecord : senMLPack){
                    //The SenML Record match target resource type/name
                    if(senMLRecord != null && this.targetResourceIdList.contains(senMLRecord.getN()))
                        resultMap.get(senMLRecord.getN()).add(senMLRecord);
                }
            }

            SenMLPack finalStatusSenmlPack = new SenMLPack();

            //Compute Averages Values
            for(String resourceType : this.targetResourceIdList){

                SenMLRecord targetSenMLRecord = null;
                double targetSum = 0;

                for(SenMLRecord senMLRecord : resultMap.get(resourceType)){

                    // Create the new aggregated SenML Record based on the original values
                    if(targetSenMLRecord == null){
                        targetSenMLRecord = new SenMLRecord();
                        targetSenMLRecord.setN(String.format("%s.aggregated", senMLRecord.getN()));
                        targetSenMLRecord.setU(senMLRecord.getU());
                        targetSenMLRecord.setT(System.currentTimeMillis());
                    }

                    targetSum += senMLRecord.getV().doubleValue();
                }

                if(targetSenMLRecord != null){
                    targetSenMLRecord.setV((targetSum)/(resultMap.get(resourceType).size()));
                    finalStatusSenmlPack.add(targetSenMLRecord);
                }
            }

            return Optional.of(finalStatusSenmlPack);

        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Optional<String> buildSenmlPayload(SenMLPack targetSenmlPack) {

        try {

            return Optional.of(this.objectMapper.writeValueAsString(targetSenmlPack));

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

}
