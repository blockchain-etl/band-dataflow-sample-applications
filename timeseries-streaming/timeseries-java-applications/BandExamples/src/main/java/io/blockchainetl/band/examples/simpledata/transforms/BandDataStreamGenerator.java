/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.blockchainetl.band.examples.simpledata.transforms;

import com.google.dataflow.sample.timeseriesflow.AllComputationsExamplePipeline;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSAccumSequence;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSDataPoint;
import com.google.dataflow.sample.timeseriesflow.io.tfexample.OutPutTFExampleToFile;
import com.google.dataflow.sample.timeseriesflow.io.tfexample.OutPutTFExampleToPubSub;
import com.google.dataflow.sample.timeseriesflow.io.tfexample.TSAccumIterableToTFExample;
import com.google.dataflow.sample.timeseriesflow.metrics.utils.AllMetricsWithDefaults;
import com.google.dataflow.sample.timeseriesflow.transforms.GenerateComputations;
import com.google.dataflow.sample.timeseriesflow.transforms.PerfectRectangles;
import io.blockchainetl.band.examples.simpledata.transforms.domain.OracleRequest;
import io.blockchainetl.band.examples.simpledata.transforms.utils.JsonUtils;
import io.blockchainetl.band.examples.simpledata.transforms.utils.TimeUtils;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tensorflow.example.Example;

import java.time.ZonedDateTime;

import static io.blockchainetl.band.examples.simpledata.transforms.utils.BandUtils.setDefaultOptions;

/**
 * This simple example data is used only to demonstrate the end to end data engineering of the
 * library from, timeseries pre-processing to model creation using TFX.
 */
public class BandDataStreamGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(BandDataStreamGenerator.class);

  private static final String PUBSUB_ID_ATTRIBUTE = "item_id";

  public static void main(String[] args) {

    /**
     * ***********************************************************************************************************
     * We hard code a few of the options for this sample application.
     * ***********************************************************************************************************
     */
    BandDataOptions options = PipelineOptionsFactory.fromArgs(args).as(BandDataOptions.class);

    options.setAppName("BandDataStreamTSDataPoints");
    setDefaultOptions(options);

    Pipeline p = Pipeline.create(options);

    /**
     * ***********************************************************************************************************
     * Generate trivial data points that follow a very simple pattern. Over 12 hours the value will
     * cycle through 0 up and then back to 0. There will be a tick every 500 ms.
     * ***********************************************************************************************************
     */

    PCollection<TSDataPoint> stream =
        p.apply("PubSubListener", PubsubIO.readStrings()
            .fromSubscription(options.getPubSubSubscriptionForOracleRequests())
            .withIdAttribute(PUBSUB_ID_ATTRIBUTE))
            .apply(
                ParDo.of(
                    new DoFn<String, TSDataPoint>() {
                      @ProcessElement
                      public void process(
                          @Element String input,
                          OutputReceiver<TSDataPoint> o) {

                        OracleRequest oracleRequest = JsonUtils.parseJson(input, OracleRequest.class);
                        ZonedDateTime zonedDateTime = TimeUtils.parseDateTime(oracleRequest.getBlock_timestamp());
                        boolean skip = false;
                        if (options.getTimestampThreshold() != null) {
                          ZonedDateTime thresholdZonedDateTime = TimeUtils.parseDateTime(options.getTimestampThreshold());
                          
                          if (zonedDateTime.isBefore(thresholdZonedDateTime)) {
                            skip = true;
                          }
                        }
                        
                        if (!skip) {
                          for (TSDataPoint tsDataPoint : OracleRequestMapper.convertOracleRequestToTSDataPoints(
                              oracleRequest)) {
                            o.outputWithTimestamp(tsDataPoint, Instant.ofEpochSecond(zonedDateTime.toEpochSecond()));
                          }
                        }
                      }

                      @Override
                      public Duration getAllowedTimestampSkew() {
                        return Duration.standardMinutes(5);
                      }
                    }));

    GenerateComputations.Builder generateComputations =
        GenerateComputations.fromPiplineOptions(options)
            .setType1NumericComputations(AllMetricsWithDefaults.getAllType1Combiners())
            .setType2NumericComputations(AllMetricsWithDefaults.getAllType2Computations());

    /**
     * ***********************************************************************************************************
     * We want to ensure that there is always a value within each timestep. This is redundant for
     * this dataset as the generated data will always have a value. But we keep this configuration
     * to ensure consistency across the sample pipelines.
     * ***********************************************************************************************************
     */
    generateComputations.setPerfectRectangles(PerfectRectangles.fromPipelineOptions(options));

    /**
     * ***********************************************************************************************************
     * All the metrics currently available will be processed for this dataset. The results will be
     * sent to two difference locations: To Google BigQuery as defined by: {@link
     * ExampleTimeseriesPipelineOptions#getBigQueryTableForTSAccumOutputLocation()} To a Google
     * Cloud Storage Bucket as defined by: {@link
     * ExampleTimeseriesPipelineOptions#getInterchangeLocation()} The TFExamples will be grouped
     * into TFRecord files as {@link OutPutTFExampleFromTSSequence#enableSingleWindowFile} is set to
     * false.
     *
     * <p>***********************************************************************************************************
     */
    AllComputationsExamplePipeline allComputationsExamplePipeline =
        AllComputationsExamplePipeline.builder()
            .setTimeseriesSourceName("BandExample")
            .setGenerateComputations(generateComputations.build())
            .build();

    PCollection<Iterable<TSAccumSequence>> metrics = stream.apply(allComputationsExamplePipeline);

    /**
     * ***********************************************************************************************************
     *
     * <p>If print metrics is enabled, output all the type 1 and type 2 metrics that have been
     * generated.
     *
     * <p>***********************************************************************************************************
     */
    if (options.getEnablePrintMetricsToLogs()) {
      metrics.apply(new Print<>());
    }

    /**
     * ***********************************************************************************************************
     *
     * <p>Files will be output to the file location specified.
     *
     * <p>Note : For processing of bootstrap history, do not use {@link
     * OutPutTFExampleFromTSSequence#withEnabledSingeWindowFile(boolean)} set to true. This causes
     * output file per type 1 window, which is inefficient when processing history of file, but fine
     * in stream mode.
     *
     * <p>***********************************************************************************************************
     */
    if (options.getInterchangeLocation() != null) {
      PCollection<Example> examples = metrics.apply(new TSAccumIterableToTFExample());
      System.out.println(
          String.format("Running Example , with output to %s", options.getInterchangeLocation()));
      examples.apply(OutPutTFExampleToFile.create());

      if (options.getEnablePrintTFExamplesToLogs()) {
        examples.apply(new Print<>());
      }
    }

    /**
     * ***********************************************************************************************************
     *
     * <p>TF.Example protos will be sent to PubSub as JSON if the PubSub output location is
     * provided.
     *
     * <p>***********************************************************************************************************
     */
    if (options.getPubSubTopicForTSAccumOutputLocation() != null) {
      PCollection<Example> examples = metrics.apply(new TSAccumIterableToTFExample());
      System.out.println(
          String.format(
              "Running Example , with output to %s",
              options.getPubSubTopicForTSAccumOutputLocation()));
      examples.apply(
          OutPutTFExampleToPubSub.create(options.getPubSubTopicForTSAccumOutputLocation()));

      if (options.getEnablePrintTFExamplesToLogs()) {
        examples.apply(new Print<>());
      }
    }

    p.run();
  }
}
