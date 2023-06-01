package it.unimore.dipi.iot.digitaltwin.odte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project wldt-digital-twin-mqtt
 * @created 08/05/2023 - 13:45
 */
public class OdteManager {

    private static final Logger logger = LoggerFactory.getLogger(OdteManager.class);

    public static final double[] DEFAULT_BUCKETS = {
            0.005d, 0.01d, 0.025d, 0.05d, 0.075d, 0.1d, 0.25d, 0.5d, 0.75d, 1.0d, 2.5d, 5.0d, 7.5d, 10.0d, Double.POSITIVE_INFINITY
    };

    private static OdteManager instance;

    private OdteManager(){
    }

    public static OdteManager getInstance(){

        if(instance == null)
            instance = new OdteManager();

        return instance;
    }

    public Optional<OdteResultDescription> computeOdte(List<Double> samplesList, double desiredTimelinessSec, double expectedMsgSec, double targetSlidingWindowSec){


        Optional<Double> timelinessOptional = computeTimeliness(samplesList, desiredTimelinessSec);
        Optional<Double> reliabilityOptional = computeReliability(samplesList, expectedMsgSec, targetSlidingWindowSec);
        Optional<Double> availabilityOptional = computeAvailability();

        if(reliabilityOptional.isPresent() && availabilityOptional.isPresent() && timelinessOptional.isPresent())
            return Optional.of(
                    new OdteResultDescription(
                            timelinessOptional.get(),
                            reliabilityOptional.get(),
                            availabilityOptional.get(),
                            reliabilityOptional.get() * availabilityOptional.get() * timelinessOptional.get())
            );
        else
            return Optional.empty();
    }

    private Optional<Double> computeReliability(List<Double> samplesList, double expectedMsgSec, double targetSlidingWindowSec){

        double receivedPackets = samplesList.size();
        double expectedPackets = expectedMsgSec * targetSlidingWindowSec;

        if(expectedPackets == 0.0){
            logger.error("Error computing Reliability ! Expected Packets ({}msg/sec*{}sec) = 0.0 !", expectedPackets, targetSlidingWindowSec);
            return Optional.empty();
        }
        else if(receivedPackets == 0.0)
            return Optional.of(0.0);
        else
            return Optional.of(receivedPackets/expectedPackets);
    }

    private Optional<Double> computeAvailability(){
        return Optional.of(1.0);
    }

    private Optional<Double> computeTimeliness(List<Double> samplesList, double desiredTimeliness){
        double[] observations = samplesList.stream().mapToDouble(Double::doubleValue).toArray();
        return computeTimeliness(observations, desiredTimeliness);
    }

    private Optional<Double> computeTimeliness(double[] observationsSec, double desiredTimelinessSec){

        try {

            // sort the observations in ascending order
            Arrays.sort(observationsSec);

            // iterate through the observations and find the index at which the desired latency belongs
            int index = -1;
            for (int i = 0; i < observationsSec.length; i++) {
                if (observationsSec[i] >= desiredTimelinessSec) {
                    index = i;
                    break;
                }
            }

            double percentile = 0.0;

            // if the desired latency is greater than the maximum observed latency, return 100 percentile
            if (index == -1)
                percentile = 1.0;
            else
                percentile = (double) (index + 1) / (double) observationsSec.length;

            logger.info("Percentile of {} Sec -> Percentile Result: {}", desiredTimelinessSec, percentile);

            return Optional.of(percentile);

        }catch (Exception e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

//    private Optional<Double> method1(double[] observationsSec, double desiredTimelinessSec){
//        try{
//
//            logger.info("Target Buckets: {}", DEFAULT_BUCKETS);
//
//            int numBuckets = DEFAULT_BUCKETS.length;
//
//            double[] bucketCounts = new double[numBuckets];
//
//            for (double obs : observationsSec) {
//                int bucketIndex = 0;
//                while (bucketIndex < numBuckets - 1 && obs > DEFAULT_BUCKETS[bucketIndex])
//                    bucketIndex++;
//                bucketCounts[bucketIndex]++;
//            }
//            double[] cdf = new double[numBuckets];
//            double sum = 0;
//
//            for (int i = 0; i < numBuckets; i++) {
//                sum += bucketCounts[i];
//                cdf[i] = sum / observationsSec.length;
//            }
//
//            double percentile = 0.0;
//            int bucketIndex = 0;
//
//            while (bucketIndex < numBuckets - 1 && desiredTimelinessSec >= DEFAULT_BUCKETS[bucketIndex])
//                bucketIndex++;
//
//            if (bucketIndex == 0)
//                percentile = cdf[0] * (desiredTimelinessSec / DEFAULT_BUCKETS[0]);
//            else {
//                double prevBucketCount = bucketCounts[bucketIndex - 1];
//                double prevBucketEnd = DEFAULT_BUCKETS[bucketIndex - 1];
//                double currBucketCount = bucketCounts[bucketIndex];
//                double currBucketStart = DEFAULT_BUCKETS[bucketIndex];
//                double deltaObs = desiredTimelinessSec - prevBucketEnd;
//                double deltaBucket = currBucketStart - prevBucketEnd;
//                double prevBucketPercentile = cdf[bucketIndex - 1];
//                percentile = prevBucketPercentile + ((deltaObs / deltaBucket) * (currBucketCount / observationsSec.length + prevBucketCount / observationsSec.length) / 2.0);
//            }
//
//            logger.info("Percentile of {} Sec -> Percentile Result: {}", desiredTimelinessSec, percentile);
//
//            return Optional.of(percentile);
//
//        }catch (Exception e){
//            e.printStackTrace();
//            return Optional.empty();
//        }
//    }
//
//
//    public static double getTargetPercentile(double[] observations, double desiredTimeliness) {
//        double[] DEFAULT_BUCKETS = {
//                0.005d, 0.01d, 0.025d, 0.05d, 0.075d, 0.1d, 0.25d, 0.5d, 0.75d, 1.0d, 2.5d, 5.0d, 7.5d, 10.0d, Double.POSITIVE_INFINITY
//        };
//
//        double[] sortedObservations = Arrays.stream(observations).sorted().toArray();
//        int n = sortedObservations.length;
//        double totalDuration = sortedObservations[n - 1] - sortedObservations[0];
//        double percentileSum = 0;
//        double lastDuration = 0;
//
//        for (int i = 0, j = 0; i < DEFAULT_BUCKETS.length && j < n; i++) {
//            double currentBucket = DEFAULT_BUCKETS[i];
//            double duration = currentBucket - lastDuration;
//            while (j < n && sortedObservations[j] - sortedObservations[0] <= currentBucket) {
//                j++;
//            }
//            double count = j;
//            double percentile = (totalDuration - duration * count) / totalDuration;
//            percentileSum += percentile * duration;
//            lastDuration = currentBucket;
//        }
//
//        double targetPercentile = 1.0;
//        double currentPercentile = 0.0;
//        double lastBucket = 0.0;
//        for (int i = 0; i < DEFAULT_BUCKETS.length; i++) {
//            double currentBucket = DEFAULT_BUCKETS[i];
//            double duration = currentBucket - lastBucket;
//            double percentile = percentileSum / totalDuration;
//            currentPercentile += percentile * duration;
//            if (currentPercentile >= desiredTimeliness) {
//                targetPercentile = currentBucket;
//                break;
//            }
//            lastBucket = currentBucket;
//        }
//
//        return targetPercentile;
//    }
//
//    public static double computePercentile(double[] observations, double desiredLatency) {
//        // sort the observations in ascending order
//        Arrays.sort(observations);
//
//        // iterate through the observations and find the index at which the desired latency belongs
//        int index = -1;
//        for (int i = 0; i < observations.length; i++) {
//            if (observations[i] >= desiredLatency) {
//                index = i;
//                break;
//            }
//        }
//
//        // if the desired latency is greater than the maximum observed latency, return 100 percentile
//        if (index == -1) {
//            return 100.0;
//        }
//
//        // calculate the percentile based on the index
//        double percentile = (double) (index + 1) / (double) observations.length;
//        return percentile;
//    }

    public static void main(String[] args) {

        double DESIRED_TIMELINESS_SEC = 0.5;
        //double[] observations = {0.018, 0.019, 0.013, 0.041, 0.02, 0.015, 0.019, 0.016, 0.014, 0.018, 0.016, 0.019, 0.019, 0.019, 0.017, 0.019, 0.018, 0.016, 0.02, 0.014, 0.02, 0.015, 0.016, 0.02, 0.013, 0.017, 0.016, 0.019, 0.018, 0.014};
        //double[] observations = {0.918, 0.019, 0.013, 0.341, 0.02, 0.015, 0.919, 0.016, 0.014, 0.018, 0.016, 0.019, 0.019, 0.019, 0.017, 0.019, 0.018, 0.016, 0.02, 0.014, 0.02, 0.015, 0.016, 0.02, 0.013, 0.017, 0.016, 0.019, 0.018, 0.014};
        double[] observations = {0.018, 0.019, 0.013, 1.041, 1.02, 1.015, 0.019, 1.016, 1.014, 0.018, 0.016, 0.019, 0.019, 0.019, 0.017, 0.019, 1.018, 0.016, 0.02, 0.014, 0.02, 0.015, 0.016, 0.02, 0.013, 0.017, 0.016, 0.019, 0.018, 0.014};

        OdteManager.getInstance().computeTimeliness(observations, DESIRED_TIMELINESS_SEC);
    }
}
