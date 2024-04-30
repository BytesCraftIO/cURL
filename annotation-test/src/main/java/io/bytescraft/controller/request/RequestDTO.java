package io.bytescraft.controller.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

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
}
