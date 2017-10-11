package tech.lapsa.insurance.facade;

import java.time.Instant;

import com.lapsa.insurance.domain.InsuranceRequest;

public interface InsuranceRequestFacade extends Acceptor<InsuranceRequest> {

    <T extends InsuranceRequest> T acceptAndReply(T request);

    void markPaymentSucces(Integer id, String paymentReference, Instant paymentInstant);
}