package ao.co.kutatelamama.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2500); // Timeout de conexão de 2.5 segundos
        factory.setReadTimeout(3500);    // Timeout de leitura de 3.5 segundos
        return new RestTemplate(factory);
    }
}