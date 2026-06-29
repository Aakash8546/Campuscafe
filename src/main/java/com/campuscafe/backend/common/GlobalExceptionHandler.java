package com.campuscafe.backend.common;

import com.campuscafe.backend.exception.*;
import com.campuscafe.backend.mail.exception.EmailSendFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), List.of(ex.getMessage())));
    }

    @ExceptionHandler({
            UserNotFoundException.class,
            OtpInvalidException.class,
            OtpExpiredException.class,
            RefreshTokenExpiredException.class,
            RefreshTokenInvalidException.class,
            UserAlreadyExistsException.class,
            RoleNotFoundException.class,
            UserInactiveException.class,
            DuplicateCategoryException.class,
            DuplicateProductException.class,
            DuplicateInventoryItemException.class,
            DuplicateInventoryCategoryException.class,
            InsufficientStockException.class,
            InventoryValidationException.class,
            InvalidOrderStatusException.class,
            InvalidOrderTransitionException.class,
            ProductUnavailableException.class,
            InvalidOrderRequestException.class,
            DuplicateDiscountException.class,
            InactiveDiscountException.class,
            InvalidDateRangeException.class,
            ReportGenerationException.class,
            InsufficientInventoryException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), List.of(ex.getMessage())));
    }

    @ExceptionHandler({
            CategoryNotFoundException.class,
            ProductNotFoundException.class,
            InventoryCategoryNotFoundException.class,
            InventoryItemNotFoundException.class,
            OrderNotFoundException.class,
            DiscountNotFoundException.class,
            NotificationNotFoundException.class,
            MerchantSettingsNotFoundException.class,
            RecipeNotFoundException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleNotFoundExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), List.of(ex.getMessage())));
    }

    @ExceptionHandler({
            com.campuscafe.backend.exception.AccessDeniedException.class,
            org.springframework.security.access.AccessDeniedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure("Access denied", List.of(ex.getMessage() != null ? ex.getMessage() : "Access denied")));
    }

    @ExceptionHandler(MerchantNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMerchantNotVerified(MerchantNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ex.getMessage(), List.of(ex.getMessage())));
    }

    @ExceptionHandler(EmailSendFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailSendFailedException(EmailSendFailedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Unable to send verification email. Please try again later.", List.of(ex.getMessage() != null ? ex.getMessage() : "Email delivery failed")));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(ex.getMessage(), List.of(ex.getMessage())));
    }

    @ExceptionHandler(com.campuscafe.backend.exception.AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(com.campuscafe.backend.exception.AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.failure("Account Locked", List.of(ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return fieldName + ": " + errorMessage;
                })
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Validation failed", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("An unexpected error occurred", List.of(ex.getMessage() != null ? ex.getMessage() : "Internal server error")));
    }
}
