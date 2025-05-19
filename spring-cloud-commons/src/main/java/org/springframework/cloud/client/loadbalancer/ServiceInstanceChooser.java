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

package org.springframework.cloud.client.loadbalancer;

import org.springframework.cloud.client.ServiceInstance;

/**
 * Implemented by classes which use a load balancer to choose a server to send a request
 * to.
 *
 * @author Ryan Baxter
 * @author Olga Maciaszek-Sharma
 */
// 由使用负载平衡器选择要向其发送请求的服务器的类实现。
public interface ServiceInstanceChooser {

	/**
	 * Chooses a ServiceInstance from the LoadBalancer for the specified service.
	 * @param serviceId The service ID to look up the LoadBalancer.
	 * @return A ServiceInstance that matches the serviceId.
	 */
	// 从 LoadBalancer 中为指定服务选择一个 ServiceInstance。
	// @param serviceId 用于查找 LoadBalancer 的服务 ID。
	// @return 与 serviceId 匹配的 ServiceInstance。
	ServiceInstance choose(String serviceId);

	/**
	 * Chooses a ServiceInstance from the LoadBalancer for the specified service and
	 * LoadBalancer request.
	 * @param serviceId The service ID to look up the LoadBalancer.
	 * @param request The request to pass on to the LoadBalancer
	 * @param <T> The type of the request context.
	 * @return A ServiceInstance that matches the serviceId.
	 */
	// 从 LoadBalancer 中为指定的服务和 LoadBalancer 请求选择一个 ServiceInstance。
	// @param serviceId 用于查找 LoadBalancer 的服务 ID。
	// @param request 要传递给 LoadBalancer 的请求。
	// @param <T> 请求上下文的类型。
	// @return 与 serviceId 匹配的 ServiceInstance。
	<T> ServiceInstance choose(String serviceId, Request<T> request);

}
