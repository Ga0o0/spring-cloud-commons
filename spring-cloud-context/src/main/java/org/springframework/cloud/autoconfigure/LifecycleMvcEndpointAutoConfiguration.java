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

package org.springframework.cloud.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.cloud.context.environment.EnvironmentManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Auto-configuration for some MVC endpoints governing the application context lifecycle.
 * Provides restart, pause, resume, refresh (environment), and environment update
 * endpoints.
 *
 * @author Dave Syer
 *
 */
// 自动配置一些管理应用程序上下文生命周期的 MVC 端点。
// 提供重新启动、暂停、恢复、刷新（环境）和环境更新端点。
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class LifecycleMvcEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public EnvironmentManager environmentManager(ConfigurableEnvironment environment) {
		return new EnvironmentManager(environment);
	}

}
