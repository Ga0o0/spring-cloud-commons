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

package org.springframework.cloud.client.loadbalancer;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import reactor.util.retry.RetryBackoffSpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.commons.util.IdUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * The base configuration bean for Spring Cloud LoadBalancer.
 *
 * See {@link LoadBalancerClientsProperties} for the {@link ConfigurationProperties}
 * annotation.
 *
 * @author Olga Maciaszek-Sharma
 * @author Gandhimathi Velusamy
 * @author Zhuozhi Ji
 * @since 2.2.1
 */
// Spring Cloud LoadBalancer 的基础配置 Bean。
//
// 请参阅 {@link LoadBalancerClientsProperties} 了解 {@link ConfigurationProperties} 注解。
public class LoadBalancerProperties {

	/**
	 * Properties for <code>HealthCheckServiceInstanceListSupplier</code>.
	 */
	// <code>HealthCheckServiceInstanceListSupplier</code> 的属性。
	private HealthCheck healthCheck = new HealthCheck();

	/**
	 * Allows setting the value of <code>hint</code> that is passed on to the LoadBalancer
	 * request and can subsequently be used in {@link ReactiveLoadBalancer}
	 * implementations.
	 */
	// 允许设置传递给 LoadBalancer 请求的 <code>hint</code> 的值，随后可在 {@link ReactiveLoadBalancer} 实现中使用。
	private Map<String, String> hint = new LinkedCaseInsensitiveMap<>();

	/**
	 * Allows setting the name of the header used for passing the hint for hint-based
	 * service instance filtering.
	 */
	// 允许设置用于传递基于提示的服务实例过滤的提示的标头的名称。
	private String hintHeaderName = "X-SC-LB-Hint";

	/**
	 * Properties for Spring-Retry and Reactor Retry support in Spring Cloud LoadBalancer.
	 */
	// Spring Cloud LoadBalancer 中 Spring-Retry 和 Reactor Retry 支持的属性。
	private Retry retry = new Retry();

	/**
	 * Properties for LoadBalancer sticky-session.
	 */
	// LoadBalancer 粘性会话的属性。
	private StickySession stickySession = new StickySession();

	/**
	 * If this flag is set to {@code true},
	 * {@code ServiceInstanceListSupplier#get(Request request)} method will be implemented
	 * to call {@code delegate.get(request)} in classes assignable from
	 * {@code DelegatingServiceInstanceListSupplier} that don't already implement that
	 * method, with the exclusion of {@code CachingServiceInstanceListSupplier} and
	 * {@code HealthCheckServiceInstanceListSupplier}, which should be placed in the
	 * instance supplier hierarchy directly after the supplier performing instance
	 * retrieval over the network, before any request-based filtering is done,
	 * {@code true} by default.
	 */
	// 如果此标志设置为 {@code true}，则将实现 {@code ServiceInstanceListSupplier#get(Request request)} 方法
	// 来调用 {@code delegate.get(request)}，这些类可从 {@code DelegatingServiceInstanceListSupplier} 分配，
	// 但尚未实现该方法，但不包括 {@code CachingServiceInstanceListSupplier} 和 {@code HealthCheckServiceInstanceListSupplier}，
	// 它们应放置在实例供应商层次结构中，在供应商通过网络执行实例检索之后，在进行任何基于请求的过滤之前，默认情况下为 {@code true}。
	private boolean callGetWithRequestOnDelegates = true;

	/**
	 * Properties for
	 * {@link org.springframework.cloud.loadbalancer.core.SubsetServiceInstanceListSupplier}.
	 */
	// {@link org.springframework.cloud.loadbalancer.core.SubsetServiceInstanceListSupplier} 的属性。
	private Subset subset = new Subset();

	public HealthCheck getHealthCheck() {
		return healthCheck;
	}

	public void setHealthCheck(HealthCheck healthCheck) {
		this.healthCheck = healthCheck;
	}

	public Map<String, String> getHint() {
		return hint;
	}

	public void setHint(Map<String, String> hint) {
		this.hint = hint;
	}

	public Retry getRetry() {
		return retry;
	}

	public void setRetry(Retry retry) {
		this.retry = retry;
	}

	public StickySession getStickySession() {
		return stickySession;
	}

	public void setStickySession(StickySession stickySession) {
		this.stickySession = stickySession;
	}

	public String getHintHeaderName() {
		return hintHeaderName;
	}

	public void setHintHeaderName(String hintHeaderName) {
		this.hintHeaderName = hintHeaderName;
	}

	/**
	 * Enabling X-Forwarded Host and Proto Headers.
	 */
	// 启用 X-Forwarded Host 和 Proto Headers。
	private XForwarded xForwarded = new XForwarded();

	// TODO: fix spelling in a major release
	public void setxForwarded(XForwarded xForwarded) {
		this.xForwarded = xForwarded;
	}

	public XForwarded getXForwarded() {
		return xForwarded;
	}

	public boolean isCallGetWithRequestOnDelegates() {
		return callGetWithRequestOnDelegates;
	}

	public Subset getSubset() {
		return subset;
	}

	public void setSubset(Subset subset) {
		this.subset = subset;
	}

	public void setCallGetWithRequestOnDelegates(boolean callGetWithRequestOnDelegates) {
		this.callGetWithRequestOnDelegates = callGetWithRequestOnDelegates;
	}

	public static class StickySession {

		/**
		 * The name of the cookie holding the preferred instance id.
		 */
		// 保存首选实例 ID 的 cookie 的名称。
		private String instanceIdCookieName = "sc-lb-instance-id";

		/**
		 * Indicates whether a cookie with the newly selected instance should be added by
		 * SC LoadBalancer.
		 */
		// 指示 SC LoadBalancer 是否应添加带有新选择的实例的 cookie。
		private boolean addServiceInstanceCookie = false;

		public String getInstanceIdCookieName() {
			return instanceIdCookieName;
		}

		public void setInstanceIdCookieName(String instanceIdCookieName) {
			this.instanceIdCookieName = instanceIdCookieName;
		}

		public boolean isAddServiceInstanceCookie() {
			return addServiceInstanceCookie;
		}

		public void setAddServiceInstanceCookie(boolean addServiceInstanceCookie) {
			this.addServiceInstanceCookie = addServiceInstanceCookie;
		}

	}

	public static class XForwarded {

		/**
		 * To Enable X-Forwarded Headers.
		 */
		// 启用 X-Forwarded 标头。
		private boolean enabled = false;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class HealthCheck {

		/**
		 * Initial delay value for the HealthCheck scheduler.
		 */
		// HealthCheck 调度程序的初始延迟值。
		private Duration initialDelay = Duration.ZERO;

		/**
		 * Interval for rerunning the HealthCheck scheduler.
		 */
		// 重新运行 HealthCheck 调度程序的间隔。
		private Duration interval = Duration.ofSeconds(25);

		/**
		 * Interval for refetching available service instances.
		 */
		// 重新获取可用服务实例的间隔。
		private Duration refetchInstancesInterval = Duration.ofSeconds(25);

		/**
		 * Path at which the health-check request should be made. Can be set up per
		 * <code>serviceId</code>. A <code>default</code> value can be set up as well. If
		 * none is set up, <code>/actuator/health</code> will be used.
		 */
		// 发送健康检查请求的路径。可以根据 <code>serviceId</code> 设置。
		// 也可以设置 <code>default</code> 值。如果未设置，则使用 <code>/actuator/health</code>。
		private Map<String, String> path = new LinkedCaseInsensitiveMap<>();

		/**
		 * Port at which the health-check request should be made. If none is set, the port
		 * under which the requested service is available at the service instance.
		 */
		// 发出健康检查请求的端口。如果未设置，则使用服务实例上请求的服务可用的端口。
		private Integer port;

		/**
		 * Indicates whether the instances should be refetched by the
		 * <code>HealthCheckServiceInstanceListSupplier</code>. This can be used if the
		 * instances can be updated and the underlying delegate does not provide an
		 * ongoing flux.
		 */
		// 指示是否应由 <code>HealthCheckServiceInstanceListSupplier</code> 重新获取实例。
		// 如果实例可更新且底层委托不提供持续更新，则可以使用此方法。
		private boolean refetchInstances = false;

		/**
		 * Indicates whether health checks should keep repeating. It might be useful to
		 * set it to <code>false</code> if periodically refetching the instances, as every
		 * refetch will also trigger a healthcheck.
		 */
		// 指示是否应重复执行健康检查。
		// 如果需要定期重新获取实例，则建议将其设置为 <code>false</code>，因为每次重新获取实例都会触发健康检查。
		private boolean repeatHealthCheck = true;

		/**
		 * Indicates whether the {@code healthCheckFlux} should emit on each alive
		 * {@link ServiceInstance} that has been retrieved. If set to {@code false}, the
		 * entire alive instances sequence is first collected into a list and only then
		 * emitted.
		 */
		// 指示 {@code healthCheckFlux} 是否应在每个已检索到的存活 {@link ServiceInstance} 上发出。
		// 如果设置为 {@code false}，则系统会先将整个存活实例序列收集到一个列表中，然后再发出。
		private boolean updateResultsList = true;

		public boolean getRefetchInstances() {
			return refetchInstances;
		}

		public void setRefetchInstances(boolean refetchInstances) {
			this.refetchInstances = refetchInstances;
		}

		public boolean getRepeatHealthCheck() {
			return repeatHealthCheck;
		}

		public void setRepeatHealthCheck(boolean repeatHealthCheck) {
			this.repeatHealthCheck = repeatHealthCheck;
		}

		public Duration getInitialDelay() {
			return initialDelay;
		}

		public void setInitialDelay(Duration initialDelay) {
			this.initialDelay = initialDelay;
		}

		public Duration getRefetchInstancesInterval() {
			return refetchInstancesInterval;
		}

		public void setRefetchInstancesInterval(Duration refetchInstancesInterval) {
			this.refetchInstancesInterval = refetchInstancesInterval;
		}

		public Map<String, String> getPath() {
			return path;
		}

		public void setPath(Map<String, String> path) {
			this.path = path;
		}

		public Duration getInterval() {
			return interval;
		}

		public void setInterval(Duration interval) {
			this.interval = interval;
		}

		public Integer getPort() {
			return port;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

		public boolean isUpdateResultsList() {
			return updateResultsList;
		}

		public void setUpdateResultsList(boolean updateResultsList) {
			this.updateResultsList = updateResultsList;
		}

	}

	public static class Retry {

		private boolean enabled = true;

		/**
		 * Indicates retries should be attempted on operations other than
		 * {@link HttpMethod#GET}.
		 */
		// 指示应尝试对除 @link HttpMethod#GET} 以外的操作进行重试。
		private boolean retryOnAllOperations = false;

		/**
		 * Indicates retries should be attempted for all exceptions, not only those
		 * specified in {@code retryableExceptions}.
		 */
		// 指示应尝试对所有异常进行重试，而不仅仅是 {@code retryableExceptions} 中指定的异常。
		private boolean retryOnAllExceptions = false;

		/**
		 * Number of retries to be executed on the same <code>ServiceInstance</code>.
		 */
		// 在同一 <code>ServiceInstance</code> 上执行的重试次数。
		private int maxRetriesOnSameServiceInstance = 0;

		/**
		 * Number of retries to be executed on the next <code>ServiceInstance</code>. A
		 * <code>ServiceInstance</code> is chosen before each retry call.
		 */
		// 下一个 <code>ServiceInstance</code> 上执行的重试次数。
		// 每次重试调用之前都会选择一个 <code>ServiceInstance</code>。
		private int maxRetriesOnNextServiceInstance = 1;

		/**
		 * A {@link Set} of status codes that should trigger a retry.
		 */
		// 应触发重试的状态码 {@link Set}。
		private Set<Integer> retryableStatusCodes = new HashSet<>();

		/**
		 * A {@link Set} of {@link Throwable} classes that should trigger a retry.
		 */
		// 一个包含 Throwable 类的 Set，用于触发重试。
		private Set<Class<? extends Throwable>> retryableExceptions = new HashSet<>(
				Arrays.asList(IOException.class, TimeoutException.class, RetryableStatusCodeException.class,
						org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException.class));

		/**
		 * Properties for Reactor Retry backoffs in Spring Cloud LoadBalancer.
		 */
		// Spring Cloud LoadBalancer 中 Reactor Retry 退避的属性。
		private Backoff backoff = new Backoff();

		/**
		 * Returns true if the load balancer should retry failed requests.
		 * @return True if the load balancer should retry failed requests; false
		 * otherwise.
		 */
		// 如果负载均衡器应该重试失败的请求，则返回 true。
		// @return 如果负载均衡器应该重试失败的请求，则返回 true；否则返回 false。
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Sets whether the load balancer should retry failed requests.
		 * @param enabled Whether the load balancer should retry failed requests.
		 */
		// 设置负载均衡器是否应重试失败的请求。
		// @param enabled 负载均衡器是否应重试失败的请求。
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isRetryOnAllOperations() {
			return retryOnAllOperations;
		}

		public void setRetryOnAllOperations(boolean retryOnAllOperations) {
			this.retryOnAllOperations = retryOnAllOperations;
		}

		public int getMaxRetriesOnSameServiceInstance() {
			return maxRetriesOnSameServiceInstance;
		}

		public void setMaxRetriesOnSameServiceInstance(int maxRetriesOnSameServiceInstance) {
			this.maxRetriesOnSameServiceInstance = maxRetriesOnSameServiceInstance;
		}

		public int getMaxRetriesOnNextServiceInstance() {
			return maxRetriesOnNextServiceInstance;
		}

		public void setMaxRetriesOnNextServiceInstance(int maxRetriesOnNextServiceInstance) {
			this.maxRetriesOnNextServiceInstance = maxRetriesOnNextServiceInstance;
		}

		public Set<Integer> getRetryableStatusCodes() {
			return retryableStatusCodes;
		}

		public void setRetryableStatusCodes(Set<Integer> retryableStatusCodes) {
			this.retryableStatusCodes = retryableStatusCodes;
		}

		public Set<Class<? extends Throwable>> getRetryableExceptions() {
			return retryableExceptions;
		}

		public void setRetryableExceptions(Set<Class<? extends Throwable>> retryableExceptions) {
			retryableExceptions
				.add(org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException.class);
			this.retryableExceptions = retryableExceptions;
		}

		public Backoff getBackoff() {
			return backoff;
		}

		public void setBackoff(Backoff backoff) {
			this.backoff = backoff;
		}

		public boolean isRetryOnAllExceptions() {
			return retryOnAllExceptions;
		}

		public void setRetryOnAllExceptions(boolean retryOnAllExceptions) {
			this.retryOnAllExceptions = retryOnAllExceptions;
		}

		public static class Backoff {

			/**
			 * Indicates whether Reactor Retry backoffs should be applied.
			 */
			// 指示是否应应用 Reactor Retry 退避。
			private boolean enabled = false;

			/**
			 * Used to set {@link RetryBackoffSpec#minBackoff}.
			 */
			// 用于设置 {@link RetryBackoffSpec#minBackoff}。
			private Duration minBackoff = Duration.ofMillis(5);

			/**
			 * Used to set {@link RetryBackoffSpec#maxBackoff}.
			 */
			// 用于设置 {@link RetryBackoffSpec#maxBackoff}。
			private Duration maxBackoff = Duration.ofMillis(Long.MAX_VALUE);

			/**
			 * Used to set {@link RetryBackoffSpec#jitter}.
			 */
			// 用于设置 {@link RetryBackoffSpec#jitter}。
			private double jitter = 0.5d;

			public Duration getMinBackoff() {
				return minBackoff;
			}

			public void setMinBackoff(Duration minBackoff) {
				this.minBackoff = minBackoff;
			}

			public Duration getMaxBackoff() {
				return maxBackoff;
			}

			public void setMaxBackoff(Duration maxBackoff) {
				this.maxBackoff = maxBackoff;
			}

			public double getJitter() {
				return jitter;
			}

			public void setJitter(double jitter) {
				this.jitter = jitter;
			}

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

	}

	public static class Subset {

		/**
		 * Instance id of deterministic subsetting. If not set,
		 * {@link IdUtils#getDefaultInstanceId(PropertyResolver)} will be used.
		 */
		// 确定性子集的实例 ID。如果未设置，将使用 {@link IdUtils#getDefaultInstanceId(PropertyResolver)}。
		private String instanceId = "";

		/**
		 * Max subset size of deterministic subsetting.
		 */
		// 确定性子集的最大子集大小。
		private int size = 100;

		public String getInstanceId() {
			return instanceId;
		}

		public void setInstanceId(String instanceId) {
			this.instanceId = instanceId;
		}

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

	}

}
