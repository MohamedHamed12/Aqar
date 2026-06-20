package com.aqar.shared.error;

import com.aqar.listing.exception.ImageLockAcquisitionException;
import com.aqar.listing.exception.ImageNotFoundException;
import com.aqar.listing.exception.InvalidImageException;
import com.aqar.listing.exception.ListingNotFoundException;
import com.aqar.listing.exception.ListingStateTransitionException;
import com.aqar.listing.exception.MaxImagesExceededException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalRestControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(error("validation_error", "Request validation failed", fieldErrors(exception.getBindingResult())));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(error("validation_error", exception.getMessage(), List.of()));
    }

    @ExceptionHandler({ListingNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("not_found", exception.getMessage(), List.of()));
    }

    @ExceptionHandler(ListingStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleStateTransition(ListingStateTransitionException exception) {
        return ResponseEntity.badRequest().body(error("invalid_state_transition", exception.getMessage(), List.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(error("bad_request", exception.getMessage(), List.of()));
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleImageNotFound(ImageNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("not_found", exception.getMessage(), List.of()));
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImage(InvalidImageException exception) {
        return ResponseEntity.badRequest().body(error("invalid_image", exception.getMessage(), List.of()));
    }

    @ExceptionHandler(MaxImagesExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxImagesExceeded(MaxImagesExceededException exception) {
        return ResponseEntity.badRequest().body(error("max_images_exceeded", exception.getMessage(), List.of()));
    }

    @ExceptionHandler(ImageLockAcquisitionException.class)
    public ResponseEntity<ErrorResponse> handleImageLockAcquisition(ImageLockAcquisitionException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error("lock_contention", exception.getMessage(), List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandled(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("internal_error", "Unexpected error", List.of()));
    }

    private ErrorResponse error(String code, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(code, message, fieldErrors, Instant.now());
    }

    private List<FieldError> fieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage(), error.getRejectedValue()))
                .toList();
    }
}