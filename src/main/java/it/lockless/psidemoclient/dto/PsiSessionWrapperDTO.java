package it.lockless.psidemoclient.dto;

import psi.dto.PsiSessionDTO;

import java.time.Instant;

public class PsiSessionWrapperDTO {

    private Long sessionId;

    private Instant expiration;

    private PsiSessionDTO psiSessionDTO;

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

    public PsiSessionDTO getPsiSessionDTO() {
        return psiSessionDTO;
    }

    public void setPsiSessionDTO(PsiSessionDTO psiSessionDTO) {
        this.psiSessionDTO = psiSessionDTO;
    }

    @Override
    public String toString() {
        return "SessionDTO{" +
                "sessionId=" + sessionId +
                ", expiration=" + expiration +
                ", psiSessionDTO=" + psiSessionDTO +
                '}';
    }
}
