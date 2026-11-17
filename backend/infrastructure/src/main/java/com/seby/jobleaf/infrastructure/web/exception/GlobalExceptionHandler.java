package com.seby.jobleaf.infrastructure.web.exception;

import com.seby.jobleaf.domain.exceptions.auth.InvalidCredentialsException;
import com.seby.jobleaf.domain.exceptions.auth.UserAlreadyExistsException;
import com.seby.jobleaf.domain.exceptions.company.CompanyAlreadyExistsException;
import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import com.seby.jobleaf.domain.exceptions.domain.ResourceNotFoundException;
import com.seby.jobleaf.infrastructure.web.security.captcha.CaptchaVerificationException;
import com.seby.jobleaf.infrastructure.web.security.ratelimit.RateLimitExceededException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
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
 * Translates exceptions into {@link ProblemDetail} responses ({@code application/problem+json}).
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
    static final String RETRY_AFTER_PROPERTY = "retryAfterSeconds";
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

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException exception,
                                                                  WebRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    /**
     * Reached through {@code ProblemDetailSecurityHandler} when a secured endpoint is called without credentials.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException exception, WebRequest request) {
        log.debug("Authentication failed for {}: {}", instancePath(request), exception.getMessage());
        String detail = switch (exception) {
            case CredentialsExpiredException _ -> "The access token has expired, please log in again";
            case BadCredentialsException _ -> "The access token is invalid";
            default -> "Authentication is required to access this resource";
        };
        return problem(HttpStatus.UNAUTHORIZED, detail, request);
    }

    @ExceptionHandler(CompanyAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleCompanyAlreadyExists(CompanyAlreadyExistsException exception,
                                                                    WebRequest request) {
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.CONFLICT, exception.getMessage(), request);
        response.getBody().setProperty(ERRORS_PROPERTY, Map.of(exception.getField(), exception.getMessage()));
        return response;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleUserAlreadyExists(UserAlreadyExistsException exception,
                                                                 WebRequest request) {
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.CONFLICT, exception.getMessage(), request);
        response.getBody().setProperty(ERRORS_PROPERTY, Map.of(exception.getField(), exception.getMessage()));
        return response;
    }

    /**
     * Also reached for a missing or wrong CSRF token: {@link CsrfException} is an {@link AccessDeniedException}.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception, WebRequest request) {
        log.debug("Access denied for {}: {}", instancePath(request), exception.getMessage());
        String detail = exception instanceof CsrfException
                ? "The CSRF token is missing or invalid. Get one from /csrf-token/public and send it in the X-XSRF-TOKEN header"
                : "You do not have permission to access this resource";
        return problem(HttpStatus.FORBIDDEN, detail, request);
    }

    /**
     * Invalid query or path parameters, e.g. {@code pageSize=1000} against {@code @Max(100)}. The generated API
     * interfaces are {@code @Validated}, so Spring validates the parameters through method validation, which throws
     * this exception instead of the MVC {@link HandlerMethodValidationException}.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception,
                                                                   WebRequest request) {
        log.debug("Parameter validation failed for {}: {}", instancePath(request), exception.getMessage());
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            errors.merge(parameterName(violation), violation.getMessage(), (a, b) -> a + ", " + b);
        }
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        response.getBody().setProperty(ERRORS_PROPERTY, errors);
        return response;
    }

    /**
     * @return the last part of the property path, e.g. "pageSize" for "fetchNewContactMsgs.pageSize"
     */
    private static String parameterName(ConstraintViolation<?> violation) {
        String name = null;
        for (Path.Node node : violation.getPropertyPath()) {
            name = node.getName();
        }
        return name == null ? "request" : name;
    }

    /**
     * A client used up its requests ({@code RateLimitFilter}). {@code Retry-After} is the standard header for "when
     * may I try again"; {@code retryAfterSeconds} in the body is the same number, easy for the frontend to read.
     */
    /**
     * No "errors" map on purpose: the register page would try to show it under a form field that does not exist.
     */
    @ExceptionHandler(CaptchaVerificationException.class)
    public ResponseEntity<ProblemDetail> handleCaptchaVerification(CaptchaVerificationException exception,
                                                                   WebRequest request) {
        log.debug("Captcha check failed for {}", instancePath(request));
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException exception,
                                                                 WebRequest request) {
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request);
        response.getBody().setProperty(RETRY_AFTER_PROPERTY, exception.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
                .body(response.getBody());
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
