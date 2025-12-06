package com.example.loadbalancer;

import com.degerli.loadbalancer.LoadBalancerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LoadBalancerProperties.class)
public class LoadBalancerApplication {

  public static void main(String[] args) {
    SpringApplication.run(LoadBalancerApplication.class, args);
  }
}