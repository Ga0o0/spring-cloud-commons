# 监控和追踪 CircuitBreaker

Spring Cloud Commons 通过在断路器机制中集成 Micrometer Observation 来实现对断路器运行状态的监控和追踪。

## ObservedCircuitBreaker

```java
// 观察到的断路器。
public class ObservedCircuitBreaker implements CircuitBreaker {
	private final CircuitBreaker delegate;
	private final ObservationRegistry observationRegistry;
	private CircuitBreakerObservationConvention customConvention;
    // ...
	@Override
	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback) {
		return this.delegate.run(
				new ObservedSupplier<>(this.customConvention,
						new CircuitBreakerObservationContext(CircuitBreakerObservationContext.Type.SUPPLIER),
						"circuit-breaker", this.observationRegistry, toRun),
				new ObservedFunction<>(this.customConvention,
						new CircuitBreakerObservationContext(CircuitBreakerObservationContext.Type.FUNCTION),
						"circuit-breaker fallback", this.observationRegistry, fallback));
	}

	@Override
	public <T> T run(Supplier<T> toRun) {
		return this.delegate.run(new ObservedSupplier<>(this.customConvention,
				new CircuitBreakerObservationContext(CircuitBreakerObservationContext.Type.SUPPLIER), "circuit-breaker",
				this.observationRegistry, toRun));
	}
    // ...
}
```

## ObservedSupplier 和 ObservedFunction

### ObservedSupplier

```java
import java.util.function.Supplier;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

class ObservedSupplier<T> implements Supplier<T> {

	private final Supplier<T> delegate;
	private final Observation observation;

	ObservedSupplier(CircuitBreakerObservationConvention customConvention, CircuitBreakerObservationContext context,
			String contextualName, ObservationRegistry observationRegistry, Supplier<T> toRun) {
		this.delegate = toRun;
		this.observation = CircuitBreakerObservationDocumentation.CIRCUIT_BREAKER_SUPPLIER_OBSERVATION
			.observation(customConvention, DefaultCircuitBreakerObservationConvention.INSTANCE, () -> context,
					observationRegistry)
			.parentObservation(observationRegistry.getCurrentObservation());
		this.observation.contextualName(contextualName);
	}

	@Override
	public T get() {
		return this.observation.observe(this.delegate);
	}

}
```

### ObservedFunction

```java
import java.util.function.Supplier;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

class ObservedSupplier<T> implements Supplier<T> {

	private final Supplier<T> delegate;
	private final Observation observation;

	ObservedSupplier(CircuitBreakerObservationConvention customConvention, CircuitBreakerObservationContext context,
			String contextualName, ObservationRegistry observationRegistry, Supplier<T> toRun) {
		this.delegate = toRun;
		this.observation = CircuitBreakerObservationDocumentation.CIRCUIT_BREAKER_SUPPLIER_OBSERVATION
			.observation(customConvention, DefaultCircuitBreakerObservationConvention.INSTANCE, () -> context,
					observationRegistry)
			.parentObservation(observationRegistry.getCurrentObservation());
		this.observation.contextualName(contextualName);
	}

	@Override
	public T get() {
		return this.observation.observe(this.delegate);
	}
}
```

## ObservedSupplier 和 ObservedFunction 使用的 Observation.Context / ObservationConvention / ObservationDocumentation

### Observation.Context

```java
import io.micrometer.observation.Observation;
public class CircuitBreakerObservationContext extends Observation.Context {
    // ...
}
```

### ObservationConvention

```java
import io.micrometer.observation.ObservationConvention;
public interface CircuitBreakerObservationConvention extends ObservationConvention<CircuitBreakerObservationContext> {
    // ...
}
public class DefaultCircuitBreakerObservationConvention implements CircuitBreakerObservationConvention {
    // ...
}
```

### ObservationDocumentation

```java
import io.micrometer.observation.docs.ObservationDocumentation;
enum CircuitBreakerObservationDocumentation implements ObservationDocumentation {
    // ...
}
```
