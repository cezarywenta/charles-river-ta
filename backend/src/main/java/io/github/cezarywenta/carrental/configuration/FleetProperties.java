package io.github.cezarywenta.carrental.configuration;

import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rental.fleet")
public record FleetProperties(@PositiveOrZero int sedan, @PositiveOrZero int suv, @PositiveOrZero int van) {
}
