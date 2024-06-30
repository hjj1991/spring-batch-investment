package com.example.springbatchinvestment.client.fixture;

import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.client.dto.CompanyArea;
import com.example.springbatchinvestment.client.dto.CompanyResult;
import com.example.springbatchinvestment.client.dto.FssResponse;

import java.util.List;
import java.util.Optional;

public class FssFixture {

    public static FssResponse getCompanyResponse() {
        return new FssResponse(
                new CompanyResult(
                       "000",
                       "정상",
                        2L,
                        Optional.of(1L),
                        Optional.of(1L),
                        List.of(new Company("202406", "0010001", "우리은행", "개인금융솔루션부, 1588-5000 부동산금융부,  1588-5000", "https://spot.wooribank.com/pot/Dream?withyou=po", "15885000", null),
                                new Company("202406", "0010002", "한국스탠다드차타드은행", "SC제일은행 고객센터 1588-1599", "http://www.standardchartered.co.kr", "15881599", null)),
                        List.of(new CompanyArea("202406", "0010001", "01", "서울", "Y"),
                                new CompanyArea("202406", "0010002", "01", "서울", "Y"))
                )
        );
    }
}
