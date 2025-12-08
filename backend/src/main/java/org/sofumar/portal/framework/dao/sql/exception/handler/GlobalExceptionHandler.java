package org.sofumar.portal.framework.dao.sql.exception.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sofumar.portal.framework.dao.sql.exception.DuplicateRecordException;
import org.sofumar.portal.framework.dao.sql.exception.ValidationException;
import org.sofumar.portal.framework.data.msg.Message;
import org.sofumar.portal.framework.data.response.GlobalResponse;
import org.sofumar.portal.framework.util.ResponseUtils;
import org.sofumar.portal.framework.vo.ValueObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DuplicateRecordException.class)
    public ResponseEntity<GlobalResponse> handleDuplicate(DuplicateRecordException ex) {
        GlobalResponse response = new GlobalResponse();
        ValueObject vo = ex.getVo();
        ResponseUtils.populateResponseFromVO(response, vo);
        response.setStatusCode(HttpStatus.CONFLICT.value());
        response.setStatusDesc(HttpStatus.CONFLICT.getReasonPhrase());

//        ApiErrorResponse response = new ApiErrorResponse(vo.getHttpStatus(), "Duplicate record error");
//        ApiErrorResponse response = ApiErrorResponse.of(Message.Type.ERROR, HttpStatus.CONFLICT, ex.getMessage());
//        response.addGlobalMessages(vo.getGlobalMessages());
//        response.addFieldMessages(vo.getFieldMessages());
//        return new ResponseEntity<>(response, vo.getHttpStatus());

        return ResponseUtils.withStatus(HttpStatus.CONFLICT, response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<GlobalResponse> handleValidation(ValidationException ex) {
        ValueObject vo = ex.getVo();
        GlobalResponse response = new GlobalResponse();
        ResponseUtils.populateResponseFromVO(response, vo);
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
        response.setStatusDesc(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return ResponseUtils.withStatus(HttpStatus.BAD_REQUEST, response);
    }

    //    @ExceptionHandler(DuplicateRecordException.class)
//    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateRecordException ex) {
//        return new ResponseEntity<>(ApiErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage(), ex), HttpStatus.CONFLICT);
//    }
//
//    @ExceptionHandler(StaleUserException.class)
//    public ResponseEntity<ApiErrorResponse> handleStale(StaleUserException ex) {
//        return new ResponseEntity<>(ApiErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage(), ex), HttpStatus.CONFLICT);
//    }
//
//    @ExceptionHandler(UserNotFoundException.class)
//    public ResponseEntity<ApiErrorResponse> handleNotFound(UserNotFoundException ex) {
//        return new ResponseEntity<>(ApiErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage(), ex), HttpStatus.NOT_FOUND);
//    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse> handleGeneric(Exception ex) {
        logger.error("Unexpected error occurred: ", ex);
        return ResponseUtils.withStatus(HttpStatus.INTERNAL_SERVER_ERROR, Message.Type.ERROR, "Unexpected error occurred. Please try again or contact support.");
    }

}