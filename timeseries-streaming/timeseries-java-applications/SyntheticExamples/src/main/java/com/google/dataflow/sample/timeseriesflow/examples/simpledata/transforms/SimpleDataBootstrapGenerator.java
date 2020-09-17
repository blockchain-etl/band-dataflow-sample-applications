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
package com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms;

import com.google.api.services.bigquery.model.TableRow;
import com.google.dataflow.sample.timeseriesflow.AllComputationsExamplePipeline;
import com.google.dataflow.sample.timeseriesflow.ExampleTimeseriesPipelineOptions;
import com.google.dataflow.sample.timeseriesflow.TimeSeriesData.TSDataPoint;
import com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.domain.OracleRequest;
import com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.utils.JsonUtils;
import com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.utils.TimeUtils;
import com.google.dataflow.sample.timeseriesflow.io.tfexample.OutPutTFExampleToFile;
import com.google.dataflow.sample.timeseriesflow.io.tfexample.TSAccumIterableToTFExample;
import com.google.dataflow.sample.timeseriesflow.metrics.utils.AllMetricsWithDefaults;
import com.google.dataflow.sample.timeseriesflow.transforms.GenerateComputations;
import com.google.dataflow.sample.timeseriesflow.transforms.PerfectRectangles;
import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Instant;

import java.time.ZonedDateTime;

/**
 * This trivial example data is used only to demonstrate the end to end data engineering of the
 * library from, timeseries pre-processing to model creation using TFX.
 *
 * <p>The learning bootstrap data will predictably rise and fall with time.
 *
 * <p>Options that are required for the pipeline:
 */
public class SimpleDataBootstrapGenerator {

  public static void main(String[] args) {

    ExampleTimeseriesPipelineOptions options =
        PipelineOptionsFactory.fromArgs(args).as(ExampleTimeseriesPipelineOptions.class);

    /**
     * ***********************************************************************************************************
     * We hard code a few of the options for this sample application.
     * ***********************************************************************************************************
     */
    options.setAppName("SimpleDataBootstrapProcessTSDataPoints");
    options.setTypeOneComputationsLengthInSecs(600);
    options.setTypeTwoComputationsLengthInSecs(3600);
    options.setSequenceLengthInSeconds(3600);
    options.setRunner(DataflowRunner.class);
    options.setMaxNumWorkers(2);
//    options.setAbsoluteStopTimeMSTimestamp(now.plus(Duration.standardSeconds(43200)).getMillis());

    Pipeline p = Pipeline.create(options);

    /**
     * ***********************************************************************************************************
     * The data has only one key, to allow the type 1 computations to be done in parallel we set the
     * {@link GenerateComputations#hotKeyFanOut()}
     * ***********************************************************************************************************
     */
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
            .setTimeseriesSourceName("SimpleExample")
            .setGenerateComputations(generateComputations.build())
            .build();

    PCollection<TSDataPoint> data =
        p
            .apply(
                "ReadBigQuery",
                BigQueryIO.readTableRows()
                    .fromQuery("SELECT block_timestamp, oracle_request_id, decoded_result "
                        + "FROM `band-etl-dev.crypto_band.oracle_requests` "
                        + "WHERE request.oracle_script_id = 8 " 
                        + "   AND DATE(block_timestamp_truncated) <= '2020-09-16' " 
                        + "ORDER BY block_timestamp_truncated")
                    .withQueryPriority(BigQueryIO.TypedRead.QueryPriority.INTERACTIVE)
                    .usingStandardSql())
            .apply(ParDo.of(
                new DoFn<TableRow, TSDataPoint>() {
                  @ProcessElement
                  public void process(
                      @Element TableRow input,
                      OutputReceiver<TSDataPoint> o) {

                    // This is not efficient, convert from TableRow instead.
                    OracleRequest oracleRequest = JsonUtils.parseJson(JsonUtils.toJson(input), OracleRequest.class);
                    ZonedDateTime zonedDateTime = TimeUtils.parseDateTime(oracleRequest.getBlock_timestamp());
                    if (oracleRequest.getDecoded_result() != null &&
                        oracleRequest.getDecoded_result().getCalldata() != null &&
                        oracleRequest.getDecoded_result().getResult() != null) {
                      for (TSDataPoint tsDataPoint : OracleRequestMapper.convertOracleRequestToTSDataPoint(
                          oracleRequest)) {
                        o.outputWithTimestamp(tsDataPoint, Instant.ofEpochSecond(zonedDateTime.toEpochSecond()));
                      }
                    }
                  }
                }));

    
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
    data
        .apply(allComputationsExamplePipeline)
        .apply(new TSAccumIterableToTFExample())
        .apply(OutPutTFExampleToFile.create().withEnabledSingeWindowFile(false));

    p.run();
  }
}
