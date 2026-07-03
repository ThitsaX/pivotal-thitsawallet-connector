/*
 * Copyright (c) 2024-2026 ThitsaWorks Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thitsaworks.mojaloop.coreconnector.payload.feeengine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CatalystFeeApi {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Request(BigDecimal amount,
                          String currency,
                          String scenario) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(FeeCalculationResultData feeCalculationResultData) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeeCalculationResultData(String transactionCurrency,
                                           BigDecimal transactionAmount,
                                           String feeCurrency,
                                           BigDecimal totalFeeAmount,
                                           Map<String, FeeSplit> feeSplits,
                                           FeePolicy feePolicy) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeeSplit(String currency,
                           BigDecimal amount) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FeePolicy(String feePolicyId,
                            String scenario,
                            String transactionCurrency,
                            Map<String, Split> splits,
                            List<Formula> formula) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Split(String type,
                        BigDecimal value,
                        boolean isRemainderRecipient) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Formula(BigDecimal gte,
                          String type,
                          BigDecimal value,
                          String currency,
                          String rounding) { }

}
