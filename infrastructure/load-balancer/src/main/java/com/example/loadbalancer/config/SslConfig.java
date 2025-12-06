package com.example.loadbalancer.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * SSL/TLS Termination Configuration
 * <p>
 * Load balancer HTTPS ile dinler, backend'lere HTTP ile iletir
 */
@Configuration
public class SslConfig {

  /**
   * HTTPS connector (port 8443)
   */
  @Bean
  @Profile("ssl")
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sslConnector() {
    return factory -> {
      factory.addAdditionalTomcatConnectors(createHttpsConnector());
    };
  }

  private Connector createHttpsConnector() {
    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setScheme("https");
    connector.setSecure(true);
    connector.setPort(8443);

    // SSL properties
    connector.setProperty("SSLEnabled", "true");
    connector.setProperty("keystoreFile", "classpath:keystore.p12");
    connector.setProperty("keystorePass", "changeit");
    connector.setProperty("keystoreType", "PKCS12");
    connector.setProperty("clientAuth", "false");
    connector.setProperty("sslProtocol", "TLS");
    connector.setProperty("sslEnabledProtocols", "TLSv1.2,TLSv1.3");

    // Cipher suites (güvenli olanlar)
    connector.setProperty("ciphers",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," + "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");

    return connector;
  }

  /**
   * HTTP'den HTTPS'e yönlendirme
   */
  @Bean
  @Profile("ssl")
  public WebServerFactoryCustomizer<TomcatServletWebServerFactory> redirectConnector() {
    return factory -> {
      factory.addAdditionalTomcatConnectors(createHttpRedirectConnector());
    };
  }

  private Connector createHttpRedirectConnector() {
    Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
    connector.setScheme("http");
    connector.setPort(8080);
    connector.setSecure(false);
    connector.setRedirectPort(8443); // HTTP -> HTTPS redirect
    return connector;
  }
}

/**
 * Self-signed certificate oluşturma komutu:
 * <p>
 * keytool -genkeypair -alias loadbalancer \
 * -keyalg RSA -keysize 2048 \
 * -storetype PKCS12 \
 * -keystore keystore.p12 \
 * -storepass changeit \
 * -validity 3650 \
 * -dname "CN=localhost, OU=LoadBalancer, O=Example, L=City, ST=State, C=US"
 */