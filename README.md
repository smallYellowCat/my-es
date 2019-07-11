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
### 3. Java High Level REST Client
