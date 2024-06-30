package com.example.springbatchinvestment.client;

import com.example.springbatchinvestment.client.dto.FssResponse;
import com.example.springbatchinvestment.client.error.FssClientError;
import com.example.springbatchinvestment.client.fixture.FssFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class FssClientTest {
    private MockWebServer mockWebServer;
    private FssClient fssClient;

    private static final String AUTH = "test-auth";
    private static final String TOP_FIN_GRP_NO = "123";
    private static final String PAGE_NO = "1";
    private static final String FINANCE_CD = "456";

    @BeforeEach
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("localhost").toString();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS)));

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(clientCodecConfigurer -> {
                            clientCodecConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                            clientCodecConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                            clientCodecConfigurer.defaultCodecs().maxInMemorySize(-1);
                        })
                        .build())
                .build();

        fssClient = new FssClient(baseUrl, AUTH);
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void testGetCompanies_success() throws Exception {
        FssResponse mockResponse = FssFixture.getCompanyResponse();
        String mockResponseBody = readResourceFile("client/fss/get-companies-response.json");
        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponseBody)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(HttpStatus.OK.value()));

        FssResponse response = fssClient.getCompanies(TOP_FIN_GRP_NO, PAGE_NO, Optional.of(FINANCE_CD));
        // Assertions to validate response
        Assertions.assertEquals(response, mockResponse);
    }

    @Test
    public void testGetCompanies_clientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.BAD_REQUEST.value()));

        assertThrows(FssClientError.class, () ->
                fssClient.getCompanies(TOP_FIN_GRP_NO, PAGE_NO, Optional.of(FINANCE_CD)));
    }

    @Test
    public void testGetCompanies_serverError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));

        assertThrows(FssClientError.class, () ->
                fssClient.getCompanies(TOP_FIN_GRP_NO, PAGE_NO, Optional.of(FINANCE_CD)));
    }


    private String readResourceFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(new ClassPathResource(filePath).getURI())));
    }
}