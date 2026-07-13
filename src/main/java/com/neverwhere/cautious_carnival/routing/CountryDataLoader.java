package com.neverwhere.cautious_carnival.routing;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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

	private static record Hop(String code, int distance) {}

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
			log.info("Sanity check: CZE -> {}", routingData.get("CZE"));
			log.info("Sanity check: NOR -> ITA: {}", routingData.get("NOR").get("ITA"));
			log.info("Sanity check: RUS -> ITA: {}", routingData.get("RUS").get("ITA"));
			log.info("Sanity check: POL -> ITA: {}", routingData.get("POL").get("ITA"));
			log.info("Sanity check: DEU -> ITA: {}", routingData.get("DEU").get("ITA"));
			log.info("Sanity check: AUT -> ITA: {}", routingData.get("AUT").get("ITA"));
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

		return workingDataMap.entrySet().stream().collect(
				Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().entrySet().stream().collect(Collectors
						.toMap(Map.Entry::getKey, hopEntry -> hopEntry.getValue().code()))));
	}


	private boolean processAllNeighbors(String me, Map<String, Map<String, Hop>> routingData) {
		final var myHops = routingData.get(me);
		final var neighbors = myHops.values().stream()
				.map(Hop::code)
				.filter(Predicate.not(me::equals))
				.collect(Collectors.toSet());
		log.info("Processing my: {}\nneighbors {} ", me, neighbors);
		return neighbors.stream()
				.map(neighbor -> addNeighborsHops(me, myHops, neighbor, routingData.get(neighbor)))
				.reduce(false, (a, b) -> a || b);
	}

	private boolean addNeighborsHops(String me, Map<String, Hop> myHops, String neighbor,
			Map<String, Hop> neighborsHops) {
		log.info("Testing my hops\n{}\nagainst neighbor {} hops\n{}", myHops, neighbor, neighborsHops);
		final var newHops = neighborsHops.entrySet().stream()
				.filter(newHopEntry -> !neighbor.equals(newHopEntry.getKey()))
				.filter(newHopEntry -> !me.equals(newHopEntry.getKey()))
				.filter(newHopEntry -> isBetterHop(myHops, newHopEntry))
				.collect(Collectors.toSet());
		if (newHops.isEmpty()) {
			return false;
		}
		final var bestHops = newHops.stream()
				.collect(
						Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a.distance() < b.distance() ? a : b));
		log.info("Adding new hops {}", bestHops);
		bestHops.entrySet().forEach(e -> myHops.put(e.getKey(), new Hop(neighbor, e.getValue().distance() + 1)));
		return true;
	}

	private boolean isBetterHop(Map<String, Hop> myHops, Map.Entry<String, Hop> neighborHopEntry) {
		final var newHop = neighborHopEntry.getValue();
		final var existingHop = myHops.get(neighborHopEntry.getKey());
		log.info("Comparing new {} hop {} with mine {}", neighborHopEntry.getKey(), newHop, existingHop);
		if (existingHop == null) {
			return true;
		}
		return (newHop.distance() + 1) < existingHop.distance();
	}

	private Map<String, Hop> initCountryEntry(CountryJson countryJson) {
		final var countryEntry = new HashMap<String, Hop>();
		final var me = countryJson.cca3();
		countryEntry.put(me, new Hop(me, 0));
		countryEntry.putAll(countryJson.borders().stream()
				.collect(Collectors.toMap(Function.identity(), neighbor -> new Hop(neighbor, 1))));
		return countryEntry;
	}
	private URL selectDataSource() {
		if (appConfigurationProperties.data().dataSourceType() == DataSourceType.CLASSPATH) {
			return this.getClass().getClassLoader().getResource(appConfigurationProperties.data().classpath());
		}

		return appConfigurationProperties.data().external();
	}

}
