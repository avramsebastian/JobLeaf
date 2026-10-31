package com.seby.jobleaf.infrastructure.web.exception;

import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.exceptions.domain.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Translates exceptions into RFC 9457 {@link ProblemDetail} responses ({@code application/problem+json}).
 * <p>
 * Spring MVC exceptions (unknown route, malformed body, unsupported method or media type, validation) keep their
 * own 4xx status through {@link ResponseEntityExceptionHandler}. Unexpected exceptions become a generic 500 whose
 * details are logged but never sent to the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String PROBLEM_TYPE_BASE = "https://jobleaf.local/problems/";
    static final String ERRORS_PROPERTY = "errors";
    static final String GENERIC_ERROR_DETAIL = "An unexpected error occurred. Please try again later.";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException exception,
                                                                WebRequest request) {
        log.debug("Resource not found: {}", exception.getMessage());
        return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemDetail> handleDomainValidation(DomainValidationException exception,
                                                                WebRequest request) {
        log.debug("Domain validation failed: {}", exception.getMessage());
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
        response.getBody().setProperty(ERRORS_PROPERTY, Map.of(exception.getField(), exception.getMessage()));
        return response;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, WebRequest request) {
        log.error("Unhandled exception for {}", instancePath(request), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_ERROR_DETAIL, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.merge(error.getField(), String.valueOf(error.getDefaultMessage()), (a, b) -> a + ", " + b);
        }
        ProblemDetail body = exception.getBody();
        body.setDetail("Request validation failed");
        body.setProperty(ERRORS_PROPERTY, errors);
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getParameterValidationResults().forEach(result -> errors.put(
                result.getMethodParameter().getParameterName(),
                result.getResolvableErrors().stream()
                        .map(MessageSourceResolvable::getDefaultMessage)
                        .collect(Collectors.joining(", "))));
        ProblemDetail body = exception.getBody();
        body.setDetail("Request validation failed");
        body.setProperty(ERRORS_PROPERTY, errors);
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("Request to {} failed with status {}", instancePath(request), statusCode.value(), exception);
        } else {
            log.debug("Request to {} rejected with status {}: {}", instancePath(request), statusCode.value(),
                    exception.getMessage());
        }
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }

    /**
     * Fills in {@code type} and {@code instance} for the problems built by Spring MVC. Done here because Spring
     * only creates the {@link ProblemDetail} body after {@link #handleExceptionInternal} was called.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
                                                          WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            enrich(problemDetail, statusCode, request);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        enrich(problemDetail, status, request);
        return ResponseEntity.status(status).body(problemDetail);
    }

    private void enrich(ProblemDetail problemDetail, HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String slug = status != null ? status.name().toLowerCase().replace('_', '-') : String.valueOf(statusCode.value());
        problemDetail.setType(URI.create(PROBLEM_TYPE_BASE + slug));
        if (problemDetail.getTitle() == null && status != null) {
            problemDetail.setTitle(status.getReasonPhrase());
        }
        problemDetail.setInstance(URI.create(instancePath(request)));
    }

    private static String instancePath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false).replaceFirst("^uri=", "");
    }
}
