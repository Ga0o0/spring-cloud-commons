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

/**
 * Allows to define actions that should be carried out before and after load-balancing.
 *
 * @author Olga Maciaszek-Sharma
 */
// 允许定义负载平衡之前和之后应执行的操作。
public interface LoadBalancerLifecycle<RC, RES, T> {

	/**
	 * Allows to assess whether the lifecycle bean's callbacks should be executed. Some
	 * examples of possible implementations could comprise of verifying whether the
	 * classes passed in parameters are exactly or extend the classes that this lifecycle
	 * bean should process.
	 * @param requestContextClass The class of the {@link Request} <code>context</code>
	 * @param responseClass The class of the {@link CompletionContext}
	 * <code>clientResponse</code>
	 * @param serverTypeClass The type of Server that the LoadBalancer retrieves
	 * @return <code>true</code> if the lifecycle should be used to process given classes
	 */
	// 允许评估是否应执行生命周期 bean 的回调。
	// 一些可能的实现示例包括验证传入参数的类是否与此生命周期 bean 应处理的类完全相同，或者是否扩展了这些类。
	// @param requestContextClass {@link Request} 的 <code>context</code> 类
	// @param respondClass {@link CompletionContext} 的类 <code>clientResponse</code>
	// @param serverTypeClass LoadBalancer 检索的服务器类型
	// @return <code>true</code> 表示应使用生命周期来处理给定的类
	default boolean supports(Class requestContextClass, Class responseClass, Class serverTypeClass) {
		return true;
	}

	/**
	 * A callback method executed before load-balancing.
	 * @param request the {@link Request} that will be used by the LoadBalancer to select
	 * a service instance
	 */
	// 负载均衡前执行的回调方法。
	// @param request {@link Request}，负载均衡器将使用该请求来选择服务实例
	void onStart(Request<RC> request);

	/**
	 * A callback method executed after a service instance has been selected, before
	 * executing the actual load-balanced request.
	 * @param request the {@link Request} that has been used by the LoadBalancer to select
	 * a service instance
	 * @param lbResponse the {@link Response} returned by the LoadBalancer
	 */
	// 在选择服务实例后、执行实际负载均衡请求之前执行的回调方法。
	// @param request 负载均衡器用于选择服务实例的 {@link Request}
	// @param lbResponse 负载均衡器返回的 {@link Response}
	void onStartRequest(Request<RC> request, Response<T> lbResponse);

	/**
	 * A callback method executed after load-balancing.
	 * @param completionContext the {@link CompletionContext} containing data relevant to
	 * the load-balancing and the response returned from the selected service instance
	 */
	// 负载均衡后执行的回调方法。
	// @param completionContext {@link CompletionContext} 包含与负载均衡相关的数据以及从所选服务实例返回的响应
	void onComplete(CompletionContext<RES, T, RC> completionContext);

}
