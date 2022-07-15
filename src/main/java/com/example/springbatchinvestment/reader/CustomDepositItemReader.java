package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.domain.dto.BankDto;
import com.example.springbatchinvestment.domain.dto.DepositDto;
import com.example.springbatchinvestment.domain.entity.Bank;
import com.example.springbatchinvestment.domain.entity.Deposit;
import org.modelmapper.ModelMapper;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class CustomDepositItemReader implements ItemReader<List<DepositDto>> {
    public CustomDepositItemReader(WebClient webClient, ModelMapper modelMapper, String topFinGrpNo) {
        this.webClient = webClient;
        this.modelMapper = modelMapper;
        this.topFinGrpNo = topFinGrpNo;
    }

    private final WebClient webClient;
    private final ModelMapper modelMapper;
    private final String topFinGrpNo;
    @Value(value = "${api.fss.host}")
    private String fssHost;
    @Value(value = "${api.fss.deposit.path}")
    private String depositPath;
    @Value(value = "${api.fss.authKey}")
    private String authKey;
    private int currentPage = 1;

    @Override
    public List<DepositDto> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        DepositDto.ResponseDepositApi result = getDepositList(currentPage, topFinGrpNo);

        /* 정상 호출이 실패한 경우 break */
        if(!result.requestSuccess()){
            throw new Exception("");
        }

        /* 마지막 페이지보다 큰 경우 */
        if(result.isOverLastPage()){
            return null;
        }


        /* 다음페이지로 셋팅한다. */
        currentPage++;


        return result.getResult().getBaseList().stream().map(depositInfo ->{
            DepositDto bankDto = modelMapper.map(depositInfo, DepositDto.class);

            result.getResult().getOptionList().stream().map(option -> {
            })


            return bankDto;
        }).collect(Collectors.toList());
    }


    public DepositDto.ResponseDepositApi getDepositList(int currentPage, String topFinGrpNo) throws Exception {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.scheme("https")
                        .host(fssHost)
                        .path(depositPath)
                        .queryParam("auth", authKey)
                        .queryParam("topFinGrpNo", topFinGrpNo)
                        .queryParam("pageNo", currentPage)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(new Exception()))
                .bodyToMono(DepositDto.ResponseDepositApi.class)
                .flux()
                .toStream()
                .findFirst().orElse(null);


    }
}
