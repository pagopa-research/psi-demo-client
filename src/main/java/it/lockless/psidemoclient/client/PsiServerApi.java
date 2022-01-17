package it.lockless.psidemoclient.client;

import psi.dto.PsiAlgorithmParameterDTO;
import psi.dto.PsiSessionDTO;
import psi.dto.ServerDatasetPageDTO;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class PsiServerApi {

    private final URL psiServerBaseUrl;

    public PsiServerApi(URL psiServerBaseUrl) {
        this.psiServerBaseUrl = psiServerBaseUrl;
    }

    public List<PsiAlgorithmParameterDTO> getPsiParameter(){
        // TODO: should implement API call
        return null;
    }

    public PsiSessionDTO postPsi(PsiAlgorithmParameterDTO psiAlgorithmParameterDTO){
        // TODO: should implement API call
        return null;
    }

    public Map<Long, String> postPsiClientSet(Map<Long, String> clientDatasetMap){
        // TODO: should implement API call
        return null;
    }

    public ServerDatasetPageDTO getPsiServerSetPage(int page, int size){
        // TODO: should implement API call
        return null;
    }

}
