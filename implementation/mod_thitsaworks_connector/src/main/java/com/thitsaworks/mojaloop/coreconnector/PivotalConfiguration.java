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
package com.thitsaworks.mojaloop.coreconnector;


import com.thitsaworks.mojaloop.coreconnector.component.ComponentConfiguration;
import com.thitsaworks.mojaloop.coreconnector.fspiop.model.Currency;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Configuration
@Import(
    value = {
        ComponentConfiguration.class})
public class PivotalConfiguration extends CoreConnectorConfiguration {


    @Bean
    public PivotalConfiguration.Settings connectorSettings() {

        return new PivotalConfiguration.Settings();
    }


    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer(
            PivotalConfiguration.Settings settings) {
        return factory -> factory.setPort(settings.getSdkConnectorPortNo());
    }


    @Getter
    @Component
    @Primary
    public static class Settings extends CoreConnectorConfiguration.Settings {

        private final List<Currency> supportedCurrencies;

        private final String backendEndpoint;
        private final int backendApiTimeoutMs;
        private final String isPrefix;
        private final String supportedCurrenciesList;

        private final String feeEngineEndpoint;
        private final int sdkConnectorPortNo;
        private final BigDecimal transactionAmountLimit;

        public Settings() {
            this.supportedCurrenciesList = prop("supportedCurrencies", "MMK");
            this.supportedCurrencies = parseCurrencies(this.supportedCurrenciesList);

            this.backendEndpoint = prop("backendEndpoint", "http://example.com:8081");
            this.backendApiTimeoutMs = propInt("backendApiTimeoutMs", 30_000);
            this.isPrefix = prop("isPrefix", "false");
            this.feeEngineEndpoint = prop("feeEngineEndpoint", "http://example.com:8082");
            this.transactionAmountLimit = propBigDecimal("transactionAmountLimit", BigDecimal.ZERO);
            this.sdkConnectorPortNo = propInt("sdkConnectorPortNo", 8080);
        }

        private static BigDecimal propBigDecimal(String key, BigDecimal def) {
            try {
                return new BigDecimal(prop(key, def.toPlainString()));
            } catch (NumberFormatException e) {
                return def;
            }
        }
        private static String prop(String key, String def) {
            String v = System.getProperty(key);
            return (v != null && !v.isBlank()) ? v : def;
        }

        private static int propInt(String key, int def) {
            try {
                return Integer.parseInt(prop(key, String.valueOf(def)));
            } catch (NumberFormatException e) {
                return def;
            }
        }

        private static boolean propBoolean(String key, boolean def) {
            String value = prop(key, String.valueOf(def)).trim();
            return "1".equals(value) || Boolean.parseBoolean(value);
        }

        private static List<Currency> parseCurrencies(String raw) {
            if (raw == null || raw.isBlank()) {
                return List.of();
            }
            return Arrays.stream(raw.split(","))
                         .map(String::trim)
                         .filter(currency -> !currency.isBlank())
                         .map(currency -> Currency.fromValue(currency.toUpperCase(Locale.ROOT)))
                         .toList();
        }
    }

    @Bean
    public OkHttpClient sharedOkHttpClient() {
        return new OkHttpClient.Builder()
                   .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                   .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                   .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                   .build();
    }
}
