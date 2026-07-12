package com.neverwhere.cautious_carnival.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.neverwhere.cautious_carnival.CautiousCarnivalApplication;

@Configuration
@EnableScheduling
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackageClasses = {
		CautiousCarnivalApplication.class,
})
public class CautiousCarnivalApplicationConfiguration {

}
