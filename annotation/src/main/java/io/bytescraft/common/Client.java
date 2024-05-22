package io.bytescraft.common;

import java.util.Arrays;
import java.util.List;

/**
 * List of supported clients
 * @author javaquery
 * @since 0.0.2
 */
public enum Client {
    POSTMAN, THUNDER_CLIENT;

    public static List<String> strValues() {
        return Arrays.asList(POSTMAN.name(), THUNDER_CLIENT.name());
    }
}
