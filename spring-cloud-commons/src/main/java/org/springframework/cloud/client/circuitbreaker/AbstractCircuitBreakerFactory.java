/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.circuitbreaker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for factories which produce circuit breakers.
 *
 * @author Ryan Baxter
 */
// 生产断路器的工厂的基类。
public abstract class AbstractCircuitBreakerFactory<CONF, CONFB extends ConfigBuilder<CONF>> {

	private final ConcurrentHashMap<String, CONF> configurations = new ConcurrentHashMap<>();

	/**
	 * Adds configurations for circuit breakers.
	 * @param ids The id of the circuit breaker
	 * @param consumer A configuration builder consumer, allows consumers to customize the
	 * builder before the configuration is built
	 */
	// 添加断路器配置。
	// @param ids 断路器 ID
	// @param consumer 配置构建器消费者，允许消费者在配置构建之前自定义构建器
	public void configure(Consumer<CONFB> consumer, String... ids) {
		for (String id : ids) {
			CONFB builder = configBuilder(id);
			consumer.accept(builder);
			CONF conf = builder.build();
			getConfigurations().put(id, conf);
		}
	}

	/**
	 * Gets the configurations for the circuit breakers.
	 * @return The configurations
	 */
	// 获取断路器的配置。
	// @return 配置
	protected ConcurrentHashMap<String, CONF> getConfigurations() {
		return configurations;
	}

	/**
	 * Creates a configuration builder for the given id.
	 * @param id The id of the circuit breaker
	 * @return The configuration builder
	 */
	// 根据给定的 ID 创建配置构建器。
	// @param id 断路器的 ID
	// @return 配置构建器
	protected abstract CONFB configBuilder(String id);

	/**
	 * Sets the default configuration for circuit breakers.
	 * @param defaultConfiguration A function that returns the default configuration
	 */
	// 设置断路器的默认配置。
	// @param defaultConfiguration 返回默认配置的函数
	public abstract void configureDefault(Function<String, CONF> defaultConfiguration);

}
