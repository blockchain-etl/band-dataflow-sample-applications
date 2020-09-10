package com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms;

import com.google.dataflow.sample.timeseriesflow.TimeSeriesData;
import com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.domain.AggregatorCalldata;
import com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.domain.AggregatorResult;
import com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.domain.OracleRequest;
import com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.utils.JsonUtils;
import com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.utils.TimeUtils;
import com.google.protobuf.util.Timestamps;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class OracleRequestMapper {

    public static List<TimeSeriesData.TSDataPoint> convertOracleRequestToTSDataPoint(OracleRequest oracleRequest) {
        String calldata = oracleRequest.getDecoded_result().getCalldata();
        AggregatorCalldata decodedCalldata = JsonUtils.parseJson(calldata, AggregatorCalldata.class);

        String result = oracleRequest.getDecoded_result().getResult();
        AggregatorResult decodedResult = JsonUtils.parseJson(result, AggregatorResult.class);

        List<TimeSeriesData.TSDataPoint> out = new ArrayList<>();
        if (decodedCalldata != null && decodedResult != null) {
            ZonedDateTime zonedDateTime = TimeUtils.parseDateTime(
                oracleRequest.getBlock_timestamp());
            
            // TODO: Validate
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
        
        return out;
    }
}
