package com.seby.jobleaf.infrastructure.web.exception;

import com.seby.jobleaf.domain.exceptions.company.CompanyNotFoundException;
import com.seby.jobleaf.domain.exceptions.domain.DomainValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private ServletWebRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new ServletWebRequest(new MockHttpServletRequest("POST", "/jobleaf/contacts"));
    }

    @SuppressWarnings("unused")
    private void createContact(Object body) {
        // target method for the MethodParameter of MethodArgumentNotValidException
    }

    private static ProblemDetail problemOf(ResponseEntity<?> response) {
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
        return (ProblemDetail) response.getBody();
    }

    @Test
    @DisplayName("Should map a missing resource to 404 with a typed problem")
    void handleResourceNotFound_returns404() {
        // Given
        CompanyNotFoundException exception = new CompanyNotFoundException("Company with id 42 not found");

        // When
        ResponseEntity<ProblemDetail> response = handler.handleResourceNotFound(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail problem = problemOf(response);
        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Not Found");
        assertThat(problem.getDetail()).isEqualTo("Company with id 42 not found");
        assertThat(problem.getType()).isEqualTo(URI.create("https://jobleaf.local/problems/not-found"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/jobleaf/contacts"));
    }

    @Test
    @DisplayName("Should map a domain validation error to 400 with the failing field")
    void handleDomainValidation_returns400WithField() {
        // Given
        DomainValidationException exception = DomainValidationException.requiredField("name", "Contact");

        // When
        ResponseEntity<ProblemDetail> response = handler.handleDomainValidation(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problem = problemOf(response);
        assertThat(problem.getDetail()).isEqualTo("Contact name is required");
        assertThat(problem.getType()).isEqualTo(URI.create("https://jobleaf.local/problems/bad-request"));
        assertThat(problem.getProperties()).containsEntry("errors", Map.of("name", "Contact name is required"));
    }

    @Test
    @DisplayName("Should map unexpected exceptions to a generic 500 without leaking the exception message")
    void handleUnexpected_returnsGeneric500() {
        // Given
        IllegalStateException exception = new IllegalStateException("could not execute statement: ERROR: relation \"contacts\"");

        // When
        ResponseEntity<ProblemDetail> response = handler.handleUnexpected(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ProblemDetail problem = problemOf(response);
        assertThat(problem.getDetail()).isEqualTo(GlobalExceptionHandler.GENERIC_ERROR_DETAIL);
        assertThat(problem.getDetail()).doesNotContain("relation");
        assertThat(problem.getType()).isEqualTo(URI.create("https://jobleaf.local/problems/internal-server-error"));
    }

    @Test
    @DisplayName("Should return 400 with one message per invalid field for body validation errors")
    void handleMethodArgumentNotValid_returnsFieldErrors() throws NoSuchMethodException {
        // Given
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "createContactRequest");
        bindingResult.addError(new FieldError("createContactRequest", "email", "must be a well-formed email address"));
        bindingResult.addError(new FieldError("createContactRequest", "message", "size must be between 10 and 255"));
        bindingResult.addError(new FieldError("createContactRequest", "message", "must not be blank"));
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("createContact", Object.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        // When
        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problem = problemOf(response);
        assertThat(problem.getDetail()).isEqualTo("Request validation failed");
        assertThat(problem.getType()).isEqualTo(URI.create("https://jobleaf.local/problems/bad-request"));
        assertThat(problem.getProperties()).containsEntry("errors", Map.of(
                "email", "must be a well-formed email address",
                "message", "size must be between 10 and 255, must not be blank"
        ));
    }

    @Test
    @DisplayName("Should return 400 with the joined messages per parameter for method validation errors")
    void handleHandlerMethodValidation_returnsJoinedMessagesPerParameter() {
        // Given
        MethodParameter parameter = mock(MethodParameter.class);
        when(parameter.getParameterName()).thenReturn("companyId");
        MessageSourceResolvable tooShort = mock(MessageSourceResolvable.class);
        when(tooShort.getDefaultMessage()).thenReturn("size must be between 36 and 36");
        MessageSourceResolvable badPattern = mock(MessageSourceResolvable.class);
        when(badPattern.getDefaultMessage()).thenReturn("must match the UUID pattern");
        ParameterValidationResult result = mock(ParameterValidationResult.class);
        when(result.getMethodParameter()).thenReturn(parameter);
        when(result.getResolvableErrors()).thenReturn(List.of(tooShort, badPattern));
        HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
        when(exception.getParameterValidationResults()).thenReturn(List.of(result));
        when(exception.getBody()).thenReturn(ProblemDetail.forStatus(HttpStatus.BAD_REQUEST));

        // When
        ResponseEntity<Object> response = handler.handleHandlerMethodValidationException(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(problemOf(response).getProperties())
                .containsEntry("errors", Map.of("companyId", "size must be between 36 and 36, must match the UUID pattern"));
    }

    @Test
    @DisplayName("Should keep Spring's 404 for unknown routes and add type and instance")
    void unknownRoute_returns404Problem() throws Exception {
        // Given
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/jobleaf/does-not-exist", "jobleaf/does-not-exist");

        // When
        ResponseEntity<Object> response = handler.handleException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail problem = problemOf(response);
        assertThat(problem.getType()).isEqualTo(URI.create("https://jobleaf.local/problems/not-found"));
        assertThat(problem.getInstance()).isEqualTo(URI.create("/jobleaf/contacts"));
    }

    @Test
    @DisplayName("Should keep Spring's 405 for unsupported methods")
    void unsupportedMethod_returns405Problem() throws Exception {
        // Given
        HttpRequestMethodNotSupportedException exception = new HttpRequestMethodNotSupportedException("DELETE", List.of("GET"));

        // When
        ResponseEntity<Object> response = handler.handleException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(problemOf(response).getType()).isEqualTo(URI.create("https://jobleaf.local/problems/method-not-allowed"));
    }
}
