package io.blockchainetl.band.examples.simpledata.transforms.domain;

import com.google.common.base.Objects;
import org.apache.avro.reflect.Nullable;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.StringJoiner;

@DefaultCoder(AvroCoder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AggregatorResult {

    @Nullable
    private List<BigDecimal> rates;

    public AggregatorResult() {}

    public List<BigDecimal> getRates() {
        return rates;
    }

    public void setRates(List<BigDecimal> rates) {
        this.rates = rates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AggregatorResult that = (AggregatorResult) o;
        return Objects.equal(rates, that.rates);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rates);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AggregatorResult.class.getSimpleName() + "[", "]")
            .add("rates=" + rates)
            .toString();
    }
}
