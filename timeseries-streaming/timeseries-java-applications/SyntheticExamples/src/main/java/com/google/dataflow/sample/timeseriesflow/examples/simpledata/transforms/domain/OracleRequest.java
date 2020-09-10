package com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.domain;

import com.google.common.base.Objects;
import org.apache.avro.reflect.Nullable;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.StringJoiner;

@DefaultCoder(AvroCoder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OracleRequest {

    @Nullable
    private Long oracle_request_id;

    @Nullable
    private String block_timestamp;

    @Nullable
    private DecodedResult decoded_result;

    public OracleRequest() {}

    public Long getOracle_request_id() {
        return oracle_request_id;
    }

    public void setOracle_request_id(Long oracle_request_id) {
        this.oracle_request_id = oracle_request_id;
    }

    public String getBlock_timestamp() {
        return block_timestamp;
    }

    public void setBlock_timestamp(String block_timestamp) {
        this.block_timestamp = block_timestamp;
    }

    public DecodedResult getDecoded_result() {
        return decoded_result;
    }

    public void setDecoded_result(DecodedResult decoded_result) {
        this.decoded_result = decoded_result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OracleRequest that = (OracleRequest) o;
        return Objects.equal(oracle_request_id, that.oracle_request_id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(oracle_request_id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OracleRequest.class.getSimpleName() + "[", "]")
            .add("oracle_request_id=" + oracle_request_id)
            .toString();
    }
}
