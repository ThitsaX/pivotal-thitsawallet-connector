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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thitsaworks.mojaloop.coreconnector.component.retrofit.RetrofitRunner;
import lombok.RequiredArgsConstructor;
import okhttp3.ResponseBody;

import java.io.IOException;

@RequiredArgsConstructor
public class FeeEngineErrorDecoder implements RetrofitRunner.ErrorDecoder<FeeEngineErrorResponse> {

    private final ObjectMapper objectMapper;

    @Override
    public FeeEngineErrorResponse decode(int status, ResponseBody errorResponseBody) {

        try {

            return this.objectMapper.readValue(
                errorResponseBody.string(), FeeEngineErrorResponse.class);

        } catch (IOException e) {

            return new FeeEngineErrorResponse("400", "Something went wrong.");

        }

    }

}
