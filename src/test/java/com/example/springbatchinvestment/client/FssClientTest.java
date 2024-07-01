package com.example.springbatchinvestment.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.springbatchinvestment.client.dto.CompanyResult;
import com.example.springbatchinvestment.client.dto.FssResponse;
import com.example.springbatchinvestment.client.error.FssClientError;
import com.example.springbatchinvestment.client.fixture.FssFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
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
import reactor.netty.http.client.HttpClient;

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
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();

        String baseUrl = mockWebServer.url("localhost").toString();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        .doOnConnected(
                                connection ->
                                        connection.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS)));

        this.fssClient = new FssClient(baseUrl, AUTH);
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.mockWebServer.shutdown();
    }

    @Test
    public void testGetCompanies_success() throws Exception {
        FssResponse mockResponse = FssFixture.getCompanyResponse();
        String mockResponseBody = this.readResourceFile("client/fss/get-companies-response.json");
        this.mockWebServer.enqueue(
                new MockResponse()
                        .setBody(mockResponseBody)
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(HttpStatus.OK.value()));

        FssResponse<CompanyResult> response =
                this.fssClient.getCompanies(TOP_FIN_GRP_NO, PAGE_NO, FINANCE_CD);
        // Assertions to validate response
        Assertions.assertEquals(response, mockResponse);
    }

    @Test
    public void testGetCompanies_clientError() {
        this.mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.BAD_REQUEST.value()));

        assertThrows(
                FssClientError.class,
                () -> this.fssClient.getCompanies(TOP_FIN_GRP_NO, PAGE_NO, FINANCE_CD));
    }

    @Test
    public void testGetCompanies_serverError() {
        this.mockWebServer.enqueue(
                new MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()));

        assertThrows(
                FssClientError.class,
                () -> this.fssClient.getCompanies(TOP_FIN_GRP_NO, PAGE_NO, FINANCE_CD));
    }

    private String readResourceFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(new ClassPathResource(filePath).getURI())));
    }
}
