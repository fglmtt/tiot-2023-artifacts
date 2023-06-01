package it.unimore.dipi.iot.digitaltwin.conf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project wldt-digital-twin-mqtt
 * @created 01/03/2022 - 17:30
 */
public class ComposedDigitalTwinConfiguration {


    public static final String OBSERVATION_BUCKET_TYPE_DEFAULT = "default";

    public static final String OBSERVATION_BUCKET_TYPE_LINEAR = "linear";

    private String digitalTwinId;

    private String deviceIdList;

    private String targetMqttBrokerAddress;

    private int targetMqttBrokerPort;

    private boolean metricsEnabled;

    private boolean aggregateWithStateMessages;

    private int httpApiPort;

    private int prometheusHttpApiPort;

    private Map<String, String> resourceMap;

    private String aggregationAction;
    private String aggregationType;

    private String aggregationUnit;

    private int aggregationWindow;

    private String aggregationStateTopic;

    private double expectedMsgSec;

    private String observationBucketType = OBSERVATION_BUCKET_TYPE_DEFAULT;

    private double observationBucketStart;

    private double observationBucketWidth;

    private int observationBucketCount;

    private int primeNumbersComputationCount;

    private long shadowedThresholdMs;

    private long unBoundThresholdMs;

    private int odteSlidingWindowSec;

    private double odteDesiredTimelinessSec;

    private double odteExpectedMsgSec;

    private double odteTargetPercentile;

    public ComposedDigitalTwinConfiguration() {
    }

    public String getDigitalTwinId() {
        return digitalTwinId;
    }

    public void setDigitalTwinId(String digitalTwinId) {
        this.digitalTwinId = digitalTwinId;
    }

    public String getDeviceIdList() {
        return deviceIdList;
    }

    public void setDeviceIdList(String deviceIdList) {
        this.deviceIdList = deviceIdList;
    }

    public String getTargetMqttBrokerAddress() {
        return targetMqttBrokerAddress;
    }

    public void setTargetMqttBrokerAddress(String targetMqttBrokerAddress) {
        this.targetMqttBrokerAddress = targetMqttBrokerAddress;
    }

    public int getTargetMqttBrokerPort() {
        return targetMqttBrokerPort;
    }

    public void setTargetMqttBrokerPort(int targetMqttBrokerPort) {
        this.targetMqttBrokerPort = targetMqttBrokerPort;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public boolean isAggregateWithStateMessages() {
        return aggregateWithStateMessages;
    }

    public void setAggregateWithStateMessages(boolean aggregateWithStateMessages) {
        this.aggregateWithStateMessages = aggregateWithStateMessages;
    }

    public List<String> getDeviceIdentifierList(){

        if(this.deviceIdList != null){

            String[] stringList = deviceIdList.split(",");
            return new ArrayList<>(Arrays.asList(stringList));
        }else
            return new ArrayList<>();
    }

    public int getHttpApiPort() {
        return httpApiPort;
    }

    public void setHttpApiPort(int httpApiPort) {
        this.httpApiPort = httpApiPort;
    }

    public int getPrometheusHttpApiPort() {
        return prometheusHttpApiPort;
    }

    public void setPrometheusHttpApiPort(int prometheusHttpApiPort) {
        this.prometheusHttpApiPort = prometheusHttpApiPort;
    }

    public Map<String, String> getResourceMap() {
        return resourceMap;
    }

    public void setResourceMap(Map<String, String> resourceMap) {
        this.resourceMap = resourceMap;
    }

    public String getAggregationAction() {
        return aggregationAction;
    }

    public void setAggregationAction(String aggregationAction) {
        this.aggregationAction = aggregationAction;
    }

    public String getAggregationType() {
        return aggregationType;
    }

    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
    }

    public String getAggregationUnit() {
        return aggregationUnit;
    }

    public void setAggregationUnit(String aggregationUnit) {
        this.aggregationUnit = aggregationUnit;
    }

    public int getAggregationWindow() {
        return aggregationWindow;
    }

    public void setAggregationWindow(int aggregationWindow) {
        this.aggregationWindow = aggregationWindow;
    }

    public String getAggregationStateTopic() {
        return aggregationStateTopic;
    }

    public void setAggregationStateTopic(String aggregationStateTopic) {
        this.aggregationStateTopic = aggregationStateTopic;
    }

    public int getPrimeNumbersComputationCount() {
        return primeNumbersComputationCount;
    }

    public void setPrimeNumbersComputationCount(int primeNumbersComputationCount) {
        this.primeNumbersComputationCount = primeNumbersComputationCount;
    }

    public long getShadowedThresholdMs() {
        return shadowedThresholdMs;
    }

    public void setShadowedThresholdMs(long shadowedThresholdMs) {
        this.shadowedThresholdMs = shadowedThresholdMs;
    }

    public long getUnBoundThresholdMs() {
        return unBoundThresholdMs;
    }

    public void setUnBoundThresholdMs(long unBoundThresholdMs) {
        this.unBoundThresholdMs = unBoundThresholdMs;
    }

    public int getOdteSlidingWindowSec() {
        return odteSlidingWindowSec;
    }

    public void setOdteSlidingWindowSec(int odteSlidingWindowSec) {
        this.odteSlidingWindowSec = odteSlidingWindowSec;
    }

    public double getOdteDesiredTimelinessSec() {
        return odteDesiredTimelinessSec;
    }

    public void setOdteDesiredTimelinessSec(double odteDesiredTimelinessSec) {
        this.odteDesiredTimelinessSec = odteDesiredTimelinessSec;
    }

    public double getOdteExpectedMsgSec() {
        return odteExpectedMsgSec;
    }

    public void setOdteExpectedMsgSec(double odteExpectedMsgSec) {
        this.odteExpectedMsgSec = odteExpectedMsgSec;
    }

    public double getOdteTargetPercentile() {
        return odteTargetPercentile;
    }

    public void setOdteTargetPercentile(double odteTargetPercentile) {
        this.odteTargetPercentile = odteTargetPercentile;
    }

    public double getExpectedMsgSec() {
        return expectedMsgSec;
    }

    public void setExpectedMsgSec(double expectedMsgSec) {
        this.expectedMsgSec = expectedMsgSec;
    }

    public String getObservationBucketType() {
        return observationBucketType;
    }

    public void setObservationBucketType(String observationBucketType) {
        this.observationBucketType = observationBucketType;
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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ComposedDigitalTwinConfiguration{");
        sb.append("digitalTwinId='").append(digitalTwinId).append('\'');
        sb.append(", deviceIdList='").append(deviceIdList).append('\'');
        sb.append(", targetMqttBrokerAddress='").append(targetMqttBrokerAddress).append('\'');
        sb.append(", targetMqttBrokerPort=").append(targetMqttBrokerPort);
        sb.append(", metricsEnabled=").append(metricsEnabled);
        sb.append(", aggregateWithStateMessages=").append(aggregateWithStateMessages);
        sb.append(", httpApiPort=").append(httpApiPort);
        sb.append(", prometheusHttpApiPort=").append(prometheusHttpApiPort);
        sb.append(", resourceMap=").append(resourceMap);
        sb.append(", aggregationAction='").append(aggregationAction).append('\'');
        sb.append(", aggregationType='").append(aggregationType).append('\'');
        sb.append(", aggregationUnit='").append(aggregationUnit).append('\'');
        sb.append(", aggregationWindow=").append(aggregationWindow);
        sb.append(", aggregationStateTopic='").append(aggregationStateTopic).append('\'');
        sb.append(", expectedMsgSec=").append(expectedMsgSec);
        sb.append(", observationBucketType='").append(observationBucketType).append('\'');
        sb.append(", observationBucketStart=").append(observationBucketStart);
        sb.append(", observationBucketWidth=").append(observationBucketWidth);
        sb.append(", observationBucketCount=").append(observationBucketCount);
        sb.append(", primeNumbersComputationCount=").append(primeNumbersComputationCount);
        sb.append(", shadowedThresholdMs=").append(shadowedThresholdMs);
        sb.append(", unBoundThresholdMs=").append(unBoundThresholdMs);
        sb.append(", odteSlidingWindowSec=").append(odteSlidingWindowSec);
        sb.append(", odteDesiredTimelinessSec=").append(odteDesiredTimelinessSec);
        sb.append(", odteExpectedMsgSec=").append(odteExpectedMsgSec);
        sb.append(", odteTargetPercentile=").append(odteTargetPercentile);
        sb.append('}');
        return sb.toString();
    }
}
