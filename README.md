# spring-boot-webflux-webclient-netty-leak
A sample Spring Boot 2.1.3 app showing the 'LEAK' in netty.

Simple application which returns fastest result of two `WebClient` requests using `Mono.first()`.
```java
@Configuration
public class RouterConfig {

  @Bean
  public RouterFunction<ServerResponse> routes() {
    return RouterFunctions.nest(
            path("/api"),
            route(GET("/test"), this::test));
  }

  public Mono<ServerResponse> test(ServerRequest request) {
    Mono<ClientResponse> exchangeA =
        WebClient.create("http://localhost:8008") // httpbin running locally
            .get()
            .uri(uriBuilder -> uriBuilder.path("/delay/5").build())
            .exchange();

    Mono<ClientResponse> exchangeB =
        WebClient.create("http://localhost:8008") // httpbin running locally
            .get()
            .uri(uriBuilder -> uriBuilder.path("/status/200").build())
            .exchange();

    return Mono.first(exchangeA, exchangeB)
        .flatMap(
            clientResponse ->
                 ServerResponse.status(clientResponse.rawStatusCode()).build());
  }
}

```
The application is failing with `LEAK` error:
```java
2019-03-28 10:52:21.203 ERROR 25237 --- [or-http-epoll-4] io.netty.util.ResourceLeakDetector       : LEAK: ByteBuf.release() was not called before it's garbage-collected. See http://netty.io/wiki/reference-counted-objects.html for more information.
```


## Steps to reporduce
Start local version of httpbin:
```bash
docker run -p 8008:80 kennethreitz/httpbin
```
Start the application (Java 11.0.1):
```bash
/usr/lib/jvm/jdk-11.0.1/bin/java -jar -Dio.netty.leakDetection.level=paranoid target/spring-boot-webflux-webclient-netty-leak-0.0.1-SNAPSHOT.jar
```
Send 2000 requests with `ab` (the number can be different on your machine):
```bash
ab -n 2000 -c 20 http://localhost:8080/api/test
```
The application fails ones with this `ERROR`:
```
2019-03-28 11:18:38.685 ERROR 26971 --- [or-http-epoll-1] io.netty.util.ResourceLeakDetector       : LEAK: ByteBuf.release() was not called before it's garbage-collected. See http://netty.io/wiki/reference-counted-objects.html for more information.
Recent access records: 
#1:
        Hint: 'reactor.right.reactiveBridge' will handle the message from this point.
        io.netty.handler.codec.http.DefaultHttpContent.touch(DefaultHttpContent.java:88)
        io.netty.handler.codec.http.DefaultLastHttpContent.touch(DefaultLastHttpContent.java:88)
        io.netty.handler.codec.http.DefaultLastHttpContent.touch(DefaultLastHttpContent.java:28)
        io.netty.channel.DefaultChannelPipeline.touch(DefaultChannelPipeline.java:116)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:345)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.handler.codec.MessageToMessageDecoder.channelRead(MessageToMessageDecoder.java:102)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:438)
        io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:323)
        io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:297)
        io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1408)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
#2:
        io.netty.handler.codec.http.DefaultHttpContent.release(DefaultHttpContent.java:94)
        io.netty.util.ReferenceCountUtil.release(ReferenceCountUtil.java:88)
        io.netty.handler.codec.MessageToMessageDecoder.channelRead(MessageToMessageDecoder.java:90)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:438)
        io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:323)
        io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:297)
        io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1408)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
#3:
        io.netty.handler.codec.http.DefaultHttpContent.retain(DefaultHttpContent.java:70)
        io.netty.handler.codec.http.DefaultLastHttpContent.retain(DefaultLastHttpContent.java:76)
        io.netty.handler.codec.http.DefaultLastHttpContent.retain(DefaultLastHttpContent.java:28)
        io.netty.handler.codec.http.HttpContentDecoder.decode(HttpContentDecoder.java:146)
        io.netty.handler.codec.http.HttpContentDecoder.decode(HttpContentDecoder.java:47)
        io.netty.handler.codec.MessageToMessageDecoder.channelRead(MessageToMessageDecoder.java:88)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:438)
        io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:323)
        io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:297)
        io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1408)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
#4:
        Hint: 'reactor.left.decompressor' will handle the message from this point.
        io.netty.handler.codec.http.DefaultHttpContent.touch(DefaultHttpContent.java:88)
        io.netty.handler.codec.http.DefaultLastHttpContent.touch(DefaultLastHttpContent.java:88)
        io.netty.handler.codec.http.DefaultLastHttpContent.touch(DefaultLastHttpContent.java:28)
        io.netty.channel.DefaultChannelPipeline.touch(DefaultChannelPipeline.java:116)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:345)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:438)
        io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:323)
        io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:297)
        io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1408)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
Created at:
        io.netty.buffer.SimpleLeakAwareByteBuf.unwrappedDerived(SimpleLeakAwareByteBuf.java:143)
        io.netty.buffer.SimpleLeakAwareByteBuf.readRetainedSlice(SimpleLeakAwareByteBuf.java:67)
        io.netty.buffer.AdvancedLeakAwareByteBuf.readRetainedSlice(AdvancedLeakAwareByteBuf.java:107)
        io.netty.handler.codec.http.HttpObjectDecoder.decode(HttpObjectDecoder.java:305)
        io.netty.handler.codec.http.HttpClientCodec$Decoder.decode(HttpClientCodec.java:202)
        io.netty.handler.codec.ByteToMessageDecoder.decodeRemovalReentryProtection(ByteToMessageDecoder.java:502)
        io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:441)
        io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:278)
        io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1408)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)

```

...and than multiple times with following `ERROR`:
```
2019-03-28 11:18:38.684 ERROR 26971 --- [or-http-epoll-4] io.netty.util.ResourceLeakDetector       : LEAK: ByteBuf.release() was not called before it's garbage-collected. See http://netty.io/wiki/reference-counted-objects.html for more information.
Recent access records: 
#1:
        io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:286)
        io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1408)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
#2:
        io.netty.buffer.AdvancedLeakAwareByteBuf.forEachByte(AdvancedLeakAwareByteBuf.java:670)
        io.netty.handler.codec.http.HttpObjectDecoder$HeaderParser.parse(HttpObjectDecoder.java:801)
        io.netty.handler.codec.http.HttpObjectDecoder.readHeaders(HttpObjectDecoder.java:601)
        io.netty.handler.codec.http.HttpObjectDecoder.decode(HttpObjectDecoder.java:227)
        io.netty.handler.codec.http.HttpClientCodec$Decoder.decode(HttpClientCodec.java:202)
        io.netty.handler.codec.ByteToMessageDecoder.decodeRemovalReentryProtection(ByteToMessageDecoder.java:502)
        io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:441)
        io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:278)
        io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1408)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
#3:
        io.netty.buffer.AdvancedLeakAwareByteBuf.forEachByte(AdvancedLeakAwareByteBuf.java:670)
        io.netty.handler.codec.http.HttpObjectDecoder$HeaderParser.parse(HttpObjectDecoder.java:801)
        io.netty.handler.codec.http.HttpObjectDecoder.readHeaders(HttpObjectDecoder.java:581)
        io.netty.handler.codec.http.HttpObjectDecoder.decode(HttpObjectDecoder.java:227)
        io.netty.handler.codec.http.HttpClientCodec$Decoder.decode(HttpClientCodec.java:202)
        io.netty.handler.codec.ByteToMessageDecoder.decodeRemovalReentryProtection(ByteToMessageDecoder.java:502)
        io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:441)
        io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:278)
        io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1408)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
#4:
        io.netty.buffer.AdvancedLeakAwareByteBuf.getUnsignedByte(AdvancedLeakAwareByteBuf.java:160)
        io.netty.handler.codec.http.HttpObjectDecoder.skipControlCharacters(HttpObjectDecoder.java:566)
        io.netty.handler.codec.http.HttpObjectDecoder.decode(HttpObjectDecoder.java:202)
        io.netty.handler.codec.http.HttpClientCodec$Decoder.decode(HttpClientCodec.java:202)
        io.netty.handler.codec.ByteToMessageDecoder.decodeRemovalReentryProtection(ByteToMessageDecoder.java:502)
        io.netty.handler.codec.ByteToMessageDecoder.callDecode(ByteToMessageDecoder.java:441)
        io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:278)
        io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:253)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1408)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
#5:
        Hint: 'reactor.left.httpCodec' will handle the message from this point.
        io.netty.channel.DefaultChannelPipeline.touch(DefaultChannelPipeline.java:116)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:345)
        io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:340)
        io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1408)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:362)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:348)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
#6:
        Hint: 'DefaultChannelPipeline$HeadContext#0' will handle the message from this point.
        io.netty.channel.DefaultChannelPipeline.touch(DefaultChannelPipeline.java:116)
        io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:345)
        io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:930)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:799)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
Created at:
        io.netty.buffer.PooledByteBufAllocator.newDirectBuffer(PooledByteBufAllocator.java:339)
        io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:185)
        io.netty.buffer.AbstractByteBufAllocator.directBuffer(AbstractByteBufAllocator.java:176)
        io.netty.channel.unix.PreferredDirectByteBufAllocator.ioBuffer(PreferredDirectByteBufAllocator.java:53)
        io.netty.channel.DefaultMaxMessagesRecvByteBufAllocator$MaxMessageHandle.allocate(DefaultMaxMessagesRecvByteBufAllocator.java:114)
        io.netty.channel.epoll.EpollRecvByteAllocatorHandle.allocate(EpollRecvByteAllocatorHandle.java:77)
        io.netty.channel.epoll.AbstractEpollStreamChannel$EpollStreamUnsafe.epollInReady(AbstractEpollStreamChannel.java:784)
        io.netty.channel.epoll.EpollEventLoop.processReady(EpollEventLoop.java:427)
        io.netty.channel.epoll.EpollEventLoop.run(EpollEventLoop.java:328)
        io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:905)
        java.base/java.lang.Thread.run(Thread.java:834)
: 8 leak records were discarded because the leak record count is targeted to 4. Use system property io.netty.leakDetection.targetRecords to increase the limit.

```
