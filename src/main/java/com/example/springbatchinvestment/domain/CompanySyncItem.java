package com.example.springbatchinvestment.domain;

import com.example.springbatchinvestment.client.dto.Company;
import com.example.springbatchinvestment.client.dto.CompanyArea;
import java.util.List;

public record CompanySyncItem(Company company, List<CompanyArea> companyAreas) {}
