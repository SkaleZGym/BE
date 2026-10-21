package com.skalez.global.error;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {return toResponse(e.getErrorCode());}
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidInput(Exception e) {return toResponse(ErrorCode.INVALID_INPUT);}
    private ResponseEntity<ErrorResponse> toResponse(ErrorCode code) {String message = messageSource.getMessage(code.messageKey(), null, LocaleContextHolder.getLocale());return ResponseEntity.status(code.status()).body(new ErrorResponse(code.name(), message));}
}