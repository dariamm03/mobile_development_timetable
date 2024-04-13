package org.hse.base;

public final class BuildConfig {
    public static final String APPLICATION_ID = "org.hse.base";
    public static final String BUILD_TYPE = "debug";
    /**
     * Suppress default constructor for noninstantiability
     */
    private BuildConfig() {
        throw new AssertionError();
    }
}
