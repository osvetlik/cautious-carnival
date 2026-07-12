package com.neverwhere.cautious_carnival.routing;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
		}
		catch (Exception e) {
			// TODO: terminate on certain exceptions, sometimes we know retries won't help.
			log.warn("Cannot load data, retry in {}", DATA_LOAD_RETRY_PERIOD, e);
			taskScheduler.schedule(this::tryLoading, Instant.now().plus(DATA_LOAD_RETRY_PERIOD));
		}
	}

	private RoutingData loadRoutingData() throws IOException {
		final var dataSourceUrl = selectDataSource();

		try (final var inputStream = dataSourceUrl.openStream()) {
			final var loaded = jsonMapper.readValue(inputStream, new TypeReference<List<CountryJson>>() {});
			log.info("Loaded:\n{}", loaded);
			return new RoutingData();
		}

	}

	private URL selectDataSource() {
		if (appConfigurationProperties.data().dataSourceType() == DataSourceType.CLASSPATH) {
			return this.getClass().getClassLoader().getResource(appConfigurationProperties.data().classpath());
		}

		return appConfigurationProperties.data().external();
	}

}
