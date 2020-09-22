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
    private OracleRequestData request;

    @Nullable
    private String block_timestamp;

    @Nullable
    private DecodedResult decoded_result;

    public OracleRequest() {}

    public OracleRequestData getRequest() {
        return request;
    }

    public void setRequest(OracleRequestData request) {
        this.request = request;
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
        return Objects.equal(request, that.request) &&
            Objects.equal(block_timestamp, that.block_timestamp) &&
            Objects.equal(decoded_result, that.decoded_result);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(request, block_timestamp, decoded_result);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OracleRequest.class.getSimpleName() + "[", "]")
            .add("request=" + request)
            .add("block_timestamp='" + block_timestamp + "'")
            .add("decoded_result=" + decoded_result)
            .toString();
    }
}
