package com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms;

public interface BandDataOptions extends SimpleDataOptions {

  String getPubSubSubscriptionForOracleRequests();

  void setPubSubSubscriptionForOracleRequests(String pubSubSubscriptionForOracleRequests);
}
