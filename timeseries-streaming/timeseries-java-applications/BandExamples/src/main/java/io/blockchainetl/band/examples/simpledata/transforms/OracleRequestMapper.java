package io.blockchainetl.band.examples.simpledata.transforms;

import com.google.dataflow.sample.timeseriesflow.TimeSeriesData;
import io.blockchainetl.band.examples.simpledata.transforms.domain.AggregatorCalldata;
import io.blockchainetl.band.examples.simpledata.transforms.domain.AggregatorResult;
import io.blockchainetl.band.examples.simpledata.transforms.domain.OracleRequest;
import io.blockchainetl.band.examples.simpledata.transforms.utils.JsonUtils;
import io.blockchainetl.band.examples.simpledata.transforms.utils.TimeUtils;
import com.google.protobuf.util.Timestamps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class OracleRequestMapper {

    private static final Long BAND_AGGREGATOR_ORACLE_SCRIPT_ID = 8L;
    
    private static final Logger LOG = LoggerFactory.getLogger(OracleRequestMapper.class);

    public static List<TimeSeriesData.TSDataPoint> convertOracleRequestToTSDataPoints(OracleRequest oracleRequest) {
        List<TimeSeriesData.TSDataPoint> out = new ArrayList<>();
        
        if (oracleRequest.getRequest() != null &&
            BAND_AGGREGATOR_ORACLE_SCRIPT_ID.equals(oracleRequest.getRequest().getOracle_script_id())) {

            LOG.info("Processing oracle request with id " + oracleRequest.getOracle_request_id());

            out = convertAggregatorOracleRequestToTSDataPoints(oracleRequest);
        }
        
        return out;
    }

    private static List<TimeSeriesData.TSDataPoint>  convertAggregatorOracleRequestToTSDataPoints(OracleRequest oracleRequest) {
        List<TimeSeriesData.TSDataPoint> out = new ArrayList<>();
        
        if (oracleRequest.getDecoded_result() != null &&
            oracleRequest.getDecoded_result().getCalldata() != null &&
            oracleRequest.getDecoded_result().getResult() != null) {
            
            String calldata = oracleRequest.getDecoded_result().getCalldata();
            AggregatorCalldata decodedCalldata = JsonUtils.parseJson(calldata, AggregatorCalldata.class);

            String result = oracleRequest.getDecoded_result().getResult();
            AggregatorResult decodedResult = JsonUtils.parseJson(result, AggregatorResult.class);


            if (decodedCalldata != null && decodedResult != null) {
                ZonedDateTime zonedDateTime = TimeUtils.parseDateTime(
                    oracleRequest.getBlock_timestamp());

                if (decodedCalldata.getSymbols() == null || decodedResult.getRates() == null ||
                    decodedCalldata.getSymbols().size() != decodedResult.getRates().size()) {
                    LOG.error("calldata.symbols or result.rates are null or have different sizes.");
                } else {
                    for (int i = 0; i < decodedCalldata.getSymbols().size(); i++) {
                        String symbol = decodedCalldata.getSymbols().get(i);
                        BigDecimal rate = decodedResult.getRates().get(i);

                        TimeSeriesData.TSKey key = TimeSeriesData.TSKey
                            .newBuilder().setMajorKey(symbol).setMinorKeyString("value").build();

                        out.add(
                            TimeSeriesData.TSDataPoint.newBuilder()
                                .setKey(key)
                                .setData(
                                    TimeSeriesData.Data.newBuilder()
                                        .setDoubleVal(rate.doubleValue()))
                                .setTimestamp(Timestamps.fromSeconds(zonedDateTime.toEpochSecond()))
                                .build());
                    }
                }
            }
        }
        return out;
    }
}
