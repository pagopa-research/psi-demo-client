package it.lockless.psidemoclient.dto;

import psi.model.PsiClientSession;

import java.time.Instant;

public class PsiClientSessionDTO {

    private Long sessionId;

    private Instant expiration;

    private PsiClientSession psiClientSession;

    private BloomFilterDTO bloomFilterDTO;

    public PsiClientSessionDTO() {
        // Constructor with no arguments is used by Jackson in serialization/deserialization
    }

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

    public BloomFilterDTO getBloomFilterDTO() {
        return bloomFilterDTO;
    }

    public void setBloomFilterDTO(BloomFilterDTO bloomFilterDTO) {
        this.bloomFilterDTO = bloomFilterDTO;
    }

    @Override
    public String toString() {
        return "PsiClientSessionDTO{" +
                "sessionId=" + sessionId +
                ", expiration=" + expiration +
                ", psiClientSession=" + psiClientSession +
                ", bloomFilterDTO=" + bloomFilterDTO +
                '}';
    }
}
