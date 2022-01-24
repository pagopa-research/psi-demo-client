package it.lockless.psidemoclient.client;

import it.lockless.psidemoclient.dto.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class PsiServerApi {

    private final String psiServerBaseUrl;

    private final RestTemplate restTemplate;

    public PsiServerApi(String psiServerBaseUrl){
        this.psiServerBaseUrl = psiServerBaseUrl;
        this.restTemplate = new RestTemplate();
    }

    public PsiAlgorithmParameterListDTO getPsiParameter(){
        String url = psiServerBaseUrl+"/psi/parameters";
        HttpHeaders requestHeaders = new HttpHeaders();
        HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);
        try{
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    PsiAlgorithmParameterListDTO.class).getBody();
        } catch (RestClientException e){
            System.err.println("Cannot connect to the server. Please verify that the url " + this.psiServerBaseUrl + " is correct");
            System.exit(1);
            return null;
        }
    }

    public PsiClientSessionDTO postPsi(PsiAlgorithmParameterDTO psiAlgorithmParameterDTO){
        String url = psiServerBaseUrl + "/psi";
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PsiAlgorithmParameterDTO> requestEntity = new HttpEntity<>(psiAlgorithmParameterDTO, requestHeaders);
        try{
            return restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    PsiClientSessionDTO.class).getBody();
        } catch (RestClientException e){
            System.err.println("Cannot connect to the server. Please verify that the url " + this.psiServerBaseUrl + " is correct");
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
            if(e instanceof HttpClientErrorException && ((HttpClientErrorException) e).getStatusCode().equals(HttpStatus.REQUEST_TIMEOUT)){
                System.err.println("The session has expired. You should start a new session to compute the Private Set Intersection");
            } else System.err.println("Cannot connect to the server. Please verify that the url " + this.psiServerBaseUrl + " is correct");
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
            if(e instanceof HttpClientErrorException && ((HttpClientErrorException) e).getStatusCode().equals(HttpStatus.REQUEST_TIMEOUT)){
                System.err.println("The session has expired. You should start a new session to compute the Private Set Intersection");
            } else System.err.println("Cannot connect to the server. Please verify that the url " + this.psiServerBaseUrl + " is correct");
            System.exit(1);
            return null;
        }
    }

}
