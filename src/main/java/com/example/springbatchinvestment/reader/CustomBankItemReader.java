package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.domain.dto.BankDto;
import com.example.springbatchinvestment.domain.entity.Bank;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CustomBankItemReader implements ItemReader<List<BankDto>> {

    public CustomBankItemReader(WebClient webClient, ModelMapper modelMapper){
        this.webClient = webClient;
        this.modelMapper = modelMapper;
    }

    private final WebClient webClient;
    private final ModelMapper modelMapper;
    @Value(value = "${api.fss.host}")
    private String fssHost;
    @Value(value = "${api.fss.bank.path}")
    private String bankPath;
    @Value(value = "${api.fss.authKey}")
    private String authKey;

    private int currentPage = 1;

    private List<String> topFinGrpNoList = new ArrayList<>(Arrays.asList("020000", "030300"));
    private int currentGrpNo = 0;




    @Override
    public List<BankDto> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

        BankDto.ResponseBankApi result = getBankList(currentPage, topFinGrpNoList.get(currentGrpNo));

        /* 정상 호출이 실패한 경우 break */
        if(!result.requestSuccess()){
            throw new Exception("");
        }

        if(result.isOverLastPage() && currentGrpNo == 0){
            currentGrpNo++;
            currentPage = 0;
        }else if(result.isOverLastPage() && currentGrpNo == 1){
            return null;
        }



        /* 다음페이지로 셋팅한다. */
        currentPage++;


        return result.getResult().getBaseList().stream().map(bankInfo ->{
            BankDto bankDto = modelMapper.map(bankInfo, BankDto.class);

            if(topFinGrpNoList.get(currentGrpNo).equals("020000")){
                bankDto.setBankType(Bank.BankType.BANK);
            }else if (topFinGrpNoList.get(currentGrpNo).equals("030300")){
                bankDto.setBankType(Bank.BankType.SAVING_BANK);
            }

            return bankDto;
        }).collect(Collectors.toList());
    }


    public BankDto.ResponseBankApi getBankList(int currentPage, String topFinGrpNo) throws Exception {

        return webClient.get()
                    .uri(uriBuilder -> uriBuilder.scheme("https")
                            .host(fssHost)
                            .path(bankPath)
                            .queryParam("auth", authKey)
                            .queryParam("topFinGrpNo", topFinGrpNo)
                            .queryParam("pageNo", currentPage)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatus::isError, clientResponse -> Mono.error(new Exception()))
                    .bodyToMono(BankDto.ResponseBankApi.class)
                    .flux()
                    .toStream()
                    .findFirst().orElse(null);


    }
}
