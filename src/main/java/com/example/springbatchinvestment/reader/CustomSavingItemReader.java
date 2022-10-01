package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.domain.dto.SavingDto;
import com.example.springbatchinvestment.domain.dto.SavingOptionDto;
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


public class CustomSavingItemReader implements ItemReader<List<SavingDto>> {

    public CustomSavingItemReader(WebClient webClient, ModelMapper modelMapper) {
        this.webClient = webClient;
        this.modelMapper = modelMapper;
    }


    private final WebClient webClient;
    private final ModelMapper modelMapper;
    @Value(value = "${api.fss.host}")
    private String fssHost;
    @Value(value = "${api.fss.saving.path}")
    private String savingPath;
    @Value(value = "${api.fss.authKey}")
    private String authKey;
    private int currentPage = 1;

    private List<String> topFinGrpNoList = new ArrayList<>(Arrays.asList("020000", "030300"));
    private int currentGrpNo = 0;


    @Override
    public List<SavingDto> read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        SavingDto.ResponseSavingApi result = getSavingList(currentPage, topFinGrpNoList.get(currentGrpNo));

        /* 정상 호출이 실패한 경우 break */
        if (!result.requestSuccess()) {
            throw new Exception("");
        }


        if (result.isOverLastPage() && currentGrpNo == 0) {
            currentGrpNo++;
            currentPage = 0;
        } else if (result.isOverLastPage() && currentGrpNo == 1) {
            return null;
        }


        /* 다음페이지로 셋팅한다. */
        currentPage++;


        return result.getResult().getBaseList().stream().map(savingInfo -> {

            List<SavingOptionDto> savingOptionDtos = new ArrayList<>();

            result.getResult().getOptionList().stream().forEach(savingOptionDto -> {
                if (savingInfo.isSavingOption(savingOptionDto)) {
                    savingOptionDtos.add(savingOptionDto);
                }
            });

            SavingDto savingDto = modelMapper.map(savingInfo, SavingDto.class);

            savingDto.setOptions(savingOptionDtos);
            return savingDto;
        }).collect(Collectors.toList());
    }


    public SavingDto.ResponseSavingApi getSavingList(int currentPage, String topFinGrpNo) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.scheme("https")
                        .host(fssHost)
                        .path(savingPath)
                        .queryParam("auth", authKey)
                        .queryParam("topFinGrpNo", topFinGrpNo)
                        .queryParam("pageNo", currentPage)
                        .build())
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> null)
                .bodyToMono(SavingDto.ResponseSavingApi.class)
                .flux()
                .toStream()
                .findFirst().orElse(null);


    }
}