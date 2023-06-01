package it.unimore.dipi.iot.digitaltwin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.HTTPServer;
import it.unimore.dipi.iot.digitaltwin.odte.OdteResultDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project wldt-digital-twin-mqtt
 * @created 23/09/2022 - 16:03
 */
public class MetricsManager {

    private static final Logger logger = LoggerFactory.getLogger(MetricsManager.class);

    private static MetricsManager instance;

    private Histogram obervationDelayHistogram;

    private Gauge expectedMessageRateGauge;

    private Gauge physicalAssetUptimeGauge;

    private Gauge digitalTwinLifeCycleStateGauge;

    private Gauge odteTimelinessGauge;

    private Gauge odteReliabilityGauge;

    private Gauge odteAvailabilityGauge;

    private Gauge odteGauge;

    private HTTPServer httpServer;

    private String observationBucketType = DigitalTwinConfiguration.OBSERVATION_BUCKET_TYPE_DEFAULT;

    private double observationBucketStart = 0.0;

    private double observationBucketWidth = 0.005;

    private int observationBucketCount = 20;

    private long lastObservationTimestampMs = System.currentTimeMillis();

    private String digitalTwinId;

    private Cache<Long, Double> timelinessCache;

    private MetricsManager(){
    }

    public static MetricsManager getInstance(){

        if(instance == null)
            instance = new MetricsManager();

        return instance;
    }

    public void init(String digitalTwinId){

        this.digitalTwinId = digitalTwinId;

        logger.info("PrometheusManager -> Building Entanglement Histogram ...");

        if(this.observationBucketType.equals(DigitalTwinConfiguration.OBSERVATION_BUCKET_TYPE_LINEAR)) {

            logger.info("PrometheusManager -> Configuring LINEAR Histogram Bucket ...");

            obervationDelayHistogram = Histogram.build()
                    .name("dt_entanglement_observation_sec")
                    .help("Digital Twin Entanglement Observation [sec]")
                    .linearBuckets(
                            this.observationBucketStart,
                            this.observationBucketWidth,
                            this.observationBucketCount)
                    .register();
        }
        else{

            logger.info("PrometheusManager -> Configuring DEFAULT Histogram Bucket ...");

            obervationDelayHistogram = Histogram.build()
                    .name("dt_entanglement_observation_sec")
                    .help("Digital Twin Entanglement Observation [sec]")
                    .register();
        }

        logger.info("PrometheusManager -> Configuring Expected Messages Gauge ...");

        expectedMessageRateGauge = Gauge.build()
                .name("dt_entanglement_msg_rate_sec")
                .help("Digital Twin Entanglement Message Rate [msg/sec]")
                .register();

        logger.info("PrometheusManager -> Configuring Physical Asset Uptime Gauge ...");

        physicalAssetUptimeGauge = Gauge.build()
                .name("dt_entanglement_physical_asset_uptime")
                .help("Digital Twin Entanglement Physical Asset Uptime %")
                .register();

        logger.info("PrometheusManager -> Configuring Digital Twin Life Cycle State Gauge ...");

        digitalTwinLifeCycleStateGauge = Gauge.build()
                .name("dt_life_cycle_state")
                .help("Digital Twin Life Cycle")
                .labelNames("digital_twin_id")
                .register();

        //Initializing internal cache for ODTE Measurements
        timelinessCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();

        odteTimelinessGauge = Gauge.build()
                .name("dt_odte_timeliness")
                .help("Digital Twin ODTE Timeliness")
                .labelNames("digital_twin_id")
                .register();

        odteReliabilityGauge = Gauge.build()
                .name("dt_odte_reliability")
                .help("Digital Twin ODTE Reliability")
                .labelNames("digital_twin_id")
                .register();

        odteAvailabilityGauge = Gauge.build()
                .name("dt_odte_availability")
                .help("Digital Twin ODTE Availability")
                .labelNames("digital_twin_id")
                .register();

        odteGauge = Gauge.build()
                .name("dt_odte_value")
                .help("Digital Twin ODTE Value")
                .labelNames("digital_twin_id")
                .register();
    }

    /*
    //Old Version where we use the current timestamp for the cache instead of the original physical value
    public void addObservationDelaySec(double newObservationDelaySec){

        long currentTimestampMs = System.currentTimeMillis();

        //TODO Remove this last observation ?
        lastObservationTimestampMs = currentTimestampMs;

        if(obervationDelayHistogram != null)
            obervationDelayHistogram.observe(newObservationDelaySec);

        if(timelinessCache != null)
            timelinessCache.put(currentTimestampMs, newObservationDelaySec);
    }
    */

    public void addObservationDelaySec(long packetTimestamp, double newObservationDelaySec){

        //long currentTimestampMs = System.currentTimeMillis();

        //TODO Remove this last observation ?
        lastObservationTimestampMs = System.currentTimeMillis();;

        if(obervationDelayHistogram != null)
            obervationDelayHistogram.observe(newObservationDelaySec);

        if(timelinessCache != null)
            timelinessCache.put(packetTimestamp, newObservationDelaySec);
    }

    public void setExpectedMessageRateValue(double newExpectedMessageRateValue){
        if(expectedMessageRateGauge != null)
            expectedMessageRateGauge.set(newExpectedMessageRateValue);
    }

    public void setPhysicalAssetUptimeValue(double newPhysicalAssetUptimeValue){
        if(physicalAssetUptimeGauge != null)
            physicalAssetUptimeGauge.set(newPhysicalAssetUptimeValue);
    }

    public void setDigitalTwinLifeCycleStateValue(DigitalTwinLifeCycleState digitalTwinLifeCycleState){
        if(digitalTwinLifeCycleStateGauge != null)
            digitalTwinLifeCycleStateGauge.labels(this.digitalTwinId).set(digitalTwinLifeCycleState.getValue());
    }

    public void setDigitalTwinOdteValue(OdteResultDescription odteResultDescription){
        if(odteResultDescription != null) {
            odteTimelinessGauge.labels(this.digitalTwinId).set(odteResultDescription.getTimeliness());
            odteReliabilityGauge.labels(this.digitalTwinId).set(odteResultDescription.getReliability());
            odteAvailabilityGauge.labels(this.digitalTwinId).set(odteResultDescription.getAvailability());
            odteGauge.labels(this.digitalTwinId).set(odteResultDescription.getOdte());
        }
    }

    public Histogram getObervationDelayHistogram() {
        return obervationDelayHistogram;
    }

    public Gauge getExpectedMessageRateGauge() {
        return expectedMessageRateGauge;
    }

    public Gauge getPhysicalAssetUptimeGauge() {
        return physicalAssetUptimeGauge;
    }

    public int getDigitalTwinLifeCycleStateGaugeValue() {
        return (int)digitalTwinLifeCycleStateGauge.labels(this.digitalTwinId).get();
    }

    public void exposeHttpServer(int targetPort){
        try{

            logger.info("EntanglementManager -> Exposing HTTP Server on Port {} ...", targetPort);

            httpServer = new HTTPServer.Builder()
                    .withPort(targetPort)
                    .build();

            logger.info("EntanglementManager -> HTTP Server Build !");

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void stopHttpServer(){
        try{
            if(httpServer != null){
                logger.info("EntanglementManager -> Stopping HTTP Server ...");
                httpServer.close();
                logger.info("EntanglementManager -> HTTP Server Stopped");
            }
            else
                logger.error("EntanglementManager -> Error stopping HTTP Server ! Server Object = null !");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public Cache<Long, Double> getTimelinessCache() {
        return timelinessCache;
    }

    public double getObservationBucketStart() {
        return observationBucketStart;
    }

    public void setObservationBucketStart(double observationBucketStart) {
        this.observationBucketStart = observationBucketStart;
    }

    public double getObservationBucketWidth() {
        return observationBucketWidth;
    }

    public void setObservationBucketWidth(double observationBucketWidth) {
        this.observationBucketWidth = observationBucketWidth;
    }

    public int getObservationBucketCount() {
        return observationBucketCount;
    }

    public void setObservationBucketCount(int observationBucketCount) {
        this.observationBucketCount = observationBucketCount;
    }

    public String getObservationBucketType() {
        return observationBucketType;
    }

    public void setObservationBucketType(String observationBucketType) {
        this.observationBucketType = observationBucketType;
    }

    public long getLastObservationTimestampMs() {
        return lastObservationTimestampMs;
    }
}
