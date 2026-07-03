/*
 * Copyright (c) 2026 ThitsaWorks
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

import com.thitsaworks.mojaloop.coreconnector.component.mojaloop.ErrorCode;
import com.thitsaworks.mojaloop.coreconnector.component.mojaloop.ErrorInformationResponse;
import com.thitsaworks.mojaloop.coreconnector.component.retrofit.RetrofitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FeeEngineErrorProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FeeEngineErrorProcessor.class);

    public ErrorInformationResponse process(Exception exception) {

        if (exception instanceof RetrofitRunner.InvocationException) {
            Object errorResponse = ((RetrofitRunner.InvocationException) exception).getErrorResponse();

            if (errorResponse instanceof FeeEngineErrorResponse) {
                return handleFeeEngineErrorResponse((FeeEngineErrorResponse) errorResponse);
            }
        }

        return buildFromErrorCode(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE);
    }

    private ErrorInformationResponse handleFeeEngineErrorResponse(FeeEngineErrorResponse feeEngineErrorResponse) {

        if (feeEngineErrorResponse == null) {
            return buildFromErrorCode(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        String errorCode = feeEngineErrorResponse.errorCode();
        String message = feeEngineErrorResponse.message();

        LOG.info("Fee engine errorCode {}, message {}", errorCode, message);

        String description = String.format("{errorCode:%s,message:%s}", errorCode, message);

        if ("INTERNAL_SERVER_ERROR".equals(errorCode)) {

            return buildErrorResponse(
                ErrorCode.INTERNAL_SERVER_ERROR.getStatusCode().toString(), description);
        }

        if ("CATALYST_ENGINE_REFRESH_FAILED".equals(errorCode)) {

            String statusCode = ErrorCode.INTERNAL_SERVER_ERROR.getStatusCode().toString();

            return buildErrorResponse(statusCode, description);
        }

        if ("NO_SUITABLE_FEE_POLICY_FOUND".equals(errorCode)) {

            String statusCode = ErrorCode.GENERIC_CLIENT_ERROR.getStatusCode().toString();

            return buildErrorResponse(statusCode, description);
        }

        if ("NO_SUITABLE_FORMULA_FOUND".equals(errorCode)) {

            String statusCode = ErrorCode.GENERIC_CLIENT_ERROR.getStatusCode().toString();

            return buildErrorResponse(statusCode, description);
        }
        if ("NO_SUITABLE_POLICY_SCHEDULE_FOUND".equals(errorCode)) {

            String statusCode = ErrorCode.GENERIC_CLIENT_ERROR.getStatusCode().toString();

            return buildErrorResponse(statusCode, description);
        }

        if ("NO_SUITABLE_SCENARIO_FOUND".equals(errorCode)) {

            String statusCode = ErrorCode.GENERIC_CLIENT_ERROR.getStatusCode().toString();

            return buildErrorResponse(statusCode, description);
        }
        if ("SPLIT_FIXED_FEES_EXCEED_TOTAL_FEE".equals(errorCode)) {

            String statusCode = ErrorCode.GENERIC_CLIENT_ERROR.getStatusCode().toString();

            return buildErrorResponse(statusCode, description);
        }
        if ("MESSAGE_NOT_READABLE".equals(errorCode)) {

            String statusCode = ErrorCode.MISSING_MANDATORY_ELEMENT.getStatusCode().toString();

            return buildErrorResponse(statusCode, description);
        }
        if ("ARGUMENT_NOT_VALID".equals(errorCode)) {

            String statusCode = ErrorCode.MALFORMED_SYNTAX.getStatusCode().toString();

            return buildErrorResponse(statusCode, description);
        }

        return buildErrorResponse(
            ErrorCode.INTERNAL_SERVER_ERROR.getStatusCode().toString(), description);
    }

    private ErrorInformationResponse buildFromErrorCode(ErrorCode errorCode) {

        return buildErrorResponse(
            errorCode.getStatusCode().toString(), errorCode.getDefaultMessage());
    }

    private ErrorInformationResponse buildErrorResponse(String statusCode, String message) {

        ErrorInformationResponse response = new ErrorInformationResponse();
        ErrorInformationResponse.ErrorInformation errorInfo = new ErrorInformationResponse.ErrorInformation();

        errorInfo.setStatusCode(statusCode);
        errorInfo.setMessage(message);

        response.setErrorInformation(errorInfo);
        return response;
    }

}
