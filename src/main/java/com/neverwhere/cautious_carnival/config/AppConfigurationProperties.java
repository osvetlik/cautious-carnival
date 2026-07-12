package com.neverwhere.cautious_carnival.config;

import java.net.URL;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "app")
@Validated
public record AppConfigurationProperties(@NotNull Data data) {

	/// Distinction between external and classpath data sources.
	public enum DataSourceType {
		EXTERNAL, CLASSPATH
	}

	/// Data source configuration.
	/// @param external The URL of the external data source.
	/// @param classpath The path to the classpath data source - will be located using ClassLoader..
	public record Data(@NotNull URL external, @NotBlank String classpath, @NotNull DataSourceType dataSourceType) {}

}
