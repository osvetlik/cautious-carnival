package com.neverwhere.cautious_carnival.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.neverwhere.cautious_carnival.routing.error.CautiousCarnivalException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
class ErrorHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(exception = CautiousCarnivalException.class)
	ProblemDetail handle(CautiousCarnivalException e) {
		return ProblemDetail.forStatus(e.getErrorType().getHttpStatus());
	}

	@ExceptionHandler(exception = ConstraintViolationException.class)
	ProblemDetail handle(ConstraintViolationException e) {
		return ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
	}

}
