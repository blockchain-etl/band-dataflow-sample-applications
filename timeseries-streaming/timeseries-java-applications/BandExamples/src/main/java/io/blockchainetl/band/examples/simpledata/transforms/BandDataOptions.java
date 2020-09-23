package io.blockchainetl.band.examples.simpledata.transforms;

public interface BandDataOptions extends SimpleDataOptions {

  String getPubSubSubscriptionForOracleRequests();

  void setPubSubSubscriptionForOracleRequests(String pubSubSubscriptionForOracleRequests);
}
