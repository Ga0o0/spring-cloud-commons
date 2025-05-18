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

package org.springframework.cloud.client.serviceregistry;

/**
 * Service registration life cycle. This life cycle is only related to
 * {@link org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration#getManagementRegistration()}.
 *
 * @author Zen Huifer
 */
// 服务注册生命周期。此生命周期仅与 {@link org.springframework.cloud.client.serviceregistry.AbstractAutoServiceRegistration#getManagementRegistration()} 相关。
public interface RegistrationManagementLifecycle<R extends Registration> extends RegistrationLifecycle<R> {

	/**
	 * A method executed before registering the local management service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	// 在使用 {@link ServiceRegistry} 注册本地管理服务之前执行的方法。
	void postProcessBeforeStartRegisterManagement(R registrationManagement);

	/**
	 * A method executed after registering the local management service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	// 在使用 {@link ServiceRegistry} 注册本地管理服务之后执行的方法。
	void postProcessAfterStartRegisterManagement(R registrationManagement);

	/**
	 * A method executed before de-registering the management local service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	// 在使用 {@link ServiceRegistry} 取消注册管理本地服务之前执行的方法。
	void postProcessBeforeStopRegisterManagement(R registrationManagement);

	/**
	 * A method executed after de-registering the management local service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	// 在使用 {@link ServiceRegistry} 取消注册管理本地服务之后执行的方法。
	void postProcessAfterStopRegisterManagement(R registrationManagement);

}
