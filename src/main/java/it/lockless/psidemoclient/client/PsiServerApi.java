package it.lockless.psidemoclient.client;

import it.lockless.psidemoclient.dto.*;
import org.springframework.http.*;
import org.springframework.web.client.*;

public class PsiServerApi {

    private final String psiServerBaseUrl;

    private final RestTemplate restTemplate;

    private static String errorMessageRadix = "Status code: ";

    public PsiServerApi(String psiServerBaseUrl){
        this.psiServerBaseUrl = psiServerBaseUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Helper method used to manage API call exceptions.
     *
     * @param e exception risen by the API call
     */
    private void handleRestClientException(RestClientException e){
        if(e instanceof HttpClientErrorException){
            HttpStatus httpStatus = ((HttpClientErrorException) e).getStatusCode();
            if(httpStatus.equals((HttpStatus.REQUEST_TIMEOUT)))
                System.err.println(errorMessageRadix + HttpStatus.REQUEST_TIMEOUT+
                        ". The session has expired. You should start a new session to compute the Private Set Intersection");
            else System.err.println(errorMessageRadix + e.getMessage());
        } else if(e instanceof HttpServerErrorException){
            System.err.println(errorMessageRadix + e.getMessage());
        }
        else if(e instanceof ResourceAccessException){
            System.err.println("Cannot connect to the server. Please verify that the url " + this.psiServerBaseUrl + " is correct");
        }
        else{
            System.err.println("Unexpected error in the communication with the server. Error Message: " + e.getMessage());
        }
        System.exit(1);
    }

    /**
     * Calls GET /psi/parameters to get a list of pairs of PSI algorithms and key sizes supported by the server.
     *
     * @return List of PsiAlgorithmParameterListDTO.
     */
    public PsiAlgorithmParameterListDTO getPsiAlgorithmParameterList(){
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
             handleRestClientException(e);
             return null;
        }
    }

    /**
     * Calls POST /psi to initialize a new PSI session.
     *
     * @return PsiClientSessionDTO object which contains parameters (e.g., public key) needed to create a
     * PsiClient object and the session identifier (sessionId).
     */
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
            handleRestClientException(e);
            return null;
        }
    }

    /**
     * Calls POST /psi/{sessionId}/clientSet to send a client-side encryption of the client dataset to the server.
     *
     * @param sessionId the session identifier.
     * @param psiDatasetMapDTO map with entries structured as <entryId, encryptedValue>. This map is generated
     *                         by the PSI-SDK by calling the loadAndEncryptClientDataset() method on the client dataset.
     * @return returns a PsiDatasetMapDTO which contains a server-side encryption of the entries in the
     * input psiDatasetMapDTO. The entries follow the same structure of the input (i.e., <entryId, encryptedValue>)
     */
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
            handleRestClientException(e);
            return null;
        }
    }

    /**
     * Calls GET /psi/{sessionId}/serverSet to retrieve a page of the encrypted server dataset.
     * If the field last of the returned page is not true, this should be called again with an incremented
     * value of page (starting from 0) to retrieve the whole server dataset.
     *
     * @param sessionId the session identifier.
     * @param page the requested page.
     * @param size the number of entries per page.
     * @return Returns a PsiServerDatasetPageDTO containing a page of entries of the server dataset (encrypted by the server)
     * and paging metadata (e.g., whether it is the last page or the total number of entries).
     */
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
            handleRestClientException(e);
            return null;
        }
    }
}
