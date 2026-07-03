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
package com.thitsaworks.mojaloop.coreconnector.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thitsaworks.mojaloop.coreconnector.component.exception.ThitsaConnectCustomException;
import com.thitsaworks.mojaloop.coreconnector.component.mojaloop.ErrorCode;
import com.thitsaworks.mojaloop.coreconnector.component.mojaloop.ErrorInformationResponse;
import com.thitsaworks.mojaloop.coreconnector.component.retrofit.RetrofitRunner;
import com.thitsaworks.mojaloop.coreconnector.component.retrofit.RetrofitServiceBuilder;
import com.thitsaworks.mojaloop.coreconnector.component.retrofit.converter.NullOrEmptyConverterFactory;
import com.thitsaworks.mojaloop.coreconnector.component.util.Utility;
import com.thitsaworks.mojaloop.coreconnector.error.FeeEngineErrorDecoder;
import com.thitsaworks.mojaloop.coreconnector.error.FeeEngineErrorProcessor;
import com.thitsaworks.mojaloop.coreconnector.error.FeeEngineErrorResponse;
import com.thitsaworks.mojaloop.coreconnector.error.ThitsaWalletErrorDecoder;
import com.thitsaworks.mojaloop.coreconnector.error.ThitsaWalletErrorProcessor;
import com.thitsaworks.mojaloop.coreconnector.error.ThitsaWalletErrorResponse;
import com.thitsaworks.mojaloop.coreconnector.fspiop.model.AmountType;
import com.thitsaworks.mojaloop.coreconnector.fspiop.model.Extension;
import com.thitsaworks.mojaloop.coreconnector.fspiop.model.ExtensionList;
import com.thitsaworks.mojaloop.coreconnector.fspiop.model.TransactionInitiatorType;
import com.thitsaworks.mojaloop.coreconnector.payload.api.LookUpApi;
import com.thitsaworks.mojaloop.coreconnector.payload.api.QuoteApi;
import com.thitsaworks.mojaloop.coreconnector.payload.api.TransferApi;
import com.thitsaworks.mojaloop.coreconnector.payload.feeengine.CatalystFeeApi;
import com.thitsaworks.mojaloop.coreconnector.payload.fspclient.ConfirmationForTransfer;
import com.thitsaworks.mojaloop.coreconnector.payload.fspclient.DoQuote;
import com.thitsaworks.mojaloop.coreconnector.payload.fspclient.LookUp;
import com.thitsaworks.mojaloop.coreconnector.payload.fspclient.ReservationForTransfer;
import com.thitsaworks.mojaloop.coreconnector.services.FeeEngineService;
import com.thitsaworks.mojaloop.coreconnector.services.FspClientService;
import com.thitsaworks.mojaloop.coreconnector.services.ThitsaWalletService;
import com.thitsaworks.mojaloop.coreconnector.PivotalConfiguration;
import okhttp3.logging.HttpLoggingInterceptor;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import retrofit2.Response;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Qualifier("thitsaWalletClientImpl")
public class ThitsaWalletClientImpl implements FspClientService {

    private static final Logger LOG = LoggerFactory.getLogger(ThitsaWalletClientImpl.class);

    private final PivotalConfiguration.Settings settings;

    private final ThitsaWalletService thitsaWalletService;

    private final FeeEngineService feeEngineService;

    private final RetrofitRunner.ErrorDecoder<ThitsaWalletErrorResponse> errorDecoder;

    private final RetrofitRunner.ErrorDecoder<FeeEngineErrorResponse> feeEngineErrorDecoder;

    private final ThitsaWalletErrorProcessor thitsawalletErrorProcessor;

    private final FeeEngineErrorProcessor feeEngineErrorProcessor;

    private final Utility utility;

    private static final String PERSON_TO_PERSON = "PERSON_TO_PERSON";

    private static final String PERSON_TO_BUSINESS = "PERSON_TO_BUSINESS";

    private static final String PERSON_TO_BUSINESS_USECASE1 = "PERSON_TO_BUSINESS_USECASE1";

    private static final String PERSON_TO_BUSINESS_USECASE2 = "PERSON_TO_BUSINESS_USECASE2";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public ThitsaWalletClientImpl(PivotalConfiguration.Settings settings,
                                  ObjectMapper objectMapper,
                                  ThitsaWalletErrorProcessor thitsawalletErrorProcessor,
                                  FeeEngineErrorProcessor feeEngineErrorProcessor,
                                  Utility utility) {

        this.settings = settings;
        this.thitsaWalletService = new RetrofitServiceBuilder<>(ThitsaWalletService.class,
                                                                this.settings.getBackendEndpoint())
                                       .withHttpLogging(HttpLoggingInterceptor.Level.BODY, true)
                                       .withDisableSSLVerification()
                                       .withConverterFactories(new NullOrEmptyConverterFactory(),
                                           ScalarsConverterFactory.create(),
                                           JacksonConverterFactory.create())
                                       .build();

        this.feeEngineService = new RetrofitServiceBuilder<>(
            FeeEngineService.class, this.settings.getFeeEngineEndpoint())
                                    .withHttpLogging(HttpLoggingInterceptor.Level.BODY, true)
                                    .withDisableSSLVerification()
                                    .withConverterFactories(new NullOrEmptyConverterFactory(),
                                        ScalarsConverterFactory.create(),
                                        JacksonConverterFactory.create())
                                    .build();

        this.errorDecoder = new ThitsaWalletErrorDecoder(objectMapper);
        this.feeEngineErrorDecoder = new FeeEngineErrorDecoder(objectMapper);
        this.thitsawalletErrorProcessor = thitsawalletErrorProcessor;
        this.feeEngineErrorProcessor = feeEngineErrorProcessor;
        this.utility = utility;
    }

    @Override
    public LookUp.Response doLookUp(LookUp.Request request) {

        LookUp.Response response = new LookUp.Response();

        String idValue = request.getIdValue();

        try {

            if (this.settings.getIsPrefix().toLowerCase().equals("true")) {

                idValue = this.utility.removePrefix(idValue);
            }

            String finalIdValue = idValue;

            LOG.info("Find User Quote Request from payee cc to thitsawallet system for idValue {} : {}",
                request.getIdValue(),
                this.objectMapper.writeValueAsString(request));

            List<String> supportedCurrencies = Arrays.stream(this.settings.getSupportedCurrenciesList()
                                                                          .split(","))
                                                     .map(String::trim)
                                                     .collect(Collectors.toList());

            Response<LookUpApi.Response> apiResponse = RetrofitRunner.invoke(this.thitsaWalletService,
                                                                             null,
                                                                             (s, r) -> s.doLookUp(finalIdValue),
                                                                             this.errorDecoder);

            var lookUpResponse = apiResponse.body();

            if (lookUpResponse != null) {

                response.setType(TransactionInitiatorType.CONSUMER.toString());
                response.setIdType(request.getIdType());
                response.setIdValue(request.getIdValue());
                response.setIdSubValue("");
                response.setDisplayName(lookUpResponse.displayName());
                response.setFirstName(lookUpResponse.firstName());
                response.setMiddleName("");
                response.setLastName(lookUpResponse.lastName());
                response.setMerchantClassificationCode("");
                response.setFspId("");
                response.setDateOfBirth("");

                response.setSupportedCurrencies(settings.getSupportedCurrencies());

            }

        } catch (Exception e) {

            try {
                if (e instanceof RetrofitRunner.InvocationException) {

                    Object errorResponse =
                        ((RetrofitRunner.InvocationException) e).getErrorResponse();

                    LOG.error("Find User LookUp Error Response from payee cc for idValue {} : {}",
                        request.getIdValue(),
                        this.objectMapper.writeValueAsString(
                            errorResponse != null ? errorResponse : e.getMessage()));

                    response.setError(
                        this.processInvocationException((RetrofitRunner.InvocationException) e));

                } else if (e instanceof ThitsaConnectCustomException) {

                    LOG.error("Find User LookUp Error Response from payee cc for idValue {} : {}",
                        request.getIdValue(),
                        e.getMessage());

                    response.setError(this.thitsawalletErrorProcessor.process(e));

                } else {

                    LOG.error("Find User LookUp Error Response from payee cc for idValue {} : {}",
                        request.getIdValue(),
                        e.getMessage());

                    throw new RuntimeException("Payee LookUp failed.");

                }

            } catch (JSONException | JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }

        return response;
    }

    @Override
    public DoQuote.Response doQuote(DoQuote.Request request) {

        DoQuote.Response response = new DoQuote.Response();

        try {

            if (request == null || !StringUtils.hasLength(request.getQuotedId()) ||
                !StringUtils.hasLength(request.getTransactionId()) || request.getAmount() == null) {

                throw new ThitsaConnectCustomException(ErrorCode.getErrorResponse(ErrorCode.MISSING_MANDATORY_ELEMENT,
                                                                                  "Required field missing"));
            }

            String idValue = request.getPayee().getIdValue();

            if (this.settings.getIsPrefix().toLowerCase().equals("true")) {

                idValue = this.utility.removePrefix(idValue);
            }

            BigDecimal amount = new BigDecimal(request.getAmount());

            CatalystFeeApi.Request catalystFeeRequest = new CatalystFeeApi.Request(
                amount, request.getCurrency().toString(),
                resolveScenario(request, amount));

            LOG.info("Catalyst Fee Request from payee connector to Catalyst fee engine for transferId {} : {}",
                request.getTransactionId(),
                this.objectMapper.writeValueAsString(catalystFeeRequest));

            CatalystFeeApi.Response catalystFeeResponse = RetrofitRunner.invoke(
                this.feeEngineService, catalystFeeRequest,
                (s, r) -> s.calculateFee(catalystFeeRequest), this.feeEngineErrorDecoder).body();

            LOG.info("Catalyst Fee Response from Catalyst fee engine to payee connector for transferId {} : {}",
                request.getTransactionId(),
                this.objectMapper.writeValueAsString(catalystFeeResponse));

            // cbs quote call

            QuoteApi.Request quoteRequest = new QuoteApi.Request(idValue, new BigDecimal(request.getAmount()));

            LOG.info("Calculate Fee Request from payee cc to thitsawallet system for transferId {} : {}",
                request.getTransactionId(),
                this.objectMapper.writeValueAsString(quoteRequest));

            Response<QuoteApi.Response> apiResponse = RetrofitRunner.invoke(this.thitsaWalletService,
                                                                            quoteRequest,
                                                                            (s, r) -> s.doQuote(
                                                                                quoteRequest),
                                                                            this.errorDecoder);

            var quoteResponse = apiResponse.body();

            LOG.info("Calculate Fee Response from thitsawallet system to payee cc for transferId {} : {}",
                request.getTransactionId(),
                this.objectMapper.writeValueAsString(apiResponse));

            if (quoteResponse != null) {
                BigDecimal fee = apiResponse.body().fee() == null ? new BigDecimal(0.00) : apiResponse.body()
                                                                                                      .fee();
                String formattedFee = fee.setScale(2, RoundingMode.HALF_UP)
                                         .stripTrailingZeros()
                                         .toPlainString()
                                         .stripTrailing();

                List<String> supportedCurrencies = Arrays.stream(this.settings.getSupportedCurrenciesList()
                                                                              .split(","))
                                                         .map(String::trim)
                                                         .collect(Collectors.toList());
                response.setQuoteId(request.getQuotedId());
                response.setTransactionId(request.getTransactionId());

                String transferAmount = request.getAmount();
                String payeeReceiveAmount = request.getAmount();

                if (request.getAmountType()
                           .equals(AmountType.RECEIVE)) {

                    BigDecimal checkAmount = new BigDecimal(transferAmount);
                    checkAmount = checkAmount.add(catalystFeeResponse.feeCalculationResultData().totalFeeAmount());
                    transferAmount = checkAmount.stripTrailingZeros().toPlainString();

                }
                else
                {
                    BigDecimal checkAmount = new BigDecimal(transferAmount);
                    checkAmount = checkAmount.subtract(catalystFeeResponse.feeCalculationResultData().totalFeeAmount());
                    payeeReceiveAmount = checkAmount.stripTrailingZeros().toPlainString();

                }

                if (new BigDecimal(payeeReceiveAmount).compareTo(BigDecimal.ZERO) <= 0) {

                    throw new ThitsaConnectCustomException(
                        ErrorCode.GENERIC_VALIDATION_ERROR.getStatusCode().toString() + ":" +
                            ErrorCode.GENERIC_VALIDATION_ERROR.getDefaultMessage() + ":" +
                            "The transaction cannot proceed because the payee received amount is zero or negative after fee calculation.");
                }

                response.setPayeeReceiveAmount(payeeReceiveAmount);
                response.setTransferAmount(transferAmount);
                response.setTransferAmountCurrency(request.getCurrency());
                response.setPayeeFspFeeAmount(formattedFee);

                response.setPayeeFspCommissionAmount("0");
                response.setExpiration(request.getExpiration());
                response.setGeoCode(request.getGeoCode());
                response.setExtensionList(request.getExtensionList());
                response.setSupportedCurrencies(supportedCurrencies);

                response = addFeeCalculationExtensions(response, catalystFeeResponse);
            }

        } catch (Exception e) {

            try {
                if (e instanceof RetrofitRunner.InvocationException) {

                    Object errorResponse =
                        ((RetrofitRunner.InvocationException) e).getErrorResponse();

                    LOG.error("Calculate Fee Error Response from payee cc for transferId {} : {}",
                        request.getTransactionId(),
                        this.objectMapper.writeValueAsString(
                            errorResponse != null ? errorResponse : e.getMessage()));

                    response.setError(
                        this.processInvocationException((RetrofitRunner.InvocationException) e));

                } else if (e instanceof ThitsaConnectCustomException) {

                    LOG.error("Calculate Fee Error Response from payee cc for transferId {} : {}",
                        request.getTransactionId(),
                        e.getMessage());

                    response.setError(this.thitsawalletErrorProcessor.process(e));

                } else {

                    LOG.error("Calculate Fee Error Response from payee cc for transferId {} : {}",
                        request.getTransactionId(),
                        e.getMessage());

                    throw new RuntimeException("Payee Quote failed.");

                }

            } catch (JSONException | JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }

        return response;
    }

    @Override
    public ReservationForTransfer.Response doReservationForTransfer(ReservationForTransfer.Request request) {

        ReservationForTransfer.Response response = new ReservationForTransfer.Response();

        try {

            if (request == null || request.getTo() == null || request.getTo().getIdValue().isEmpty() ||
                request.getAmount() == null) {

                throw new ThitsaConnectCustomException(ErrorCode.getErrorResponse(ErrorCode.MISSING_MANDATORY_ELEMENT,
                                                                                  "Required field missing"));
            }

            String idValue = request.getTo().getIdValue();

            if (this.settings.getIsPrefix().toLowerCase().equals("true")) {

                idValue = this.utility.removePrefix(idValue);
            }

            TransferApi.Request transferRequest = new TransferApi.Request(idValue,
                                                                              request.getQuote()
                                                                                     .getPayeeReceiveAmount(),
                                                                          request.getTransferId());

            LOG.info("Credit Amount Request from payee cc to thitsawallet system for transferId {} : {}",
                request.getTransferId(),
                this.objectMapper.writeValueAsString(transferRequest));

            Response<TransferApi.Response> apiResponse = RetrofitRunner.invoke(this.thitsaWalletService,
                                                                               transferRequest,
                                                                               (s, r) -> s.doTransfer(transferRequest),
                                                                               this.errorDecoder);
            var transferResponse = apiResponse.body();

            LOG.info("Credit Amount Response from thitsawallet system to payee cc for transferId {} : {}",
                request.getTransferId(),
                this.objectMapper.writeValueAsString(transferResponse));

            if (transferResponse != null) {

                response.setHomeTransactionId(String.valueOf(transferResponse.transactionId()));

            }

        } catch (Exception e) {

            try {
                if (e instanceof RetrofitRunner.InvocationException) {

                    Object errorResponse =
                        ((RetrofitRunner.InvocationException) e).getErrorResponse();

                    LOG.error("Credit Amount Error Response from payee cc for transferId {} : {}",
                        request.getTransferId(),
                        this.objectMapper.writeValueAsString(
                            errorResponse != null ? errorResponse : e.getMessage()));

                    response.setError(
                        this.processInvocationException((RetrofitRunner.InvocationException) e));
                } else if (e instanceof ThitsaConnectCustomException) {

                    LOG.error("Credit Amount Error Response from payee cc for transferId {} : {}",
                        request.getTransferId(),
                        e.getMessage());

                    response.setError(this.thitsawalletErrorProcessor.process(e));

                } else {

                    LOG.error("Credit Amount Error Response from payee cc for transferId {} : {}",
                        request.getTransferId(),
                        e.getMessage());

                    throw new RuntimeException("Payee Transfer failed.");

                }
            } catch (JSONException | JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }

        return response;
    }

    @Override
    public ConfirmationForTransfer.Response doConfirmationForTransfer(ConfirmationForTransfer.Request request) {

        ConfirmationForTransfer.Response response = new ConfirmationForTransfer.Response();

        response.setHomeTransactionId(request.getHomeTransactionId());

        return response;
    }

    private DoQuote.Response addFeeCalculationExtensions(DoQuote.Response response,
                                                         CatalystFeeApi.Response calculateFeeResponse) {

        if (calculateFeeResponse == null ||
            calculateFeeResponse.feeCalculationResultData() == null ||
            calculateFeeResponse.feeCalculationResultData().feeSplits() == null) {
            return response;
        }

        CatalystFeeApi.FeeCalculationResultData result = calculateFeeResponse.feeCalculationResultData();
        ExtensionList extensionList =
            response.getExtensionList() != null ? response.getExtensionList() : new ExtensionList();

        addExtension(
            extensionList, "payerFeeCatalyst",
            feeSplitAmount(result, "payerFeeCatalyst"));
        addExtension(
            extensionList, "payeeFeeCatalyst",
            feeSplitAmount(result, "payeeFeeCatalyst"));
        addExtension(
            extensionList, "schemeFeeCatalyst",
            feeSplitAmount(result, "schemeFeeCatalyst"));

        addExtension(extensionList, "payerFee", feeSplitAmount(result, "payerFeeCatalyst"));
        addExtension(extensionList, "payeeFee", feeSplitAmount(result, "payeeFeeCatalyst"));
        addExtension(extensionList, "schemeFee", feeSplitAmount(result, "schemeFeeCatalyst"));

        response.setExtensionList(extensionList);
        return response;
    }

    private void addExtension(ExtensionList extensionList, String key, String value) {

        if (value == null || value.isBlank()) {
            return;
        }

        Extension extension = new Extension();
        extension.setKey(key);
        extension.setValue(value);
        extensionList.addExtensionItem(extension);
    }

    private String feeSplitAmount(CatalystFeeApi.FeeCalculationResultData result, String key) {

        CatalystFeeApi.FeeSplit feeSplit = result.feeSplits().get(key);
        if (feeSplit == null || feeSplit.amount() == null) {
            return null;
        }

        return feeSplit.amount().stripTrailingZeros().toPlainString();
    }

    private String resolveScenario(DoQuote.Request request, BigDecimal amount) {

        String subScenario = request.getSubScenario();
        if (subScenario != null && !subScenario.isBlank()) {
            if (PERSON_TO_BUSINESS.equals(subScenario)) {
                return amount.compareTo(this.settings.getTransactionAmountLimit()) >= 0 ?
                           PERSON_TO_BUSINESS_USECASE2 : PERSON_TO_BUSINESS_USECASE1;
            }

            if (PERSON_TO_PERSON.equals(subScenario)) {
                return subScenario;
            }

            return subScenario;
        }

        return "";
    }


    private ErrorInformationResponse processInvocationException(RetrofitRunner.InvocationException exception)
        throws JSONException {

        Object errorResponse = exception.getErrorResponse();

        if (errorResponse instanceof ThitsaWalletErrorResponse) {
            return this.thitsawalletErrorProcessor.process(exception);
        }

        if (errorResponse instanceof FeeEngineErrorResponse) {
            return this.feeEngineErrorProcessor.process(exception);
        }

        return this.thitsawalletErrorProcessor.process(exception);
    }

}
