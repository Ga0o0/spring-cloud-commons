# Service Registry

## UML

![Service Registry](spring-cloud-commons-client-serviceregistry.drawio.png)

## 重要类

### 1. 服务在服务注册表中的信息类：Registration

```java
// 1. ServiceInstance
// 表示发现系统中服务的一个实例。
public interface ServiceInstance {
    
    // @return 注册的唯一实例 ID。
    default String getInstanceId() {
        return null;
    }
    
    // @return 注册的服务 ID。
    String getServiceId();
    
    // @return 已注册服务实例的主机名。
    String getHost();
    
    // @return 已注册服务实例的端口。
    int getPort();
    
    // @return 已注册服务实例的端口是否使用 HTTPS。
    boolean isSecure();
    
    // @return 服务 URI 地址。
    URI getUri();
    
    // @return 与服务实例关联的键/值对元数据。
    Map<String, String> getMetadata();
    
    // @return 服务实例的 scheme。
    default String getScheme() {
        return null;
    }

}

public interface Registration extends ServiceInstance {

}
```

### 2. 服务注册表：ServiceRegistry

```java
// 签订合同以向服务注册表注册和注销实例。
public interface ServiceRegistry<R extends Registration> {
    
	// 注册注册信息。注册信息通常包含实例的相关信息，例如主机名和端口。
	void register(R registration);
    
	// 注销注册信息。
	void deregister(R registration);
    
	// 关闭 ServiceRegistry。这是一个生命周期方法。
	void close();
    
	// 设置注册状态。状态值由各个实现决定。
	void setStatus(R registration, String status);
    
	// 获取特定注册的状态。
	<T> T getStatus(R registration);

}
```

### 3. 服务自动注册 --> 本机服务自动注册到服务注册表

#### 3.1. AutoServiceRegistrationAutoConfiguration

```java
@Configuration(proxyBeanMethods = false)
@Import(AutoServiceRegistrationConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
public class AutoServiceRegistrationAutoConfiguration implements InitializingBean {

	@Autowired(required = false)
	private AutoServiceRegistration autoServiceRegistration;

	@Autowired
	private AutoServiceRegistrationProperties properties;

	@Override
	public void afterPropertiesSet() {
		if (this.autoServiceRegistration == null && this.properties.isFailFast()) {
			// 已请求自动服务注册，但不存在 AutoServiceRegistration bean
			throw new IllegalStateException(
					"Auto Service Registration has " + "been requested, but there is no AutoServiceRegistration bean");
		}
	}

}
```

#### 3.2. AutoServiceRegistrationConfiguration

```java
// 1. AutoServiceRegistrationConfiguration
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AutoServiceRegistrationProperties.class)
@ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
public class AutoServiceRegistrationConfiguration {

}

// 2. AutoServiceRegistrationProperties
@ConfigurationProperties("spring.cloud.service-registry.auto-registration")
public class AutoServiceRegistrationProperties {

    /** Whether service auto-registration is enabled. Defaults to true. */
    // 是否启用服务自动注册。默认为 true。
    private boolean enabled = true;

    /** Whether to register the management as a service. Defaults to true. */
    // 是否将管理注册为服务。默认为 true。
    private boolean registerManagement = true;

    /**
     * Whether startup fails if there is no AutoServiceRegistration. Defaults to false.
     */
    // 如果没有 AutoServiceRegistration，启动是否失败。默认为 false。
    private boolean failFast = false;
    // ...
}
```

#### 3.3. AutoServiceRegistration

```java
// 1. AutoServiceRegistration
public interface AutoServiceRegistration {

}

public abstract class AbstractAutoServiceRegistration<R extends Registration>
        implements AutoServiceRegistration, ApplicationContextAware, ApplicationListener<WebServerInitializedEvent> {

    // ...
    private final ServiceRegistry<R> serviceRegistry;
    // ...

    protected AbstractAutoServiceRegistration(ServiceRegistry<R> serviceRegistry,
                                              AutoServiceRegistrationProperties properties) {
        this.serviceRegistry = serviceRegistry;
        this.properties = properties;
    }
    
    // ...
    @Override
    @SuppressWarnings("deprecation")
    public void onApplicationEvent(WebServerInitializedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        if (context instanceof ConfigurableWebServerApplicationContext) {
            if ("management".equals(((ConfigurableWebServerApplicationContext) context).getServerNamespace())) {
                return;
            }
        }
        this.port.compareAndSet(0, event.getWebServer().getPort());
        this.start();
    }
    // ...

    public void start() {
        if (!isEnabled()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Discovery Lifecycle disabled. Not starting"); // 发现生命周期已禁用。未启动
            }
            return;
        }

        // only initialize if nonSecurePort is greater than 0 and it isn't already running
        // because of containerPortInitializer below
        // --> 译文：仅当 nonSecurePort 大于 0 并且由于下面的 containerPortInitializer 尚未运行时才进行初始化
        if (!this.running.get()) {
            // InstancePreRegisteredEvent --> 服务注册之前触发的事件。
            this.context.publishEvent(new InstancePreRegisteredEvent(this, getRegistration()));
            // RegistrationLifecycle#postProcessBeforeStartRegister()：在使用 ServiceRegistry 注册本地服务之前执行的方法
            registrationLifecycles.forEach(
                    registrationLifecycle -> registrationLifecycle.postProcessBeforeStartRegister(getRegistration()));
            // 使用 ServiceRegistry 注册本地服务。
            register();
            // RegistrationLifecycle#postProcessAfterStartRegister()：在使用 ServiceRegistry 注册本地服务之后执行的方法
            this.registrationLifecycles.forEach(
                    registrationLifecycle -> registrationLifecycle.postProcessAfterStartRegister(getRegistration()));
            // 是否应该向 ServiceRegistry 注册管理服务。
            if (shouldRegisterManagement()) {
                // 在使用 ServiceRegistry 注册本地管理服务之前执行的方法。
                this.registrationManagementLifecycles
                        .forEach(registrationManagementLifecycle -> registrationManagementLifecycle
                                .postProcessBeforeStartRegisterManagement(getManagementRegistration()));
                // 使用 ServiceRegistry 注册本地管理服务。
                this.registerManagement();
                // 在使用 ServiceRegistry 注册本地管理服务之后执行的方法。
                registrationManagementLifecycles
                        .forEach(registrationManagementLifecycle -> registrationManagementLifecycle
                                .postProcessAfterStartRegisterManagement(getManagementRegistration()));

            }
            // InstanceRegisteredEvent --> 本地服务实例向发现服务注册后要发布的事件。
            this.context.publishEvent(new InstanceRegisteredEvent<>(this, getConfiguration()));
            this.running.compareAndSet(false, true);
        }

    }
    
    // ...

    protected abstract R getRegistration();

    protected abstract R getManagementRegistration();

    // 使用 {@link ServiceRegistry} 注册本地服务。
    protected void register() {
        this.serviceRegistry.register(getRegistration());
    }

    // 使用 {@link ServiceRegistry} 注册本地管理服务。
    protected void registerManagement() {
        R registration = getManagementRegistration();
        if (registration != null) {
            this.serviceRegistry.register(registration);
        }
    }

    // 使用 {@link ServiceRegistry} 取消注册本地服务。
    protected void deregister() {
        this.serviceRegistry.deregister(getRegistration());
    }

    // 使用 {@link ServiceRegistry} 取消注册本地管理服务。
    protected void deregisterManagement() {
        R registration = getManagementRegistration();
        if (registration != null) {
            this.serviceRegistry.deregister(registration);
        }
    }
    // ...
}
```

## 使用示例

### 1. 服务注册

```java
class XxxRegistration implements Registration {
    // ...
}

class XxxServiceRegistry implements ServiceRegistry<XxxRegistration> {
    // ...
    public void register(XxxRegistration registration) {
        // TODO: 注册服务信息
    }
}

// 使用示例
Registration registration = new XxxRegistration();
ServiceRegistry serviceRegistry = new XxxServiceRegistry();
serviceRegistry.register(registration);
```

### 2. 将本机服务自动注册到服务注册表

```java
// 1. 服务注册信息
class XxxRegistration implements Registration {
    // ...
}

// 2. 服务注册表
class XxxServiceRegistry implements ServiceRegistry<XxxRegistration> {
    // ...
    public void register(XxxRegistration registration) {
        // TODO: 注册服务信息
    }
}

// 3. 将本机服务自动注册到服务注册表
class XxxAutoServiceRegistration extends AbstractAutoServiceRegistration {
    
    private final ServiceRegistry<R> serviceRegistry;
    
    private final XxxRegistration registration;
    
    public XxxAutoServiceRegistration(ServiceRegistry<XxxRegistration> serviceRegistry,
                                      AutoServiceRegistrationProperties properties,
                                      XxxRegistration registration) {
        super(serviceRegistry, registration);
        this.registration = registration;
    }

    @Override
    public XxxRegistration getRegistration() {
        // 服务信息，关于本机的
        // TODO 设置相关服务信息，关于本机的
        return registration;
    }

    @Override
    public XxxRegistration getManagementRegistration() {
        // 服务管理信息，关于本机的
        // TODO 设置相关服务管理信息，关于本机的
        return null;
    }
}

// 4. 自动装配类
@Configuration(proxyBeanMethods = false)
class XxxAutoServiceRegistrationAutoConfiguration {
    
    @Bean
    public XxxServiceRegistry xxxServiceRegistry() {
        return new XxxServiceRegistry();
    }

    @Bean
    public XxxRegistration xxxRegistration() {
        return new XxxRegistration();
    }

    @Bean
    @ConditionalOnBean(AutoServiceRegistrationProperties.class)
    @ConditionalOnBean(XxxServiceRegistry.class)
    @ConditionalOnBean(XxxRegistration.class)
    public XxxAutoServiceRegistration nacosAutoServiceRegistration(
            AutoServiceRegistrationProperties autoServiceRegistrationProperties,
            XxxServiceRegistry registry,
            XxxRegistration registration) {
        return new NacosAutoServiceRegistration(registry,
                autoServiceRegistrationProperties, registration);
    }
    
}
```

Nacos 官方的自动装配类：`com.alibaba.cloud.nacos.registry.NacosServiceRegistryAutoConfiguration`


