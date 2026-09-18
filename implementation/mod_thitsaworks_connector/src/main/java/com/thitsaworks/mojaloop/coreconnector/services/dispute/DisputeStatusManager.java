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
package com.thitsaworks.mojaloop.coreconnector.services.dispute;

import com.thitsaworks.mojaloop.coreconnector.fspiop.model.ExtensionList;
import com.thitsaworks.mojaloop.coreconnector.payload.fspclient.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
@Component
public class DisputeStatusManager implements InitializingBean, DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(DisputeStatusManager.class);

    private static final long STATUS_CHECK_INITIAL_DELAY_MINUTES = 1L;

    private static final long STATUS_CHECK_PERIOD_MINUTES = 1L;

    private CbsTransactionStatusProvider statusProvider;

    private final Map<String, DisputedTransfer> disputedTransfers = new ConcurrentHashMap<>();

    private final Map<String, TransactionStatus.Response> disputeResults = new ConcurrentHashMap<>();

    private final ScheduledExecutorService checker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "dispute-status-checker");
        thread.setDaemon(true);
        return thread;
    });

    public DisputeStatusManager() { }

    @Autowired
    public void setStatusProvider(CbsTransactionStatusProvider statusProvider) {

        this.statusProvider = statusProvider;
    }

    @Override
    public void afterPropertiesSet() {

        this.checker.scheduleAtFixedRate(
            this::checkDisputedTransfers,
            STATUS_CHECK_INITIAL_DELAY_MINUTES,
            STATUS_CHECK_PERIOD_MINUTES,
            TimeUnit.MINUTES);
    }

    @Override
    public void destroy() {

        this.checker.shutdownNow();
    }

    public void markDispute(String transferId, ExtensionList extensionList) {

        if (!StringUtils.hasLength(transferId)) {
            return;
        }

        DisputedTransfer existing = this.disputedTransfers.putIfAbsent(
            transferId,
            new DisputedTransfer(transferId, extensionList, System.currentTimeMillis()));

        if (existing == null) {
            LOG.warn(
                "Marked transferId {} as dispute. It will be checked every {} minute(s).",
                transferId,
                STATUS_CHECK_PERIOD_MINUTES);
        }
    }

    public TransactionStatus.Response getStatus(TransactionStatus.Request request) {

        if (request == null || !StringUtils.hasLength(request.transferId())) {
            return new TransactionStatus.Response(true);
        }

        return this.disputeResults.get(request.transferId());
    }

    private void checkDisputedTransfers() {

        this.disputedTransfers.values().forEach(disputedTransfer -> {
            if (this.isReadyForStatusCheck(disputedTransfer)) {
                this.checkDisputedTransfer(disputedTransfer);
            }
        });
    }

    private void checkDisputedTransfer(DisputedTransfer disputedTransfer) {

        boolean dispute = this.resolveDispute(disputedTransfer);

        this.disputedTransfers.remove(disputedTransfer.transferId());
        this.disputeResults.put(disputedTransfer.transferId(), new TransactionStatus.Response(dispute));

        if (dispute) {
            LOG.info(
                "Confirmed dispute for transferId {} because transaction status is not successful.",
                disputedTransfer.transferId());
        } else {
            LOG.info(
                "Resolved dispute for transferId {} because transaction status is successful.",
                disputedTransfer.transferId());
        }
    }

    private boolean resolveDispute(DisputedTransfer disputedTransfer) {

        try {
            Boolean cbsTransactionSuccessful = this.statusProvider.getCbsTransactionStatus(
                disputedTransfer.transferId(),
                disputedTransfer.extensionList());

            if (!Boolean.TRUE.equals(cbsTransactionSuccessful)) {
                return true;
            }

            return false;

        } catch (Exception e) {
            LOG.error(
                "Transaction status check failed for transferId {}. Dispute remains true.",
                disputedTransfer.transferId(),
                e);
            return true;
        }
    }

    private boolean isReadyForStatusCheck(DisputedTransfer disputedTransfer) {

        return System.currentTimeMillis() - disputedTransfer.markedAt() >= TimeUnit.MINUTES.toMillis(1);
    }

    private record DisputedTransfer(String transferId,
                                    ExtensionList extensionList,
                                    long markedAt) { }
}
