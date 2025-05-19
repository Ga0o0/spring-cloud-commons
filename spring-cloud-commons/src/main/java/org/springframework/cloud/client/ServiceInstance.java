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

package org.springframework.cloud.client;

import java.net.URI;
import java.util.Map;

/**
 * Represents an instance of a service in a discovery system.
 *
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
// 表示发现系统中服务的一个实例。
public interface ServiceInstance {

	/**
	 * @return The unique instance ID as registered.
	 */
	// @return 注册的唯一实例 ID。
	default String getInstanceId() {
		return null;
	}

	/**
	 * @return The service ID as registered.
	 */
	// @return 注册的服务 ID。
	String getServiceId();

	/**
	 * @return The hostname of the registered service instance.
	 */
	// @return 已注册服务实例的主机名。
	String getHost();

	/**
	 * @return The port of the registered service instance.
	 */
	// @return 已注册服务实例的端口。
	int getPort();

	/**
	 * @return Whether the port of the registered service instance uses HTTPS.
	 */
	// @return 已注册服务实例的端口是否使用 HTTPS。
	boolean isSecure();

	/**
	 * @return The service URI address.
	 */
	// @return 服务 URI 地址。
	URI getUri();

	/**
	 * @return The key / value pair metadata associated with the service instance.
	 */
	// @return 与服务实例关联的键/值对元数据。
	Map<String, String> getMetadata();

	/**
	 * @return The scheme of the service instance.
	 */
	// @return 服务实例的 scheme。
	default String getScheme() {
		return null;
	}

}
