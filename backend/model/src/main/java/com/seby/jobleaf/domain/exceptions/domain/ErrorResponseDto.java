package com.seby.jobleaf.domain.exceptions.domain;

import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;

public record ErrorResponseDto(String apiPath, HttpStatus errorCode, String errorMessage,
                               LocalDateTime errorTime) {
}

