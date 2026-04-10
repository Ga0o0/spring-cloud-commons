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

package org.springframework.cloud.bootstrap;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker interface used as a key in <code>META-INF/spring.factories</code>. Entries in
 * the factories file are used to create the bootstrap application context.
 *
 * @author Dave Syer
 *
 */
// 一个标记接口，用作 <code>META-INF/spring.factories</code> 中的键。
// factories 文件中的条目用于创建引导应用程序上下文。
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BootstrapConfiguration {

	/**
	 * Excludes specific auto-configuration classes such that they will never be applied.
	 * @return classes to exclude
	 */
	// 排除特定的自动配置类，使它们永远不会被应用。
	// @return 要排除的类
	Class<?>[] exclude() default {};

}
