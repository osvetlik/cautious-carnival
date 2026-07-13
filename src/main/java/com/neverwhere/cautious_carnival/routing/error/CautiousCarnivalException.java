package com.neverwhere.cautious_carnival.routing.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class CautiousCarnivalException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	@RequiredArgsConstructor
	@Getter
	public enum ErrorType {
		UNKNOWN_ORIGIN_COUNTRY_CODE(HttpStatus.NOT_FOUND),
		UNKNOWN_DESTINATION_COUNTRY_CODE(HttpStatus.NOT_FOUND),
		ROUTE_UNAVAILABLE(HttpStatus.BAD_REQUEST);

		private final HttpStatus httpStatus;
	}

	private final ErrorType errorType;

}
