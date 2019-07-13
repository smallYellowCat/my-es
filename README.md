# Elasticsearch搜索引擎学习笔记及其示例


## 1. 简介

Elasticsearch是一个高度可扩展的开源的全文检索和分析引擎。它能够近乎实时的快速存储，搜索和分析大量数据。通常用作底层的引擎技术，为具有复杂搜索功能和
要求的应用程序提供支持。

### 1.1. 基本概念（行业黑话）

 1. 近乎实时（Near Realtime (NRT)）
 
    Elasticsearch是一个近乎实时的搜索平台。这意味着从索引一个文档到可以被搜索有一点延时（通常是一秒）。
    
 2. 集群（Cluster）
 
    集群是一个或多个节点（服务）集合，这些节点将您的整个数据集中在一起，并在所有节点上提供联合索引和搜索功能。集群由一个唯一的名称标识，默认名称为
    “ElasticSearch”。此名称很重要，因为如果将节点设置为通过其名称加入集群，则节点只能是集群的一部分。
    
    请确保不要在不同的环境中重用相同的集群名称，否则最终可能会导致节点加入错误的集群。例如，您可以使用LoggingDev、LoggingStage和LoggingProd来
    用于开发、分段和生产集群。
    
    请注意，拥有一个只包含单个节点的集群是完全正常的。 此外，您还可以拥有多个独立的集群，每个集群都有自己唯一的集群名称。
    
 3. 节点（Node）
 
    节点是作为集群一部分的单个服务器，存储数据并参与集群的索引和搜索功能。与集群一样，节点由名称标识，默认情况下，该名称是在启动时分配给节点的随机通
    用唯一标识符（UUID）。如果不需要默认值，可以定义所需的任何节点名称。此名称对于管理目的非常重要，您可以在其中识别网络中的哪些服务器与Elasticsearch
    集群中的哪些节点相对应。
    
    可以将节点配置为按集群名称加入特定集群。 默认情况下，每个节点都设置为加入名为elasticsearch的集群，这意味着如果您在网络上启动了许多节点并且假设
    它们可以相互发现 - 它们将自动形成并加入一个名为elasticsearch的集群。
    
    在单个集群中，您可以拥有任意数量的节点。 此外，如果您的网络上当前没有其他Elasticsearch节点正在运行，则默认情况下启动单个节点将形成名为elasticsearch
    的新单节点集群。
 
 
 4. 索引（Index）
 
    索引是具有某些类似特征的文档集合。 例如，您可以拥有客户数据的索引，产品目录的另一个索引以及订单数据的另一个索引。 索引由名称标识（必须全部为小写），
    并且此名称用于在对其中的文档执行索引，搜索，更新和删除操作时引用索引。
    
    在单个群集中，您可以根据需要定义任意数量的索引。
 
 
 5. 文档（Document）
 
    文档是可以被索引的基本信息单元。例如，您可以为单个客户创建一个文档，为单个产品创建另一个文档，为单个订单创建另一个文档。本文档以JSON表示，JSON
    是一种普遍存在的Internet数据交换格式。
    
    在索引或类型(index/type)中，您可以存储任意多的文档。请注意，尽管文档实际上驻留在索引中，但文档实际上必须被索引或分配给索引中的类型。
 
 
 6. 分片和副本（Shards & Replicasedit）
 
    索引可能会存储大量数据，这些数据可能会超出单个节点的硬件限制。例如，占用1TB磁盘空间的10亿个文档的单个索引可能不适合单个节点的磁盘，或者速度太慢，
    无法单独满足单个节点的搜索请求。
    
    为了解决这个问题，Elasticsearch提供了将索引细分为多个称为分片的功能。创建索引时，只需定义所需的分片数即可。每个分片本身都是一个功能齐全且独立
    的“索引”，可以托管在集群中的任何节点上。
    
    分片很重要，主要有两个原因：
    
    * 它允许您水平拆分/缩放内容量
    * 它允许您跨分片（可能在多个节点上）分布和并行化操作，从而提高性能/吞吐量
        
    分片的分配方式以及如何将其文档聚合回搜索请求的机制完全由Elasticsearch管理，对用户而言是透明的。
    
    强烈建议在分片/节点以某种方式脱机或因任何原因消失时使用故障转移机制（在随时可能发生故障的网络/云环境中，非常有用）。为此，ElasticSearch允许您将
    索引分片的一个或多个副本复制成所谓的副本分片（或分片副本）。
    
    副本很重要，主要有两个原因：  
    
    * 它在分片或节点发生故障时提供高可用性。因此，需要注意的是，分片副本永远不会分配到它的原始（主）分片所在的节点上。
    
    * 它允许你扩展搜索量或吞吐量，因为可以在所有副本上并行执行搜索。  
    
    总而言之，每个索引可以分割成多个分片。索引也可以被复制零次（意味着没有副本）或多次。复制后，每个索引将具有主分片（从中复制的原始分片）和副本分片
    （主分片的副本）。
 
    可以在创建索引时为每个索引定义碎片和副本的数量。创建索引后，您还可以随时动态更改副本的数量。您可以使用-shrink和_split API更改现有索引的分片数量，但是这
    不是一项简单的任务，并且预先计划正确数量的分片是最佳方法。
    
    默认情况下，Elasticsearch中的每个索引都分配了5个主分片和1个副本，这意味着如果群集中至少有两个节点，则索引将包含5个主分片和另外5个副本分片（1个完整副本），
    总计为每个索引10个分片。
    
    注意
    
    每个ElasticSearch分片都是一个Lucene索引。在一个Lucene索引中，文档数量有个最大数限制。从Lucene-5843起，限制为2147483519（=integer.max_value-128）个文档。
    您可以使用 cat/shards api监视分片大小。
    
    
 
 
### 1.2. 安装

本次学习是基于6.5.4版本，要求DJK至少是8，系统是Win7。windows中安装es官网建议使用MSI Installer package来安装，安装过程比较简单，具体步骤参考
官网[windows安装教程](https://www.elastic.co/guide/en/elasticsearch/reference/6.5/getting-started-install.html)

### 1.3. 启动测试（windows环境）


安装完成之后在安装根目录下的bin中有elasticsearch.exe可以直接启动es，启动后的截图如下：

同时也可以在浏览器中通过9200（默认）端口查看启动状况，如下所示：



## 2. ES相关知识

此部分暂时不记录，由于现实原因需要尽快使用es，所以暂时隐去此部分，待日后补全.


## Java REST Client（推荐的方式）

### 1. 概述

Java REST Client有两个版本：  
* Java Low Level REST Client ：Elasticsearch的官方低级客户端。 它允许通过http与Elasticsearch集群通信。 用户自己编写请求JSON串，自己解
析响应JSON字符串。 它与所有Elasticsearch版本兼容。  
* Java High Level REST Client ：Java高级REST客户端：Elasticsearch的官方高级客户端。 它基于低级客户端，公开API特定方法，并负责编组请求JSON串
和解析响应JSON字符串。

    
### 2. Java Low Level REST Client

1. Maven仓库

低级的java rest client要求的最低java版本是1.7。低级REST Client 与ElasticSearch的发布周期相同。将版本替换为所需的客户端版本。客户端版本和客户端
可以通信的ElasticSearch版本之间没有关系。低级REST Client与所有ElasticSearch版本兼容。

Maven依赖：

```xml
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-client</artifactId>
    <version>7.2.0</version>
</dependency>
```

Gradle依赖：

```groovy
dependencies {
    compile 'org.elasticsearch.client:elasticsearch-rest-client:7.2.0'
}
```

2. 所需依赖

低级Java REST Client内部使用Apache Http Async Client 发送HTTP请求。它依赖于以下构件，即异步HTTP客户端及其自身的可传递依赖项：

* org.apache.httpcomponents:httpasyncclient
* org.apache.httpcomponents:httpcore-nio
* org.apache.httpcomponents:httpclient
* org.apache.httpcomponents:httpcore
* commons-codec:commons-codec
* commons-logging:commons-logging


3. Shading

为了避免版本冲突，可以将依赖项隐藏并打包到客户端的单个JAR文件中（有时称为“uber JAR”或“fat JAR”）。隐藏依赖性包括获取其内容（资源文件和Java类文件）
并将其包重新命名，然后将它们放入与低级别Java REST Client相同的JAR文件中。对于Gradle和Maven，可以通过第三方插件对jar进行遮蔽。
         
请注意，对JAR进行隐藏也会有影响。例如，对Commons Logging层进行隐藏意味着第三方日志记录后端也需要进行隐藏。

Maven配置

下面是使用Maven的Shade插件的配置。将其添加到你的pom.xml文件。

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.1.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals><goal>shade</goal></goals>
                    <configuration>
                        <relocations>
                            <relocation>
                                <pattern>org.apache.http</pattern>
                                <shadedPattern>hidden.org.apache.http</shadedPattern>
                            </relocation>
                            <relocation>
                                <pattern>org.apache.logging</pattern>
                                <shadedPattern>hidden.org.apache.logging</shadedPattern>
                            </relocation>
                            <relocation>
                                <pattern>org.apache.commons.codec</pattern>
                                <shadedPattern>hidden.org.apache.commons.codec</shadedPattern>
                            </relocation>
                            <relocation>
                                <pattern>org.apache.commons.logging</pattern>
                                <shadedPattern>hidden.org.apache.commons.logging</shadedPattern>
                            </relocation>
                        </relocations>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Gradle配置

下面是使用Gradle的ShadowJar插件的配置。添加下面的配置到你的build.gradle文件。

```groovy
shadowJar {
    relocate 'org.apache.http', 'hidden.org.apache.http'
    relocate 'org.apache.logging', 'hidden.org.apache.logging'
    relocate 'org.apache.commons.codec', 'hidden.org.apache.commons.codec'
    relocate 'org.apache.commons.logging', 'hidden.org.apache.commons.logging'
}
```


4. 初始化

可以通过RestClient.builder（HttpHost ...）静态方法创建的相应RestClientBuilder类构建RestClient实例。 唯一必需的参数是客户端将与之通信的
一个或多个主机，作为HttpHost的实例提供，如下所示：

```java
RestClient restClient = RestClient.builder(
    new HttpHost("localhost", 9200, "http"),
    new HttpHost("localhost", 9201, "http")).build();
```

RestClient类是线程安全的，理想情况下与使用它的应用程序具有相同的生命周期。 重要的是它在不再需要时关闭，以便它使用的所有资源得到正确释放，例如底层的
http客户端实例及其线程：

```java
restClient.close();
```

RestClientBuilder还允许在构建RestClient实例时可选地设置以下配置参数：

```java
RestClientBuilder builder = RestClient.builder(
    new HttpHost("localhost", 9200, "http"));
Header[] defaultHeaders = new Header[]{new BasicHeader("header", "value")};
//设置需要随每个请求一起发送的默认标头，以防止必须为每个请求指定它们
builder.setDefaultHeaders(defaultHeaders);
```


设置一个侦听器，该侦听器在每次节点失败时都会收到通知，以防需要采取操作。在启用故障嗅探时在内部使用。

```java
RestClientBuilder builder = RestClient.builder(
        new HttpHost("localhost", 9200, "http"));
builder.setFailureListener(new RestClient.FailureListener() {
    @Override
    public void onFailure(Node node) {
        
    }
});    
```
设置节点选择器，用于过滤客户端将请求发送到客户端本身的节点。 这有助于防止在启用嗅探时向专用主节点发送请求。 默认情况下，客户端向每个配置的节点发送请求。

```java
RestClientBuilder builder = RestClient.builder(
    new HttpHost("localhost", 9200, "http"));
builder.setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS); 
```

设置允许修改默认请求配置的回调（例如，请求超时，身份验证或org.apache.http.client.config.RequestConfig.Builder允许设置的任何内容）

```java
RestClientBuilder builder = RestClient.builder(
        new HttpHost("localhost", 9200, "http"));
builder.setRequestConfigCallback(
    new RestClientBuilder.RequestConfigCallback() {
        @Override
        public RequestConfig.Builder customizeRequestConfig(
                RequestConfig.Builder requestConfigBuilder) {
            return requestConfigBuilder.setSocketTimeout(10000); 
        }
    });
```
设置允许修改http客户端配置的回调（例如，通过ssl进行加密通信，或者org.apache.http.impl.nio.client.HttpAsyncClientBuilder允许设置的任何内容）

```java
RestClientBuilder builder = RestClient.builder(
    new HttpHost("localhost", 9200, "http"));
builder.setHttpClientConfigCallback(new HttpClientConfigCallback() {
        @Override
        public HttpAsyncClientBuilder customizeHttpClient(
                HttpAsyncClientBuilder httpClientBuilder) {
            return httpClientBuilder.setProxy(
                new HttpHost("proxy", 9000, "http"));  
        }
    });
```

5. 执行请求

创建RestClient后，可以通过调用performRequest或performRequestAsync来发送请求。 performRequest是同步的，将阻塞调用线程并在请求成功时返回Response，
如果失败则抛出异常。 performRequestAsync是异步的，它接受一个ResponseListener参数，它在请求成功时调用Response，如果it4失败则调用Exception。

同步方式：

```java

Request request = new Request(
    "GET",  //The HTTP method (GET, POST, HEAD, etc)
    "/"); //The endpoint on the server
Response response = restClient.performRequest(request);
```

异步方式：

```java
Request request = new Request(
    "GET",  
    "/");   
restClient.performRequestAsync(request, new ResponseListener() {
    @Override
    public void onSuccess(Response response) {
        //	Handle the response
    }

    @Override
    public void onFailure(Exception exception) {
        //Handle the failure
    }
});
```
你可以向request对象中添加请求参数

```java
request.addParameter("pretty", "true");
```

你可以在任何HttpEntity中设置请求体

```java
request.setEntity(new NStringEntity(
        "{\"json\":\"text\"}",
        ContentType.APPLICATION_JSON));
```
**注意：为httpEntity指定的ContentType很重要，因为它将用于设置Content-Type头，以便ElasticSearch可以正确分析内容。**


您还可以将其设置为String，默认为ContentType为application/json。

```java
request.setJsonEntity("{\"json\":\"text\"}");
```

6. 请求选项

RequestOptions类保存请求的部分，这些部分应该在同一应用程序中的多个请求之间共享。您可以创建一个单实例并在所有请求之间共享它：

```java
    private static final RequestOptions COMMON_OPTIONS;
    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader("Authorization", "Bearer " + TOKEN); //添加请求需要的任何头
        builder.setHttpAsyncResponseConsumerFactory(    //自定义响应的消费者       
            new HttpAsyncResponseConsumerFactory
                .HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }
```

addHeader用于授权或在Elasticsearch前使用代理所需的标头。 无需设置Content-Type标头，因为客户端将自动从附加到请求的HttpEntity设置该标头。

您可以设置NodeSelector来控制哪些节点将接收请求。 NodeSelector.NOT_MASTER_ONLY是一个不错的选择。

您还可以自定义用于缓冲异步响应的响应消费者。 默认使用者将在JVM堆上缓冲最多100MB的响应。 如果响应较大，则请求将失败。 例如，如果您在如上例所示的堆约
束环境中运行，则可以降低可能有用的最大大小。

创建单例后，您可以在发出请求时使用它：

```java
request.setOptions(COMMON_OPTIONS);
```

您还可以根据请求自定义这些选项。 例如，这会添加一个额外的头：

```java
RequestOptions.Builder options = COMMON_OPTIONS.toBuilder();
options.addHeader("cats", "knock things off of other things");
request.setOptions(options);
```

7. 多个并行异步操作

客户很乐意并行执行许多操作。 以下示例并行索引许多文档。 在现实世界中，您可能希望使用_bulk API，但示例是说明性的。

```java
final CountDownLatch latch = new CountDownLatch(documents.length);
for (int i = 0; i < documents.length; i++) {
    Request request = new Request("PUT", "/posts/doc/" + i);
    //let's assume that the documents are stored in an HttpEntity array
    request.setEntity(documents[i]);
    restClient.performRequestAsync(
            request,
            new ResponseListener() {
                @Override
                public void onSuccess(Response response) {
                    //处理返回的响应
                    latch.countDown();
                }

                @Override
                public void onFailure(Exception exception) {
                    //处理返回的异常，由于通信错误或带有错误码的响应，处理返回的异常
                    latch.countDown();
                }
            }
    );
}
latch.await();
```

8. 读取响应

Response对象（由同步performRequest方法返回或作为ResponseListener.onSuccess（Response）中的参数接收）包装http客户端返回的响应对象并公开
一些其他信息。

```java
Response response = restClient.performRequest(new Request("GET", "/"));
RequestLine requestLine = response.getRequestLine(); //执行请求的信息
HttpHost host = response.getHost(); //返回响应的主机
int statusCode = response.getStatusLine().getStatusCode(); //响应状态行，你可以从中检索状态代码
Header[] headers = response.getHeaders(); //响应头，也可以通过getHeader(String)按名称检索
String responseBody = EntityUtils.toString(response.getEntity()); //响应主体包含在org.apache.http.HttpEntity对象中
```

执行请求时，会抛出异常（或在以下方案中作为ResponseListener.onFailure（Exception）中的参数接收：

**IOException**

  通信问题 (例如. SocketTimeoutException)
    
**ResponseException**

  返回了响应，但其状态代码表示错误（不是2xx）。 ResponseException源自有效的http响应，因此它公开其相应的Response对象，该对象提供对返回的响应的
  访问。
  
  
  
注意：对于返回404状态代码的HEAD请求，不会抛出ResponseException，因为它是一个预期的HEAD响应，只表示找不到该资源。 除非ignore参数包含404，否则
所有其他HTTP方法（例如，GET）都会为404响应抛出ResponseException.ignore是一个特殊的客户端参数，它不会被发送到Elasticsearch并包含逗号分隔的错
误状态代码列表。 它允许控制是否应将某些错误状态代码视为预期响应而不是异常。 这对于例如get api很有用，因为它可以在文档丢失时返回404，在这种情况下，
响应正文将不包含错误，而是通常的get api响应，只是没有找到未找到的文档。

请注意，低级客户端不会公开任何json编组和取消编组的帮助程序。 用户可以自由地使用他们喜欢的库。

底层的Apache Async Http Client附带了不同的org.apache.http.HttpEntity实现，这些实现允许以不同的格式（流，字节数组，字符串等）提供请求主体。 
至于读取响应体，HttpEntity.getContent方法很方便，它返回从先前缓冲的响应体读取的InputStream。 作为替代方案，可以提供一个自定义的
org.apache.http.nio.protocol.HttpAsyncResponseConsumer来控制字节的读取和缓冲方式。


9. 日志

Java REST Client使用与Apache Async Http Client使用的相同的日志库：Apache Commons Logging，它支持许多流行的日志记录实现。 要启用日志记录
的java包是客户端本身的org.elasticsearch.client和嗅探器的org.elasticsearch.client.sniffer。

还可以启用请求跟踪器日志记录，以便以curl格式记录每个请求和相应的响应。 这在调试时很方便，例如，如果需要手动执行请求以检查它是否仍然产生与它相同的响应。 
为跟踪器包启用跟踪日志记录以打印出此类日志行。 请注意，此类日志记录非常昂贵，不应在生产环境中始终启用，而应仅在需要时暂时使用。

10. 公有配置

正如Initialization中所解释的，RestClientBuilder支持提供RequestConfigCallback和HttpClientConfigCallback，它们允许
Apache Async Http Client公开的任何自定义。 这些回调可以修改客户端的某些特定行为，而不会覆盖RestClient初始化的所有其他默认配置。 本节介绍一些
需要为低级Java REST Client进行其他配置的常见方案。

10.1 超时设置

配置请求超时可以通过在其构建器构建RestClient时提供RequestConfigCallback实例来完成。 该接口有一个方法，它接收org.apache.http.client
.config.RequestConfig.Builder的实例作为参数并具有相同的返回类型。 可以修改请求配置构建器，然后返回。 在以下示例中，我们将连接超时（默认为1秒）
和套接字超时（默认为30秒）增加。

```java
RestClientBuilder builder = RestClient.builder(
    new HttpHost("localhost", 9200))
    .setRequestConfigCallback(
        new RestClientBuilder.RequestConfigCallback() {
            @Override
            public RequestConfig.Builder customizeRequestConfig(
                    RequestConfig.Builder requestConfigBuilder) {
                return requestConfigBuilder
                    .setConnectTimeout(5000)
                    .setSocketTimeout(60000);
            }
        });
```
10.2 线程数量

Apache Http Async Client默认启动一个调度程序线程，以及连接管理器使用的许多工作线程，与本地检测到的处理器数量一样多（取决于Runtime.getRuntime()
.availableProcessors()返回的数量）。 线程数可以修改如下：

```java
RestClientBuilder builder = RestClient.builder(
    new HttpHost("localhost", 9200))
    .setHttpClientConfigCallback(new HttpClientConfigCallback() {
        @Override
        public HttpAsyncClientBuilder customizeHttpClient(
                HttpAsyncClientBuilder httpClientBuilder) {
            return httpClientBuilder.setDefaultIOReactorConfig(
                IOReactorConfig.custom()
                    .setIoThreadCount(1)
                    .build());
        }
    });
```
10.3 基本认证

配置基本身份验证可以通过在其构建器构建RestClient时提供HttpClientConfigCallback来完成。 该接口有一个方法，它接收org.apache.http.impl.
nio.client.HttpAsyncClientBuilder的实例作为参数，并具有相同的返回类型。 可以修改http客户端构建器，然后返回。 在以下示例中，我们设置了需要基
本身份验证的默认凭据提供程序。

```java
final CredentialsProvider credentialsProvider =
    new BasicCredentialsProvider();
credentialsProvider.setCredentials(AuthScope.ANY,
    new UsernamePasswordCredentials("user", "password"));

RestClientBuilder builder = RestClient.builder(
    new HttpHost("localhost", 9200))
    .setHttpClientConfigCallback(new HttpClientConfigCallback() {
        @Override
        public HttpAsyncClientBuilder customizeHttpClient(
                HttpAsyncClientBuilder httpClientBuilder) {
            return httpClientBuilder
                .setDefaultCredentialsProvider(credentialsProvider);
        }
    });
```
抢占式身份验证可以被禁用，这意味着每个请求都将在没有授权头的情况下发送，以查看它是否被接受，并且在收到HTTP 401响应后，它将使用基本身份验证头重新发送
完全相同的请求。 如果您希望这样做，那么您可以通过HttpAsyncClientBuilder禁用它：

```java
final CredentialsProvider credentialsProvider =
    new BasicCredentialsProvider();
credentialsProvider.setCredentials(AuthScope.ANY,
    new UsernamePasswordCredentials("user", "password"));

RestClientBuilder builder = RestClient.builder(
    new HttpHost("localhost", 9200))
    .setHttpClientConfigCallback(new HttpClientConfigCallback() {
        @Override
        public HttpAsyncClientBuilder customizeHttpClient(
                HttpAsyncClientBuilder httpClientBuilder) {
            httpClientBuilder.disableAuthCaching(); //Disable preemptive authentication
            return httpClientBuilder
                .setDefaultCredentialsProvider(credentialsProvider);
        }
    });
```

10.4 通信加密

也可以通过HttpClientConfigCallback配置加密通信。 作为参数接收的org.apache.http.impl.nio.client.HttpAsyncClientBuilder公开了多种方法
来配置加密通信：setSSLContext，setSSLSessionStrategy和setConnectionManager，按照最不重要的优先顺序排列。 以下是一个例子：

```java
KeyStore truststore = KeyStore.getInstance("jks");
try (InputStream is = Files.newInputStream(keyStorePath)) {
    truststore.load(is, keyStorePass.toCharArray());
}
SSLContextBuilder sslBuilder = SSLContexts.custom()
    .loadTrustMaterial(truststore, null);
final SSLContext sslContext = sslBuilder.build();
RestClientBuilder builder = RestClient.builder(
    new HttpHost("localhost", 9200, "https"))
    .setHttpClientConfigCallback(new HttpClientConfigCallback() {
        @Override
        public HttpAsyncClientBuilder customizeHttpClient(
                HttpAsyncClientBuilder httpClientBuilder) {
            return httpClientBuilder.setSSLContext(sslContext);
        }
    });
```
如果未提供显式配置，则将使用系统默认配置。

10.5 其他

对于所需的任何其他所需配置，应参考[Apache HttpAsyncClient文档](https：//hc.apache.org/httpcomponents-asyncclient-4.1.x/)。

注意:

如果您的应用程序在安全管理器下运行，则可能会受到JVM默认策略的限制，即无限期缓存正主机名解析和负主机名解析，持续10秒。 如果您连接客户端的主机的已解析
地址随时间变化，那么您可能希望修改默认的JVM行为。 可以通过将networkaddress.cache.ttl = <timeout>和networkaddress.cache.negative.ttl = <timeout>
添加到Java安全策略来修改这些。

10.6 节点选择器

客户端以循环方式将每个请求发送到其中一个配置的节点。 可以选择通过在初始化客户端时需要提供的节点选择器来过滤节点。 这在启用嗅探时很有用，以防只有HTTP
请求才能访问专用主节点。 对于每个请求，客户端将运行最终配置的节点选择器以过滤候选节点，然后从列表中选择下一个节点选择器。

```java
RestClientBuilder builder = RestClient.builder(
        new HttpHost("localhost", 9200, "http"));
/*
 *设置一个分配感知节点选择器，允许在本地机架中选择一个节点（如果有），否则转到任何机架中的任何其他节点。
 *它作为首选项而不是严格的要求，因为如果没有任何本地节点可用，它将转到另一个机架，而不是返回任何节点，
 * 在这种情况下，当首选机架中没有任何节点可用时，客户端将强制恢复本地节点。
 */
builder.setNodeSelector(new NodeSelector() { 
    @Override
    public void select(Iterable<Node> nodes) {
        /*
         * Prefer any node that belongs to rack_one. If none is around
         * we will go to another rack till it's time to try and revive
         * some of the nodes that belong to rack_one.
         */
        boolean foundOne = false;
        for (Node node : nodes) {
            String rackId = node.getAttributes().get("rack_id").get(0);
            if ("rack_one".equals(rackId)) {
                foundOne = true;
                break;
            }
        }
        if (foundOne) {
            Iterator<Node> nodesIt = nodes.iterator();
            while (nodesIt.hasNext()) {
                Node node = nodesIt.next();
                String rackId = node.getAttributes().get("rack_id").get(0);
                if ("rack_one".equals(rackId) == false) {
                    nodesIt.remove();
                }
            }
        }
    }
});

```
警告：
不一致地选择同一组节点的节点选择器将使循环行为不可预测，并且可能不公平。上面的首选项示例是很好的，因为它是关于节点可用性的原因，这些节点已经影响了循环
的可预测性。节点选择不应依赖于其他外部因素，否则循环将无法正常工作。

11. 嗅探器

嗅探器
最小库，允许自动发现运行中的ElasticSearch集群中的节点，并将其设置为现有的RestClient实例。默认情况下，它使用nodes info API检索属于集群的节点，
并使用jackson解析获得的JSON响应。

与ElasticSearch 2.x及更高版本兼容。

关于Rest Client sniffer的java文档可在此[链接](https://artifacts.elastic.co/javadoc/org/elasticsearch/client/elasticsearch-rest-client-sniffer/7.2.0/index.html)中找到。

11.1 Maven Repository

REST客户端嗅探器与Elasticsearch具有相同的发布周期。 将版本替换为所需的嗅探器版本，首先使用5.0.0-alpha4发布。 嗅探器版本与客户端可以与之通信的
Elasticsearch版本之间没有任何关系。 Sniffer支持从Elasticsearch 2.x及以后获取节点列表。

Maven依赖

```xml
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-client-sniffer</artifactId>
    <version>7.2.0</version>
</dependency>
```
Gradle依赖

```groovy
dependencies {
    compile 'org.elasticsearch.client:elasticsearch-rest-client-sniffer:7.2.0'
}
```

11.2 用法

一旦创建了RestClient实例，如初始化中所示，可以将Sniffer与其关联。 Sniffer将定期（默认情况下每隔5分钟）使用提供的RestClient从集群中获取当前节
点列表，并通过调用RestClient.setNodes来更新它们。

```java
RestClient restClient = RestClient.builder(
    new HttpHost("localhost", 9200, "http"))
    .build();
Sniffer sniffer = Sniffer.builder(restClient).build();
```
关闭Sniffer以使其后台线程正确关闭并释放其所有资源非常重要。 Sniffer对象应具有与RestClient相同的生命周期，并在客户端之前关闭：

```java
sniffer.close();
restClient.close();
```
Sniffer默认每5分钟更新一次节点。 可以通过提供以毫秒为单位的值来定制此间隔，如下所示：

```java
RestClient restClient = RestClient.builder(
    new HttpHost("localhost", 9200, "http"))
    .build();
Sniffer sniffer = Sniffer.builder(restClient)
    .setSniffIntervalMillis(60000).build();
```

也可以在失败时启用嗅探，这意味着在每次失败后，节点列表都会立即更新，而不是在接下来的普通嗅探循环中更新。在这种情况下，需要首先创建一个snifonfailureListener，
然后在restclient创建时提供它。另外，在稍后创建嗅探器后，它需要与同一个snifonfailureListener实例相关联，在每次失败时都会通知该实例，并使用嗅探
器执行所描述的其他嗅探循环。

```java
SniffOnFailureListener sniffOnFailureListener =
    new SniffOnFailureListener();
RestClient restClient = RestClient.builder(
    new HttpHost("localhost", 9200))
    .setFailureListener(sniffOnFailureListener) //设置失败监听器
    .build();
Sniffer sniffer = Sniffer.builder(restClient)
    /*
    * 当嗅探失败时，不仅节点在每次失败后都会得到更新，而且默认情况下在失败后的一分钟，还会提前安排一次额外的嗅探循环，假设情况会恢复正常，我们希望尽
    * 快检测到这一点。所述间隔可以通过setsniffafterfiluredelaymillis方法在嗅探器创建时定制。请注意，最后一个配置参数在未启用故障嗅探时不起作
    * 用，如上文所述。
    * 
    **/
    .setSniffAfterFailureDelayMillis(30000) 
    .build();
sniffOnFailureListener.setSniffer(sniffer); //设置嗅探实例的失败监听器
```
Elasticsearch Nodes Info api不会返回连接到节点时要使用的协议，而只返回它们的host：port键值对，因此默认使用http。 如果应该使用https，则必须
手动创建ElasticsearchNodesSniffer实例并按如下方式提供：
```java
RestClient restClient = RestClient.builder(
        new HttpHost("localhost", 9200, "http"))
        .build();
NodesSniffer nodesSniffer = new ElasticsearchNodesSniffer(
        restClient,
        ElasticsearchNodesSniffer.DEFAULT_SNIFF_REQUEST_TIMEOUT,
        ElasticsearchNodesSniffer.Scheme.HTTPS);
Sniffer sniffer = Sniffer.builder(restClient)
        .setNodesSniffer(nodesSniffer).build();
```

同样，还可以自定义snifkRequestTimeout，默认为1秒。这是在调用nodes info api时作为querystring参数提供的超时参数，因此当服务器端的超时到期时，
仍然会返回有效的响应，尽管它可能只包含属于集群的节点的一个子集，但直到那时（超时时）才响应。

```java
RestClient restClient = RestClient.builder(
    new HttpHost("localhost", 9200, "http"))
    .build();
NodesSniffer nodesSniffer = new ElasticsearchNodesSniffer(
    restClient,
    TimeUnit.SECONDS.toMillis(5),
    ElasticsearchNodesSniffer.Scheme.HTTP);
Sniffer sniffer = Sniffer.builder(restClient)
    .setNodesSniffer(nodesSniffer).build();
```

此外，可以为高级用例提供自定义NodesSniffer实现，这些用例可能需要从外部源而不是从Elasticsearch获取`Node`s：

```java
RestClient restClient = RestClient.builder(
    new HttpHost("localhost", 9200, "http"))
    .build();
NodesSniffer nodesSniffer = new NodesSniffer() {
        @Override
        public List<Node> sniff() throws IOException {
            return null; //从外部源获取主机
        }
    };
Sniffer sniffer = Sniffer.builder(restClient)
    .setNodesSniffer(nodesSniffer).build();
```



## 3. Java High Level REST Client

### 1. Getting started

Java高级REST客户端在Java低级REST客户端之上工作。 它的主要目标是公开API特定的方法，接受请求对象作为参数并返回响应对象，以便客户端本身处理请求编组
和响应非编组。

可以同步或异步调用每个API。 同步方法返回响应对象，而名称以async后缀结尾的异步方法需要在收到响应或错误后通知（在由低级客户端管理的线程池上）的侦听器
参数。

Java高级REST客户端依赖于Elasticsearch核心项目。 它接受与TransportClient相同的请求参数，并返回相同的响应对象。

1.1. 兼容性

Java高级REST客户端需要Java 1.8并依赖于Elasticsearch核心项目。客户端版本与客户端开发的Elasticsearch版本相同。它接受与TransportClient相同
的请求参数，并返回相同的响应对象。如果需要将应用程序从TransportClient迁移到新的REST客户端，请参阅“迁移指南”。

高级客户端保证能够与运行在相同主要版本和更大或相同次要版本上的任何Elasticsearch节点进行通信。它不需要与它与之通信的Elasticsearch节点处于相同的次
要版本，因为它是向前兼容的，这意味着它支持与Elasticsearch的更高版本进行通信，而不是与其开发的版本进行通信。

6.0客户端能够与任何6.x Elasticsearch节点通信，而6.1客户端肯定能够与6.1,6.2和任何更高版本的6.x版本进行通信，但在与先前的Elasticsearch节点通
信时可能存在不兼容问题版本，例如在6.1和6.0之间，以防6.1客户端支持6.0节点不知道的某些API的新请求主体字段。

建议在将Elasticsearch集群升级到新的主要版本时升级高级客户端，因为REST API中断更改可能会导致意外结果，具体取决于请求所触及的节点，并且新添加的API
仅受到更新版本的客户端。一旦集群中的所有节点都升级到新的主要版本，客户端应始终最后更新。

1.2. Javadoc
你可以在此[链接](https://artifacts.elastic.co/javadoc/org/elasticsearch/client/elasticsearch-rest-high-level-client/7.2.0/index.html)找到REST high level client的javadoc


1.3. Maven Repository

high-level Java REST client要求java的版本至少是1.8。高级REST客户端与Elasticsearch具有相同的发布周期。 将版本替换为所需的客户端版本。

Maven依赖

```xml
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
    <version>7.2.0</version>
</dependency>
```
Gradle依赖

```groovy
dependencies {
    compile 'org.elasticsearch.client:elasticsearch-rest-high-level-client:7.2.0'
}
```

Lucene Snapshot repository

任何主要版本（如测试版）的最新版本可能都是基于Lucene Snapshot版本构建的。 在这种情况下，您将无法解析客户端的Lucene依赖关系。

例如，如果要使用依赖于Lucene 8.0.0-snapshot-83f9835的7.0.0-beta1版本，则必须定义以下存储库。

对于Maven

```xml
<repository>
    <id>elastic-lucene-snapshots</id>
    <name>Elastic Lucene Snapshots</name>
    <url>https://s3.amazonaws.com/download.elasticsearch.org/lucenesnapshots/83f9835</url>
    <releases><enabled>true</enabled></releases>
    <snapshots><enabled>false</enabled></snapshots>
</repository>
```

对于Gradle

```groovy
maven {
    name 'lucene-snapshots'
    url 'https://s3.amazonaws.com/download.elasticsearch.org/lucenesnapshots/83f9835'
}
```

1.4. 依赖
高级Java REST客户端依赖于以下工件及其传递依赖性：

* org.elasticsearch.client:elasticsearch-rest-client
* org.elasticsearch:elasticsearch

1.5. 初始化

RestHighLevelClient实例需要构建REST低级客户端构建器，如下所示：

```java
RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(
                new HttpHost("localhost", 9200, "http"),
                new HttpHost("localhost", 9201, "http")));
```

高级客户端将在内部创建用于根据提供的构建器执行请求的低级客户端。 该低级客户端维护一个连接池并启动一些线程，因此当您完好无损地关闭高级客户端时，它将关
闭内部低级客户端以释放这些资源。 这可以通过关闭来完成：

```java
client.close();
```

在关于Java高级客户端的本文档的其余部分中，RestHighLevelClient实例将被引用为客户端。

1.6. RequestOptions
RestHighLevelClient中的所有API都接受RequestOptions，您可以使用这些RequestOptions以不会改变Elasticsearch执行请求的方式自定义请求。 例如，
您可以在此处指定NodeSelector来控制哪个节点接收请求。 有关自定义选项的更多示例，请参阅低级客户端文档。


### 2. Document API

2.1. Index API

1. Index Request
IndexRequest需要以下参数：

```java
IndexRequest request = new IndexRequest("posts"); //索引
request.id("1"); //请求的文档id
String jsonString = "{" +
        "\"user\":\"kimchy\"," +
        "\"postDate\":\"2013-01-30\"," +
        "\"message\":\"trying out Elasticsearch\"" +
        "}";
request.source(jsonString, XContentType.JSON); //以string的形式提供文档源
```
2. 提供文档源

文档源可以以不同的方式提供（除了上述的string示例）

文档源作为Map提供，自动转换为JSON格式

```java
Map<String, Object> jsonMap = new HashMap<>();
jsonMap.put("user", "kimchy");
jsonMap.put("postDate", new Date());
jsonMap.put("message", "trying out Elasticsearch");
IndexRequest indexRequest = new IndexRequest("posts")
    .id("1").source(jsonMap); //
```

文档源作为XContentBuilder对象提供，Elasticsearch内置助手生成JSON内容

```java
XContentBuilder builder = XContentFactory.jsonBuilder();
builder.startObject();
{
    builder.field("user", "kimchy");
    builder.timeField("postDate", new Date());
    builder.field("message", "trying out Elasticsearch");
}
builder.endObject();
IndexRequest indexRequest = new IndexRequest("posts")
    .id("1").source(builder);  //
```

文档源作为Object键对提供，转换为JSON格式

```java
IndexRequest indexRequest = new IndexRequest("posts")
    .id("1")
    .source("user", "kimchy",
        "postDate", new Date(),
        "message", "trying out Elasticsearch");//
```


3. 可选参数

可以选择提供以下参数：

Routing value(路由值)

```java
request.routing("routing");
```

等待主分片变为可用的超时时间TimeValue； 等待主分片变为可用的超时时间String。

```java
request.timeout(TimeValue.timeValueSeconds(1)); 
request.timeout("1s");
```

将策略刷新为WriteRequest.RefreshPolicy实例; 将策略刷新为String.

```java
request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL); 
request.setRefreshPolicy("wait_for"); 
```

版本

```java
request.version(2);
```

版本类型

```java
request.versionType(VersionType.EXTERNAL);
```
提供DocWriteRequest.OpType操作类型； 提供String类的操作类型（默认可以是create或是update）

```java
request.opType(DocWriteRequest.OpType.CREATE); 
request.opType("create");
```

索引文档之前要执行的摄取管道的名称

```java
request.setPipeline("pipeline");
```

4. 同步执行

以下列方式执行IndexRequest时，客户端在继续执行代码之前等待返回IndexResponse：

```java
IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
```
如果无法解析高级REST客户端中的REST响应，请求超时或类似情况没有从服务器返回响应，则同步调用可能会抛出IOException。

如果服务器返回4xx或5xx错误代码，则高级客户端会尝试解析响应正文错误详细信息，然后抛出通用ElasticsearchException并将原始ResponseException作为
抑制异常添加到其中。

5. 异步执行

执行IndexRequest也可以以异步方式完成，以便客户端可以直接返回。 用户需要通过将请求和侦听器传递给异步索引方法来指定响应或潜在故障的处理方式：

```java
//要执行的IndexRequest和执行完成时要使用的ActionListener
client.indexAsync(request, RequestOptions.DEFAULT, listener);
```

异步方法不会阻塞并立即返回。 一旦完成，如果执行成功完成，则使用onResponse方法回调ActionListener，如果失败则使用onFailure方法。 故障情形和预期
异常与同步执行情况相同。

如下是一个典型的index的listener：

```java
listener = new ActionListener<IndexResponse>() {
    @Override
    public void onResponse(IndexResponse indexResponse) {
        //调用成功执行
    }

    @Override
    public void onFailure(Exception e) {
        //调用失败执行
    }
};
```

6. Index Response

返回的IndexResponse允许检索有关已执行操作的信息，如下所示：

```java
String index = indexResponse.getIndex();
String id = indexResponse.getId();
if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
    //处理（如果需要）第一次创建文档的情况

} else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
    //处理（如果需要）文档被重写的情况，因为它已经存在
}
ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
    //处理成功分片数小于总分片数的情况
}
if (shardInfo.getFailed() > 0) {
    for (ReplicationResponse.ShardInfo.Failure failure :
            shardInfo.getFailures()) {
        //处理潜在的失败
        String reason = failure.reason(); 
    }
}
```

如果存在版本冲突，将抛出ElasticsearchException：

```java
IndexRequest request = new IndexRequest("posts")
    .id("1")
    .source("field", "value")
    .setIfSeqNo(10L)
    .setIfPrimaryTerm(20);
try {
    IndexResponse response = client.index(request, RequestOptions.DEFAULT);
} catch(ElasticsearchException e) {
    if (e.status() == RestStatus.CONFLICT) {
        //引发的异常表示返回了版本冲突错误
    }
}
```

如果opType设置为create并且已存在具有相同索引和id的文档，则会发生相同的情况：


```java
IndexRequest request = new IndexRequest("posts")
    .id("1")
    .source("field", "value")
    .opType(DocWriteRequest.OpType.CREATE);
try {
    IndexResponse response = client.index(request, RequestOptions.DEFAULT);
} catch(ElasticsearchException e) {
    if (e.status() == RestStatus.CONFLICT) {
        //引发的异常表示返回了版本冲突错误
    }
}
```


2.2. Get API

1. Get Request

GetRequest需要如下参数：

```java
GetRequest getRequest = new GetRequest(
        "posts", //索引
        "1");   //文档id
```

2. 可选参数

提供一下的可选参数：

```java
//禁用源检索，默认情况下启用
request.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE);

String[] includes = new String[]{"message", "*Date"};
String[] excludes = Strings.EMPTY_ARRAY;
FetchSourceContext fetchSourceContext =
        new FetchSourceContext(true, includes, excludes);
//配置源包含的特定字段
request.fetchSourceContext(fetchSourceContext); 


String[] includes = Strings.EMPTY_ARRAY;
String[] excludes = new String[]{"message"};
FetchSourceContext fetchSourceContext =
        new FetchSourceContext(true, includes, excludes);
//配置源排除的特定字段
request.fetchSourceContext(fetchSourceContext); 


//配置特定存储字段的检索（要求字段分别存储在映射中）
request.storedFields("message"); 
GetResponse getResponse = client.get(request, RequestOptions.DEFAULT);
//检索消息存储字段（要求字段分别存储在映射中）
String message = getResponse.getField("message").getValue(); 

//路由值
request.routing("routing");
//偏好值
request.preference("preference");
//设置实时标志位false（默认为true）
request.realtime(false);
//在检索文档之前执行刷新（默认为false）
request.refresh(true);
//版本
request.version(2);
//版本类型
request.versionType(VersionType.EXTERNAL); 

//
```

3. 同步执行

以下列方式执行GetRequest时，客户端在继续执行代码之前等待返回GetResponse：

```java
GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
```

如果无法解析高级REST客户端中的REST响应，请求超时或类似情况没有从服务器返回响应，则同步调用可能会抛出IOException。

如果服务器返回4xx或5xx错误代码，则高级客户端会尝试解析响应正文错误详细信息，然后抛出通用ElasticsearchException并将原始ResponseException作为
抑制异常添加到其中。


4. 异步执行

执行GetRequest也可以以异步方式完成，以便客户端可以直接返回。 用户需要通过将请求和侦听器传递给异步get方法来指定响应或潜在故障的处理方式：

```java
//要执行的GetRequest和执行完成时要使用的ActionListener
client.getAsync(request, RequestOptions.DEFAULT, listener);
```

异步方法不会阻塞并立即返回。 一旦完成，如果执行成功完成，则使用onResponse方法回调ActionListener，如果失败则使用onFailure方法。 故障情形和预期异常与同步执行情况相同。

get的典型监听器如下：

```java
ActionListener<GetResponse> listener = new ActionListener<GetResponse>() {
    @Override
    public void onResponse(GetResponse getResponse) {
        //Called when the execution is successfully completed.
    }

    @Override
    public void onFailure(Exception e) {
        //Called when the whole GetRequest fails.
    }
};
```

5. Get Response

返回的GetResponse允许检索所请求的文档及其元数据和最终存储的字段。

```java
String index = getResponse.getIndex();
String id = getResponse.getId();
if (getResponse.isExists()) {
    long version = getResponse.getVersion();
    //Retrieve the document as a String
    String sourceAsString = getResponse.getSourceAsString();   
    //Retrieve the document as a Map<String, Object>
    Map<String, Object> sourceAsMap = getResponse.getSourceAsMap(); 
    //Retrieve the document as a byte[]
    byte[] sourceAsBytes = getResponse.getSourceAsBytes();          
} else {
    /*
    * 处理未找到文档的方案。 请注意，虽然返回的响应具有404状态代码，但返回有效的GetResponse而不是抛出异常。 
    * 此类响应不包含任何源文档，并且其isExists方法返回false。
    * */
}
```

当针对不存在的索引执行get请求时，响应具有404状态代码，抛出ElasticsearchException，需要按如下方式处理：

```java
GetRequest request = new GetRequest("does_not_exist", "1");
try {
    GetResponse getResponse = client.get(request, RequestOptions.DEFAULT);
} catch (ElasticsearchException e) {
    if (e.status() == RestStatus.NOT_FOUND) {
        //Handle the exception thrown because the index does not exist
    }
}
```

如果已请求特定文档版本，并且现有文档具有不同的版本号，则会引发版本冲突：

```java
try {
    GetRequest request = new GetRequest("posts", "1").version(2);
    GetResponse getResponse = client.get(request, RequestOptions.DEFAULT);
} catch (ElasticsearchException exception) {
    if (exception.status() == RestStatus.CONFLICT) {
        //引发的异常表示返回了版本冲突错误
    }
}
```

2.3. Exists API

如果文档存在，则exists API返回true，否则返回false。

1. Exists Request

它就像Get API一样使用GetRequest。 支持所有可选参数。 由于exists（）只返回true或false，我们建议关闭获取_source和任何存储的字段，以便请求稍微
轻一点：

```java
GetRequest getRequest = new GetRequest(
    "posts", //索引
    "1");    //文档id
getRequest.fetchSourceContext(new FetchSourceContext(false)); //禁止获取_source
getRequest.storedFields("_none_"); //禁止获取存储字段
```

2. 同步执行

以下列方式执行GetRequest时，客户端在继续执行代码之前等待返回布尔值：

```java
boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
```
如果无法解析高级REST客户端中的REST响应，请求超时或类似情况没有从服务器返回响应，则同步调用可能会抛出IOException。

如果服务器返回4xx或5xx错误代码，则高级客户端会尝试解析响应正文错误详细信息，然后抛出通用ElasticsearchException并将原始ResponseException作为
抑制异常添加到其中。


3. 异步执行

执行GetRequest也可以以异步方式完成，以便客户端可以直接返回。 用户需要通过将请求和侦听器传递给异步exists方法来指定响应或潜在故障的处理方式：

```java
client.existsAsync(getRequest, RequestOptions.DEFAULT, listener);
```

异步方法不会阻塞并立即返回。 一旦完成，如果执行成功完成，则使用onResponse方法回调ActionListener，如果失败则使用onFailure方法。 故障情形和预期
异常与同步执行情况相同。

典型的监听器如下所示：

```java
ActionListener<Boolean> listener = new ActionListener<Boolean>() {
    @Override
    public void onResponse(Boolean exists) {
        
    }

    @Override
    public void onFailure(Exception e) {
        
    }
};
```

4. 源存在请求

存在请求的变体是existsSource方法，该方法附加检查所讨论的文档是否存储了源。 如果索引的映射选择删除对在文档中存储JSON源的支持，则此方法将为此索引中
的文档返回false。


2.4. Delete API

1. Delete Request

DeleteRequest有两个必须的参数：

```java
DeleteRequest request = new DeleteRequest(
        "posts",    
        "1"); 
```

2. 可选参数

提供以下可选参数：

```java
//路由值
request.routing("routing");

//等待主分片变为可用的超时时间，两种方式：
request.timeout(TimeValue.timeValueMinutes(2)); 
request.timeout("2m");

//设置刷新策略，两种方式
request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL); 
request.setRefreshPolicy("wait_for"); 
//版本
request.version(2);
//版本类型
request.versionType(VersionType.EXTERNAL);
```

3. 同步执行

以下列方式执行DeleteRequest时，客户端在继续执行代码之前等待返回DeleteResponse：

```java
DeleteResponse deleteResponse = client.delete(
        request, RequestOptions.DEFAULT);
```

如果无法解析高级REST客户端中的REST响应，请求超时或类似情况没有从服务器返回响应，则同步调用可能会抛出IOException。

如果服务器返回4xx或5xx错误代码，则高级客户端会尝试解析响应正文错误详细信息，然后抛出通用ElasticsearchException并将原始ResponseException作为
抑制异常添加到其中。

4. 异步执行

执行DeleteRequest也可以以异步方式完成，以便客户端可以直接返回。 用户需要通过将请求和侦听器传递给异步删除方法来指定响应或潜在故障的处理方式：

```java
client.deleteAsync(request, RequestOptions.DEFAULT, listener);
```

异步方法不会阻塞并立即返回。 一旦完成，如果执行成功完成，则使用onResponse方法回调ActionListener，如果失败则使用onFailure方法。 故障情形和预期
异常与同步执行情况相同。

删除的典型监听器如下所示：

```java
listener = new ActionListener<DeleteResponse>() {
    @Override
    public void onResponse(DeleteResponse deleteResponse) {
        
    }

    @Override
    public void onFailure(Exception e) {
        
    }
};
```
5. Delete Response

返回的DeleteResponse允许检索有关已执行操作的信息，如下所示：

```java

String index = deleteResponse.getIndex();
String id = deleteResponse.getId();
long version = deleteResponse.getVersion();
ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
    //处理成功分片数小于总分片数的情况
}
if (shardInfo.getFailed() > 0) {
    for (ReplicationResponse.ShardInfo.Failure failure :
            shardInfo.getFailures()) {
        //处理潜在的失败
        String reason = failure.reason(); 
    }
}
```

还可以检查文档是否被找到：

```java
DeleteRequest request = new DeleteRequest("posts", "does_not_exist");
DeleteResponse deleteResponse = client.delete(
        request, RequestOptions.DEFAULT);
if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
    //Do something if the document to be deleted was not found
}
```

如果存在版本冲突，将抛出ElasticsearchException：

```java
try {
    DeleteResponse deleteResponse = client.delete(
        new DeleteRequest("posts", "1").setIfSeqNo(100).setIfPrimaryTerm(2),
            RequestOptions.DEFAULT);
} catch (ElasticsearchException exception) {
    if (exception.status() == RestStatus.CONFLICT) {
        //The raised exception indicates that a version conflict error was returned
    }
}
```

2.5. Update API

1. Update Request

UpdateRequest必填参数如下所示：

```java
UpdateRequest request = new UpdateRequest(
        "posts", 
        "1");   
```
Update API允许使用脚本或传递部分文档来更新现有文档。

2. 使用脚本更新

该脚本可以作为内联脚本提供：

```java

//Script parameters provided as a Map of objects
Map<String, Object> parameters = singletonMap("count", 4); 

//Create an inline script using the painless language and the previous parameters
Script inline = new Script(ScriptType.INLINE, "painless",
        "ctx._source.field += params.count", parameters); 
//Sets the script to the update request
request.script(inline);  
```
或者作为存储脚本：

```java
//Reference to a script stored under the name increment-field in the painless language
Script stored = new Script(
        ScriptType.STORED, null, "increment-field", parameters);  
//Sets the script in the update request
request.script(stored);
```

3. 更新部分文档

对部分文档更新时使用，部分文档将与现有文档合并。

部分文档可以以不同方式提供：

```java
UpdateRequest request = new UpdateRequest("posts", "1");
String jsonString = "{" +
        "\"updated\":\"2017-01-01\"," +
        "\"reason\":\"daily update\"" +
        "}";


//Partial document source provided as a String in JSON format
request.doc(jsonString, XContentType.JSON);
```

部分文档源作为Map提供，可自动转换为JSON格式:

```java
XContentBuilder builder = XContentFactory.jsonBuilder();
builder.startObject();
{
    builder.timeField("updated", new Date());
    builder.field("reason", "daily update");
}
builder.endObject();
UpdateRequest request = new UpdateRequest("posts", "1")
        .doc(builder);
```
部分文档源作为Object键对提供，转换为JSON格式:

```java
UpdateRequest request = new UpdateRequest("posts", "1")
        .doc("updated", new Date(),
             "reason", "daily update");
```

4. Upserts

如果文档尚不存在，则可以使用upsert方法将某些内容定义为新文档：

```java
String jsonString = "{\"created\":\"2017-01-01\"}";
//Upsert document source provided as a String
request.upsert(jsonString, XContentType.JSON);
```
与部分文档更新类似，可以使用接受String，Map，XContentBuilder或Object键对的方法来定义upsert文档的内容。

5. 可选参数

提供以下可选参数：

```java
request.routing("routing"); 
request.timeout(TimeValue.timeValueSeconds(1)); 
request.timeout("1s");
request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL); 
request.setRefreshPolicy("wait_for"); 
//如果更新操作的get和indexing阶段之间的另一个操作更改了要更新的文档，则重试更新操作的次数
request.retryOnConflict(3); 
//启用源检索，默认情况下禁用
request.fetchSource(true);

String[] includes = new String[]{"updated", "r*"};
String[] excludes = Strings.EMPTY_ARRAY;\
//Configure source inclusion for specific fields
request.fetchSource(
        new FetchSourceContext(true, includes, excludes));

String[] includes = Strings.EMPTY_ARRAY;
String[] excludes = new String[]{"updated"};
//Configure source exclusion for specific fields
request.fetchSource(
        new FetchSourceContext(true, includes, excludes));
//ifSeqNo
request.setIfSeqNo(2L); 
//ifPrimaryTerm
request.setIfPrimaryTerm(1L);
//禁用noop检测
request.detectNoop(false);
//指示无论文档是否存在，脚本都必须运行，即如果文档尚不存在，脚本将负责创建文档。
request.scriptedUpsert(true);
//如果尚未存在，则表明必须将部分文档用作upsert文档。
request.docAsUpsert(true);
//设置在继续更新操作之前必须处于活动状态的分片副本数。
request.waitForActiveShards(2); 
//作为ActiveShardCount提供的分片副本数：可以是ActiveShardCount.ALL，ActiveShardCount.ONE或ActiveShardCount.DEFAULT（默认）
request.waitForActiveShards(ActiveShardCount.ALL);
```

6. 同步执行

以下列方式执行UpdateRequest时，客户端在继续执行代码之前等待返回UpdateResponse：

```java
UpdateResponse updateResponse = client.update(
        request, RequestOptions.DEFAULT);
```
如果无法解析高级REST客户端中的REST响应，请求超时或类似情况没有从服务器返回响应，则同步调用可能会抛出IOException。

如果服务器返回4xx或5xx错误代码，则高级客户端会尝试解析响应正文错误详细信息，然后抛出通用ElasticsearchException并将原始ResponseException作为
抑制异常添加到其中。

7. 异步执行

执行UpdateRequest也可以以异步方式完成，以便客户端可以直接返回。 用户需要通过将请求和侦听器传递给异步更新方法来指定响应或潜在故障的处理方式：

```java
//The UpdateRequest to execute and the ActionListener to use when the execution completes
client.updateAsync(request, RequestOptions.DEFAULT, listener);
```
异步方法不会阻塞并立即返回。 一旦完成，如果执行成功完成，则使用onResponse方法回调ActionListener，如果失败则使用onFailure方法。 故障情形和预期
异常与同步执行情况相同。

典型的更新监听器如下所示：

```java
listener = new ActionListener<UpdateResponse>() {
    @Override
    public void onResponse(UpdateResponse updateResponse) {
        
    }

    @Override
    public void onFailure(Exception e) {
        
    }
};
```

5. Update Response

返回的UpdateResponse允许检索有关已执行操作的信息，如下所示：

```java
String index = updateResponse.getIndex();
String id = updateResponse.getId();
long version = updateResponse.getVersion();
if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {
    //处理首次创建文档的情况（upsert）
} else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {
    //处理文档更新的情况
} else if (updateResponse.getResult() == DocWriteResponse.Result.DELETED) {
    //处理删除文档的情况
} else if (updateResponse.getResult() == DocWriteResponse.Result.NOOP) {
    //处理文档未受更新影响的情况，即未对文档执行任何操作（noop）
}
```
通过fetchSource方法在UpdateRequest中启用源检索时，响应包含更新文档的来源：

```java
//以GetResult的形式检索更新的文档
GetResult result = updateResponse.getGetResult(); 
if (result.isExists()) {
    //以String形式检索更新文档的源
    String sourceAsString = result.sourceAsString(); 
    //以Map <String，Object>的形式检索更新文档的源
    Map<String, Object> sourceAsMap = result.sourceAsMap(); 
    //以byte []的形式检索更新文档的来源
    byte[] sourceAsBytes = result.source(); 
} else {
    //处理响应中不存在文档源的场景（默认情况下是这种情况）
}
```

还可以检查分片失败：

```java
ReplicationResponse.ShardInfo shardInfo = updateResponse.getShardInfo();
if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
    //处理成功分片数小于总分片数的情况
}
if (shardInfo.getFailed() > 0) {
    for (ReplicationResponse.ShardInfo.Failure failure :
            shardInfo.getFailures()) {
        //处理潜在的失败
        String reason = failure.reason(); 
    }
}
```

对不存在的文档执行UpdateRequest时，响应有404状态码，抛出ElasticsearchException，需要按如下方式处理：

```java
UpdateRequest request = new UpdateRequest("posts", "does_not_exist")
        .doc("field", "value");
try {
    UpdateResponse updateResponse = client.update(
            request, RequestOptions.DEFAULT);
} catch (ElasticsearchException e) {
    if (e.status() == RestStatus.NOT_FOUND) {
       //Handle the exception thrown because the document not exist 
    }
}
```

如果存在版本冲突，将抛出ElasticsearchException：

```java
UpdateRequest request = new UpdateRequest("posts", "1")
        .doc("field", "value")
        .setIfSeqNo(101L)
        .setIfPrimaryTerm(200L);
try {
    UpdateResponse updateResponse = client.update(
            request, RequestOptions.DEFAULT);
} catch(ElasticsearchException e) {
    if (e.status() == RestStatus.CONFLICT) {
        //The raised exception indicates that a version conflict error was returned.
    }
}
```
2.6. Term Vectors API

Term Vectors API返回特定文档字段中Term的信息和统计信息。 该文档可以存储在索引中或由用户人工提供。

1. Term Vectors Request

termVectorRequest需要一个索引、类型和ID来指定一个特定的文档，以及为其检索信息的字段。

```java
TermVectorsRequest request = new TermVectorsRequest("authors", "1");
request.setFields("user");
```
2.7. Bulk API

2.8. Multi-Get API

2.9. Reindex API

2.10. Update By Query API

2.11. Delete By Query API

2.12. Rethrottle API

2.13. Multi Term Vectors API


### 3. Search APIs

3.1. Search API

1. Search Request

SearchRequest用于与搜索文档，聚合，建议有关的任何操作，还提供了在结果文档上请求高亮显示的方法。

在最基本的形式中，我们可以向请求添加查询：

```java
//创建SeachRequest。 没有参数，这将针对所有索引运行。
SearchRequest searchRequest = new SearchRequest(); 
//大多数搜索参数都添加到SearchSourceBuilder中。 它为搜索请求正文中的所有内容提供了setter。
SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); 
//将match_all查询添加到SearchSourceBuilder。
searchSourceBuilder.query(QueryBuilders.matchAllQuery()); 
//将SearchSourceBuilder添加到SeachRequest。
searchRequest.source(searchSourceBuilder);
```

1.1. 可选参数

首先来看下SearchRequest的可选参数：

```java
//指定请求的索引
SearchRequest searchRequest = new SearchRequest("posts");
```

还有一些其他有趣的参数：

```java
//设置路由参数
searchRequest.routing("routing");
//设置IndicesOptions控制如何解析不可用的索引以及如何扩展通配符表达式
searchRequest.indicesOptions(IndicesOptions.lenientExpandOpen());
//首选参数，例如首先在本地分片执行搜索，默认是随机跨分片。
searchRequest.preference("_local");
```

1.2. 使用SearchSourceBuilder

控制搜索行为的大多数选项都可以在SearchSourceBuilder上设置，SearchSourceBuilder包含与Rest API的搜索请求主体中的选项相当或更少的选项。

以下是一些常见选项的几个示例：

```java
//创建默认的SearchSourceBuilder
SearchSourceBuilder sourceBuilder = new SearchSourceBuilder(); 
//设置Query，可是是任意类型的QueryBuilder
sourceBuilder.query(QueryBuilders.termQuery("user", "kimchy")); 
//设置确定结果索引的from选项以开始搜索。 默认为0。
sourceBuilder.from(0); 
//设置size选项，确定要返回的搜索命中数。 默认为10。
sourceBuilder.size(5); 
//设置一个可选的超时，控制允许搜索的时间。
sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS)); 
```

在此之后，只需将SearchSourceBuilder添加到SearchRequest：

```java
SearchRequest searchRequest = new SearchRequest();
searchRequest.indices("posts");
searchRequest.source(sourceBuilder);
```

1.3. 构建查询

使用QueryBuilder对象创建搜索查询。 对于Elasticsearch的Query DSL支持的每种搜索查询类型，都存在QueryBuilder。

可以使用其构造函数创建QueryBuilder：

```java
//创建一个user字段上匹配kimchy的全文匹配检索
MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("user", "kimchy");
```

创建后， QueryBuilder对象提供方法去配置它创建的搜索查询的选项：

```java
//Enable fuzzy matching on the match query
matchQueryBuilder.fuzziness(Fuzziness.AUTO); 
//在匹配查询上设置前缀长度选项
matchQueryBuilder.prefixLength(3); 
//设置最大扩展选项以控制查询的模糊过程
matchQueryBuilder.maxExpansions(10); 
```

也可以使用QueryBuilders实用程序类创建QueryBuilder对象。 此类提供了可用于使用流畅的编程样式创建QueryBuilder对象的辅助方法：

```java
QueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("user", "kimchy")
                                                .fuzziness(Fuzziness.AUTO)
                                                .prefixLength(3)
                                                .maxExpansions(10);
```
无论使用什么方式来创建它，QueryBuilder对象必须被添加到SearchSourceBuilder，如下所示：

```java
searchSourceBuilder.query(matchQueryBuilder);
```

[Building Queries页面](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.2/java-rest-high-query-builders.html)提供了所有可用搜索查询的列表及其相应的QueryBuilder对象和QueryBuilders辅助方法。


1.4. 指定排序

SearchSourceBuilder 允许添加一个或多个SortBuilder实例。有四个可以指定的实现（Field-， Score-， GeoDistance- and ScriptSortBuilder）。

```java
//按_score降序排序（默认值）
sourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC)); 
//还可以按_id字段进行升序排序
sourceBuilder.sort(new FieldSortBuilder("_id").order(SortOrder.ASC));
```

1.5. 源过滤

默认情况下，搜索请求会返回文档_source的内容，但在Rest API中，您可以覆盖此行为。 例如，您可以完全关闭_source检索：

```java
sourceBuilder.fetchSource(false);
```

该方法还接受一个或多个通配符模式的数组，以更精细的方式控制包含或排除哪些字段：

```java
String[] includeFields = new String[] {"title", "innerObject.*"};
String[] excludeFields = new String[] {"user"};
sourceBuilder.fetchSource(includeFields, excludeFields);
```
1.6. 请求高亮

通过在SearchSourceBuilder上设置HighlightBuilder，可以突出显示搜索结果。 通过将一个或多个HighlightBuilder.Field实例添加到HighlightBuilder，
可以为每个字段定义不同的突出显示行为。

```java

SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
HighlightBuilder highlightBuilder = new HighlightBuilder(); 
HighlightBuilder.Field highlightTitle =
        new HighlightBuilder.Field("title"); 
//设置字段高亮类型
highlightTitle.highlighterType("unified");  
highlightBuilder.field(highlightTitle);  
HighlightBuilder.Field highlightUser = new HighlightBuilder.Field("user");
highlightBuilder.field(highlightUser);
searchSourceBuilder.highlighter(highlightBuilder);
```
Rest API文档中有许多选项需要详细说明。 Rest API参数（例如pre_tags）通常由具有相似名称的setter（例如#preTags（String ...））更改。

稍后可以从SearchResponse中检索突出显示的文本片段。


1.7. 请求聚合

首先通过创建适当的AggregationBuilder，然后在SearchSourceBuilder上设置聚合，可以将聚合添加到搜索中。 在以下示例中，我们使用公司员工平均年龄的
子聚合创建公司名称的术语聚合：

```java
Aggregations aggregations = searchResponse.getAggregations();
//Get the by_company terms aggregation
Terms byCompanyAggregation = aggregations.get("by_company"); 
//Get the buckets that is keyed with Elastic
Bucket elasticBucket = byCompanyAggregation.getBucketByKey("Elastic"); 
//Get the average_age sub-aggregation from that bucket
Avg averageAge = elasticBucket.getAggregations().get("average_age"); 
double avg = averageAge.getValue();
```
[Building Aggregations](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.2/java-rest-high-aggregation-builders.html)页提供所有可用聚合的列表及其相应的AggregationBuilder对象和AggregationBuilder帮助器方法。

稍后我们将了解如何[访问SearchResponse中的聚合](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/7.2/java-rest-high-search.html#java-rest-high-search-response-aggs)。

1.8. 请求建议

要向搜索请求添加建议，请使用可从SuggestBuilders工厂类轻松访问的SuggestionBuilder实现之一。 需要将建议构建器添加到顶级SuggestBuilder，它本身
可以在SearchSourceBuilder上设置。

```java
SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//Creates a new TermSuggestionBuilder for the user field and the text kmichy
SuggestionBuilder termSuggestionBuilder =
    SuggestBuilders.termSuggestion("user").text("kmichy"); 
SuggestBuilder suggestBuilder = new SuggestBuilder();
//Adds the suggestion builder and names it suggest_user
suggestBuilder.addSuggestion("suggest_user", termSuggestionBuilder); 
searchSourceBuilder.suggest(suggestBuilder);
```
稍后我们在SearchResponse可以看到检索建议


1.9. Profiling Queries and Aggregations

Profile API可用于分析特定搜索请求的查询和聚合的执行情况。 为了使用它，必须在SearchSourceBuilder上将profile标志设置为true：

```java
SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
searchSourceBuilder.profile(true);
```

执行SearchRequest后，相应的SearchResponse将包含分析结果。



2. 同步执行

以下列方式执行SearchRequest时，客户端在继续执行代码之前等待返回SearchResponse：

```java
SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

```

如果无法解析高级REST客户端中的REST响应，请求超时或类似情况没有从服务器返回响应，则同步调用可能会抛出IOException。

如果服务器返回4xx或5xx错误代码，则高级客户端会尝试解析响应正文错误详细信息，然后抛出通用ElasticsearchException并将原始ResponseException作为
抑制异常添加到其中。

3. 异步执行

执行SearchRequest也可以以异步方式完成，以便客户端可以直接返回。 用户需要通过将请求和监听器传递给异步搜索方法来指定响应或潜在故障的处理方式：

```java
client.searchAsync(searchRequest, RequestOptions.DEFAULT, listener);
```
异步方法不会阻塞并立即返回。 一旦完成，如果执行成功完成，则使用onResponse方法回调ActionListener，如果失败则使用onFailure方法。 故障情形和预期
异常与同步执行情况相同。

典型的搜索监听器如下所示：

```java
ActionListener<SearchResponse> listener = new ActionListener<SearchResponse>() {
    @Override
    public void onResponse(SearchResponse searchResponse) {
        
    }

    @Override
    public void onFailure(Exception e) {
        
    }
};

```
4. SearchResponse

通过执行搜索返回的SearchResponse提供有关搜索执行本身以及对返回文档的访问的详细信息。 首先，有关于请求执行本身的有用信息，例如HTTP状态代码，执行时
间或请求是提前终止还是超时：

```java
RestStatus status = searchResponse.status();
TimeValue took = searchResponse.getTook();
Boolean terminatedEarly = searchResponse.isTerminatedEarly();
boolean timedOut = searchResponse.isTimedOut();
```

其次，响应还通过提供有关搜索影响的分片总数以及成功与不成功分片的统计信息，提供有关分片级别执行的信息。 可以通过在ShardSearchFailures上迭代数组来处
理可能的失败，如下例所示：

```java
int totalShards = searchResponse.getTotalShards();
int successfulShards = searchResponse.getSuccessfulShards();
int failedShards = searchResponse.getFailedShards();
for (ShardSearchFailure failure : searchResponse.getShardFailures()) {
    // failures should be handled here
}

```



4.1. Retrieving SearchHits

要访问返回的文档，我们需要首先获取响应中包含的SearchHits：

```java
SearchHits hits = searchResponse.getHits();
```

SearchHits提供有关所有匹配的全局信息，例如总命中数或最高分数：

```java
TotalHits totalHits = hits.getTotalHits();
// the total number of hits, must be interpreted in the context of totalHits.relation
long numHits = totalHits.value;
// whether the number of hits is accurate (EQUAL_TO) or a lower bound of the total (GREATER_THAN_OR_EQUAL_TO)
TotalHits.Relation relation = totalHits.relation;
float maxScore = hits.getMaxScore();
```

嵌套在SearchHits中的是可以迭代的单个搜索结果：

```java
SearchHit[] searchHits = hits.getHits();
for (SearchHit hit : searchHits) {
    // do something with the SearchHit
}
```
SearchHit提供对索引，文档ID和每个搜索命中得分等基本信息的访问：
```java
String index = hit.getIndex();
String id = hit.getId();
float score = hit.getScore();
```
此外，它还允许您以简单的JSON-String或键/值对的映射形式返回文档源。 在此映射中，常规字段由字段名称键控并包含字段值。 多值字段作为对象列表返回，嵌套
对象作为另一个键/值映射返回。 这些案件需要相应地进行投射：

```java
String sourceAsString = hit.getSourceAsString();
Map<String, Object> sourceAsMap = hit.getSourceAsMap();
String documentTitle = (String) sourceAsMap.get("title");
List<Object> users = (List<Object>) sourceAsMap.get("user");
Map<String, Object> innerObject =
        (Map<String, Object>) sourceAsMap.get("innerObject");
```

4.2. Retrieving Highlighting

如果需要，可以从结果中的每个SearchHit检索突出显示的文本片段。 命中对象提供对HighlightField实例的字段名称映射的访问，每个实例包含一个或多个突出显
示的文本片段：

```java
SearchHits hits = searchResponse.getHits();
for (SearchHit hit : hits.getHits()) {
    Map<String, HighlightField> highlightFields = hit.getHighlightFields();
    //Get the highlighting for the title field
    HighlightField highlight = highlightFields.get("title"); 
    //Get one or many fragments containing the highlighted field content
    Text[] fragments = highlight.fragments();  
    String fragmentString = fragments[0].string();
}
```

4.3. Retrieving Aggregations

首先通过获取聚合树的根，Aggregations对象，然后按名称获取聚合，可以从SearchResponse检索聚合。

```java
Aggregations aggregations = searchResponse.getAggregations();
//Get the by_company terms aggregation
Terms byCompanyAggregation = aggregations.get("by_company"); 
//Get the buckets that is keyed with Elastic
Bucket elasticBucket = byCompanyAggregation.getBucketByKey("Elastic");
//Get the average_age sub-aggregation from that bucket
Avg averageAge = elasticBucket.getAggregations().get("average_age"); 
double avg = averageAge.getValue();
```
请注意，如果按名称访问聚合，则需要根据所请求的聚合类型指定聚合接口，否则将引发ClassCastException：

```java
//这将引发异常，因为“by_company”是一个术语聚合，但我们尝试将其作为范围聚合进行检索
Range range = aggregations.get("by_company"); 
```

还可以将所有聚合作为由聚合名称键入的映射进行访问。 在这种情况下，需要显式地进行正确聚合接口的转换：

```java
Map<String, Aggregation> aggregationMap = aggregations.getAsMap();
Terms companyAggregation = (Terms) aggregationMap.get("by_company");
```

还有一些getter将所有顶级聚合作为列表返回：

```java
List<Aggregation> aggregationList = aggregations.asList();
```
最后，但并非最不重要的是，您可以对所有聚合进行迭代，然后根据它们的类型决定如何进一步处理它们：

```java
for (Aggregation agg : aggregations) {
    String type = agg.getType();
    if (type.equals(TermsAggregationBuilder.NAME)) {
        Bucket elasticBucket = ((Terms) agg).getBucketByKey("Elastic");
        long numberOfDocs = elasticBucket.getDocCount();
    }
}
```

4.4. Retrieving Suggestions

要从SearchResponse获取建议，请使用Suggest对象作为入口点，然后检索嵌套的建议对象：

```java
//Use the Suggest class to access suggestions
Suggest suggest = searchResponse.getSuggest(); 
//Suggestions can be retrieved by name. You need to assign them to the correct type of Suggestion 
// class (here TermSuggestion), otherwise a ClassCastException is thrown
TermSuggestion termSuggestion = suggest.getSuggestion("suggest_user"); 
//Iterate over the suggestion entries
for (TermSuggestion.Entry entry : termSuggestion.getEntries()) { 
    //Iterate over the options in one entry
    for (TermSuggestion.Entry.Option option : entry) { 
        String suggestText = option.getText().string();
    }
}
```

4.5. 检索分析结果

使用getProfileResults（）方法从SearchResponse检索分析结果。 此方法返回包含SearchSquest执行中涉及的每个分片的ProfileShardResult对象的Map。 
ProfileShardResult使用一个键存储在Map中，该键唯一标识配置文件结果对应的分片。

下面是一个示例代码，显示如何迭代每个分片的所有分析结果：

```java
//Retrieve the Map of ProfileShardResult from the SearchResponse
Map<String, ProfileShardResult> profilingResults =
        searchResponse.getProfileResults(); 
/*
* Profiling results can be retrieved by shard’s key if the key is known, otherwise it might be simpler to iterate over 
* all the profiling results
*/
/
for (Map.Entry<String, ProfileShardResult> profilingResult : profilingResults.entrySet()) { 
    //Retrieve the key that identifies which shard the ProfileShardResult belongs to
    String key = profilingResult.getKey(); 
    //Retrieve the ProfileShardResult for the given shard
    ProfileShardResult profileShardResult = profilingResult.getValue(); 
}
```

ProfileShardResult对象本身包含一个或多个查询概要文件结果，每个结果对应于基础Lucene索引执行的每个查询：

```java
//Retrieve the list of QueryProfileShardResult
List<QueryProfileShardResult> queryProfileShardResults =
        profileShardResult.getQueryProfileResults(); 
//Iterate over each QueryProfileShardResult
for (QueryProfileShardResult queryProfileResult : queryProfileShardResults) { 

}
```

每个QueryProfileShardResult都提供对详细查询树执行的访问权限，作为ProfileResult对象列表返回：

```java
//Iterate over the profile results
for (ProfileResult profileResult : queryProfileResult.getQueryResults()) { 
    //Retrieve the name of the Lucene query
    String queryName = profileResult.getQueryName();
    //Retrieve the time in millis spent executing the Lucene query
    long queryTimeInMillis = profileResult.getTime(); 
    //Retrieve the profile results for the sub-queries (if any)
    List<ProfileResult> profiledChildren = profileResult.getProfiledChildren(); 
}
```
Rest API文档包含有关分析查询的更多信息以及查询分析信息的描述。

QueryProfileShardResult还可以访问Lucene收集器的分析信息：

```java
//Retrieve the profiling result of the Lucene collector
CollectorResult collectorResult = queryProfileResult.getCollectorResult(); 
//Retrieve the name of the Lucene collector
String collectorName = collectorResult.getName(); 
//Retrieve the time in millis spent executing the Lucene collector
Long collectorTimeInMillis = collectorResult.getTime(); 
//Retrieve the profile results for the sub-collectors (if any)
List<CollectorResult> profiledChildren = collectorResult.getProfiledChildren();
```

Rest API文档包含有关Lucene收集器的分析信息的更多信息。 请参阅分析查询。

以与查询树执行非常类似的方式，QueryProfileShardResult对象可以访问详细的聚合树执行：

```java
//Retrieve the AggregationProfileShardResult
AggregationProfileShardResult aggsProfileResults =
        profileShardResult.getAggregationProfileResults(); 
//Iterate over the aggregation profile results
for (ProfileResult profileResult : aggsProfileResults.getProfileResults()) { 
    //Retrieve the type of the aggregation (corresponds to Java class used to execute the aggregation)
    String aggName = profileResult.getQueryName(); 
    //Retrieve the time in millis spent executing the Lucene collector
    long aggTimeInMillis = profileResult.getTime(); 
    //Retrieve the profile results for the sub-aggregations (if any)
    List<ProfileResult> profiledChildren = profileResult.getProfiledChildren(); 
}
```
Rest API文档包含有关[分析聚合](https://www.elastic.co/guide/en/elasticsearch/reference/7.2/search-profile-aggregations.html)的更多信息。


3.2. Search Scroll API

Scroll API可用于从搜索请求中检索大量结果。

为了使用滚动，需要按给定顺序执行以下步骤。

1. 初始化搜索滚动contextedit

必须执行带有滚动参数的初始搜索请求，以通过Search API初始化滚动会话。 处理此SearchRequest时，Elasticsearch检测到滚动参数的存在，并使搜索上下文
保持活动状态达到相应的时间间隔。

```java
SearchRequest searchRequest = new SearchRequest("posts");
SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
searchSourceBuilder.query(matchQuery("title", "Elasticsearch"));
//创建SearchRequest及其相应的SearchSourceBuilder。 还可以选择设置大小以控制一次检索多少结果。
searchSourceBuilder.size(size); 
searchRequest.source(searchSourceBuilder);
//设置滚动间隔
searchRequest.scroll(TimeValue.timeValueMinutes(1L)); 
SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//读取返回的滚动ID，该ID指向保持活动的搜索上下文，并在以下搜索滚动调用中需要
String scrollId = searchResponse.getScrollId();
//检索第一批搜索命中
SearchHits hits = searchResponse.getHits();  
```
2.检索所有相关文档

第二步，必须将收到的滚动标识符设置为SearchScrollRequest以及新的滚动间隔，并通过searchScroll方法发送。 Elasticsearch使用新的滚动标识符返回另
一批结果。 然后，可以在随后的SearchScrollRequest中使用此新的滚动标识符来检索下一批结果，依此类推。 这个过程应该循环重复，直到不再返回结果，这意味
着滚动已经用尽并且已经检索了所有匹配的文档。

```java
//通过设置所需的滚动ID和滚动间隔来创建SearchScrollRequest
SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId); 
scrollRequest.scroll(TimeValue.timeValueSeconds(30));
SearchResponse searchScrollResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
//读取新的滚动ID，该ID指向保持活动的搜索上下文，并在下面的搜索滚动调用中需要
scrollId = searchScrollResponse.getScrollId(); 
//检索另一批搜索命中<4>
hits = searchScrollResponse.getHits(); 
assertEquals(3, hits.getTotalHits().value);
assertEquals(1, hits.getHits().length);
assertNotNull(scrollId);
```

3.清除滚动上下文
  最后，可以使用Clear Scroll API删除最后一个滚动标识符，以释放搜索上下文。 滚动到期时会自动发生这种情况，但滚动会话完成后立即执行此操作是很好的做法。
  
4.可选参数
  构造SearchScrollRequest时，可以选择提供以下参数：
```java
//Scroll interval as a TimeValue
scrollRequest.scroll(TimeValue.timeValueSeconds(60L));
//Scroll interval as a String
scrollRequest.scroll("60s"); 
```
如果没有为SearchScrollRequest设置滚动值，则一旦初始滚动时间到期（即，在初始搜索请求中设置的滚动时间），搜索上下文将到期。


5. 同步执行

```java
SearchResponse searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
```

6. 异步执行

搜索滚动请求的异步执行需要将SearchScrollRequest实例和ActionListener实例传递给异步方法：

```java
//要执行的SearchScrollRequest和执行完成时要使用的ActionListener
client.scrollAsync(scrollRequest, RequestOptions.DEFAULT, scrollListener); 
```

异步方法不会阻塞并立即返回。 一旦完成，如果执行成功完成，则使用onResponse方法回调ActionListener，如果失败则使用onFailure方法。

SearchResponse的典型监听器如下所示：

```java
ActionListener<SearchResponse> scrollListener =
        new ActionListener<SearchResponse>() {
    @Override
    public void onResponse(SearchResponse searchResponse) {
        
    }

    @Override
    public void onFailure(Exception e) {
        
    }
};
```

7. Response

搜索滚动API返回SearchResponse对象，与Search API相同。

8. 完整示例

以下是滚动搜索的完整示例。

```java
final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(1L));
SearchRequest searchRequest = new SearchRequest("posts");
searchRequest.scroll(scroll);
SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
searchSourceBuilder.query(matchQuery("title", "Elasticsearch"));
searchRequest.source(searchSourceBuilder);

//通过发送初始SearchRequest初始化搜索上下文
SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT); 
String scrollId = searchResponse.getScrollId();
SearchHit[] searchHits = searchResponse.getHits().getHits();

//通过循环调用Search Scroll api检索所有搜索命中，直到不返回任何文档
while (searchHits != null && searchHits.length > 0) { 
    
    //处理返回的搜索结果
    //创建一个新的SearchScrollRequest，保存最后返回的滚动标识符和滚动间隔
    SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId); 
    scrollRequest.scroll(scroll);
    searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
    scrollId = searchResponse.getScrollId();
    searchHits = searchResponse.getHits().getHits();
}

//滚动完成后清除滚动上下文
ClearScrollRequest clearScrollRequest = new ClearScrollRequest(); 
clearScrollRequest.addScrollId(scrollId);
ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
boolean succeeded = clearScrollResponse.isSucceeded();
```

3.3. Clear Scroll API

当滚动超时时，搜索滚动API使用的搜索上下文将自动删除。但建议在不需要使用clear scroll api时尽快释放搜索上下文。

1. 清除滚动请求

ClearRequest可以如下创建：

```java
//实例化ClearScrollRequest
ClearScrollRequest request = new ClearScrollRequest(); 
//添加滚动id到滚动标识集合用于清除
request.addScrollId(scrollId); 
```

2. 提供滚动标识

ClearScrollRequest允许清除单个请求中的一个或多个滚动标识。

滚动标识可以逐个添加到请求中

```java
request.addScrollId(scrollId);
```
或者一起使用：

```java
request.setScrollIds(scrollIds);
```

3. 同步执行

```java
ClearScrollResponse response = client.clearScroll(request, RequestOptions.DEFAULT);
```

4. 异步执行

清除滚动请求的异步执行需要将ClearScrollRequest实例和ActionListener实例传递给异步方法：

```java
//he ClearScrollRequest to execute and the ActionListener to use when the execution completes
client.clearScrollAsync(request, RequestOptions.DEFAULT, listener);
```

异步方法不会阻塞并立即返回。 一旦完成，如果执行成功完成，则使用onResponse方法回调ActionListener，如果失败则使用onFailure方法。

ClearScrollResponse的典型监听器如下所示：

```java
ActionListener<ClearScrollResponse> listener =
        new ActionListener<ClearScrollResponse>() {
    @Override
    public void onResponse(ClearScrollResponse clearScrollResponse) {
        
    }

    @Override
    public void onFailure(Exception e) {
        
    }
};
```

5. 清除滚动的响应

返回的ClearScrollResponse允许检索有关已发布搜索上下文的信息：

```java
//Return true if the request succeeded
boolean success = response.isSucceeded(); 
//Return the number of released search contexts
int released = response.getNumFreed(); 
```



3.4. Multi-Search API

3.5. Search Template API

3.6. Multi-Search-Template API

3.7. Field Capabilities API

3.8. Ranking Evaluation API

3.9. Explain API

3.10. Count API

### 4. Miscellaneous APIs
### 5. Indices APIs
5.1. Analyze API

1. Analyze Request

AnalyzeRequest包含要分析的文本，以及指定应如何执行分析的若干选项之一。

最简单的版本使用内置分析器：

```java
AnalyzeRequest request = new AnalyzeRequest();
//he text to include. Multiple strings are treated as a multi-valued field
request.text("Some text to analyze", "Some more text to analyze"); 
//A built-in analyzer
request.analyzer("english");    
```

您可以配置自定义分析器：

```java
AnalyzeRequest request = new AnalyzeRequest();
request.text("<b>Some text to analyze</b>");
//Configure char filters
request.addCharFilter("html_strip"); 
//Configure the tokenizer
request.tokenizer("standard");
//Add a built-in tokenfilter
request.addTokenFilter("lowercase");                

Map<String, Object> stopFilter = new HashMap<>();
stopFilter.put("type", "stop");
//Configuration for a custom tokenfilter
stopFilter.put("stopwords", new String[]{ "to" });  
//Add the custom tokenfilter
request.addTokenFilter(stopFilter);                
```
您还可以通过仅包含charfilters和tokenfilters来构建自定义规范化器：

```java
AnalyzeRequest request = new AnalyzeRequest();
request.text("<b>BaR</b>");
request.addTokenFilter("lowercase");
```

您可以使用现有索引中定义的分析器分析文本：

```java
AnalyzeRequest request = new AnalyzeRequest();
//索引包含的映射
request.index("my_index"); 
//分析器在此索引上定义使用
request.analyzer("my_analyzer");        
request.text("some text to analyze");
```

或者你可以使用normalizer：

```java
AnalyzeRequest request = new AnalyzeRequest();
//The index containing the mappings
request.index("my_index"); 
//The normalizer defined on this index to use
request.normalizer("my_normalizer");        
request.text("some text to analyze");
```
您可以使用索引中特定字段的映射来分析文本：

```java
AnalyzeRequest request = new AnalyzeRequest();
request.index("my_index");
request.field("my_field");
request.text("some text to analyze");
```

2. 可选参数

提供以下可选参数：

```java
//将explain设置为true将为响应添加更多详细信息
request.explain(true);  
//设置属性允许您仅返回您感兴趣的令牌属性
request.attributes("keyword", "type");
```

3. 同步执行

以下列方式执行AnalyzeRequest时，客户端在继续执行代码之前等待返回AnalyzeResponse：

```java
AnalyzeResponse response = client.indices().analyze(request, RequestOptions.DEFAULT);

```

如果无法解析高级REST客户端中的REST响应，请求超时或类似情况没有从服务器返回响应，则同步调用可能会抛出IOException。

如果服务器返回4xx或5xx错误代码，则高级客户端会尝试解析响应正文错误详细信息，然后抛出通用ElasticsearchException并将原始ResponseException作为
抑制异常添加到其中。

4. 异步执行

执行AnalyzeRequest也可以以异步方式完成，以便客户端可以直接返回。 用户需要通过将请求和侦听器传递给异步分析方法来指定响应或潜在故障的处理方式：

```java
client.indices().analyzeAsync(request, RequestOptions.DEFAULT, listener);
```

异步方法不会阻塞并立即返回。 一旦完成，如果执行成功完成，则使用onResponse方法回调ActionListener，如果失败则使用onFailure方法。 故障情形和预期
异常与同步执行情况相同。

典型的分析监听器如下所示：

```java
ActionListener<AnalyzeResponse> listener = new ActionListener<AnalyzeResponse>() {
    @Override
    public void onResponse(AnalyzeResponse analyzeTokens) {
        
    }

    @Override
    public void onFailure(Exception e) {
        
    }
};
```

5. Analyze Response

返回的AnalyzeResponse允许您检索分析的详细信息，如下所示：
```java
//AnalyzeToken保存有关通过分析生成的各个令牌的信息
List<AnalyzeResponse.AnalyzeToken> tokens = response.getTokens(); 
```
如果explain设置为true，那么将从detail（）方法返回信息：

```java
//DetailAnalyzeResponse包含有关分析链中各个子步骤生成的令牌的更多详细信息。
DetailAnalyzeResponse detail = response.detail();  
```



5.2. Create Index API
5.3. Delete Index API
5.4. Indices Exists API

5.5. Open Index API

5.6. Close Index API

5.7. Shrink Index API

5.8. Split Index API

5.9. Refresh API

5.10. Flush API

5.11. Flush Synced API

5.12. Clear Cache API

5.13. Force Merge API

5.14. Rollover Index API

5.15. Put Mapping API

5.16. Get Mapping API

5.17. Get Field Mappings API

5.18. Index Aliases API

5.19. Exists Alias API

5.20. Get Alias API

5.21. Update Indices Settings API

5.22. Get Setting API

5.23. Put Template API

5.24. Validate Query API

5.25. Get Template API

5.26. Templates Exist API

5.27. Get Index API

5.28. Freeze Index API

5.29. Unfreeze Index API

5.30. Delete Template API
