package it.lockless.psidemoclient.dto;

import psi.model.PsiAlgorithmParameter;

public class PsiAlgorithmParameterDTO {

    private PsiAlgorithmParameter content;

    public PsiAlgorithmParameterDTO() {
        // Constructor with no arguments is used by Jackson in serialization/deserialization
    }

    public PsiAlgorithmParameterDTO(PsiAlgorithmParameter content) {
        this.content = content;
    }

    public PsiAlgorithmParameter getContent() {
        return content;
    }

    public void setContent(PsiAlgorithmParameter content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "PsiAlgorithmParameterDTO{" +
                "content=" + content +
                '}';
    }
}
