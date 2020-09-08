package com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.domain;

import com.google.common.base.Objects;
import org.apache.avro.reflect.Nullable;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.StringJoiner;

@DefaultCoder(AvroCoder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DecodedResult {

    @Nullable
    private String calldata;

    @Nullable
    private String result;

    public DecodedResult() {}

    public String getCalldata() {
        return calldata;
    }

    public void setCalldata(String calldata) {
        this.calldata = calldata;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DecodedResult that = (DecodedResult) o;
        return Objects.equal(calldata, that.calldata) &&
            Objects.equal(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(calldata, result);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DecodedResult.class.getSimpleName() + "[", "]")
            .add("calldata='" + calldata + "'")
            .add("result='" + result + "'")
            .toString();
    }
}
