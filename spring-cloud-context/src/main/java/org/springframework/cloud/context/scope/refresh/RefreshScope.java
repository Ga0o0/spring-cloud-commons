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

package org.springframework.cloud.context.scope.refresh;

import java.io.Serializable;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.context.scope.GenericScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>
 * A Scope implementation that allows for beans to be refreshed dynamically at runtime
 * (see {@link #refresh(String)} and {@link #refreshAll()}). If a bean is refreshed then
 * the next time the bean is accessed (i.e. a method is executed) a new instance is
 * created. All lifecycle methods are applied to the bean instances, so any destruction
 * callbacks that were registered in the bean factory are called when it is refreshed, and
 * then the initialization callbacks are invoked as normal when the new instance is
 * created. A new bean instance is created from the original bean definition, so any
 * externalized content (property placeholders or expressions in string literals) is
 * re-evaluated when it is created.
 * </p>
 *
 * <p>
 * Note that all beans in this scope are <em>only</em> initialized when first accessed, so
 * the scope forces lazy initialization semantics.
 * </p>
 *
 * <p>
 * The scoped proxy approach adopted here has a side benefit that bean instances are
 * automatically {@link Serializable}, and can be sent across the wire as long as the
 * receiver has an identical application context on the other side. To ensure that the two
 * contexts agree that they are identical, they have to have the same serialization ID.
 * One will be generated automatically by default from the bean names, so two contexts
 * with the same bean names are by default able to exchange beans by name. If you need to
 * override the default ID, then provide an explicit {@link #setId(String) id} when the
 * Scope is declared.
 * </p>
 *
 * @author Dave Syer
 * @since 3.1
 *
 */
// <p>
// 允许在运行时动态刷新 Bean 的 Scope 实现（参见 {@link #refresh(String)} 和 {@link #refreshAll()}）。
// 如果 Bean 被刷新，则下次访问该 Bean（即执行方法）时会创建一个新实例。
// 所有生命周期方法都会应用于 Bean 实例，因此在 Bean 工厂中注册的所有销毁回调都会在刷新时调用，然后在创建新实例时照常调用初始化回调。
// 新的 Bean 实例是根据原始 Bean 定义创建的，因此任何外部化的内容（属性占位符或字符串字面量中的表达式）都会在创建时重新计算。
// </p>
//
// <p>
// 请注意，此作用域中的所有 Bean 仅在首次访问时初始化，因此该作用域强制使用延迟初始化语义。
// </p>
//
// <p>
// 此处采用的作用域代理方法有一个附带好处，即 Bean 实例自动 {@link Serializable}，
// 并且只要接收方在另一端具有相同的应用程序上下文，就可以跨网络发送。
// 为了确保两个上下文确认它们是相同的，它们必须具有相同的序列化 ID。
// 默认情况下，系统会根据 Bean 名称自动生成一个序列化 ID，因此两个具有相同 Bean 名称的上下文默认能够通过名称交换 Bean。
// 如果需要覆盖默认 ID，请在声明作用域时提供显式的 {@link #setId(String) id}。
// </p>
@ManagedResource
public class RefreshScope extends GenericScope
		implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, Ordered {

	private ApplicationContext context;

	private BeanDefinitionRegistry registry;

	private boolean eager = true;

	private int order = Ordered.LOWEST_PRECEDENCE - 100;

	/**
	 * Creates a scope instance and gives it the default name: "refresh".
	 */
	// 创建一个范围实例并赋予其默认名称：“refresh”。
	public RefreshScope() {
		super.setName("refresh");
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Flag to determine whether all beans in refresh scope should be instantiated eagerly
	 * on startup. Default true.
	 * @param eager The flag to set.
	 */
	// 此标志用于确定是否应在启动时立即实例化刷新范围内的所有 Bean。默认为 true。
	// @param eager 需要设置的标志。
	public void setEager(boolean eager) {
		this.eager = eager;
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		this.registry = registry;
		super.postProcessBeanDefinitionRegistry(registry);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		start(event);
	}

	public void start(ContextRefreshedEvent event) {
		if (event.getApplicationContext() == this.context && this.eager && this.registry != null) {
			eagerlyInitialize();
		}
	}

	private void eagerlyInitialize() { // 热切地初始化
		for (String name : this.context.getBeanDefinitionNames()) {
			BeanDefinition definition = this.registry.getBeanDefinition(name);
			if (this.getName().equals(definition.getScope()) && !definition.isLazyInit()) {
				Object bean = this.context.getBean(name);
				if (bean != null) {
					bean.getClass();
				}
			}
		}
	}

	/**
	 * WARNING: This method refreshes beans from any context in the hierarchy using the
	 * main application context.
	 * @param type bean type to rebind.
	 * @return true, if successful.
	 */
	// 警告：此方法会使用主应用程序上下文刷新层次结构中任何上下文的 bean。
	// @param type 要重新绑定的 bean 类型。
	// @return true（如果成功）。
	public boolean refresh(Class type) {
		String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.context, type);
		if (beanNamesForType.length > 0) {
			return refresh(beanNamesForType[0]);
		}
		return false;
	}

	// 处理所提供的 bean 名称的当前实例并在下一次方法执行时强制刷新。
	@ManagedOperation(description = "Dispose of the current instance of bean name "
			+ "provided and force a refresh on next method execution.")
	public boolean refresh(String name) {
		if (!ScopedProxyUtils.isScopedTarget(name)) {
			// User wants to refresh the bean with this name but that isn't the one in the
			// cache... --> 译文：用户想要刷新具有此名称的 bean，但这不是缓存中的 bean...
			name = ScopedProxyUtils.getTargetBeanName(name);
		}
		// Ensure lifecycle is finished if bean was disposable --> 译文：如果 bean
		// 是一次性的，请确保其生命周期已经结束
		if (super.destroy(name)) {
			this.context.publishEvent(new RefreshScopeRefreshedEvent(name));
			return true;
		}
		return false;
	}

	// 处理此范围内所有 bean 的当前实例，并在下次执行方法时强制刷新。
	@ManagedOperation(description = "Dispose of the current instance of all beans "
			+ "in this scope and force a refresh on next method execution.")
	public void refreshAll() {
		super.destroy();
		this.context.publishEvent(new RefreshScopeRefreshedEvent());
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

}
