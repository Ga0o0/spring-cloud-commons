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

package org.springframework.cloud.context.scope;

import java.util.Collection;

/**
 * A special-purpose cache interface specifically for the {@link GenericScope} to use to
 * manage cached bean instances. Implementations generally fall into two categories: those
 * that store values "globally" (i.e. one instance per key), and those that store
 * potentially multiple instances per key based on context (e.g. via a thread local). All
 * implementations should be thread safe.
 *
 * @author Dave Syer
 *
 */
// 专用于 {@link GenericScope} 的缓存接口，用于管理缓存的 Bean 实例。
// 其实现通常分为两类：一类是“全局”存储值的接口（即每个键对应一个实例），
// 另一类是基于上下文（例如通过线程本地）每个键可能存储多个实例的接口。
// 所有实现都应该是线程安全的。
public interface ScopeCache {

	/**
	 * Removes the object with this name from the cache.
	 * @param name The object name.
	 * @return The object removed, or null if there was none.
	 */
	// 从缓存中移除此名称的对象。
	// @param name 对象名称。
	// @return 移除的对象，如果不存在则返回 null。
	Object remove(String name);

	/**
	 * Clears the cache and returns all objects in an unmodifiable collection.
	 * @return All objects stored in the cache.
	 */
	// 清除缓存并以不可修改的集合形式返回所有对象。
	// @return 缓存中存储的所有对象。
	Collection<Object> clear();

	/**
	 * Gets the named object from the cache.
	 * @param name The name of the object.
	 * @return The object with that name, or null if there is none.
	 */
	// 从缓存中获取指定名称的对象。
	// @param name 对象的名称。
	// @return 具有该名称的对象，如果不存在则返回 null。
	Object get(String name);

	/**
	 * Put a value in the cache if the key is not already used. If one is already present
	 * with the name provided, it is not replaced, but is returned to the caller.
	 * @param name The key.
	 * @param value The new candidate value.
	 * @return The value that is in the cache at the end of the operation.
	 */
	// 如果该键尚未使用，则将值放入缓存中。如果已存在一个与指定名称相同的值，则不会替换该值，而是将其返回给调用者。
	// @param name 键。
	// @param value 新的候选值。
	// @return 操作结束时缓存中的值。
	Object put(String name, Object value);

}
