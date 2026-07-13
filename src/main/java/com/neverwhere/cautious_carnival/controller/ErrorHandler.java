package com.neverwhere.cautious_carnival.controller;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.neverwhere.cautious_carnival.routing.error.CautiousCarnivalException;

@RestControllerAdvice
class ErrorHandler {

	@ExceptionHandler(exception = CautiousCarnivalException.class)
	ProblemDetail handle(CautiousCarnivalException e) {
		return ProblemDetail.forStatus(e.getErrorType().getHttpStatus());
	}

}
