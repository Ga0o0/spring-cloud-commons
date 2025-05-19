# Discovery

## 一、UML

![Discovery](spring-cloud-commons-client-discovery-simple.drawio.png)

## 二、重要类

### @EnableDiscoveryClient

```java
// 用于启用 DiscoveryClient 实现的注释。
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(EnableDiscoveryClientImportSelector.class)
public @interface EnableDiscoveryClient {
    
	// 如果为 true，ServiceRegistry 将自动注册本地服务器。
	// @return - 如果要自动注册，则返回 {@code true}。
	boolean autoRegister() default true;

}
```

#### 1. EnableDiscoveryClientImportSelector

```java
public class EnableDiscoveryClientImportSelector extends SpringFactoryImportSelector<EnableDiscoveryClient> {

    @Override
    public String[] selectImports(AnnotationMetadata metadata) {
        String[] imports = super.selectImports(metadata);

        AnnotationAttributes attributes = AnnotationAttributes
                .fromMap(metadata.getAnnotationAttributes(getAnnotationClass().getName(), true));

        boolean autoRegister = attributes.getBoolean("autoRegister");

        if (autoRegister) {
            List<String> importsList = new ArrayList<>(Arrays.asList(imports));
            // important
            importsList.add("org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration");
            imports = importsList.toArray(new String[0]);
        } else {
            Environment env = getEnvironment();
            if (env instanceof ConfigurableEnvironment configEnv) {
                LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                map.put("spring.cloud.service-registry.auto-registration.enabled", false);
                MapPropertySource propertySource = new MapPropertySource("springCloudDiscoveryClient", map);
                configEnv.getPropertySources().addLast(propertySource);
            }

        }

        return imports;
    }
    // ...
}
```

#### 2. AutoServiceRegistrationConfiguration

```java
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AutoServiceRegistrationProperties.class) // -->
@ConditionalOnProperty(value = "spring.cloud.service-registry.auto-registration.enabled", matchIfMissing = true)
public class AutoServiceRegistrationConfiguration {

}

// --> @EnableConfigurationProperties(AutoServiceRegistrationProperties.class)
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

### SimpleDiscoveryClientAutoConfiguration/SimpleReactiveDiscoveryClientAutoConfiguration

#### DiscoveryClient

```java
// 表示通常可用于发现服务（例如 Netflix Eureka 或 consul.io）的读取操作。
public interface DiscoveryClient extends Ordered {
    
	// 发现客户端的默认顺序。
	int DEFAULT_ORDER = 0;
    
	// HealthIndicator 中使用的实现的人类可读的描述。
	// @return 描述。
	String description();
    
	// 获取与特定 serviceId 关联的所有 ServiceInstances。
	// @param serviceId 要查询的服务 ID。
	// @return 服务实例列表。
	List<ServiceInstance> getInstances(String serviceId);
    
	// @return 所有已知的服务 ID。
	List<String> getServices();
    
	// 可用于验证客户端是否有效以及是否能够进行调用。
	// <p>调用成功且未抛出任何异常，则表示客户端能够进行调用。
	// <p>默认实现仅调用 {@link #getServices()} - 客户端实现可以选择使用更轻量的操作进行覆盖。
	// probe --> 探测
	default void probe() {
		getServices();
	}
    
	// 获取发现客户端顺序的默认实现。
	@Override
	default int getOrder() {
		return DEFAULT_ORDER;
	}

}
```

#### SimpleDiscoveryClientAutoConfiguration

<img src="./spring-cloud-commons-client-discovery-simple.drawio.png">

```java
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({ CommonsClientAutoConfiguration.class })
public class SimpleDiscoveryClientAutoConfiguration implements ApplicationListener<WebServerInitializedEvent> {

    private ServerProperties server;

    private InetUtils inet;

    private int port = 0;

    private SimpleDiscoveryProperties simple = new SimpleDiscoveryProperties();

    @Autowired(required = false)
    public void setServer(ServerProperties server) {
        this.server = server;
    }

    @Autowired
    public void setInet(InetUtils inet) {
        this.inet = inet;
    }

    @Bean
    @ConditionalOnMissingBean
    public SimpleDiscoveryProperties simpleDiscoveryProperties(
            @Value("${spring.application.name:application}") String serviceId) {
        simple.getLocal().setServiceId(serviceId);
        simple.getLocal().setHost(inet.findFirstNonLoopbackHostInfo().getHostname());
        simple.getLocal().setPort(findPort());
        return simple;
    }

    @Bean
    @Order
    public DiscoveryClient simpleDiscoveryClient(SimpleDiscoveryProperties properties) {
        return new SimpleDiscoveryClient(properties);
    }
    
    // ...
}
```

##### 1. SimpleDiscoveryProperties

```java
@ConfigurationProperties(prefix = "spring.cloud.discovery.client.simple")
public class SimpleDiscoveryProperties implements InitializingBean {

    // 实例映射
    private Map<String, List<DefaultServiceInstance>> instances = new HashMap<>();

    // 本地实例的属性（如果存在）。如果用户要导出需要服务实例识别的数据（例如指标），则应明确设置这些属性。
    @NestedConfigurationProperty
    private DefaultServiceInstance local = new DefaultServiceInstance(null, null, null, 0, false);

    private int order = DiscoveryClient.DEFAULT_ORDER;

    // ...

    @Override
    public void afterPropertiesSet() {
        for (String key : this.instances.keySet()) {
            for (DefaultServiceInstance instance : this.instances.get(key)) {
                instance.setServiceId(key);
            }
        }
    }

    // ...
}
```

##### 2. org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient

```java
public class SimpleDiscoveryClient implements DiscoveryClient {

	private SimpleDiscoveryProperties simpleDiscoveryProperties;

	public SimpleDiscoveryClient(SimpleDiscoveryProperties simpleDiscoveryProperties) {
		this.simpleDiscoveryProperties = simpleDiscoveryProperties;
	}

	@Override
	public String description() {
		return "Simple Discovery Client";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		List<ServiceInstance> serviceInstances = new ArrayList<>();
		List<DefaultServiceInstance> serviceInstanceForService = this.simpleDiscoveryProperties.getInstances()
			.get(serviceId);
		if (serviceInstanceForService != null) {
			serviceInstances.addAll(serviceInstanceForService);
		}
		return serviceInstances;
	}

	@Override
	public List<String> getServices() {
		return new ArrayList<>(this.simpleDiscoveryProperties.getInstances().keySet());
	}

	@Override
	public int getOrder() {
		return this.simpleDiscoveryProperties.getOrder();
	}

}
```

### org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient


## 三、使用示例


```java
// 1. XxxDiscoveryProperties
public class XxxDiscoveryProperties implements InitializingBean {

    private Map<String, List<DefaultServiceInstance>> instances = new HashMap<>();

    public Map<String, List<DefaultServiceInstance>> getInstances() {
        return this.instances;
    }

    public void setInstances(Map<String, List<DefaultServiceInstance>> instances) {
        this.instances = instances;
    }
    //...
}

// 2. XxxDiscoveryClient
public class XxxDiscoveryClient implements DiscoveryClient {
    
    private XxxDiscoveryProperties properties;

    public XxxDiscoveryClient(XxxDiscoveryProperties xxxDiscoveryProperties) {
        this.properties = xxxDiscoveryProperties;
    }

    public String description() {
        return "Xxx DiscoveryClient";
    }

    public List<ServiceInstance> getInstances(String serviceId) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        List<DefaultServiceInstance> serviceInstanceForService = this.properties.getInstances()
                .get(serviceId);
        if (serviceInstanceForService != null) {
            serviceInstances.addAll(serviceInstanceForService);
        }
        return serviceInstances;
    }

    public List<String> getServices() {
        return this.properties.getInstances().keySet();
    }
    
}

// 3. 示例
XxxDiscoveryProperties properties = new XxxDiscoveryProperties();
properties.setInstances(...);
DiscoveryClient discoveryClient = new  XxxDiscoveryClient(properties);

String serviceId = "order-service";
List<ServiceInstance> instanceList = discoveryClient.getInstances(serviceId);
```



