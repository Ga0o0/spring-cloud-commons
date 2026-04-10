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

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.cloud.context.properties.ConfigurationPropertiesBeans;
import org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for {@link ConfigurationPropertiesRebinder}.
 *
 * @author Dave Syer
 */
// {@link ConfigurationPropertiesRebinder} 的自动配置。
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(ConfigurationPropertiesBindingPostProcessor.class)
public class ConfigurationPropertiesRebinderAutoConfiguration
		implements ApplicationContextAware, SmartInitializingSingleton {

	private ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.context = applicationContext;
	}

	@Bean
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	public static ConfigurationPropertiesBeans configurationPropertiesBeans() {
		return new ConfigurationPropertiesBeans();
	}

	@Bean
	@ConditionalOnMissingBean(search = SearchStrategy.CURRENT)
	public ConfigurationPropertiesRebinder configurationPropertiesRebinder(ConfigurationPropertiesBeans beans) {
		ConfigurationPropertiesRebinder rebinder = new ConfigurationPropertiesRebinder(beans);
		return rebinder;
	}

	@Override
	public void afterSingletonsInstantiated() {
		// After all beans are initialized explicitly rebind beans from the parent
		// so that changes during the initialization of the current context are
		// reflected. In particular this can be important when low level services like
		// decryption are bootstrapped in the parent, but need to change their
		// configuration before the child context is processed.
		// 所有 Bean 初始化完成后，显式地重新绑定父 Bean，以便当前上下文初始化期间的更改能够反映出来。
		// 当诸如解密之类的低级服务在父级中引导，但需要在处理子上下文之前更改其配置时，这一点尤其重要。
		if (this.context.getParent() != null) {
			// TODO: make this optional? (E.g. when creating child contexts that prefer to
			// be isolated.) --> 使这个成为可选的？（例如，当创建倾向于隔离的子上下文时。）
			ConfigurationPropertiesRebinder rebinder = this.context.getBean(ConfigurationPropertiesRebinder.class);
			for (String name : this.context.getParent().getBeanDefinitionNames()) {
				rebinder.rebind(name);
			}
		}
	}

}
