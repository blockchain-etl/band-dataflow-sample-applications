package io.blockchainetl.band.examples.simpledata.transforms.domain;

import com.google.common.base.Objects;
import org.apache.avro.reflect.Nullable;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.StringJoiner;

@DefaultCoder(AvroCoder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OracleRequestData {

    @Nullable
    private Long oracle_script_id;

    public OracleRequestData() {}

    public Long getOracle_script_id() {
        return oracle_script_id;
    }

    public void setOracle_script_id(Long oracle_script_id) {
        this.oracle_script_id = oracle_script_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OracleRequestData that = (OracleRequestData) o;
        return Objects.equal(oracle_script_id, that.oracle_script_id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(oracle_script_id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OracleRequestData.class.getSimpleName() + "[", "]")
            .add("oracle_script_id=" + oracle_script_id)
            .toString();
    }
}
