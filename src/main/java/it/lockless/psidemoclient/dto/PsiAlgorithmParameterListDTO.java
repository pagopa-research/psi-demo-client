package it.lockless.psidemoclient.dto;

import psi.model.PsiAlgorithmParameter;

import java.util.List;

public class PsiAlgorithmParameterListDTO {

    List<PsiAlgorithmParameter> content;

    public PsiAlgorithmParameterListDTO(List<PsiAlgorithmParameter> content) {
        this.content = content;
    }

    public PsiAlgorithmParameterListDTO() {
        // Constructor with no arguments is used by Jackson in serialization/deserialization
    }

    public List<PsiAlgorithmParameter> getContent() {
        return content;
    }

    public void setContent(List<PsiAlgorithmParameter> content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "PsiAlgorithmParameterListDTO{" +
                "content=" + content +
                '}';
    }
}
