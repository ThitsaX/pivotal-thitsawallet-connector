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
package com.thitsaworks.mojaloop.coreconnector.error;

import com.thitsaworks.mojaloop.coreconnector.component.exception.ThitsaConnectCustomException;
import com.thitsaworks.mojaloop.coreconnector.component.mojaloop.ErrorCode;
import com.thitsaworks.mojaloop.coreconnector.component.mojaloop.ErrorInformationResponse;
import com.thitsaworks.mojaloop.coreconnector.component.retrofit.RetrofitRunner;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("customErrorProcessor")
public class ThitsaWalletErrorProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ThitsaWalletErrorProcessor.class);

    public ErrorInformationResponse process(Exception exception) throws JSONException {

        if (exception instanceof RetrofitRunner.InvocationException invocationException) {
            return handleRetrofitInvocationException(invocationException);
        }

        if (exception instanceof ThitsaConnectCustomException customException) {
            return handleThitsaConnectCustomException(customException);
        }

        if (exception instanceof JSONException) {
            String sanitizedMessage = exception.getMessage() != null
                ? exception.getMessage().replaceAll("\"", "'")
                : null;
            return buildFromErrorCode(ErrorCode.INTERNAL_SERVER_ERROR, sanitizedMessage);
        }

        return buildFromErrorCode(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, null);
    }

    private ErrorInformationResponse handleRetrofitInvocationException(
        RetrofitRunner.InvocationException exception) throws JSONException {

        ThitsaWalletErrorResponse errorResponse = (ThitsaWalletErrorResponse) exception.getErrorResponse();

        if (errorResponse == null) {
            return buildFromErrorCode(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, null);
        }

        LOG.info("code {}, message {}", errorResponse.statusCode(), errorResponse.message());

        String description = String.format(
            "{statusCode:%s,message:%s,localeMessage:%s,detailedDescription:%s}",
            errorResponse.statusCode(),
            errorResponse.message(),
            errorResponse.localeMessage(),
            errorResponse.detailedDescription());

        if ("3204".equals(errorResponse.statusCode())) {
            return buildErrorResponse(ErrorCode.PARTY_NOT_FOUND.getStatusCode().toString(), description);
        }

        return buildErrorResponse(
            ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE.getStatusCode().toString(), description);
    }

    private ErrorInformationResponse handleThitsaConnectCustomException(
        ThitsaConnectCustomException exception) throws JSONException {

        if (exception.getMessage() == null) {
            return buildFromErrorCode(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, null);
        }

        JSONObject errorResponse = new JSONObject(exception.getMessage());
        JSONObject errorInformation = errorResponse.getJSONObject("errorInformation");

        return buildErrorResponse(
            String.valueOf(errorInformation.getInt("statusCode")),
            errorInformation.getString("description"));
    }

    private ErrorInformationResponse buildFromErrorCode(ErrorCode errorCode, String customMessage)
        throws JSONException {

        String errorResponseJson = customMessage != null
            ? ErrorCode.getErrorResponse(errorCode, customMessage)
            : ErrorCode.getErrorResponse(errorCode);

        JSONObject errorResponse = new JSONObject(errorResponseJson);
        JSONObject errorInformation = errorResponse.getJSONObject("errorInformation");

        return buildErrorResponse(
            String.valueOf(errorInformation.getInt("statusCode")),
            errorInformation.getString("description"));
    }

    private ErrorInformationResponse buildErrorResponse(String statusCode, String message) {

        ErrorInformationResponse response = new ErrorInformationResponse();
        ErrorInformationResponse.ErrorInformation errorInformation =
            new ErrorInformationResponse.ErrorInformation();

        errorInformation.setStatusCode(statusCode);
        errorInformation.setMessage(message);
        response.setErrorInformation(errorInformation);

        return response;
    }
}
