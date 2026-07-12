package com.neverwhere.cautious_carnival.routing;

import java.util.Map;

public interface RoutingDataHolder {

	boolean ready();
	void setRoutingData(Map<String, Map<String, String>> routingData);

}
