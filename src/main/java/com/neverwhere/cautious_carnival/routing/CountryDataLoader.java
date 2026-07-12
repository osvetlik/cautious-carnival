package com.neverwhere.cautious_carnival.routing;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.neverwhere.cautious_carnival.config.AppConfigurationProperties;
import com.neverwhere.cautious_carnival.config.AppConfigurationProperties.DataSourceType;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
class CountryDataLoader {

	private static final Duration DATA_LOAD_RETRY_PERIOD = Duration.parse("PT10S");

	private final TaskScheduler taskScheduler;
	private final RoutingDataHolder routingDataHolder;
	private final AppConfigurationProperties appConfigurationProperties;
	private final JsonMapper jsonMapper;

	@EventListener(classes = {
			ApplicationReadyEvent.class,
	})
	void tryLoading() {
		try {
			final var routingData = loadRoutingData();
			routingDataHolder.setRoutingData(routingData);
			log.info("Sanity check: BRA -> {}", routingData.get("BRA"));
			log.info("Sanity check: NOR -> ITA: {}", routingData.get("NOR").get("ITA"));
			log.info("Sanity check: NOR -> VNM: {}", routingData.get("NOR").get("VNM"));
		}
		catch (Exception e) {
			// TODO: terminate on certain exceptions, sometimes we know retries won't help.
			log.warn("Cannot load data, retry in {}", DATA_LOAD_RETRY_PERIOD, e);
			taskScheduler.schedule(this::tryLoading, Instant.now().plus(DATA_LOAD_RETRY_PERIOD));
		}
	}

	private Map<String, Map<String, String>> loadRoutingData() throws IOException {
		final var dataSourceUrl = selectDataSource();

		try (final var inputStream = dataSourceUrl.openStream()) {
			final var loaded = jsonMapper.readValue(inputStream, new TypeReference<List<CountryJson>>() {});
			log.info("Loaded:\n{}", loaded);
			return parseRoutingData(loaded);
		}

	}

	private Map<String, Map<String, String>> parseRoutingData(List<CountryJson> countryJsonList) {
		final var workingDataMap = countryJsonList.stream()
				.collect(Collectors.toMap(CountryJson::cca3, this::initCountryEntry));
		final var remainingCountries = new ArrayList<>(workingDataMap.keySet());

		boolean firstPass = true;

		while (!remainingCountries.isEmpty()) {
			final var noNewHops = remainingCountries.stream()
					.filter(me -> !processAllNeighbors(me, workingDataMap))
					.toList();
			if (!firstPass) {
				remainingCountries.removeAll(noNewHops);
				log.info("Removing {}, remaining: {}", noNewHops, remainingCountries);
			}
			else {
				firstPass = false;
				log.info("First pass, not removing any countries yet, remaining: {}", remainingCountries);
			}
		}

		return workingDataMap;
	}


	private boolean processAllNeighbors(String me, Map<String, Map<String, String>> routingData) {
		final var myHops = routingData.get(me);
		final var neighbors = new HashSet<>(routingData.get(me).values());
		log.info("Processing neighbors {} for {}", neighbors, me);
		return neighbors.stream()
				.filter(Predicate.not(me::equals))
				.map(neighbor -> addNeighborsHops(myHops, neighbor, routingData.get(neighbor)))
				.reduce(false, (a, b) -> a || b);
	}

	private boolean addNeighborsHops(Map<String, String> myHops, String neighbor,
			Map<String, String> neighborsHops) {
		final var newHops = neighborsHops.keySet().stream()
				.filter(Predicate.not(myHops::containsKey))
				.collect(Collectors.toSet());
		if (newHops.isEmpty()) {
			return false;
		}
		log.info("Adding new hops {}", newHops);
		myHops.putAll(newHops.stream().collect(Collectors.toMap(Function.identity(), _ -> neighbor)));
		return true;
	}

	private Map<String, String> initCountryEntry(CountryJson countryJson) {
		final var countryEntry = new HashMap<String, String>();
		final var me = countryJson.cca3();
		countryEntry.put(me, me);
		countryEntry.putAll(
				countryJson.borders().stream().collect(Collectors.toMap(Function.identity(), Function.identity())));
		return countryEntry;
	}
	private URL selectDataSource() {
		if (appConfigurationProperties.data().dataSourceType() == DataSourceType.CLASSPATH) {
			return this.getClass().getClassLoader().getResource(appConfigurationProperties.data().classpath());
		}

		return appConfigurationProperties.data().external();
	}

}
