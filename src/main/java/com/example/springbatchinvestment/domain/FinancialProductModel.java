package com.example.springbatchinvestment.domain;

import com.example.springbatchinvestment.client.dto.FinancialProduct;
import com.example.springbatchinvestment.client.dto.FinancialProductOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

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

    public String generateContentHash() {
        StringBuilder content = new StringBuilder();
        content.append(Objects.toString(this.finPrdtNm, ""));
        content.append(Objects.toString(this.korCoNm, ""));
        content.append(Objects.toString(this.spclCnd, ""));
        content.append(Objects.toString(this.joinWay, ""));
        content.append(Objects.toString(this.mtrtInt, ""));
        content.append(Objects.toString(this.etcNote, ""));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
