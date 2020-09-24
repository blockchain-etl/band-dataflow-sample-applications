# Quickstart

This repository contains code for running [Dataflow](https://cloud.google.com/dataflow) pipelines for processing 
public [Band Protocol](https://bandprotocol.com) data in Google Cloud Platform.  

- **Metrics calculation** - price feeds from [Aggregator](https://guanyu-poa.cosmoscan.io/oracle-script/8) oracle script 
    are aggregated into windows and the following metrics are calculated for each symbol:
    - Open, High, Low, Close (OHLC)
    - Simple Moving Average (SMA)
    - Exponential Moving Average (EMA)
    - Relative Strength (RS)
    - Relative Strength Indicator (RSI)

## Setup

* Install
    * You will need to have `gcloud` and `gradle` already installed.
    * Download the repository, `git clone https://github.com/blockchain-etl/band-dataflow-sample-applications`.
* Build
    * `cd dataflow-sample-applications/timeseries-streaming/timeseries-java-applications/`
    * `./gradlew build`
    

## Bootstrap of metrics to BigQuery

The [BandDataBootstrapGenerator](BandExamples/src/main/java/io/blockchainetl/band/examples/simpledata/transforms/BandDataBootstrapGenerator.java) 
pipeline allows you to calculate metrics for historical oracle requests.
The data is sourced from `crypto_band.oracle_requests` table in BigQuery.

Run the pipeline:

``` 
PROJECT=$(gcloud config get-value project 2> /dev/null)    
# Make sure to update the date to yesterday's date
TIMESTAMP_THRESHOLD="2020-09-23T00:00:00.0Z"   
DATASET_NAME=crypto_band
TEMP_BUCKET=dataflow-temp0
  
./gradlew generate_band_bootstrap_data --args="\
    --bigQueryTableForTSAccumOutputLocation=$PROJECT:$DATASET_NAME.metrics \
    --timestampThreshold="$TIMESTAMP_THRESHOLD" \
    --runner=DataflowRunner \
    --tempLocation=gs://$TEMP_BUCKET/temp \
    --maxNumWorkers=1 \
    --region=us-central1 \
"
```     

You can observe the results by running the following SQL command in BigQuery.

```
SELECT
  lower_window_boundary,
  upper_window_boundary,
  is_gap_fill_value,
  DATA.metric,
  CASE DATA.metric
    WHEN 'FIRST_TIMESTAMP' THEN CAST(TIMESTAMP_MILLIS(DATA.lng_data) AS STRING )
    WHEN 'LAST_TIMESTAMP' THEN CAST(TIMESTAMP_MILLIS(DATA.lng_data) AS STRING)
  ELSE
  CAST(DATA.dbl_data AS STRING)
END
FROM
  `<yourtable>`
CROSS JOIN
  UNNEST(DATA) AS DATA
WHERE
  DATA.metric IN ("LAST",
    "FIRST",
    "DATA_POINT_COUNT",
    "FIRST_TIMESTAMP",
    "LAST_TIMESTAMP")
ORDER BY
  lower_window_boundary,
  DATA.metric
```

## Calculating metrics from a Pub/Sub stream

The [BandDataStreamGenerator](BandExamples/src/main/java/io/blockchainetl/band/examples/simpledata/transforms/BandDataStreamGenerator.java) 
pipeline allows you to calculate metrics for oracle requests pulled from a Pub/Sub subscription. It will output
the result to the specified BigQuery table and Pub/Sub topic.

Create a Pub/Sub subscription for Band Protocol oracle requests:

```bash
gcloud pubsub subscriptions create crypto_band.oracle_requests.metrics \
    --topic=crypto_band.oracle_requests \
    --topic-project=band-etl
```

Start the pipeline in Dataflow:

``` 
TOPIC_NAME=crypto_band.metrics   

./gradlew run_band_example --args="\
    --pubSubSubscriptionForOracleRequests=projects/$PROJECT/subscriptions/crypto_band.oracle_requests.metrics \
    --bigQueryTableForTSAccumOutputLocation=$PROJECT:$DATASET_NAME.$TABLE_NAME \
    --timestampThreshold="$TIMESTAMP_THRESHOLD" \
    --pubSubTopicForTSAccumOutputLocation=projects/$PROJECT/topics/$TOPIC_NAME \
    --runner=DataflowRunner \
    --maxNumWorkers=1 \
    --workerMachineType=n1-standard-1 \
    --diskSizeGb=30 \
    --region=us-central1 \
"
```

## Output of metrics to LOG for testing

This example will spin up a local pipeline using the Direct Runner, the metrics will be output to log files.

Create a Pub/Sub subscription for Band Protocol oracle requests:

```bash
gcloud pubsub subscriptions create crypto_band.oracle_requests.metrics.test0 \
    --topic=crypto_band.oracle_requests \
    --topic-project=band-etl-dev
```

Start the pipeline:  
   
```     
PROJECT=$(gcloud config get-value project 2> /dev/null)
./gradlew run_band_example --args="\
    --runner=DirectRunner \
    --enablePrintMetricsToLogs \
    --pubSubSubscriptionForOracleRequests=projects/$PROJECT/subscriptions/crypto_band.oracle_requests.metrics.test0 \
    --typeOneComputationsLengthInSecs=5 \
    --typeTwoComputationsLengthInSecs=60 \
    --sequenceLengthInSeconds=60 \
"
```

You will see several messages about incomplete TSAccumSequences, once there is enough data, usually 5 minutes, 
you will see values output to the logs. To stop you can use CTRL-C.

---

For more example pipelines refer to this repository https://github.com/GoogleCloudPlatform/dataflow-sample-applications/tree/master/timeseries-streaming/timeseries-java-applications.
