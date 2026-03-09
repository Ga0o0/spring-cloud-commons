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

package org.springframework.cloud.loadbalancer.core;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;

/**
 * A Reactor based implementation of {@link ReactiveLoadBalancer}.
 *
 * @param <T> - type of the response
 * @author Spencer Gibb
 */
// 基于 Reactor 的 {@link ReactiveLoadBalancer} 实现。
public interface ReactorLoadBalancer<T> extends ReactiveLoadBalancer<T> {

	/**
	 * Choose the next server based on the load balancing algorithm.
	 * @param request - an input request
	 * @return - mono of response
	 */
	// 根据负载均衡算法选择下一个服务器。
	// @param request - 输入请求
	// @return - 响应的 mono 对象
	@SuppressWarnings("rawtypes")
	Mono<Response<T>> choose(Request request);

	default Mono<Response<T>> choose() {
		return choose(REQUEST);
	}

}
