package com.neverwhere.cautious_carnival.health;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import com.neverwhere.cautious_carnival.routing.RoutingDataHolder;

import lombok.RequiredArgsConstructor;

@Component("routingData")
@RequiredArgsConstructor
public class RoutingDataHealthIndicator implements HealthIndicator {

	private static Health READY = Health.up().build();
	private static Health OUT_OF_SERVICE = Health.outOfService().build();

	private final RoutingDataHolder routingDataHolder;

	@Override
	public @Nullable Health health() {
		return routingDataHolder.ready() ? READY : OUT_OF_SERVICE;
	}

}
