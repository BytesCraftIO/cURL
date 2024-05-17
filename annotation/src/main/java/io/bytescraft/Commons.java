package io.bytescraft;

/**
 * @author javaquery
 * @since 0.0.1
 */
public class Commons {

    /**
     * Convert camel case string to name
     *
     * value: message , expected: Message
     * value: messageData, expected: Message Data
     * value: userAuth, expected: User Auth
     * @param value
     * @return
     */
    public static String convertCamelCaseToName(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(value.charAt(0))); // Capitalize the first letter
        int length = value.length();
        for (int i = 1; i < length; i++) {
            char currentChar = value.charAt(i);
            if (Character.isUpperCase(currentChar)) {
                result.append(' '); // Add space before uppercase letters
            }
            result.append(currentChar);
        }
        return result.toString();
    }
}
