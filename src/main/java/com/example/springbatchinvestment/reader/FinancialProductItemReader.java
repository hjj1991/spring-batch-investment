package com.example.springbatchinvestment.reader;

import com.example.springbatchinvestment.client.FssClient;
import com.example.springbatchinvestment.client.dto.*;
import com.example.springbatchinvestment.domain.FinancialProductModel;
import com.example.springbatchinvestment.domain.FinancialProductType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;

@Slf4j
public class FinancialProductItemReader
        extends AbstractFinancialItemReader<
                FinancialProduct, FinancialProductOption, FinancialProductModel, FinancialProductResult> {

    private final FinancialProductType financialProductType;

    public FinancialProductItemReader(
            final String baseUrl, final String authKey, final FinancialProductType financialProductType) {
        super(new FssClient(baseUrl, authKey));
        this.financialProductType = financialProductType;
    }

    @Override
    protected FssResponse<FinancialProductResult> fetchDataFromClient() {
        return super.fssClient.getFinancialProducts(
                super.currentFinancialGroupType.getFinancialGroupCode(),
                String.valueOf(super.currentPage),
                null,
                this.financialProductType);
    }

    @Override
    protected List<FinancialProductModel> getItems(
            List<FinancialProduct> financialProducts,
            List<FinancialProductOption> financialProductOptions) {
        return financialProducts.stream()
                .map(
                        financialProduct ->
                                FinancialProductModel.from(
                                        financialProduct,
                                        financialProductOptions,
                                        super.currentFinancialGroupType,
                                        this.financialProductType))
                .toList();
    }
}
