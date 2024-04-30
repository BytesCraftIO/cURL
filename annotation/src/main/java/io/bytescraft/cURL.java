package io.bytescraft;

import java.lang.annotation.*;

/**
 * @author javaquery
 * @since 2024-04-30
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface cURL {
    String output() default "postman";
    String collection() default "/api";
    String name();
}
