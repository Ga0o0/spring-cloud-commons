# Hypermedia

CloudHypermediaAutoConfiguration 是 Spring Cloud Commons 中一个特定场景的自动配置类。
它的核心应用场景是：在微服务中动态发现和刷新指向其他服务的 HATEOAS（超媒体）链接，并自动检查这些链接的可用性。

具体来说，当你的服务需要消费另一个服务的 HATEOAS（超媒体驱动架构）资源，并且不想硬编码对方的地址，
而是希望通过服务发现来动态获取时，这个自动配置就会发挥作用。