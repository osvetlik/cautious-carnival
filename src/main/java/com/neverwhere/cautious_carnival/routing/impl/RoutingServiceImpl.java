package com.neverwhere.cautious_carnival.routing.impl;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.neverwhere.cautious_carnival.routing.RoutingDataHolder;
import com.neverwhere.cautious_carnival.routing.RoutingService;
import com.neverwhere.cautious_carnival.routing.error.CautiousCarnivalException;
import com.neverwhere.cautious_carnival.routing.error.CautiousCarnivalException.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoutingServiceImpl implements RoutingService {

	private final RoutingDataHolder routingDataHolder;

	@Override
	public List<String> getRoute(String originCountryCode, String destinationCountryCode) {
		final var routingData = routingDataHolder.getRoutingData();
		final var startCountryRoutes = routingData.get(originCountryCode);
		if (startCountryRoutes == null) {
			throw new CautiousCarnivalException(ErrorType.UNKNOWN_ORIGIN_COUNTRY_CODE);
		}
		if (!routingData.containsKey(destinationCountryCode)) {
			throw new CautiousCarnivalException(ErrorType.UNKNOWN_DESTINATION_COUNTRY_CODE);
		}
		if (!startCountryRoutes.containsKey(destinationCountryCode)) {
			throw new CautiousCarnivalException(ErrorType.UNKNOWN_DESTINATION_COUNTRY_CODE);
		}

		final var builder = Stream.<String>builder();
		builder.add(originCountryCode);
		String nextCountryCode = originCountryCode;
		while (!nextCountryCode.equals(destinationCountryCode)) {
			nextCountryCode = routingData.get(nextCountryCode).get(destinationCountryCode);
			builder.add(nextCountryCode);
		}
		return builder.build().toList();
	}

}
