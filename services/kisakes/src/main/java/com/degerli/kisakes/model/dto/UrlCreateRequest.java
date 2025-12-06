package com.degerli.kisakes.model.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record UrlCreateRequest(
    @NotBlank(message = "URL cannot be blank") @URL(message = "A valid URL format is "
        + "required") String originalUrl) {}