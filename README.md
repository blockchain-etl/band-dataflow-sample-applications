## Band Protocol Public Data Sample Applications

This repository contains code for running [Dataflow](https://cloud.google.com/dataflow) pipelines for processing 
public [Band Protocol](https://bandprotocol.com) data in Google Cloud Platform.  

We included two applications:

- **Metrics calculation** - price feeds from [Aggregator](https://guanyu-poa.cosmoscan.io/oracle-script/8) oracle script 
    are aggregated into windows and the following metrics are calculated for each symbol:
    - Open, High, Low, Close (OHLC)
    - Simple Moving Average (SMA)
    - Exponential Moving Average (EMA)
    - Relative Strength (RS)
    - Relative Strength Indicator (RSI)

- **Anomaly Detection** - TODO
