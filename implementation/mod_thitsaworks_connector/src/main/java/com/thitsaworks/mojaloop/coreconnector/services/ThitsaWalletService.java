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
package com.thitsaworks.mojaloop.coreconnector.services;

import com.thitsaworks.mojaloop.coreconnector.payload.api.LookUpApi;
import com.thitsaworks.mojaloop.coreconnector.payload.api.QuoteApi;
import com.thitsaworks.mojaloop.coreconnector.payload.api.TransferApi;
import retrofit2.Call;
import retrofit2.http.*;

public interface ThitsaWalletService {

    @POST("find_user_quote")
    Call<LookUpApi.Response> doLookUp(@Query("mobile") String mobile);

    @POST("calculate_fee")
    Call<QuoteApi.Response> doQuote(@Body QuoteApi.Request request);

    @POST("credit_amount")
    Call<TransferApi.Response> doTransfer(@Body TransferApi.Request request);
}
