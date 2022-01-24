package it.lockless.psidemoclient.dto;

import java.util.List;

public class PsiAlgorithmParameterListDTO {

    List<PsiAlgorithmParameterDTO> content;

    public PsiAlgorithmParameterListDTO(List<PsiAlgorithmParameterDTO> content) {
        this.content = content;
    }

    public PsiAlgorithmParameterListDTO() {
    }

    public List<PsiAlgorithmParameterDTO> getContent() {
        return content;
    }

    public void setContent(List<PsiAlgorithmParameterDTO> content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "PsiAlgorithmParameterListDTO{" +
                "content=" + content +
                '}';
    }
}
