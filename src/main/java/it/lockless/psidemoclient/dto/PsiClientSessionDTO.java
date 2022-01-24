package it.lockless.psidemoclient.dto;

import psi.client.PsiClient;
import psi.model.PsiClientSession;

import java.time.Instant;

public class PsiClientSessionDTO {

    private Long sessionId;

    private Instant expiration;

    private PsiClientSession psiClientSession;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    public PsiClientSession getPsiClientSession() {
        return psiClientSession;
    }

    public void setPsiClientSession(PsiClientSession psiClientSession) {
        this.psiClientSession = psiClientSession;
    }

    @Override
    public String toString() {
        return "PsiClientSessionDTO{" +
                "sessionId=" + sessionId +
                ", expiration=" + expiration +
                ", psiClientSession=" + psiClientSession +
                '}';
    }
}
