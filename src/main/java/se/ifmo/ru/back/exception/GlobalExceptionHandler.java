package se.ifmo.ru.back.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import se.ifmo.ru.back.dto.ErrorResponse;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex, 
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            "ENTITY_NOT_FOUND",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, 
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            "VALIDATION_ERROR",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicException(
            BusinessLogicException ex, 
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            "BUSINESS_LOGIC_ERROR",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, 
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            "ACCESS_DENIED",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            "ILLEGAL_ARGUMENT",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        
        if (errorMessage.isEmpty()) {
            errorMessage = "Validation failed";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            errorMessage,
            "VALIDATION_ERROR",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, 
            HttpServletRequest request) {
        String errorMessage = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining("; "));
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Validation failed: " + errorMessage,
            "CONSTRAINT_VIOLATION",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, 
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            "Missing required parameter: " + ex.getParameterName(),
            "MISSING_PARAMETER",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, 
            HttpServletRequest request) {
        String errorMessage = String.format(
            "Parameter '%s' has invalid type. Expected: %s, but got: %s",
            ex.getName(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown",
            ex.getValue()
        );
        
        ErrorResponse errorResponse = new ErrorResponse(
            errorMessage,
            "TYPE_MISMATCH",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, 
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            "Invalid request body: " + ex.getMessage(),
            "INVALID_REQUEST_BODY",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, 
            HttpServletRequest request) {
        String supportedMethods = String.join(", ", ex.getSupportedMethods());
        ErrorResponse errorResponse = new ErrorResponse(
            String.format("Method %s is not supported. Supported methods: %s", 
                ex.getMethod(), supportedMethods),
            "METHOD_NOT_SUPPORTED",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException ex, 
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            "Media type not supported: " + ex.getContentType(),
            "MEDIA_TYPE_NOT_SUPPORTED",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, 
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            "No handler found for " + ex.getHttpMethod() + " " + ex.getRequestURL(),
            "NOT_FOUND",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJpaEntityNotFoundException(
            jakarta.persistence.EntityNotFoundException ex, 
            HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
            "Entity not found: " + ex.getMessage(),
            "ENTITY_NOT_FOUND",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            HttpServletRequest request) {
        // Логируем полный стек для внутренних ошибок
        logger.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "Internal server error: " + ex.getMessage(),
            "INTERNAL_ERROR",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
