package io.blockchainetl.band.examples.simpledata.transforms.utils;

import io.blockchainetl.band.examples.simpledata.transforms.BandDataOptions;

public class BandUtils {

    public static final int DEFAULT_TYPE_ONE_COMPUTATIONS_LENGTH_IN_SECS = 600;
    public static final int DEFAULT_TYPE_TWO_COMPUTATIONS_LENGTH_IN_SECS = 3600;
    public static final int DEFAULT_SEQUENCE_LENGTH_IN_SECONDS = 600;
    public static final int DEFAULT_TTL_DURATION_SECS = 600;

    public static void setDefaultOptions(BandDataOptions options) {
        if (options.getTypeOneComputationsLengthInSecs() == null) {
            options.setTypeOneComputationsLengthInSecs(DEFAULT_TYPE_ONE_COMPUTATIONS_LENGTH_IN_SECS);
        }
        if (options.getTypeTwoComputationsLengthInSecs() == null) {
            options.setTypeTwoComputationsLengthInSecs(DEFAULT_TYPE_TWO_COMPUTATIONS_LENGTH_IN_SECS);
        }
        if (options.getSequenceLengthInSeconds() == null) {
            options.setSequenceLengthInSeconds(DEFAULT_SEQUENCE_LENGTH_IN_SECONDS);
        }
        if (options.getTTLDurationSecs() == null) {
            options.setTTLDurationSecs(DEFAULT_TTL_DURATION_SECS);
        }
    }
}
