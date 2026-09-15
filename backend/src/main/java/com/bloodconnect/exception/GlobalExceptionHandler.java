package com.bloodconnect.exception;

import io.modelcontextprotocol.spec.McpError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.UNPROCESSABLE_ENTITY, message, request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene un valor o formato no válido",
                request.getRequestURI()
        );
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiError> handleAuth(Exception ex, HttpServletRequest request) {
        String message = ex instanceof BadCredentialsException && ex.getMessage() != null
                ? ex.getMessage()
                : "Credenciales inválidas o sesión no autenticada";
        return build(HttpStatus.UNAUTHORIZED, message, request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción", request.getRequestURI());
    }

    @ExceptionHandler(McpError.class)
    public ResponseEntity<?> handleMcpError(McpError ex, HttpServletRequest request) {
        if (isMcp(request)) {
            return jsonRpcError(HttpStatus.BAD_REQUEST, -32600, safeMessage(ex));
        }
        return build(HttpStatus.BAD_REQUEST, "La solicitud MCP no es válida", request.getRequestURI());
    }

    @ExceptionHandler({HttpMessageNotWritableException.class, HttpMediaTypeNotAcceptableException.class})
    public ResponseEntity<?> handleMcpMedia(Exception ex, HttpServletRequest request) {
        log.warn("Error de negociación HTTP en {}: {}", request.getRequestURI(), ex.getMessage());
        if (isMcp(request)) {
            return jsonRpcError(
                    HttpStatus.BAD_REQUEST,
                    -32600,
                    "El endpoint MCP requiere Accept: application/json, text/event-stream"
            );
        }
        return build(HttpStatus.BAD_REQUEST, "La solicitud contiene un valor o formato no válido", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        if (isMcp(request)) {
            return jsonRpcError(HttpStatus.INTERNAL_SERVER_ERROR, -32603, safeMessage(ex));
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Ha ocurrido un error interno. Intente nuevamente más tarde.", request.getRequestURI());
    }

    private boolean isMcp(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && (path.equals("/mcp") || path.startsWith("/mcp/"));
    }

    private ResponseEntity<Map<String, Object>> jsonRpcError(HttpStatus status, int code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        body.put("id", null);
        body.put("error", error);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, String path) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status).body(body);
    }
}
