package org.sofumar.portal.framework.exceptionhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.framework.exception.AuthenticationException;
import org.sofumar.portal.framework.exception.DuplicateRecordException;
import org.sofumar.portal.framework.exception.RecordNotFoundException;
import org.sofumar.portal.framework.exception.ValidationException;
import org.sofumar.portal.framework.message.MessageType;
import org.sofumar.portal.framework.data.response.FieldMsg;
import org.sofumar.portal.framework.data.response.GlobalMsg;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.framework.vo.ValueObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldMsg> fieldMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldMsg(MessageType.ERROR, fe.getField(), fe.getDefaultMessage()))
                .toList();

        GlobalResponse<Void> response = GlobalResponse.getInstance();
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
        response.setStatusDesc(HttpStatus.BAD_REQUEST.getReasonPhrase());
        response.setFieldMessages(fieldMessages);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DuplicateRecordException.class)
    public ResponseEntity<GlobalResponse<Void>> handleDuplicate(DuplicateRecordException ex) {
        GlobalResponse<Void> response = GlobalResponse.getInstance();
        ValueObject vo = ex.getVo();
        ResponseUtils.populateResponseFromVO(response, vo);
        response.setStatusCode(HttpStatus.CONFLICT.value());
        response.setStatusDesc(HttpStatus.CONFLICT.getReasonPhrase());
        return ResponseUtils.withStatus(HttpStatus.CONFLICT, response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<GlobalResponse<Void>> handleValidation(ValidationException ex) {
        ValueObject vo = ex.getVo();
        GlobalResponse<Void> response = GlobalResponse.getInstance();

        if (vo != null) {
            ResponseUtils.populateResponseFromVO(response, vo);
        } else {
            response.setGlobalMessages(List.of(new GlobalMsg(MessageType.ERROR, ex.getMessage())));
        }

        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
        response.setStatusDesc(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return ResponseUtils.withStatus(HttpStatus.BAD_REQUEST, response);
    }

    @ExceptionHandler(RecordNotFoundException.class)
    public ResponseEntity<GlobalResponse<Void>> handleNotFound(RecordNotFoundException ex) {
        return ResponseUtils.notFound(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<GlobalResponse<Void>> handleIllegalState(IllegalStateException ex) {
        logger.error("System configuration error: ", ex);
        return ResponseUtils.withStatus(HttpStatus.SERVICE_UNAVAILABLE, MessageType.ERROR, "System configuration error. Please contact support.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<Void>> handleGeneric(Exception ex) {
        logger.error("Unexpected error occurred: ", ex);
        return ResponseUtils.withStatus(HttpStatus.INTERNAL_SERVER_ERROR, MessageType.ERROR, "Unexpected error occurred. Please try again or contact support.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseUtils.unauthenticated();
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<GlobalResponse<Void>> handleAuthentication(org.springframework.security.core.AuthenticationException ex) {
        return ResponseUtils.unauthenticated();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GlobalResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseUtils.accessDenied();
    }

}