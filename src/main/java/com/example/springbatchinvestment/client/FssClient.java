package com.example.springbatchinvestment.client;

import com.example.springbatchinvestment.client.dto.CompanyResult;
import com.example.springbatchinvestment.client.dto.FinancialProductResult;
import com.example.springbatchinvestment.client.dto.FssResponse;
import com.example.springbatchinvestment.client.error.FssClientError;
import com.example.springbatchinvestment.client.error.FssUnavailableError;
import com.example.springbatchinvestment.domain.FinancialProductType;
import io.netty.channel.ChannelOption;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class FssClient {
    private final WebClient webClient;
    private final String auth;
    private static final String GET_COMPANIES_PATH = "/finlifeapi/companySearch.json";
    private static final String GET_DEPOSITS_PATH = "/finlifeapi/savingProductsSearch.json";
    private static final String GET_SAVINGS_PATH = "/finlifeapi/depositProductsSearch.json";

    public FssClient(String baseUrl, String auth) {
        this.auth = auth;
        JsonMapper objectMapper =
                JsonMapper.builder()
                        .configureForJackson2()
                        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                        .defaultTimeZone(TimeZone.getDefault())
                        .build();
        ExchangeStrategies exchangeStrategies =
                ExchangeStrategies.builder()
                        .codecs(
                                clientCodecConfigurer -> {
                                    clientCodecConfigurer
                                            .defaultCodecs()
                                            .jacksonJsonDecoder(new JacksonJsonDecoder(objectMapper));
                                    clientCodecConfigurer
                                            .defaultCodecs()
                                            .jacksonJsonEncoder(new JacksonJsonEncoder(objectMapper));
                                    clientCodecConfigurer.defaultCodecs().maxInMemorySize(-1);
                                })
                        .build();

        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        .doOnConnected(
                                connection ->
                                        connection.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS)));

        this.webClient =
                WebClient.builder()
                        .baseUrl(baseUrl)
                        .exchangeStrategies(exchangeStrategies)
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .build();
    }

    public FssResponse<CompanyResult> getCompanies(
            final String topFinGrpNo, final String pageNo, final String financeCd) {
        final String getCompaniesUri =
                UriComponentsBuilder.fromPath(GET_COMPANIES_PATH)
                        .queryParam("auth", this.auth)
                        .queryParam("topFinGrpNo", topFinGrpNo)
                        .queryParam("pageNo", pageNo)
                        .queryParamIfPresent("financeCd", Optional.ofNullable(financeCd))
                        .build()
                        .toUriString();
        return this.webClient
                .get()
                .uri(getCompaniesUri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new FssClientError()))
                .onStatus(
                        HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new FssClientError()))
                .bodyToMono(new ParameterizedTypeReference<FssResponse<CompanyResult>>() {})
                .onErrorResume(
                        throwable -> {
                            if (throwable instanceof WebClientRequestException
                                    && (throwable.getCause() instanceof ReadTimeoutException
                                            || throwable.getCause() instanceof ConnectTimeoutException)) {
                                log.warn(
                                        "RCV | {} | ReadTimeoutException or ConnectTimeoutException occurred",
                                        getCompaniesUri);
                                return Mono.error(FssUnavailableError::new);
                            } else if (ExceptionUtils.getRootCause(throwable)
                                    instanceof PrematureCloseException) {
                                log.warn("RCV | {} | PrematureCloseException occurred", getCompaniesUri);
                                return Mono.error(FssUnavailableError::new);
                            }
                            log.error("Error occurred during calling GET {}", getCompaniesUri, throwable);
                            return Mono.error(throwable);
                        })
                .retryWhen(
                        Retry.backoff(3, Duration.ofMillis(1000))
                                .filter(throwable -> throwable instanceof FssUnavailableError)
                                .doAfterRetry(
                                        retrySignal ->
                                                log.warn(
                                                        "SNT | {} | Retrying #{}", getCompaniesUri, retrySignal.totalRetries()))
                                .onRetryExhaustedThrow(
                                        (retryBackoffSpec, retrySignal) -> {
                                            log.error(
                                                    "RCV | {} | Reached at max retry count {}",
                                                    getCompaniesUri,
                                                    retrySignal.totalRetries());
                                            throw new FssUnavailableError();
                                        }))
                .map(
                        fssResponse -> {
                            log.info("RCV | {}, | CompanyResponse: {}", getCompaniesUri, fssResponse);
                            return fssResponse;
                        })
                .block();
    }

    public FssResponse<FinancialProductResult> getFinancialProducts(
            final String topFinGrpNo,
            final String pageNo,
            final String financeCd,
            final FinancialProductType financialProductType) {

        final String getFinancialProductsUri =
                UriComponentsBuilder.fromPath(this.getFinancialProductsUri(financialProductType))
                        .queryParam("auth", this.auth)
                        .queryParam("topFinGrpNo", topFinGrpNo)
                        .queryParam("pageNo", pageNo)
                        .queryParamIfPresent("financeCd", Optional.ofNullable(financeCd))
                        .build()
                        .toUriString();
        return this.webClient
                .get()
                .uri(getFinancialProductsUri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError, clientResponse -> Mono.error(new FssClientError()))
                .onStatus(
                        HttpStatusCode::is5xxServerError, clientResponse -> Mono.error(new FssClientError()))
                .bodyToMono(new ParameterizedTypeReference<FssResponse<FinancialProductResult>>() {})
                .onErrorResume(
                        throwable -> {
                            if (throwable instanceof WebClientRequestException
                                    && (throwable.getCause() instanceof ReadTimeoutException
                                            || throwable.getCause() instanceof ConnectTimeoutException)) {
                                log.warn(
                                        "RCV | {} | ReadTimeoutException or ConnectTimeoutException occurred",
                                        getFinancialProductsUri);
                                return Mono.error(FssUnavailableError::new);
                            } else if (ExceptionUtils.getRootCause(throwable)
                                    instanceof PrematureCloseException) {
                                log.warn("RCV | {} | PrematureCloseException occurred", getFinancialProductsUri);
                                return Mono.error(FssUnavailableError::new);
                            }
                            log.error("Error occurred during calling GET {}", getFinancialProductsUri, throwable);
                            return Mono.error(throwable);
                        })
                .retryWhen(
                        Retry.backoff(3, Duration.ofMillis(1000))
                                .filter(throwable -> throwable instanceof FssUnavailableError)
                                .doAfterRetry(
                                        retrySignal ->
                                                log.warn(
                                                        "SNT | {} | Retrying #{}",
                                                        getFinancialProductsUri,
                                                        retrySignal.totalRetries()))
                                .onRetryExhaustedThrow(
                                        (retryBackoffSpec, retrySignal) -> {
                                            log.error(
                                                    "RCV | {} | Reached at max retry count {}",
                                                    getFinancialProductsUri,
                                                    retrySignal.totalRetries());
                                            throw new FssUnavailableError();
                                        }))
                .map(
                        fssResponse -> {
                            log.info(
                                    "RCV | {}, | FinancialProductsResponse: {}",
                                    getFinancialProductsUri,
                                    fssResponse);
                            return fssResponse;
                        })
                .block();
    }

    private String getFinancialProductsUri(FinancialProductType financialProductType) {
        return switch (financialProductType) {
            case SAVINGS -> GET_SAVINGS_PATH;
            case INSTALLMENT_SAVINGS -> GET_DEPOSITS_PATH;
        };
    }

}
