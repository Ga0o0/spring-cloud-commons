/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.cloud.context.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A custom event for spring cloud context use cases that need the saved Spring
 * Application. This prevents a duplicated ApplicationPreparedEvent from being
 * republished.
 */
// 针对需要保存 Spring Application 的 Spring Cloud 上下文用例的自定义事件。
// 这可以防止重复发布 ApplicationPreparedEvent。
public class ContextRefreshedWithApplicationEvent extends SpringApplicationEvent {

	private final ConfigurableApplicationContext context;

	/**
	 * Create a new {@link ContextRefreshedWithApplicationEvent} instance.
	 * @param application the current application
	 * @param args the arguments the application is running with
	 * @param context the ApplicationContext about to be refreshed
	 */
	// 创建一个新的 {@link ContextRefreshedWithApplicationEvent} 实例。
	// @param application 当前应用程序
	// @param args 应用程序运行时使用的参数
	// @param context 即将刷新的 ApplicationContext
	public ContextRefreshedWithApplicationEvent(SpringApplication application, String[] args,
			ConfigurableApplicationContext context) {
		super(application, args);
		this.context = context;
	}

	/**
	 * Return the application context.
	 * @return the context
	 */
	// 返回应用程序上下文。
	// @return the context
	public ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

}
