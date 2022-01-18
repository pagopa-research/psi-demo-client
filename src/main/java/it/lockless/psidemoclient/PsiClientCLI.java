package it.lockless.psidemoclient;
import it.lockless.psidemoclient.client.PsiServerApi;
import it.lockless.psidemoclient.dto.PsiDatasetMapDTO;
import it.lockless.psidemoclient.dto.PsiServerDatasetPageDTO;
import it.lockless.psidemoclient.dto.PsiSessionWrapperDTO;
import picocli.CommandLine;
import picocli.CommandLine.*;
import psi.client.PsiClient;
import psi.client.PsiClientFactory;
import psi.dto.PsiAlgorithmParameterDTO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Command(name = "psiClientCLI", mixinStandardHelpOptions = true, version = "psiClientCLI 1.0", description = "Demo implementation of a PSI client using the psi-sdk.")
public class PsiClientCLI implements Runnable{

    Set<String> clientDataset;

    @Spec
    Model.CommandSpec spec;

    @Option(names = { "-i", "--inputDataset" }, paramLabel = "FILE", required = true, description = "File containing the client dataset. Each line of the file is interpreted as an entry of the dataset.")
    File inputDataset;

    @Option(names = { "-url", "--serverUrl" }, paramLabel = "URL", required = true, description = "URL of the server offering the PSI server API")
    String serverBaseUrl;

    public static void main(String... args) {
        int exitCode = new CommandLine(new it.lockless.psidemoclient.PsiClientCLI()).execute(args);
        System.exit(exitCode);
    }

    private void loadDatasetFromFile(){
        clientDataset = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputDataset))) {
            String line;
            while ((line = br.readLine()) != null) {
                clientDataset.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot parse the input dataset");
        }
        System.out.println(clientDataset.size()+ " entries loaded from dataset file");
        System.out.println(clientDataset);
    }

    private void validateServerBaseUrl(){
        // Remove any trailing slash in the url
        while(serverBaseUrl.endsWith("/"))
            serverBaseUrl = serverBaseUrl.substring(0, serverBaseUrl.length()-1);
        try {
            new URL(serverBaseUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("The input serverBaseUrl is not a valid URL");
        }
    }


    @Override
    public void run() {
        validateServerBaseUrl();
        loadDatasetFromFile();

        // TODO: we should switch to dynamic algorithm parameters
        PsiAlgorithmParameterDTO psiAlgorithmParameterDTO = new PsiAlgorithmParameterDTO();
        psiAlgorithmParameterDTO.setAlgorithm("BS");
        psiAlgorithmParameterDTO.setKeySize(2048);

        // Init the API client that calls the PSI server by passing the server base URL
        PsiServerApi psiServerApi = new PsiServerApi(serverBaseUrl);

        // Create the session by calling POST /psi passing the selected psiAlgorithmParameterDTO as body
        PsiSessionWrapperDTO psiSessionWrapperDTO= psiServerApi.postPsi(psiAlgorithmParameterDTO);
        PsiClient psiClient = PsiClientFactory.loadSession(psiSessionWrapperDTO.getPsiSessionDTO());

        // Send the encrypted client dataset and load the returned entries (double encrypted client dataset)
        Map<Long, String> encryptedMap = psiClient.loadAndEncryptClientDataset(clientDataset);
        PsiDatasetMapDTO doubleEncryptedMapWrapped = psiServerApi.postPsiClientSet(psiSessionWrapperDTO.getSessionId(), new PsiDatasetMapDTO(encryptedMap));
        psiClient.loadDoubleEncryptedClientDataset(doubleEncryptedMapWrapped.getContent());

        // Read the encrypted server dataset
        // TODO: should add a default for page and size
        int page = 0;
        int size = 1000;
        PsiServerDatasetPageDTO serverDatasetPageDTO;
        do{
            serverDatasetPageDTO = psiServerApi.getPsiServerSetPage(psiSessionWrapperDTO.getSessionId(), page++, size);
            psiClient.loadServerDataset(serverDatasetPageDTO.getContent());
        } while(!serverDatasetPageDTO.isLast());

        // Compute PSI
        Set<String> psiResult = psiClient.computePsi();

        System.out.println("PSI computed correctly. Intersection size = "+psiResult.size());
    }
}
