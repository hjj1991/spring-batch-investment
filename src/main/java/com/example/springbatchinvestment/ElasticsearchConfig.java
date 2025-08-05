package com.example.springbatchinvestment;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Bean
    @Primary
    public RestClient elasticsearchRestClient() {
        try {
            String[] uriArray = this.elasticsearchUris.split(",");
            HttpHost[] hosts =
                    Arrays.stream(uriArray).map(String::trim).map(HttpHost::create).toArray(HttpHost[]::new);

            RestClientBuilder builder = RestClient.builder(hosts);

            // HTTPS인 경우 SSL 검증 비활성화
            if (this.elasticsearchUris.contains("https://")) {
                builder.setHttpClientConfigCallback(
                        httpClientBuilder -> {
                            try {
                                SSLContext sslContext = SSLContext.getInstance("TLS");
                                sslContext.init(
                                        null,
                                        new TrustManager[] {
                                            new X509TrustManager() {
                                                @Override
                                                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                                                @Override
                                                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                                                @Override
                                                public X509Certificate[] getAcceptedIssuers() {
                                                    return new X509Certificate[0];
                                                }
                                            }
                                        },
                                        new SecureRandom());

                                httpClientBuilder.setSSLContext(sslContext);
                                httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                            } catch (Exception e) {
                                throw new RuntimeException("SSL 설정 실패", e);
                            }

                            // 인증 설정
                            if (!this.username.isEmpty() && !this.password.isEmpty()) {
                                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                                credentialsProvider.setCredentials(
                                        AuthScope.ANY, new UsernamePasswordCredentials(this.username, this.password));
                                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                            }

                            return httpClientBuilder;
                        });
            } else {
                // HTTP인 경우 기본 인증만 설정
                if (!this.username.isEmpty() && !this.password.isEmpty()) {
                    builder.setHttpClientConfigCallback(
                            httpClientBuilder -> {
                                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                                credentialsProvider.setCredentials(
                                        AuthScope.ANY, new UsernamePasswordCredentials(this.username, this.password));
                                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                                return httpClientBuilder;
                            });
                }
            }

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch RestClient 생성 실패", e);
        }
    }

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ElasticsearchTransport transport =
                new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    @Bean
    @Primary
    public ElasticsearchTemplate elasticsearchTemplate(ElasticsearchClient client) {
        return new ElasticsearchTemplate(client);
    }
}
