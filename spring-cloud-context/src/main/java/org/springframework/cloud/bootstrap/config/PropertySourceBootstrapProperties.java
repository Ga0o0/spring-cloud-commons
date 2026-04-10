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

package org.springframework.cloud.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Spring Cloud Config bootstrap.
 *
 * @author Dave Syer
 */
// Spring Cloud Config 引导程序的属性。
@ConfigurationProperties("spring.cloud.config")
public class PropertySourceBootstrapProperties {

	/**
	 * Flag to indicate that the external properties should override system properties.
	 * Default true.
	 */
	// 用于指示外部属性是否应覆盖系统属性的标志。
	// 默认为 true。
	private boolean overrideSystemProperties = true;

	/**
	 * Flag to indicate that {@link #isOverrideSystemProperties()
	 * systemPropertiesOverride} can be used. Set to false to prevent users from changing
	 * the default accidentally. Default true.
	 */
	// 用于指示是否可以使用 {@link #isOverrideSystemProperties() systemPropertiesOverride} 的标志。
	// 设置为 false 可防止用户意外更改默认值。默认为 true。
	private boolean allowOverride = true;

	/**
	 * Flag to indicate that when {@link #setAllowOverride(boolean) allowOverride} is
	 * true, external properties should take lowest priority and should not override any
	 * existing property sources (including local config files). Default false. This will
	 * only have an effect when using config first bootstrap.
	 */
	// 用于指示当 {@link #setAllowOverride(boolean) allowOverride} 为 true 时，
	// 外部属性应具有最低优先级，并且不应覆盖任何现有属性源（包括本地配置文件）。
	// 默认为 false。这仅在使用配置优先引导程序时有效。
	private boolean overrideNone = false;

	/**
	 * Flag to initialize bootstrap configuration on context refresh event. Default false.
	 */
	// 用于在上下文刷新事件中初始化引导程序配置的标志。默认为 false。
	private boolean initializeOnContextRefresh = false;

	public boolean isInitializeOnContextRefresh() {
		return initializeOnContextRefresh;
	}

	public void setInitializeOnContextRefresh(boolean initializeOnContextRefresh) {
		this.initializeOnContextRefresh = initializeOnContextRefresh;
	}

	public boolean isOverrideNone() {
		return this.overrideNone;
	}

	public void setOverrideNone(boolean overrideNone) {
		this.overrideNone = overrideNone;
	}

	public boolean isOverrideSystemProperties() {
		return this.overrideSystemProperties;
	}

	public void setOverrideSystemProperties(boolean overrideSystemProperties) {
		this.overrideSystemProperties = overrideSystemProperties;
	}

	public boolean isAllowOverride() {
		return this.allowOverride;
	}

	public void setAllowOverride(boolean allowOverride) {
		this.allowOverride = allowOverride;
	}

}
