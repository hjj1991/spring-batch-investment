package com.example.springbatchinvestment.domain;

import com.example.springbatchinvestment.client.dto.FinancialProduct;
import com.example.springbatchinvestment.client.dto.FinancialProductOption;
import java.util.List;

public record FinancialProductModel(
        String dclsMonth,
        String finCoNo,
        String finPrdtCd,
        String korCoNm,
        String finPrdtNm,
        String joinWay,
        String mtrtInt,
        String spclCnd,
        String joinDeny,
        String joinMember,
        String etcNote,
        String maxLimit,
        String dclsStrtDay,
        String dclsEndDay,
        String finCoSubmDay,
        FinancialGroupType financialGroupType,
        List<FinancialProductOptionModel> financialProductOptionModels,
        FinancialProductType financialProductType) {

    public static FinancialProductModel from(
            FinancialProduct financialProduct,
            List<FinancialProductOption> financialProductOptions,
            FinancialGroupType financialGroupType,
            FinancialProductType financialProductType) {
        return new FinancialProductModel(
                financialProduct.dclsMonth(),
                financialProduct.finCoNo(),
                financialProduct.finPrdtCd(),
                financialProduct.korCoNm(),
                financialProduct.finPrdtNm(),
                financialProduct.joinWay(),
                financialProduct.mtrtInt(),
                financialProduct.spclCnd(),
                financialProduct.joinDeny(),
                financialProduct.joinMember(),
                financialProduct.etcNote(),
                financialProduct.maxLimit(),
                financialProduct.dclsStrtDay(),
                financialProduct.dclsEndDay(),
                financialProduct.finCoSubmDay(),
                financialGroupType,
                financialProductOptions.stream()
                        .filter(
                                financialProductOption ->
                                        isOptionForProduct(financialProductOption, financialProduct))
                        .map(FinancialProductOptionModel::from)
                        .toList(),
                financialProductType);
    }

    private static boolean isOptionForProduct(
            FinancialProductOption option, FinancialProduct product) {
        return option.finCoNo().equals(product.finCoNo())
                && option.finPrdtCd().equals(product.finPrdtCd());
    }
}
