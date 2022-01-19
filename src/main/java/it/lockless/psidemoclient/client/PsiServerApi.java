package it.lockless.psidemoclient.client;

import it.lockless.psidemoclient.dto.PsiAlgorithmParameterListDTO;
import it.lockless.psidemoclient.dto.PsiDatasetMapDTO;
import it.lockless.psidemoclient.dto.PsiServerDatasetPageDTO;
import it.lockless.psidemoclient.dto.PsiSessionWrapperDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import psi.dto.PsiAlgorithmParameterDTO;

public class PsiServerApi {

    private final String psiServerBaseUrl;

    private final RestTemplate restTemplate;

    public PsiServerApi(String psiServerBaseUrl){
        this.psiServerBaseUrl = psiServerBaseUrl;
        this.restTemplate = new RestTemplate();
    }

    public PsiAlgorithmParameterListDTO getPsiParameter(){
        String url = psiServerBaseUrl+"/psi/parameter";
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);
        try{
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    PsiAlgorithmParameterListDTO.class).getBody();
        } catch (RestClientException e){
            System.err.println("Cannot connect to the PSI server. Please verify that the server url " + this.psiServerBaseUrl + " is correct");
            System.exit(1);
            return null;
        }
    }

    public PsiSessionWrapperDTO postPsi(PsiAlgorithmParameterDTO psiAlgorithmParameterDTO){
        String url = psiServerBaseUrl + "/psi";
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity<PsiAlgorithmParameterDTO> requestEntity = new HttpEntity<>(psiAlgorithmParameterDTO, requestHeaders);
        try{
            return restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    PsiSessionWrapperDTO.class).getBody();
        } catch (RestClientException e){
            System.err.println("Cannot connect to the PSI server. Please verify that the input server url " + this.psiServerBaseUrl + " is correct");
            System.exit(1);
            return null;
        }
    }

    public PsiDatasetMapDTO postPsiClientSet(Long sessionId, PsiDatasetMapDTO psiDatasetMapDTO){
        String url = psiServerBaseUrl + "/psi/"+sessionId+"/clientSet";
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity<PsiDatasetMapDTO> requestEntity = new HttpEntity<>(psiDatasetMapDTO, requestHeaders);
        try{
            return restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    PsiDatasetMapDTO.class).getBody();
        } catch (RestClientException e){
            System.err.println("Cannot connect to the PSI server. Please verify that the server url " + this.psiServerBaseUrl + " is correct");
            System.exit(1);
            return null;
        }
    }

    public PsiServerDatasetPageDTO getPsiServerSetPage(Long sessionId, int page, int size){
        String url = psiServerBaseUrl + "/psi/"+sessionId+"/serverSet?page="+page+"&size="+size;
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);
        try{
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    PsiServerDatasetPageDTO.class).getBody();
        } catch (RestClientException e){
            System.err.println("Cannot connect to the PSI server. Please verify that the server url " + this.psiServerBaseUrl + " is correct");
            System.exit(1);
            return null;
        }
    }

}
