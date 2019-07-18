package com.doudou.es.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * swagger config bean
 * @author 豆豆
 * @date 2019/6/13 9:59
 * @flag 以万物智能，化百千万亿身
 */
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {


    /**
     *
     * @return
     */
    @Bean
    public Docket createRestApi(){
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .enable(true)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.doudou.es.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * build document information
     * @return
     */
    private ApiInfo apiInfo(){
        return new ApiInfoBuilder()
                .title("MyES在线接口文档")
                .contact(new Contact("豆豆", "", ""))
                .version("1.0.0")
                .description("MyES在线接口文档")
                .build();
    }


}