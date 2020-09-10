package com.google.dataflow.sample.timeseriesflow.examples.simpledata.transforms.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class TimeUtils {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd'T'HH:mm:ss.")
        .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false)
        .appendPattern("'Z'")
        .toFormatter().withZone(UTC);
    
    public static ZonedDateTime convertToZonedDateTime(Long unixTimestamp) {
        Instant blockInstant = Instant.ofEpochSecond(unixTimestamp);
        return ZonedDateTime.from(blockInstant.atZone(UTC));
    }

    public static ZonedDateTime convertToZonedDateTime(String timestamp, DateTimeFormatter formatter) {
        return ZonedDateTime.parse(timestamp, formatter.withZone(UTC));
    }
    
    public static String formatTimestamp(ZonedDateTime dateTime) {
        return TIMESTAMP_FORMATTER.format(dateTime); 
    }
    
    public static ZonedDateTime parseDateTime(String str) {
        return ZonedDateTime.parse(str, TIMESTAMP_FORMATTER);
    }

    public static void main(String[] args) {
        String date = "2020-09-07T13:46:56.579123123Z";
        ZonedDateTime zonedDateTime = parseDateTime(date);
        System.out.println(zonedDateTime);

    }
}
