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

import java.net.URI;

import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryException;

/**
 * An implementation of {@link RecoveryCallback} which relies on an implementation of
 * {@link RetryableStatusCodeException} to contain the last response object from the
 * request.
 *
 * @param <T> - response type to return
 * @param <R> - response type from the HTTP client
 * @author LiYuan Lee
 */
// {@link RecoveryCallback} 的实现依赖于 {@link RetryableStatusCodeException} 的实现来包含来自请求的最后一个响应对象。
public abstract class LoadBalancedRecoveryCallback<T, R> implements RecoveryCallback<T> {

	/**
	 * Creates the response returned in the {@link RecoveryCallback}.
	 * @param response The response from the HTTP client.
	 * @param uri The URI the response is from.
	 * @return The response to be returned.
	 */
	// 创建 {@link RecoveryCallback} 中返回的响应。
	// @param respond 来自 HTTP 客户端的响应。
	// @param uri 响应的 URI。
	// @return 要返回的响应。
	protected abstract T createResponse(R response, URI uri);

	@Override
	public T recover(RetryContext context) throws Exception {
		Throwable lastThrowable = context.getLastThrowable();
		if (lastThrowable != null) {
			if (lastThrowable instanceof RetryableStatusCodeException ex) {
				return createResponse((R) ex.getResponse(), ex.getUri());
			}
			else if (lastThrowable instanceof Exception) {
				throw (Exception) lastThrowable;
			}
		}
		throw new RetryException("Could not recover", lastThrowable);
	}

}
