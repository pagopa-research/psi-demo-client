package it.lockless.psidemoclient;
import it.lockless.psidemoclient.client.PsiServerApi;
import picocli.CommandLine;
import picocli.CommandLine.*;
import psi.client.PsiClient;
import psi.client.PsiClientFactory;
import psi.dto.PsiAlgorithmParameterDTO;
import psi.dto.PsiSessionDTO;
import psi.dto.ServerDatasetPageDTO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Command(name = "psiClientCLI", mixinStandardHelpOptions = true, version = "psiClientCLI 1.0", description = "Demo implementation of a PSI client using the psi-sdk.")
public class PsiClientCLI implements Runnable{

    Map<Long, String> clientSetMap;

    @Spec
    Model.CommandSpec spec;

    @Option(names = { "-i", "--inputDataset" }, paramLabel = "FILE", required = true, description = "File containing the client dataset. Each line of the file is interpreted as an entry of the dataset.")
    File inputDataset;

    @Option(names = { "-url", "--serverUrl" }, paramLabel = "URL", required = true, description = "URL of the server offering the PSI server API")
    URL serverBaseUrl;

    public static void main(String... args) {
        int exitCode = new CommandLine(new it.lockless.psidemoclient.PsiClientCLI()).execute(args);
        System.exit(exitCode);
    }

    private void loadDatasetFromFile(){
        clientSetMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputDataset))) {
            String line;
            long cont = 0;
            while ((line = br.readLine()) != null) {
                clientSetMap.put(cont++, line);
            }
        } catch (IOException e) {
            System.out.println("Cannot parse the input dataset");
            System.exit(1);
        }
        System.out.println(clientSetMap.size()+ " entries loaded from dataset file");
        System.out.println(clientSetMap);
    }

    @Override
    public void run() {
        loadDatasetFromFile();

        // TODO: we should switch to dynamic algorithm parameters
        PsiAlgorithmParameterDTO psiAlgorithmParameterDTO = new PsiAlgorithmParameterDTO();
        psiAlgorithmParameterDTO.setAlgorithm("BS");
        psiAlgorithmParameterDTO.setKeySize(2048);

        // Init the API client that calls the PSI server by passing the server base URL
        PsiServerApi psiServerApi = new PsiServerApi(serverBaseUrl);

        // Create the session by calling POST /psi passing the selected psiAlgorithmParameterDTO as body
        PsiSessionDTO psiSessionDTO = psiServerApi.postPsi(psiAlgorithmParameterDTO);
        PsiClient psiClient = PsiClientFactory.loadSession(psiSessionDTO);

        // Send the encrypted client dataset and load the returned entries (double encrypted client dataset)
        Map<Long, String> encryptedMap = psiClient.loadAndEncryptClientDataset(clientSetMap);
        Map<Long, String> doubleEncryptedMap = psiServerApi.postPsiClientSet(encryptedMap);
        psiClient.loadDoubleEncryptedClientDataset(doubleEncryptedMap);

        // Read the encrypted server dataset
        // TODO: should add a default for page and size
        int page = 0;
        int size = 1000;
        ServerDatasetPageDTO serverDatasetPageDTO;
        do{
            serverDatasetPageDTO = psiServerApi.getPsiServerSetPage(page++, size);
            psiClient.loadServerDataset(serverDatasetPageDTO.getContent());
        } while(!serverDatasetPageDTO.isLast());

        // Compute PSI
        Set<String> psiResult = psiClient.computePsi();

        System.out.println("PSI computed correctly. Intersection size = "+psiResult.size());
    }
}
