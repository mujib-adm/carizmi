package io.carizmi.domain.platform.constants;

public final class BaselineConstants {

    private BaselineConstants() {
    }

    public static final String NAME_BASELINE = "BASELINE_REVENUE";
    public static final String KEY_SEED = "BASELINE_SEED";
    public static final String KEY_PREFIX_YEARLY = "YEARLY_BASELINE_";

    public static String getYearlyBaselineKey(int year) {
        return KEY_PREFIX_YEARLY + year;
    }
}