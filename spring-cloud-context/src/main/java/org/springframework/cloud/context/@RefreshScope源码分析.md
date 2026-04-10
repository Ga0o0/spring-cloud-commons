# @RefreshScope 源码分析

## AutoConfiguration.imports

```properties
# spring-cloud-context/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
# ...
org.springframework.cloud.autoconfigure.RefreshAutoConfiguration
# ...
```

```java
// RefreshAutoConfiguration
// 自动配置刷新范围和与环境变化相关的功能（例如重新绑定记录器级别）。
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RefreshScope.class)
// ...
public class RefreshAutoConfiguration {
    // ...
    @Bean
    @ConditionalOnMissingBean(RefreshScope.class)
    public static RefreshScope refreshScope() {
        return new RefreshScope();
    }
    // ...
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBootstrapEnabled
    public LegacyContextRefresher legacyContextRefresher(ConfigurableApplicationContext context, RefreshScope scope,
                                                         RefreshProperties properties) {
        return new LegacyContextRefresher(context, scope, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBootstrapDisabled
    public ConfigDataContextRefresher configDataContextRefresher(ConfigurableApplicationContext context,
                                                                 RefreshScope scope, RefreshProperties properties) {
        return new ConfigDataContextRefresher(context, scope, properties);
    }
    // ...

    @Bean
    public RefreshEventListener refreshEventListener(ContextRefresher contextRefresher) {
        return new RefreshEventListener(contextRefresher); // important -> go
    }
    // ...
}
```

## 源码执行顺序

```text
1. 为 BeanDefinition 提供值为 refresh 的 scope
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
1.1. @RefreshScope -> 为 BeanDefinition 提供值为 refresh 的 scope


2. BeanDefinition 的后处理：处理 scope = refresh 的 BeanDefinition，并将其 BeanClass 设置为 LockedScopedProxyFactoryBean
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
2.1. RefreshScope#postProcessBeanDefinitionRegistry()                           -> 处理 scope = refresh 的 BeanDefinition，并将其 BeanClass 设置为 LockedScopedProxyFactoryBean
2.2. RefreshScopeBeanDefinitionEnhancer#postProcessBeanDefinitionRegistry()     -> 暂时没发现如何使用！！！


3. 通过发布并监听 ContextRefreshedEvent 事件，来将 scope = refresh 的 bean 的 ObjectFactory 存储到 GenericScope#cache 中，并使用 ObjectFactory 来初始化 bean
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
3.1. 发布 ContextRefreshedEvent 事件：AbstractApplicationContext#finishRefresh()
3.2. 监听 ContextRefreshedEvent 事件：RefreshScope#onApplicationEvent()
    \-- RefreshScope#eagerlyInitialize()                                        -> 初始化 scope = refresh 的 bean
        \-- BeanFactory#getBean(java.lang.String)
            \-- GenericScope#get()                                              -> 将 bean 的 ObjectFactory 存储到 GenericScope#cache 中，并初始化 bean（ObjectFactory 用于初始化 bean）


4. 通过发布并监听 RefreshEvent 和 ApplicationReadyEvent 事件，来刷新 Environment 和清理 GenericScope#cache（存储 scope = refresh 的 bean）
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
4.1. 发布 RefreshEvent 和 ApplicationReadyEvent 事件：框架需要实现的；例如 Nacos 监听到自己的 Config 发生变化时，需要发布该事件
4.2. 监听 RefreshEvent 和 ApplicationReadyEvent 事件：RefreshEventListener#onApplicationEvent() 
    \-- 处理 RefreshEvent 事件
        \-- ContextRefresher#refresh()
            \-- ContextRefresher#refreshEnvironment()                           -> 刷新 Environment，并发布 EnvironmentChangeEvent 事件
                \-- ContextRefresher#updateEnvironment()                        -> 该方法有两种实现：ConfigDataContextRefresher#updateEnvironment() 和 LegacyContextRefresher#updateEnvironment()
            \-- RefreshScope#refreshAll()                                       -> 清理 GenericScope#cache 和发布 RefreshScopeRefreshedEvent 事件


5. ConfigData 上下文的刷新，通过 EnvironmentPostProcessor 来实现
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
5.1. ConfigDataContextRefresher#updateEnvironment()
    \-- EnvironmentPostProcessor#postProcessEnvironment()                       -> Environment 的后处理器
    
    
6. scope = refresh 的 bean（LockedScopedProxyFactoryBean 类型） 的方法的执行
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------
6.1. LockedScopedProxyFactoryBean#invoke()
```

## 源码执行中的相关类和方法

### 1. @RefreshScope

```java
// 便捷的注解，用于将 <code>@Bean</code> 定义放入 {@link
// org.springframework.cloud.context.scope.refresh.RefreshScope 刷新范围}。
// 通过这种方式注解的 Bean 可以在运行时刷新，并且任何使用它们的组件都会在下一次方法调用时获得一个新实例，
// 该实例已完全初始化并注入了所有依赖项。
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Scope("refresh")
@Documented
public @interface RefreshScope {

	/**
	 * Alias for {@link Scope#proxyMode}.
	 * @see Scope#proxyMode()
	 * @return proxy mode
	 */
	// {@link Scope#proxyMode} 的别名。
	// @see Scope#proxyMode()
	// @return 代理模式
	@AliasFor(annotation = Scope.class)
	ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;

}
```


### 2. RefreshScope

```java
// 1. RefreshAutoConfiguration
// 自动配置刷新范围和与环境变化相关的功能（例如重新绑定记录器级别）。
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RefreshScope.class)
// ...
public class RefreshAutoConfiguration {
    // ...
    @Bean
    @ConditionalOnMissingBean(RefreshScope.class)
    public static RefreshScope refreshScope() {
        return new RefreshScope();  // important -> go
    }
    // ...
}

// 2. RefreshScope
public class RefreshScope extends GenericScope
        implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, Ordered {
    // ...
    private int order = Ordered.LOWEST_PRECEDENCE - 100; // important
    
    // 创建一个范围实例并赋予其默认名称：“refresh”。
    public RefreshScope() {
        super.setName("refresh"); // important -> go
    }
    // ...
}

// 3. GenericScope
// 通用 Scope 实现。
public class GenericScope
        implements Scope, BeanFactoryPostProcessor, BeanDefinitionRegistryPostProcessor, DisposableBean {
    // ...
}
```

#### 2.1. RefreshScope#postProcessBeanDefinitionRegistry()

```java
// 1. RefreshScope#postProcessBeanDefinitionRegistry()
public class RefreshScope extends GenericScope
		implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, Ordered {
    // ...
    // 创建一个范围实例并赋予其默认名称：“refresh”。
    public RefreshScope() {
        super.setName("refresh");
    }

    // ...
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
        super.postProcessBeanDefinitionRegistry(registry);  // important -> go
    }
}

// 2. GenericScope#postProcessBeanDefinitionRegistry()
@Override
public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
    for (String name : registry.getBeanDefinitionNames()) {
        BeanDefinition definition = registry.getBeanDefinition(name);
        if (definition instanceof RootBeanDefinition root) {
            if (root.getDecoratedDefinition() != null && root.hasBeanClass()
                    && root.getBeanClass() == ScopedProxyFactoryBean.class) {
                if (getName().equals(root.getDecoratedDefinition().getBeanDefinition().getScope())) {  // important -> go // getName() = refresh
                    root.setBeanClass(LockedScopedProxyFactoryBean.class); // important
                    root.getConstructorArgumentValues().addGenericArgumentValue(this);
                    // 令人惊讶的是，一个作用域代理 Bean 定义竟然还没有被标记为合成（synthetic）？
                    root.setSynthetic(true);
                }
            }
        }
    }
}
```

#### 2.2. RefreshScope#onApplicationEvent()

##### 1. 发布 ContextRefreshedEvent 事件

```java
/*
org.springframework.context.support.AbstractApplicationContext#refresh()
    \-- org.springframework.context.support.AbstractApplicationContext#finishRefresh()
        \-- org.springframework.context.support.AbstractApplicationContext#publishEvent(org.springframework.context.ApplicationEvent)
            \-- org.springframework.context.event.ContextRefreshedEvent()
**/

// AbstractApplicationContext#finishRefresh()
protected void finishRefresh() {
    // ...
    publishEvent(new ContextRefreshedEvent(this));  // important -> go
}
```

##### 2. 监听事件：RefreshScope#onApplicationEvent()

```java
// 1. RefreshScope#onApplicationEvent()
public class RefreshScope extends GenericScope
		implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, Ordered {
    // ...
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        start(event); // important -> go
    }

    public void start(ContextRefreshedEvent event) {
        if (event.getApplicationContext() == this.context && this.eager && this.registry != null) {
            eagerlyInitialize(); // important -> go
        }
    }

    private void eagerlyInitialize() { // 热切地初始化
        for (String name : this.context.getBeanDefinitionNames()) {
            BeanDefinition definition = this.registry.getBeanDefinition(name);
            if (this.getName().equals(definition.getScope()) && !definition.isLazyInit()) { // getName() = refresh
                Object bean = this.context.getBean(name); // important -> go
                if (bean != null) {
                    bean.getClass();
                }
            }
        }
    }
    // ...
}

/*
org.springframework.beans.factory.BeanFactory#getBean(java.lang.String)
    \-- org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String)
        \-- org.springframework.beans.factory.support.AbstractBeanFactory#getBean(java.lang.String)
            \-- org.springframework.beans.factory.support.AbstractBeanFactory#doGetBean()
                    \-- org.springframework.cloud.context.scope.GenericScope#get()
**/


// 2. AbstractBeanFactory#doGetBean()
protected <T> T doGetBean(String name, @Nullable Class<T> requiredType, @Nullable Object[] args, boolean typeCheckOnly) throws BeansException {
    // ...
    if (mbd.isSingleton()) {
        // ...
    } else if (mbd.isPrototype()) {
        // ...
    } else {
        String scopeName = mbd.getScope();
        // ...

        Scope scope = (Scope)this.scopes.get(scopeName);
        if (scope == null) {
            throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
        }

        try {
            Object scopedInstance = scope.get(beanName, () -> {  // important -> go
                this.beforePrototypeCreation(beanName);

                Object var4;
                try {
                    var4 = this.createBean(beanName, mbd, args);
                } finally {
                    this.afterPrototypeCreation(beanName);
                }

                return var4;
            });
            beanInstance = this.getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
        } catch (IllegalStateException ex) {
            throw new ScopeNotActiveException(beanName, scopeName, ex);
        }
    }
    // ...
}

// 3. GenericScope#get()
public class GenericScope
        implements Scope, BeanFactoryPostProcessor, BeanDefinitionRegistryPostProcessor, DisposableBean {
    // ...
    private BeanLifecycleWrapperCache cache = new BeanLifecycleWrapperCache(new StandardScopeCache());

    // ...
    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        BeanLifecycleWrapper value = this.cache.put(name, new BeanLifecycleWrapper(name, objectFactory)); // important -> go
        this.locks.putIfAbsent(name, new ReentrantReadWriteLock());
        try {
            return value.getBean();
        } catch (RuntimeException e) {
            this.errors.put(name, e);
            throw e;
        }
    }
    // ...
}
```


### 3. RefreshScopeBeanDefinitionEnhancer

#### 3.1. RefreshScopeBeanDefinitionEnhancer#postProcessBeanDefinitionRegistry()

```java
// 自动配置刷新范围和与环境变化相关的功能（例如重新绑定记录器级别）。
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RefreshScope.class)
// ...
public class RefreshAutoConfiguration {
    public static final String REFRESH_SCOPE_NAME = "refresh";
    // ...
    
    // RefreshAutoConfiguration.RefreshScopeBeanDefinitionEnhancer
    @Component
    protected static class RefreshScopeBeanDefinitionEnhancer
            implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
        // 用于将 bean 提交到刷新范围的类名。当您无法控制 bean 的定义（例如，它来自自动配置）时很有用。
        private Set<String> refreshables = new HashSet<>();
        
        // 1. RefreshAutoConfiguration.RefreshScopeBeanDefinitionEnhancer#postProcessBeanDefinitionRegistry()
        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            bindEnvironmentIfNeeded(registry);
            for (String name : registry.getBeanDefinitionNames()) {
                BeanDefinition definition = registry.getBeanDefinition(name);
                if (isApplicable(registry, name, definition)) { // important -> go
                    BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, name);
                    BeanDefinitionHolder proxy = ScopedProxyUtils.createScopedProxy(holder, registry, true);
                    definition.setScope(REFRESH_SCOPE_NAME);
                    if (registry.containsBeanDefinition(proxy.getBeanName())) {
                        registry.removeBeanDefinition(proxy.getBeanName());
                    }
                    registry.registerBeanDefinition(proxy.getBeanName(), proxy.getBeanDefinition());
                }
            }
        }

        // 2. RefreshAutoConfiguration.RefreshScopeBeanDefinitionEnhancer#isApplicable()
        private boolean isApplicable(BeanDefinitionRegistry registry, String name, BeanDefinition definition) {
            String scope = definition.getScope();
            if (REFRESH_SCOPE_NAME.equals(scope)) {
                // Already refresh scoped
                return false;
            }
            String type = definition.getBeanClassName();
            if (!StringUtils.hasText(type) && registry instanceof BeanFactory) {
                Class<?> cls = ((BeanFactory) registry).getType(name);
                if (cls != null) {
                    type = cls.getName();
                }
            }
            if (type != null) {
                return this.refreshables.contains(type); // important -> go
            }
            return false;
        }
    }
    // ...
}
```



### 4. RefreshEventListener

#### 4.1. 事件 RefreshEvent 和 ApplicationReadyEvent 的监听器

```java
// 1. RefreshAutoConfiguration
// 自动配置刷新范围和与环境变化相关的功能（例如重新绑定记录器级别）。
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RefreshScope.class)
// ...
public class RefreshAutoConfiguration {
    // ...
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBootstrapEnabled
    public LegacyContextRefresher legacyContextRefresher(ConfigurableApplicationContext context, RefreshScope scope,
                                                         RefreshProperties properties) {
        return new LegacyContextRefresher(context, scope, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBootstrapDisabled
    public ConfigDataContextRefresher configDataContextRefresher(ConfigurableApplicationContext context,
                                                                 RefreshScope scope, RefreshProperties properties) {
        return new ConfigDataContextRefresher(context, scope, properties);
    }
    // ...

    @Bean
    public RefreshEventListener refreshEventListener(ContextRefresher contextRefresher) {
        return new RefreshEventListener(contextRefresher); // important -> go
    }
    // ...
}

// 2. RefreshEventListener#onApplicationEvent()
public class RefreshEventListener implements SmartApplicationListener {
    private ContextRefresher refresh;
    private AtomicBoolean ready = new AtomicBoolean(false);
    
    // ...
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent) {
            handle((ApplicationReadyEvent) event); // important -> go
        }
        else if (event instanceof RefreshEvent) {
            handle((RefreshEvent) event); // important -> go
        }
    }

    public void handle(ApplicationReadyEvent event) {
        this.ready.compareAndSet(false, true);
    }

    public void handle(RefreshEvent event) {
        if (this.ready.get()) { // don't handle events before app is ready
            log.debug("Event received " + event.getEventDesc());
            Set<String> keys = this.refresh.refresh(); // important -> go
            log.info("Refresh keys changed: " + keys);
        }
    }
}

// 3. ContextRefresher#refresh()
public abstract class ContextRefresher {
    private RefreshScope scope;
    
    // 3.1. ContextRefresher#refresh()
    public synchronized Set<String> refresh() {
        Set<String> keys = refreshEnvironment(); // important -> go
        this.scope.refreshAll(); // important -> go
        return keys;
    }

    // 3.2. ContextRefresher#refreshEnvironment()
    public synchronized Set<String> refreshEnvironment() {
        Map<String, Object> before = extract(this.context.getEnvironment().getPropertySources());
        updateEnvironment();
        Set<String> keys = changes(before, extract(this.context.getEnvironment().getPropertySources())).keySet();
        // 发布事件 EnvironmentChangeEvent，该事件的监听：
        //     \-- ConfigurationPropertiesRebinder#onApplicationEvent()
        //     \-- LoggingRebinder#onApplicationEvent()
        this.context.publishEvent(new EnvironmentChangeEvent(this.context, keys)); // important -> go
        return keys;
    }
    
    // ContextRefresher#updateEnvironment() 方法的实现有以下两种：
    //    \-- ConfigDataContextRefresher#updateEnvironment()
    //    \-- LegacyContextRefresher#updateEnvironment()
    protected abstract void updateEnvironment(); // important -> go
    
    // ...
}

// 4. RefreshScope#refreshAll()
public class RefreshScope extends GenericScope
        implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, Ordered {
    private ApplicationContext context;
    // ...
    
    public void refreshAll() {
        super.destroy(); // important -> go
        // 发布事件 RefreshScopeRefreshedEvent
        this.context.publishEvent(new RefreshScopeRefreshedEvent());
    }
    // ...
}

// 5. GenericScope#destroy()
public class GenericScope
        implements Scope, BeanFactoryPostProcessor, BeanDefinitionRegistryPostProcessor, DisposableBean {
    // ...
    private BeanLifecycleWrapperCache cache = new BeanLifecycleWrapperCache(new StandardScopeCache());

    @Override
    public void destroy() {
        List<Throwable> errors = new ArrayList<>();
        Collection<BeanLifecycleWrapper> wrappers = this.cache.clear(); // important -> go
        for (BeanLifecycleWrapper wrapper : wrappers) {
            try {
                Lock lock = this.locks.get(wrapper.getName()).writeLock();
                lock.lock();
                try {
                    wrapper.destroy(); // important -> go
                } finally {
                    lock.unlock();
                }
            } catch (RuntimeException e) {
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) {
            throw wrapIfNecessary(errors.get(0));
        }
        this.errors.clear();
    }
    
    private static class BeanLifecycleWrapper {
        // ...
        // BeanLifecycleWrapper#destroy()
        public void destroy() {
            if (this.callback == null) {
                return;
            }
            synchronized (this.name) {
                Runnable callback = this.callback;
                if (callback != null) {
                    callback.run();  // important -> go
                }
                this.callback = null;
                this.bean = null;
            }
        }
        // ...
    }
}
```

### 5. ContextRefresher

#### ConfigDataContextRefresher#updateEnvironment()

```java
public class ConfigDataContextRefresher extends ContextRefresher
		implements ApplicationListener<ContextRefreshedWithApplicationEvent> {

    private SpringApplication application;
    // ...

    @Override
    protected void updateEnvironment() {
        if (logger.isTraceEnabled()) {
            logger.trace("Re-processing environment to add config data");
        }
        StandardEnvironment environment = copyEnvironment(getContext().getEnvironment());
        ConfigurableBootstrapContext bootstrapContext = getContext().getBeanProvider(ConfigurableBootstrapContext.class)
                .getIfAvailable(DefaultBootstrapContext::new);

        // 遍历所有 EnvironmentPostProcessor 实例。这样，诸如 vcap 和解密之类的操作就可以在刷新后执行。
        // 对 ConfigDataEnvironmentPostProcessor.applyTo() 的硬编码调用现在也已实现自动化。
        DeferredLogFactory logFactory = new PassthruDeferredLogFactory();
        List<String> classNames = SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor.class,
                getClass().getClassLoader());
        Instantiator<EnvironmentPostProcessor> instantiator = new Instantiator<>(EnvironmentPostProcessor.class,
                (parameters) -> {
                    parameters.add(DeferredLogFactory.class, logFactory);
                    parameters.add(Log.class, logFactory::getLog);
                    parameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
                    parameters.add(BootstrapContext.class, bootstrapContext);
                    parameters.add(BootstrapRegistry.class, bootstrapContext);
                });
        
        List<EnvironmentPostProcessor> postProcessors = instantiator.instantiate(classNames);
        for (EnvironmentPostProcessor postProcessor : postProcessors) {
            postProcessor.postProcessEnvironment(environment, application); // important
        }

        MutablePropertySources target = getContext().getEnvironment().getPropertySources();
        String targetName = null;
        for (PropertySource<?> source : environment.getPropertySources()) {
            String name = source.getName();
            if (target.contains(name)) {
                targetName = name;
            }
            if (!this.standardSources.contains(name)) {
                if (target.contains(name)) {
                    target.replace(name, source);
                } else {
                    if (targetName != null) {
                        target.addAfter(targetName, source);
                        // update targetName to preserve ordering
                        targetName = name;
                    } else {
                        // targetName was null so we are at the start of the list
                        target.addFirst(source);
                        targetName = name;
                    }
                }
            }
        }
    }
    // ...
}
```

### 6. LockedScopedProxyFactoryBean

```java
// GenericScope.LockedScopedProxyFactoryBean
public class GenericScope
        implements Scope, BeanFactoryPostProcessor, BeanDefinitionRegistryPostProcessor, DisposableBean {
    // ...
    public static class LockedScopedProxyFactoryBean<S extends GenericScope> extends ScopedProxyFactoryBean
            implements MethodInterceptor {
        private final S scope;
        // ...

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Method method = invocation.getMethod();
            if (AopUtils.isEqualsMethod(method) || AopUtils.isToStringMethod(method)
                    || AopUtils.isHashCodeMethod(method) || isScopedObjectGetTargetObject(method)) {
                return invocation.proceed();
            }
            Object proxy = getObject();
            ReadWriteLock readWriteLock = this.scope.getLock(this.targetBeanName);
            if (readWriteLock == null) {
                // ...
                readWriteLock = new ReentrantReadWriteLock();
            }
            Lock lock = readWriteLock.readLock();
            lock.lock();
            try {
                if (proxy instanceof Advised advised) {
                    ReflectionUtils.makeAccessible(method);
                    return ReflectionUtils.invokeMethod(method, advised.getTargetSource().getTarget(),
                            invocation.getArguments());
                }
                return invocation.proceed();
            }
            // 参见 gh-349。抛出原始异常，而不是 UndeclaredThrowableException。
            catch (UndeclaredThrowableException e) {
                throw e.getUndeclaredThrowable();
            }
            finally {
                lock.unlock();
            }
        }
        // ...
    }
    // ...
}
```


## 实际使用

### Nacos

```text
1. 事件 ApplicationReadyEvent 监听器，用于注册 Nacos Listeners（Listeners 需要做的事：当 Nacos Config 发生变化时，发布 RefreshEvent 事件）

2. 发布 RefreshEvent 事件后，对 Nacos Config 文件的加载
    由上一节可知：监听 RefreshEvent 和 ApplicationReadyEvent 事件的方法是 RefreshEventListener#onApplicationEvent()，
    它会调用 ConfigDataContextRefresher#updateEnvironment() 方法来刷新 Environment；
    而 ConfigDataContextRefresher#updateEnvironment() 方法会调用 NacosConfigDataLoader#load() 方法
        \-- NacosConfigDataLoader#load() -> Nacos Config 文件的加载
```


#### 1. NacosContextRefresher

```properties
// spring-cloud-alibaba-starters/spring-cloud-starter-alibaba-nacos-config/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.alibaba.cloud.nacos.NacosConfigAutoConfiguration
com.alibaba.cloud.nacos.endpoint.NacosConfigEndpointAutoConfiguration
```

```java
// NacosConfigAutoConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.nacos.config.enabled", matchIfMissing = true)
public class NacosConfigAutoConfiguration {
    // ...
    @Bean
    public NacosContextRefresher nacosContextRefresher(
            NacosConfigManager nacosConfigManager,
            NacosRefreshHistory nacosRefreshHistory) {
        // Consider that it is not necessary to be compatible with the previous
        // configuration
        // and use the new configuration if necessary.
        return new NacosContextRefresher(nacosConfigManager, nacosRefreshHistory); // important -> go
    }
    // ...
}

// 应用程序启动时，NacosContextRefresher 会将 nacos 监听器添加到所有应用程序级别的 dataIds，当数据发生变化时，监听器将刷新配置。
public class NacosContextRefresher
        implements ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // many Spring context
        if (this.ready.compareAndSet(false, true)) {
            this.registerNacosListenersForApplications(); // important -> go
        }
    }

    // 注册 Nacos Listeners
    private void registerNacosListenersForApplications() {
        if (isRefreshEnabled()) {
            for (NacosPropertySource propertySource : NacosPropertySourceRepository
                    .getAll()) {
                if (!propertySource.isRefreshable()) {
                    continue;
                }
                String dataId = propertySource.getDataId();
                registerNacosListener(propertySource.getGroup(), dataId); // important -> go
            }
        }
    }

    private void registerNacosListener(final String groupKey, final String dataKey) {
        String key = NacosPropertySourceRepository.getMapKey(dataKey, groupKey);
        Listener listener = listenerMap.computeIfAbsent(key,
                lst -> new AbstractSharedListener() {
                    @Override
                    public void innerReceive(String dataId, String group,
                                             String configInfo) {
                        refreshCountIncrement();
                        nacosRefreshHistory.addRefreshRecord(dataId, group, configInfo);
                        applicationContext.publishEvent(new RefreshEvent(this, null, "Refresh Nacos config")); // important -> go
                        // ...
                    }
                });
        try {
            configService.addListener(dataKey, groupKey, listener); // important -> go
            // ...
        }
        catch (NacosException e) {
            // ...
        }
    }
}
```


#### 2. NacosConfigDataLoader#load()

```java
/*
\-- ConfigDataContextRefresher#updateEnvironment() -> 见上一节中的 ContextRefresher
  \-- org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor#postProcessEnvironment(ConfigurableEnvironment, ResourceLoader, Collection<String>)
    \-- org.springframework.boot.context.config.ConfigDataEnvironment#processAndApply()
      \-- org.springframework.boot.context.config.ConfigDataEnvironment#processInitial()
        \-- org.springframework.boot.context.config.ConfigDataEnvironmentContributors#withProcessedImports()
          \-- org.springframework.boot.context.config.ConfigDataImporter#resolveAndLoad()
            \-- org.springframework.boot.context.config.ConfigDataLoaders#load()
              \-- org.springframework.boot.context.config.ConfigDataLoaders#load()
                \-- com.alibaba.cloud.nacos.configdata.NacosConfigDataLoader#load()
*/

// 1. NacosConfigDataLoader#load()
public ConfigData load(ConfigDataLoaderContext context, NacosConfigDataResource resource) {
    return doLoad(context, resource);
}

// 2. NacosConfigDataLoader#doLoad()
public ConfigData doLoad(ConfigDataLoaderContext context,
                         NacosConfigDataResource resource) {
    try {
        ConfigService configService = getBean(context, NacosConfigManager.class)
                .getConfigService();
        NacosConfigProperties properties = getBean(context,
                NacosConfigProperties.class);

        NacosItemConfig config = resource.getConfig();
        // pull config from nacos
        List<PropertySource<?>> propertySources = pullConfig(configService,
                config.getGroup(), config.getDataId(), config.getSuffix(),
                properties.getTimeout());

        NacosPropertySource propertySource = new NacosPropertySource(propertySources,
                config.getGroup(), config.getDataId(), new Date(),
                config.isRefreshEnabled());

        NacosPropertySourceRepository.collectNacosPropertySource(propertySource);

        return new ConfigData(propertySources, getOptions(context, resource));
    }
    catch (Exception e) {
        log.error("Error getting properties from nacos: " + resource, e);
        if (!resource.isOptional()) {
            throw new ConfigDataResourceNotFoundException(resource, e);
        }
    }
    return null;
}
```

