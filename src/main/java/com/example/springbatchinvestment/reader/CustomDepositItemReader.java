package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.domain.dto.DepositDto;
import com.example.springbatchinvestment.domain.dto.DepositOptionDto;
import org.modelmapper.ModelMapper;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CustomDepositItemReader implements ItemReader<List<DepositDto>> {
    public CustomDepositItemReader(WebClient webClient, ModelMapper modelMapper) {
        this.webClient = webClient;
        this.modelMapper = modelMapper;
    }

    private final WebClient webClient;
    private final ModelMapper modelMapper;
    @Value(value = "${api.fss.host}")
    private String fssHost;
    @Value(value = "${api.fss.deposit.path}")
    private String depositPath;
    @Value(value = "${api.fss.authKey}")
    private String authKey;
    private int currentPage = 1;

    private List<String> topFinGrpNoList = new ArrayList<>(Arrays.asList("020000", "030300"));
    private int currentGrpNo = 0;

    @Override
    public List<DepositDto> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        DepositDto.ResponseDepositApi result = getDepositList(currentPage, topFinGrpNoList.get(currentGrpNo));

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


        return result.getResult().getBaseList().stream().map(depositInfo ->{

            List<DepositOptionDto> depositOptionDtos = new ArrayList<>();

            result.getResult().getOptionList().stream().forEach(depositOptionDto -> {
                if(depositInfo.isDepositOption(depositOptionDto)){
                    depositOptionDtos.add(depositOptionDto);
                }
            });

            DepositDto depositDto = modelMapper.map(depositInfo, DepositDto.class);
            depositDto.setOptions(depositOptionDtos);
            return depositDto;
        }).collect(Collectors.toList());
    }


    public DepositDto.ResponseDepositApi getDepositList(int currentPage, String topFinGrpNo) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.scheme("https")
                        .host(fssHost)
                        .path(depositPath)
                        .queryParam("auth", authKey)
                        .queryParam("topFinGrpNo", topFinGrpNo)
                        .queryParam("pageNo", currentPage)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> null)
                .bodyToMono(DepositDto.ResponseDepositApi.class)
                .flux()
                .toStream()
                .findFirst().orElse(null);


    }
}
