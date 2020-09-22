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
    
## Creating metrics from a stream

 * example_1 : In this mode the metrics generated are sent to output as logs.
 * example_2 : In this mode the metrics generated are sent to BigQuery and Pub/Sub.

## Example 1 : Output of metrics to LOG

This example will spin up a local pipeline using the Direct Runner, the metrics will be output to log files.

Create a Pub/Sub subscription for Band Protocol oracle requests:

```bash
gcloud pubsub subscriptions create crypto_band.oracle_requests.subscription0 --topic=crypto_band.oracle_requests --topic-project=band-etl-dev
```

Start the pipeline:  
   
```     
PROJECT=$(gcloud config get-value project 2> /dev/null)
./gradlew run_example --args="\
    --enablePrintMetricsToLogs \
    --pubSubSubscriptionForOracleRequests=projects/$PROJECT/subscriptions/crypto_band.oracle_requests.subscription0 \
    --typeOneComputationsLengthInSecs=5 \
    --typeTwoComputationsLengthInSecs=60 \
    --sequenceLengthInSeconds=60 \
"
```

You will see several messages about incomplete TSAccumSequences, once there is enough data, usually 5 minutes, 
you will see values output to the logs. To stop you can use CTRL-C.

You can customize window size and the 

## Example 2 : Output of metrics to BigQuery and Pub/Sub

---
***NOTE***
The following examples will use resources on your Google Cloud Platform account, which will incur costs. 
---

* In order to run this example you will need to set up a BigQuery [dataset](https://cloud.google.com/bigquery/docs/datasets-intro) that your local environment will send data to.
* You will need to be authenticated as a user account to run this sample [BigQuery Authentication](https://cloud.google.com/bigquery/docs/authentication)

Create a BigQuery table:

```bash        
DATASET_NAME=band_metrics
bq --location=US mk --dataset $DATASET_NAME 
``` 

Run the pipeline:

``` 
PROJECT=$(gcloud config get-value project 2> /dev/null)   
TOPIC_NAME=band_metrics
./gradlew run_example --args="--bigQueryTableForTSAccumOutputLocation=$PROJECT:$DATASET_NAME.metrics --pubSubTopicForTSAccumOutputLocation=projects/$PROJECT/topics/$TOPIC_NAME"
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
