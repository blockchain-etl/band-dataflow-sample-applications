package io.blockchainetl.band.examples.simpledata.transforms;

import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSAccum;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSAccumSequence;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSDataPoint;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSKey;
import com.google.dataflow.sample.timeseriesflow.combiners.typeone.TSNumericCombiner;
import com.google.dataflow.sample.timeseriesflow.metrics.BB;
import com.google.dataflow.sample.timeseriesflow.metrics.MA;
import com.google.dataflow.sample.timeseriesflow.metrics.RSI;
import org.apache.beam.sdk.annotations.Experimental;
import org.apache.beam.sdk.transforms.Combine.CombineFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.collect.ImmutableList;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wrapper class used to deploy pipelines with all available metrics. Currently includes:
 *
 * <p>Type 1 {@link TSNumericCombiner}
 *
 * <p>Type 2 {@link RSI},{@link MA},{@link BB}
 */
@Experimental
public class BandMetrics {

  public static List<CombineFn<TSDataPoint, TSAccum, TSAccum>> getAllType1Combiners() {
    return ImmutableList.of(new TSNumericCombiner());
  }

  public static ImmutableList<
          PTransform<PCollection<KV<TSKey, TSAccumSequence>>, PCollection<KV<TSKey, TSAccum>>>>
      getAllType2Computations() {
    return ImmutableList.of(
        RSI.toBuilder()
            .setAverageComputationMethod(RSI.AverageComputationMethod.ALL)
            .build()
            .create(),
        MA.toBuilder()
            .setAverageComputationMethod(MA.AverageComputationMethod.SIMPLE_MOVING_AVERAGE)
            .build()
            .create(),
        MA.toBuilder()
            .setAverageComputationMethod(MA.AverageComputationMethod.EXPONENTIAL_MOVING_AVERAGE)
            .setWeight(BigDecimal.valueOf(2D / (3D + 1D)))
            .build()
            .create(),
        BB.toBuilder()
            .setAverageComputationMethod(BB.AverageComputationMethod.EXPONENTIAL_MOVING_AVERAGE)
            .setWeight(BigDecimal.valueOf(2D / (3D + 1D)))
            .setDevFactor(2)
            .build()
            .create(),
        BB.toBuilder()
            .setAverageComputationMethod(BB.AverageComputationMethod.SIMPLE_MOVING_AVERAGE)
            .setDevFactor(2)
            .build()
            .create());
  }
}
