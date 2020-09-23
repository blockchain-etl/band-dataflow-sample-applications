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
public class AggregatorCalldata {

    @Nullable
    private List<String> symbols;

    @Nullable
    private BigDecimal multiplier;

    public AggregatorCalldata() {}

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AggregatorCalldata that = (AggregatorCalldata) o;
        return Objects.equal(symbols, that.symbols) &&
            Objects.equal(multiplier, that.multiplier);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(symbols, multiplier);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AggregatorCalldata.class.getSimpleName() + "[", "]")
            .add("symbols=" + symbols)
            .add("multiplier=" + multiplier)
            .toString();
    }
}
