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

package org.springframework.cloud.client.hypermedia;

import org.springframework.hateoas.Link;

/**
 * A REST resource that can be discovered and can be either gone or available.
 *
 * @author Oliver Gierke
 */
// 一个可以被发现的 REST 资源，该资源可能已不存在或可用。
public interface RemoteResource {

	/**
	 * Returns the {@link Link} to the resource if it is available, or {@literal null} if
	 * it is gone (i.e. it either is generally unavailable or can't be discovered).
	 * @return a link to the resource.
	 */
	// 如果资源可用，则返回指向该资源的 {@link Link}；如果资源已消失（即通常不可用或无法发现），则返回 {@literal null}。
	// @return 返回一个指向资源的链接。
	Link getLink();

	/**
	 * Discovers the resource if it hasn't been discovered yet or has become unavailable.
	 * If a link has been discovered previously, it is verified and either confirmed or
	 * removed to indicate that it's not available anymore.
	 */
	// 如果资源尚未被发现或已变得不可用，则进行发现。
	// 如果链接之前已被发现，则对其进行验证，并确认或删除，以表明该链接已不再可用。
	void verifyOrDiscover();

}
