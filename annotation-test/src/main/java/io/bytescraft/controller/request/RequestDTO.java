package io.bytescraft.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author javaquery
 * @since 2024-04-30
 */
@Getter
@Setter
public class RequestDTO {
    @NotEmpty
    String name;
    String email;
    Map<String, Integer> metadata;
}
