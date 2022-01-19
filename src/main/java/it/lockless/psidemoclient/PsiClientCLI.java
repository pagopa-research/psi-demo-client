package it.lockless.psidemoclient;
import it.lockless.psidemoclient.client.PsiServerApi;
import it.lockless.psidemoclient.dto.PsiDatasetMapDTO;
import it.lockless.psidemoclient.dto.PsiServerDatasetPageDTO;
import it.lockless.psidemoclient.dto.PsiSessionWrapperDTO;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import picocli.CommandLine;
import picocli.CommandLine.*;
import psi.client.PsiClient;
import psi.client.PsiClientFactory;
import psi.client.PsiClientKeyDescription;
import psi.client.PsiClientKeyDescriptionFactory;
import psi.dto.PsiAlgorithmDTO;
import psi.dto.PsiAlgorithmParameterDTO;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    @Option(names = { "-o", "--output" }, paramLabel = "FILE", required = false, defaultValue = "out.txt", description = "Output file containing the result of the PSI")
    File outputFile;

    @Option(names = { "-k", "--keyDescription" }, paramLabel = "FILE", required = false, description = "Yaml file containing the key description for the specific algorithm")
    File keyDescriptionFile;

    @Option(names = { "-outk", "--outputKeyDescription" }, paramLabel = "FILE", required = false, defaultValue = "output-key.yaml", description = "Output file on which the key description used by the algorithm is printed at the end of the execution")
    File outputKeyDescriptionFile;

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
    }

    private void writeOnOutputFile(Set<String> set) {
        try {
            Files.write(Paths.get(outputFile.getPath()), set);
        } catch (IOException e) {
            throw new RuntimeException("Error writing the output file");
        }
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

    private PsiClientKeyDescription readKeyDescriptionFromFile(File keyDescriptionFile){
        if(keyDescriptionFile == null)
            return null;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(keyDescriptionFile);
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find the input key description file" +keyDescriptionFile.getPath());
            System.exit(1);
        }
        Yaml yaml = new Yaml(new Constructor(PsiClientKeyDescription.class));
        return yaml.load(inputStream);
    }

    private void writeKeyDescriptionToFile(PsiClientKeyDescription psiClientKeyDescription, File outputYamlFile){
        Yaml yaml = new Yaml();
        String yamlString = yaml.dumpAs(psiClientKeyDescription, Tag.MAP, DumperOptions.FlowStyle.BLOCK);
        try {
            Files.write(outputYamlFile.toPath(), yamlString.getBytes());
        } catch (IOException e) {
            System.out.println("Cannot write the output key description file "+outputYamlFile.getPath());
            System.exit(1);
        }
    }

    @Override
    public void run() {
        validateServerBaseUrl();
        loadDatasetFromFile();

        // TODO: we should switch to dynamic algorithm parameters
        PsiAlgorithmParameterDTO psiAlgorithmParameterDTO = new PsiAlgorithmParameterDTO();
        psiAlgorithmParameterDTO.setAlgorithm(PsiAlgorithmDTO.BS);
        psiAlgorithmParameterDTO.setKeySize(2048);

        // Init the API client that calls the PSI server by passing the server base URL
        PsiServerApi psiServerApi = new PsiServerApi(serverBaseUrl);

        // Create the session by calling POST /psi passing the selected psiAlgorithmParameterDTO as body
        PsiSessionWrapperDTO psiSessionWrapperDTO= psiServerApi.postPsi(psiAlgorithmParameterDTO);

        // When creating the psiClient, if a key description file is passed as parameter, we it for keys.
        // Similarly, if enabled, set up and validate the cache
        // TODO: add cache logic
        PsiClient psiClient;
        if(keyDescriptionFile == null)
            psiClient = PsiClientFactory.loadSession(psiSessionWrapperDTO.getPsiSessionDTO());
        else
            psiClient = PsiClientFactory.loadSession(psiSessionWrapperDTO.getPsiSessionDTO(), readKeyDescriptionFromFile(keyDescriptionFile));

        // Send the encrypted client dataset and load the returned entries (double encrypted client dataset)
        Map<Long, String> encryptedMap = psiClient.loadAndEncryptClientDataset(clientDataset);
        PsiDatasetMapDTO doubleEncryptedMapWrapped = psiServerApi.postPsiClientSet(psiSessionWrapperDTO.getSessionId(), new PsiDatasetMapDTO(encryptedMap));
        psiClient.loadDoubleEncryptedClientDataset(doubleEncryptedMapWrapped.getContent());

        // Read the encrypted server dataset
        int page = 0;
        int size = 100;
        PsiServerDatasetPageDTO serverDatasetPageDTO;
        do{
            serverDatasetPageDTO = psiServerApi.getPsiServerSetPage(psiSessionWrapperDTO.getSessionId(), page++, size);
            psiClient.loadServerDataset(serverDatasetPageDTO.getContent());
        } while(!serverDatasetPageDTO.isLast());

        // Compute PSI and write the result on the output file
        Set<String> psiResult = psiClient.computePsi();
        writeOnOutputFile(psiResult);

        // Save key description used during by the execution in the outputKeyDescriptionFile
        writeKeyDescriptionToFile(psiClient.getClientKeyDescription(), outputKeyDescriptionFile);

        System.out.println("PSI computed correctly. PSI result written on "+outputFile.getPath()+". The size of the intersection is  = " + psiResult.size());
    }
}
