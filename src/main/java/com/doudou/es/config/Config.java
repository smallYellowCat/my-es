package com.doudou.es.config;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class Config {

    @Bean(name = "propertiesFactoryBean")
    public PropertiesFactoryBean properties(){
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("classpath:config.properties"));
        return propertiesFactoryBean;
    }
}
