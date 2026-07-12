package com.neverwhere.cautious_carnival.routing.impl;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.neverwhere.cautious_carnival.routing.RoutingData;
import com.neverwhere.cautious_carnival.routing.RoutingDataHolder;

@Component
public class RoutingDataHolderImpl implements RoutingDataHolder {

	private final AtomicReference<RoutingData> routingDataRef = new AtomicReference<>();

	@Override
	public boolean ready() {
		return routingDataRef.get() != null;
	}

	@Override
	public void setRoutingData(RoutingData routingData) {
		if (!routingDataRef.compareAndSet(null, routingData)) {
			throw new IllegalStateException("Routing data has already been set and cannot be changed.");
		}
	}

}
