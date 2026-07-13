package com.neverwhere.cautious_carnival.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.neverwhere.cautious_carnival.rest.generated.controller.RoutingController;
import com.neverwhere.cautious_carnival.rest.generated.dto.RouteDto;
import com.neverwhere.cautious_carnival.routing.RoutingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RoutingControllerImpl implements RoutingController {

	private final RoutingService routingService;

	@Override
	public ResponseEntity<RouteDto> findRoute(String origin, String destination) {
		return ResponseEntity.ok(new RouteDto(routingService.getRoute(origin, destination)));
	}

}
