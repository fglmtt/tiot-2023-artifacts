package it.unimore.dipi.iot.digitaltwin;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project wldt-digital-twin-mqtt
 * @created 01/03/2022 - 17:30
 */
public class DigitalTwinConfiguration {

    public static final String OBSERVATION_BUCKET_TYPE_DEFAULT = "default";

    public static final String OBSERVATION_BUCKET_TYPE_LINEAR = "linear";

    private String digitalTwinId;

    private String targetDeviceId;

    private String physicalMqttBrokerAddress;

    private int physicalMqttBrokerPort;

    private String digitalMqttBrokerAddress;

    private int digitalMqttBrokerPort;

    private boolean metricsEnabled;

    private int httpApiPort;

    private int prometheusHttpApiPort;

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

    private double odteTargetPercentile;

    private double odteExpectedMsgSec;

    public DigitalTwinConfiguration() {
    }

    public String getDigitalTwinId() {
        return digitalTwinId;
    }

    public void setDigitalTwinId(String digitalTwinId) {
        this.digitalTwinId = digitalTwinId;
    }

    public String getTargetDeviceId() {
        return targetDeviceId;
    }

    public void setTargetDeviceId(String targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
    }

    public String getPhysicalMqttBrokerAddress() {
        return physicalMqttBrokerAddress;
    }

    public void setPhysicalMqttBrokerAddress(String physicalMqttBrokerAddress) {
        this.physicalMqttBrokerAddress = physicalMqttBrokerAddress;
    }

    public int getPhysicalMqttBrokerPort() {
        return physicalMqttBrokerPort;
    }

    public void setPhysicalMqttBrokerPort(int physicalMqttBrokerPort) {
        this.physicalMqttBrokerPort = physicalMqttBrokerPort;
    }

    public String getDigitalMqttBrokerAddress() {
        return digitalMqttBrokerAddress;
    }

    public void setDigitalMqttBrokerAddress(String digitalMqttBrokerAddress) {
        this.digitalMqttBrokerAddress = digitalMqttBrokerAddress;
    }

    public int getDigitalMqttBrokerPort() {
        return digitalMqttBrokerPort;
    }

    public void setDigitalMqttBrokerPort(int digitalMqttBrokerPort) {
        this.digitalMqttBrokerPort = digitalMqttBrokerPort;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
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

    public double getExpectedMsgSec() {
        return expectedMsgSec;
    }

    public void setExpectedMsgSec(double expectedMsgSec) {
        this.expectedMsgSec = expectedMsgSec;
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

    public int getPrimeNumbersComputationCount() {
        return primeNumbersComputationCount;
    }

    public void setPrimeNumbersComputationCount(int primeNumbersComputationCount) {
        this.primeNumbersComputationCount = primeNumbersComputationCount;
    }

    public String getObservationBucketType() {
        return observationBucketType;
    }

    public void setObservationBucketType(String observationBucketType) {
        this.observationBucketType = observationBucketType;
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

    public double getOdteTargetPercentile() {
        return odteTargetPercentile;
    }

    public void setOdteTargetPercentile(double odteTargetPercentile) {
        this.odteTargetPercentile = odteTargetPercentile;
    }

    public double getOdteExpectedMsgSec() {
        return odteExpectedMsgSec;
    }

    public void setOdteExpectedMsgSec(double odteExpectedMsgSec) {
        this.odteExpectedMsgSec = odteExpectedMsgSec;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DigitalTwinConfiguration{");
        sb.append("digitalTwinId='").append(digitalTwinId).append('\'');
        sb.append(", targetDeviceId='").append(targetDeviceId).append('\'');
        sb.append(", physicalMqttBrokerAddress='").append(physicalMqttBrokerAddress).append('\'');
        sb.append(", physicalMqttBrokerPort=").append(physicalMqttBrokerPort);
        sb.append(", digitalMqttBrokerAddress='").append(digitalMqttBrokerAddress).append('\'');
        sb.append(", digitalMqttBrokerPort=").append(digitalMqttBrokerPort);
        sb.append(", metricsEnabled=").append(metricsEnabled);
        sb.append(", httpApiPort=").append(httpApiPort);
        sb.append(", prometheusHttpApiPort=").append(prometheusHttpApiPort);
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
        sb.append(", odteTargetPercentile=").append(odteTargetPercentile);
        sb.append(", odteExpectedMsgSec=").append(odteExpectedMsgSec);
        sb.append('}');
        return sb.toString();
    }
}
