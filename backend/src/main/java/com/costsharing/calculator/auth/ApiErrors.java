package com.costsharing.calculator.auth;
import com.costsharing.calculator.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(AccessException.class)
    public ResponseEntity<ApiResponse<Void>> access(AccessException error) {
        return ResponseEntity.status(error.getStatus()).body(ApiResponse.error(error.getStatus(), error.getMessage()));
    }
}
