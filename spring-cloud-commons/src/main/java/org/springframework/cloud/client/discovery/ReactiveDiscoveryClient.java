/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.cloud.client.discovery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.Ordered;

/**
 * Represents read operations commonly available to discovery services such as Netflix
 * Eureka or consul.io.
 *
 * @author Tim Ysewyn
 * @author Olga Maciaszek-Sharma
 */
// 表示通常可用于发现服务（例如 Netflix Eureka 或 consul.io）的读取操作。
public interface ReactiveDiscoveryClient extends Ordered {

	Log LOG = LogFactory.getLog(ReactiveDiscoveryClient.class);

	/**
	 * Default order of the discovery client.
	 */
	// 发现客户端的默认顺序。
	int DEFAULT_ORDER = 0;

	/**
	 * A human-readable description of the implementation, used in HealthIndicator.
	 * @return The description.
	 */
	// HealthIndicator 中使用的实现的人类可读的描述。
	// @return 描述。
	String description();

	/**
	 * Gets all ServiceInstances associated with a particular serviceId.
	 * @param serviceId The serviceId to query.
	 * @return A List of ServiceInstance.
	 */
	// 获取与特定 serviceId 关联的所有 ServiceInstances。
	// @param serviceId 要查询的服务 ID。
	// @return 服务实例列表。
	Flux<ServiceInstance> getInstances(String serviceId);

	/**
	 * @return All known service IDs.
	 */
	// @return 所有已知的服务 ID。
	Flux<String> getServices();

	/**
	 * Can be used to verify the client is still valid and able to make calls.
	 * <p>
	 * A successful invocation with no exception thrown implies the client is able to make
	 * calls.
	 * <p>
	 * The default implementation simply calls {@link #getServices()} - client
	 * implementations can override with a lighter weight operation if they choose to.
	 * @deprecated in favour of {@link ReactiveDiscoveryClient#reactiveProbe()}. This
	 * method should not be used as is, as it contains a bug - the method called within
	 * returns a {@link Flux}, which is not accessible for subscription or blocking from
	 * within. We are leaving it with a deprecation in order not to bring downstream
	 * implementations.
	 */
	// 可用于验证客户端是否仍然有效且能够进行调用。
	//<p>成功调用且未抛出任何异常意味着客户端能够进行调用。
	//<p>默认实现仅调用 {@link #getServices()} - 客户端实现可以选择使用更轻量级的操作进行覆盖。
	// @deprecated 已支持 {@link ReactiveDiscoveryClient#reactiveProbe()}。
	// 此方法不应按原样使用，因为它包含一个错误 - 调用的方法会返回一个 {@link Flux}，而该 Flux 无法从内部进行订阅或阻塞。我们将其弃用，以免引入下游实现。
	@Deprecated
	default void probe() {
		if (LOG.isWarnEnabled()) {
			LOG.warn("ReactiveDiscoveryClient#probe has been called. If you're calling this method directly, "
					+ "use ReactiveDiscoveryClient#reactiveProbe instead.");
		}
		getServices();
	}

	/**
	 * Can be used to verify the client is still valid and able to make calls.
	 * <p>
	 * A successful invocation with no exception thrown implies the client is able to make
	 * calls.
	 * <p>
	 * The default implementation simply calls {@link #getServices()} and wraps it with a
	 * {@link Mono} - client implementations can override with a lighter weight operation
	 * if they choose to.
	 */
	// 可用于验证客户端是否仍然有效且能够进行调用。
	// <p>成功调用且未抛出任何异常意味着客户端能够进行调用。
	// <p>默认实现只是调用 {@link #getServices()} 并将其包装到 {@link Mono} 中 - 客户端实现可以选择使用更轻量的操作进行覆盖。
	default Mono<Void> reactiveProbe() {
		return getServices().then();
	}

	/**
	 * Default implementation for getting order of discovery clients.
	 * @return order
	 */
	// 获取发现客户端顺序的默认实现。
	@Override
	default int getOrder() {
		return DEFAULT_ORDER;
	}

}
