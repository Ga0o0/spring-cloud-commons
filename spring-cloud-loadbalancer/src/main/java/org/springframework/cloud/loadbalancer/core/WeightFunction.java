/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.cloud.client.ServiceInstance;

/**
 * Represents a function that calculate the weight of the given service instance.
 *
 * <p>
 * This is a functional interface whose functional method is
 * {@link #apply(ServiceInstance)}.
 *
 * @author Zhuozhi Ji
 * @see java.util.function.ToIntFunction
 */
// 表示一个计算给定服务实例权重的函数。
//
// <p>这是一个函数式接口，其函数式方法是 {@link #apply(ServiceInstance)}。
@FunctionalInterface
public interface WeightFunction {

	/**
	 * Applies this function to the given service instance.
	 * @param instance the service instance
	 * @return the weight of service instance
	 */
	// 将此函数应用于给定的服务实例。
	// @param instance 服务实例
	// @return 服务实例的权重
	int apply(ServiceInstance instance);

}
