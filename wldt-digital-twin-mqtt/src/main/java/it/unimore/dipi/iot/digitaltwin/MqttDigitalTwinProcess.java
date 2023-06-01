package it.unimore.dipi.iot.digitaltwin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;
import it.unimore.dipi.iot.digitaltwin.odte.OdteManager;
import it.unimore.dipi.iot.digitaltwin.odte.OdteResultDescription;
import it.unimore.dipi.iot.wldt.engine.WldtConfiguration;
import it.unimore.dipi.iot.wldt.engine.WldtEngine;
import it.unimore.dipi.iot.wldt.processing.ProcessingPipeline;
import it.unimore.dipi.iot.wldt.worker.mqtt.Mqtt2MqttConfiguration;
import it.unimore.dipi.iot.wldt.worker.mqtt.Mqtt2MqttWorker;
import it.unimore.dipi.iot.wldt.worker.mqtt.MqttTopicDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 *
 * Mvn Command: mvn exec:java -Dexec.mainClass="it.unimore.dipi.edt.experiment.digitaltwin.MqttDigitalTwin"
 *
 * @project edt-sdn-experiments
 * @created 09/11/2020 - 17:49
 */
public class MqttDigitalTwinProcess {

    private static final String TAG = "[WLDT-MQTT-Process]";

    private static final Logger logger = LoggerFactory.getLogger(MqttDigitalTwinProcess.class);

    private static final String ENERGY_TOPIC_ID = "energy_topic";
    private static final String ENERGY_RESOURCE_ID = "energy";

    private static final String TEMPERATURE_TOPIC_ID = "temperature_topic";
    private static final String TEMPERATURE_RESOURCE_ID = "temperature";

    private static final String DEVICE_STATE_TOPIC_ID = "device_state_topic";
    private static final String DEVICE_STATE_RESOURCE_ID = "device_state";

    private static final String DT_CONFIGURATION_FILE = "dt_conf.yaml";

    private static HttpConfigurationHandler httpConfigurationHandler = null;

    private static DigitalTwinConfiguration dtConfiguration = null;

    public static void main(String[] args)  {

        try{

            logger.info("{} Initializing WLDT-Engine ... ", TAG);

            //Read Local DT Configuration File
            dtConfiguration = readConfigurationFile();
            logger.info("DT Configuration: {}", dtConfiguration);

            if(dtConfiguration != null){

                //Initialize Prometheus Metrics
                initPrometheusMetrics(dtConfiguration.getDigitalTwinId());

                //Update starting expected Msgrate
                MetricsManager.getInstance().setExpectedMessageRateValue(dtConfiguration.getExpectedMsgSec());

                //Update Physical Asset Uptime
                MetricsManager.getInstance().setPhysicalAssetUptimeValue(1.0);

                //Set Digital Twin Life Cycle State Value
                MetricsManager.getInstance().setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState.STARTED);

                Thread.sleep(5000);

                //Create HTTP Configuration Handler
                httpConfigurationHandler = new HttpConfigurationHandler(dtConfiguration);

                //Manual creation of the WldtConfiguration
                WldtConfiguration wldtConfiguration = new WldtConfiguration();
                wldtConfiguration.setDeviceNameSpace("it.unimore.dipi.things");
                wldtConfiguration.setWldtBaseIdentifier("wldt");
                wldtConfiguration.setWldtStartupTimeSeconds(10);
                wldtConfiguration.setApplicationMetricsEnabled(dtConfiguration.isMetricsEnabled());
                wldtConfiguration.setApplicationMetricsReportingPeriodSeconds(10);
                wldtConfiguration.setMetricsReporterList(Collections.singletonList("csv"));

                WldtEngine wldtEngine = new WldtEngine(wldtConfiguration);

                Mqtt2MqttWorker mqtt2MqttWorker = new Mqtt2MqttWorker(
                        wldtEngine.getWldtId(),
                        getMqttProtocolConfiguration(dtConfiguration)
                );

                //Add Processing Pipeline for target topics
                mqtt2MqttWorker.addTopicProcessingPipeline(ENERGY_TOPIC_ID,
                        new ProcessingPipeline(
                                new SenmlMqttProcessingStep(wldtEngine.getWldtId())
                        )
                );

                mqtt2MqttWorker.addTopicProcessingPipeline(TEMPERATURE_TOPIC_ID,
                        new ProcessingPipeline(
                                new SenmlMqttProcessingStep(wldtEngine.getWldtId())
                        )
                );

                mqtt2MqttWorker.addTopicProcessingPipeline(DEVICE_STATE_TOPIC_ID,
                        new ProcessingPipeline(
                                new DigitalStateProcessingStep(dtConfiguration),
                                new DigitalTwinStateSenmlMqttProcessingStep(wldtEngine.getWldtId())
                        ));

                wldtEngine.addNewWorker(mqtt2MqttWorker);

                //Set Digital Twin Life Cycle State Value
                MetricsManager.getInstance().setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState.UN_BOUND);

                Thread.sleep(5000);

                wldtEngine.startWorkers();

                startHttpMetricsApi();

                startPeriodicUnSyncStateMonitoring(dtConfiguration.getShadowedThresholdMs(), dtConfiguration.getUnBoundThresholdMs());
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void startPeriodicUnSyncStateMonitoring(long shadowedThresholdMs, long unBoundThresholdMs){

        try{
            TimerTask task = new TimerTask() {
                public void run() {

                    long diff = System.currentTimeMillis() - MetricsManager.getInstance().getLastObservationTimestampMs();

                    Optional<HashMap<Long, Double>> timelinessMapOptional = loadTimelinessValuesInRange(dtConfiguration.getOdteSlidingWindowSec());

                    if(timelinessMapOptional.isPresent() && timelinessMapOptional.get().size() > 0){

                        HashMap<Long, Double> timelinessMap = timelinessMapOptional.get();
                        logger.info("ODTE -> Correctly Loaded Timeliness Map of {} elements !", timelinessMap.size());

                        //Computing ODTE Value with the target array of timeliness values and the target timeliness in Sec
                        Optional<OdteResultDescription> computedOdteOptional = OdteManager.getInstance().computeOdte(
                                new ArrayList<>(timelinessMap.values()),
                                dtConfiguration.getOdteDesiredTimelinessSec(),
                                dtConfiguration.getOdteExpectedMsgSec(),
                                dtConfiguration.getOdteSlidingWindowSec());

                        if(computedOdteOptional.isPresent()) {
                            logger.info("ODTE -> New Value: {}", computedOdteOptional.get());

                            //Publish ODTE Metrics
                            MetricsManager.getInstance().setDigitalTwinOdteValue(computedOdteOptional.get());

                            double odteValue = computedOdteOptional.get().getOdte();

                            //Check DT State according to the computed ODTE

                            if(odteValue >= dtConfiguration.getOdteTargetPercentile() && (
                                                    MetricsManager.getInstance().getDigitalTwinLifeCycleStateGaugeValue() == DigitalTwinLifeCycleState.BOUND.getValue() ||
                                                    MetricsManager.getInstance().getDigitalTwinLifeCycleStateGaugeValue() == DigitalTwinLifeCycleState.UN_SYNC.getValue())
                            )
                                MetricsManager.getInstance().setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState.SHADOWED);

                            if(MetricsManager.getInstance().getDigitalTwinLifeCycleStateGaugeValue() == DigitalTwinLifeCycleState.SHADOWED.getValue() &&  odteValue < dtConfiguration.getOdteTargetPercentile())
                                MetricsManager.getInstance().setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState.UN_SYNC);


                            if(MetricsManager.getInstance().getDigitalTwinLifeCycleStateGaugeValue() == DigitalTwinLifeCycleState.UN_SYNC.getValue() && diff > unBoundThresholdMs)
                                MetricsManager.getInstance().setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState.UN_BOUND);
                        }
                        else
                            logger.error("ODTE -> Error computing ! EMPTY Result");

                    }
                    else{
                        logger.error("ODTE -> Error loading Timeliness values in range ({} sec) Data Map is Null or EMPTY !", dtConfiguration.getOdteSlidingWindowSec());
                    }

                    /*
                    long diff = System.currentTimeMillis() - PrometheusManager.getInstance().getLastObservationTimestampMs();

                    if(PrometheusManager.getInstance().getDigitalTwinLifeCycleStateGaugeValue() == DigitalTwinLifeCycleState.SHADOWED.getValue() && diff > shadowedThresholdMs)
                        PrometheusManager.getInstance().setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState.UN_SYNC);

                    if(PrometheusManager.getInstance().getDigitalTwinLifeCycleStateGaugeValue() == DigitalTwinLifeCycleState.UN_SYNC.getValue() && diff > unBoundThresholdMs)
                        PrometheusManager.getInstance().setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState.UN_BOUND);
                     */
                }
            };

            Timer timer = new Timer("UnSync State Timer");
            long delay = 1000L;
            timer.schedule(task, delay, delay);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static long startTimestampMs = Long.MAX_VALUE;
    private static long endTimestampMs = Long.MIN_VALUE;

    private static Optional<HashMap<Long, Double>> loadTimelinessValuesInRange(long slidingWindowSizeSec){

        try{

            startTimestampMs = Long.MAX_VALUE;
            endTimestampMs = Long.MIN_VALUE;

            long currentTimestamp = System.currentTimeMillis();

            ConcurrentMap<Long, Double> cacheMap = MetricsManager.getInstance().getTimelinessCache().asMap();
            //logger.info("CACHE MAP SIZE: {}", cacheMap.size());

            HashMap<Long, Double> timelinessValuesMap = new HashMap<>();

            cacheMap.forEach((observationTimestamp, observationSec) -> {
                if(observationTimestamp <= currentTimestamp && observationTimestamp >= (currentTimestamp - TimeUnit.SECONDS.toMillis(slidingWindowSizeSec))) {
                    timelinessValuesMap.put(observationTimestamp, observationSec);

                    if(observationTimestamp < startTimestampMs)
                        startTimestampMs = observationTimestamp;

                    if(observationTimestamp > endTimestampMs)
                        endTimestampMs = observationTimestamp;
                }
            });

            LocalDateTime startDate = Instant.ofEpochMilli(startTimestampMs).atZone(ZoneId.of("UTC")).toLocalDateTime();
            LocalDateTime endDate = Instant.ofEpochMilli(endTimestampMs).atZone(ZoneId.of("UTC")).toLocalDateTime();

            logger.debug("CACHE Size: {} RANGE: From: {} To: {}", timelinessValuesMap.size(),
                    startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")),
                    endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));

            return Optional.of(timelinessValuesMap);

        } catch (Exception e){
            logger.error("Error checking ODTE Values: {}", e.getLocalizedMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static void initPrometheusMetrics(String digitalTwinId){

        //Enable Prometheus HTTP Server Monitoring & Configure Metrics Type
        MetricsManager.getInstance().setObservationBucketStart(dtConfiguration.getObservationBucketStart());
        MetricsManager.getInstance().setObservationBucketWidth(dtConfiguration.getObservationBucketWidth());
        MetricsManager.getInstance().setObservationBucketCount(dtConfiguration.getObservationBucketCount());
        MetricsManager.getInstance().setObservationBucketType(dtConfiguration.getObservationBucketType());
        MetricsManager.getInstance().init(digitalTwinId);

        //Expose Prometheus Metrics through dedicated HTTP Server
        MetricsManager.getInstance().exposeHttpServer(dtConfiguration.getPrometheusHttpApiPort());
    }

    /**
     * Example configuration for the MQTT-to-MQTT WLDT Worker
     * @return
     */
    private static Mqtt2MqttConfiguration getMqttProtocolConfiguration(DigitalTwinConfiguration digitalTwinConfiguration){

        //Configuration associated to the MQTT experimental use-case available in the dedicated project
        //Demo Telemetry topic -> telemetry/com:iot:dummy:dummyMqttDevice001/resource/dummy_string_resource

        Mqtt2MqttConfiguration mqtt2MqttConfiguration = new Mqtt2MqttConfiguration();

        mqtt2MqttConfiguration.setBrokerAddress(digitalTwinConfiguration.getPhysicalMqttBrokerAddress());
        mqtt2MqttConfiguration.setBrokerPort(digitalTwinConfiguration.getPhysicalMqttBrokerPort());
        mqtt2MqttConfiguration.setDestinationBrokerAddress(digitalTwinConfiguration.getDigitalMqttBrokerAddress());
        mqtt2MqttConfiguration.setDestinationBrokerPort(digitalTwinConfiguration.getDigitalMqttBrokerPort());
        mqtt2MqttConfiguration.setDeviceId(digitalTwinConfiguration.getTargetDeviceId());

        mqtt2MqttConfiguration.setBrokerClientId(String.format("%s-%s",
                digitalTwinConfiguration.getDigitalTwinId(),
                "PhysicalBrokerTestClientId"));
        mqtt2MqttConfiguration.setDestinationBrokerClientId(String.format("%s-%s",
                digitalTwinConfiguration.getDigitalTwinId(),
                "DigitalBrokerTestClientId"));

        //Specify Topic List Configuration
        mqtt2MqttConfiguration.setTopicList(
                Arrays.asList(
                        new MqttTopicDescriptor(DEVICE_STATE_TOPIC_ID,
                                DEVICE_STATE_RESOURCE_ID,
                                "device/{{device_id}}/telemetry/{{resource_id}}",
                                MqttTopicDescriptor.MQTT_TOPIC_TYPE_DEVICE_OUTGOING),
                        new MqttTopicDescriptor(ENERGY_TOPIC_ID,
                                ENERGY_RESOURCE_ID,
                                "device/{{device_id}}/telemetry/{{resource_id}}",
                                MqttTopicDescriptor.MQTT_TOPIC_TYPE_DEVICE_OUTGOING),
                        new MqttTopicDescriptor(TEMPERATURE_TOPIC_ID,
                                TEMPERATURE_RESOURCE_ID,
                                "device/{{device_id}}/telemetry/{{resource_id}}",
                                MqttTopicDescriptor.MQTT_TOPIC_TYPE_DEVICE_OUTGOING)
                )
        );

        return mqtt2MqttConfiguration;
    }

    private static void startHttpMetricsApi(){

        try{

            Undertow server = Undertow.builder().addHttpListener(dtConfiguration.getHttpApiPort(), "0.0.0.0", getBasicRoutes()).build();
            server.start();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static final HttpHandler getBasicRoutes() {
        return new RoutingHandler()
                .add(Methods.GET, "/metrics", new HttpMetricsHandler())
                .add(Methods.GET, "/metrics/{folderId}", new HttpMetricsFolderHandler())
                .add(Methods.GET,"/metrics/{folderId}/{fileId}", new HttpMetricsSingleFileHandler())
                .add(Methods.GET, "/conf", httpConfigurationHandler)
                .add(Methods.PUT, "/conf", httpConfigurationHandler);
    }

    private static DigitalTwinConfiguration readConfigurationFile() {
        try{
            //ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            //File file = new File(classLoader.getResource(WLDT_CONFIGURATION_FILE).getFile());
            File file = new File(DT_CONFIGURATION_FILE);
            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            return om.readValue(file, DigitalTwinConfiguration.class);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


}
