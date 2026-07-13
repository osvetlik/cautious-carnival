package com.neverwhere.cautious_carnival.routing;

import java.util.List;

public interface RoutingService {

	List<String> getRoute(String originCountryCode, String destinationCountryCode);

}
