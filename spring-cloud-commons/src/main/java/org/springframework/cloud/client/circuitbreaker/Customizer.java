/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.client.circuitbreaker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Customizes the parameterized class.
 *
 * @author Ryan Baxter
 * @author Toshiaki Maki
 */
// 自定义参数化类。
public interface Customizer<TOCUSTOMIZE> {

	void customize(TOCUSTOMIZE tocustomize);

	/**
	 * Create a wrapped customizer that guarantees that the {@link #customize(Object)}
	 * method of the delegated <code>customizer</code> is called at most once per target.
	 * @param customizer a customizer to be delegated
	 * @param keyMapper a mapping function to produce the identifier of the target
	 * @param <T> the type of the target to customize
	 * @param <K> the type of the identifier of the target
	 * @return a wrapped customizer
	 */
	// 创建一个包装好的定制器，保证被委托的 <code>customizer</code> 的 {@link #customize(Object)}
	// 方法在每个目标上最多被调用一次。
	// @param customizer 被委托的定制器
	// @param keyMapper 生成目标标识符的映射函数
	// @param <T> 待定制目标的类型
	// @param <K> 目标标识符的类型
	// @return 包装好的定制器
	static <T, K> Customizer<T> once(Customizer<T> customizer, Function<? super T, ? extends K> keyMapper) {
		final ConcurrentMap<K, Boolean> customized = new ConcurrentHashMap<>();
		return t -> {
			final K key = keyMapper.apply(t);
			customized.computeIfAbsent(key, k -> {
				customizer.customize(t);
				return true;
			});
		};
	}

}
