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
import com.google.dataflow.sample.timeseriesflow.io.tfexample.OutPutTFExampleFromTSSequence;
import com.google.dataflow.sample.timeseriesflow.metrics.utils.AllMetricsGeneratorWithDefaults;
import com.google.dataflow.sample.timeseriesflow.transforms.GenerateComputations;
import com.google.dataflow.sample.timeseriesflow.transforms.PerfectRectangles;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

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
    
    /**
     * ***********************************************************************************************************
     * We want to ensure that there is always a value within each timestep. This is redundent for
     * this dataset as the generated data will always have a value. But we keep this configuration
     * to ensure consistency accross the sample pipelines.
     * ***********************************************************************************************************
     */
    PerfectRectangles perfectRectangles =
        PerfectRectangles.builder()
            .setEnableHoldAndPropogate(false)
            .setFixedWindow(Duration.standardMinutes(10))
            .setTtlDuration(Duration.standardMinutes(60))
            .build();

    /**
     * ***********************************************************************************************************
     * The data has only one key, to allow the type 1 computations to be done in parrallal we set
     * the {@link GenerateComputations#hotKeyFanOut()}
     * ***********************************************************************************************************
     */
    GenerateComputations generateComputations =
        AllMetricsGeneratorWithDefaults.getGenerateComputationsWithAllKnownMetrics()
            .setType1FixedWindow(Duration.standardMinutes(10))
            .setType2SlidingWindowDuration(Duration.standardMinutes(60))
            .setHotKeyFanOut(5)
            .setPerfectRectangles(perfectRectangles)
            .build();

    ExampleTimeseriesPipelineOptions options =
        PipelineOptionsFactory.fromArgs(args).as(ExampleTimeseriesPipelineOptions.class);

    /**
     * ***********************************************************************************************************
     * We hard code a few of the options for this sample application.
     * ***********************************************************************************************************
     */
    options.setAppName("SimpleDataBootstrapProcessTSDataPoints");
    options.setTypeOneComputationsLengthInSecs(1);
    options.setTypeTwoComputationsLengthInSecs(5);
    options.setSequenceLengthInSeconds(5);
//    options.setRunner(DataflowRunner.class);
//    options.setMaxNumWorkers(2);

    Pipeline p = Pipeline.create(options);

    
    PCollection<TSDataPoint> data =
        p
            .apply(
                "ReadBigQuery",
                BigQueryIO.readTableRows()
                    .fromQuery("SELECT block_timestamp, oracle_request_id, decoded_result " 
                        + "FROM `band-etl-dev.crypto_band.oracle_requests` " 
                        + "WHERE request.oracle_script_id = 8 ")
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
                    if (oracleRequest.getDecoded_result() != null &&
                        oracleRequest.getDecoded_result().getCalldata() != null &&
                        oracleRequest.getDecoded_result().getResult() != null) {
                      for (TSDataPoint tsDataPoint : OracleRequestMapper.convertOracleRequestToTSDataPoint(
                          oracleRequest)) {
                        o.output(tsDataPoint);
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
        .apply(
            AllComputationsExamplePipeline.builder()
                .setTimeseriesSourceName("SimpleExample")
                .setOutputToBigQuery(false)
                .setGenerateComputations(generateComputations)
                .build())
        .apply(OutPutTFExampleFromTSSequence.create().withEnabledSingeWindowFile(false));

    p.run();
  }
}
