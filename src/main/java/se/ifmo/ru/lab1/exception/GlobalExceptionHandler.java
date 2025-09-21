package se.ifmo.ru.lab1.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import se.ifmo.ru.lab1.dto.ErrorResponse;

import java.util.Set;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        
        // Custom exceptions
        if (exception instanceof EntityNotFoundException) {
            ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), "ENTITY_NOT_FOUND");
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse)
                    .build();
        }
        
        if (exception instanceof ValidationException) {
            ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), "VALIDATION_ERROR");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
        }
        
        if (exception instanceof BusinessLogicException) {
            ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), "BUSINESS_LOGIC_ERROR");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
        }
        
        // Jakarta validation
        if (exception instanceof ConstraintViolationException) {
            ConstraintViolationException cve = (ConstraintViolationException) exception;
            Set<ConstraintViolation<?>> violations = cve.getConstraintViolations();
            
            StringBuilder errorMessage = new StringBuilder("Validation failed: ");
            for (ConstraintViolation<?> violation : violations) {
                errorMessage.append(violation.getMessage()).append("; ");
            }
            
            ErrorResponse errorResponse = new ErrorResponse(errorMessage.toString(), "CONSTRAINT_VIOLATION");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
        }
        
        if (exception instanceof IllegalArgumentException) {
            ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), "ILLEGAL_ARGUMENT");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse)
                    .build();
        }
        
        if (exception instanceof jakarta.persistence.EntityNotFoundException) {
            ErrorResponse errorResponse = new ErrorResponse("Entity not found: " + exception.getMessage(), "ENTITY_NOT_FOUND");
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse)
                    .build();
        }
        
        // Generic exception handler
        ErrorResponse errorResponse = new ErrorResponse("Internal server error: " + exception.getMessage(), "INTERNAL_ERROR");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
    }
}
