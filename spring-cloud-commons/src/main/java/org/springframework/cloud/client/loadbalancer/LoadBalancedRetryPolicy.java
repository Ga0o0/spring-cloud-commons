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
 * Retry logic to use for the {@link LoadBalancerClient}.
 *
 * @author Ryan Baxter
 * @author Olga Maciaszek-Sharma
 */
// 重试用于 {@link LoadBalancerClient} 的逻辑。
public interface LoadBalancedRetryPolicy {

	/**
	 * Return true to retry the failed request on the same server. This method may be
	 * called more than once when executing a single operation.
	 * @param context The context for the retry operation.
	 * @return True to retry the failed request on the same server; false otherwise.
	 */
	// 返回 true 则在同一服务器上重试失败的请求。此方法可能在执行单个操作时被多次调用。
	// @param context 重试操作的上下文。
	// @return True 则在同一服务器上重试失败的请求；否则为 false。
	boolean canRetrySameServer(LoadBalancedRetryContext context);

	/**
	 * Return true to retry the failed request on the next server from the load balancer.
	 * This method may be called more than once when executing a single operation.
	 * @param context The context for the retry operation.
	 * @return True to retry the failed request on the next server from the load balancer;
	 * false otherwise.
	 */
	// 返回 true 则表示从负载均衡器在下一个服务器上重试失败的请求。执行单个操作时，此方法可能会被多次调用。
	// @param context 重试操作的上下文。
	// @return True 则表示从负载均衡器在下一个服务器上重试失败的请求；否则，返回 false。
	boolean canRetryNextServer(LoadBalancedRetryContext context);

	/**
	 * Called when the retry operation has ended.
	 * @param context The context for the retry operation.
	 */
	// 重试操作结束时调用。
	// @param context 重试操作的上下文。
	void close(LoadBalancedRetryContext context);

	/**
	 * Called when the execution fails.
	 * @param context The context for the retry operation.
	 * @param throwable The throwable from the failed execution.
	 */
	// 执行失败时调用。
	// @param context 重试操作的上下文。
	// @param throwable 执行失败时抛出的异常。
	void registerThrowable(LoadBalancedRetryContext context, Throwable throwable);

	/**
	 * If an exception is not thrown when making a request, this method will be called to
	 * see if the client would like to retry the request based on the status code
	 * returned. For example, in Cloud Foundry, the router will return a <code>404</code>
	 * when an app is not available. Since HTTP clients do not throw an exception when a
	 * <code>404</code> is returned, <code>retryableStatusCode</code> allows clients to
	 * force a retry.
	 * @param statusCode The HTTP status code.
	 * @return True if a retry should be attempted; false to just return the response.
	 */
	// 如果发出请求时未抛出异常，则会调用此方法，根据返回的状态码判断客户端是否希望重试请求。
	// 例如，在 Cloud Foundry 中，当应用不可用时，路由器将返回 <code>404</code>。
	// 由于 HTTP 客户端在返回 <code>404</code> 时不会抛出异常，因此 <code>retryableStatusCode</code>
	// 允许客户端强制重试。
	// @param statusCode HTTP 状态码。
	// @return 设置为 True 则表示应尝试重试；设置为 false 则表示仅返回响应。
	boolean retryableStatusCode(int statusCode);

	/**
	 * Return <code>true</code> to retry if the provided exception is thrown.
	 * @param exception the {@link Throwable} to evaluate
	 * @return true to retry on the provided exception
	 */
	// 如果抛出了指定的异常，则返回 <code>true</code> 进行重试。
	// @param exception 需要计算的 {@link Throwable}
	// @return true 针对指定的异常进行重试
	boolean retryableException(Throwable exception);

}
