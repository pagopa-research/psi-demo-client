package it.lockless.psidemoclient;
import it.lockless.psidemoclient.cache.RedisPsiCacheProvider;
import it.lockless.psidemoclient.client.PsiServerApi;
import it.lockless.psidemoclient.dto.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import picocli.CommandLine;
import picocli.CommandLine.*;
import psi.client.PsiClient;
import psi.client.PsiClientFactory;
import psi.client.PsiClientKeyDescription;
import psi.dto.PsiAlgorithmDTO;
import psi.dto.PsiAlgorithmParameterDTO;
import psi.utils.StatisticsFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Command(name = "psiClientCLI", mixinStandardHelpOptions = true, version = "psiClientCLI 1.0", description = "Demo implementation of a PSI client using the psi-sdk.")
public class PsiClientCLI implements Runnable{

    Set<String> clientDataset;

    @CommandLine.Parameters(description = "Should either be list or compute.")
    private String command;

    @Spec
    Model.CommandSpec spec;

    @Option(names = { "-url", "--serverUrl" }, paramLabel = "URL", required = true, description = "URL of the server offering the PSI server API")
    private String serverBaseUrl;

    @Option(names = { "-i", "--inputDataset" }, paramLabel = "FILE", description = "File containing the client dataset. Each line of the file is interpreted as an entry of the dataset. Required if command is compute")
    private File inputDataset;

    @Option(names = { "-o", "--output" }, paramLabel = "FILE", defaultValue = "out.txt", description = "Output file containing the result of the PSI")
    private File outputFile;

    @Option(names = { "-a", "--algorithm" }, paramLabel = "String", defaultValue = "BS", description = "Algorithm used for the PSI computation. Should be complaint with the values provided by the list command")
    private String algorithm;

    @Option(names = { "-k", "--keysize" }, paramLabel = "Integer", defaultValue = "2048", description = "Key size used for the PSI computation. Should be complaint with the values provided by the list command")
    private int keySize;

    @Option(names = { "-key", "--keyDescription" }, paramLabel = "FILE", description = "Yaml file containing the key description for the specific algorithm")
    private File keyDescriptionFile;

    @Option(names = { "-outkey", "--outputKeyDescription" }, paramLabel = "FILE", defaultValue = "output-key.yaml", description = "Output file on which the key description used by the algorithm is printed at the end of the execution")
    private File outputKeyDescriptionFile;

    @Option(names = { "-c", "--cache" }, paramLabel = "Boolean", description = "Defines whether the client-side PSI calculation should use the redis cache. If not modified with --cacheUrl and --cachePort, attempts to connect to redis on localhost:6379")
    private boolean cache;

    @Option(names = { "-curl", "--cacheUrl" }, paramLabel = "URL", defaultValue = "localhost", description = "Defines the url of the redis cache. Default value is localhost")
    private String cacheUrl;

    @Option(names = { "-cport", "--cachePort" }, paramLabel = "Integer", defaultValue = "6379", description = "Defines the port of the redis cache. Default value is 6379")
    private Integer cachePort;

    public static void main(String... args) {
        int exitCode = new CommandLine(new it.lockless.psidemoclient.PsiClientCLI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        switch (command) {
            case "list":
                runList();
                break;

            case "compute":
                runCompute();
                break;

            default:
                throw new CommandLine.ParameterException(spec.commandLine(), "The first parameter should either be list or compute");
        }
    }

    public void runCompute() {
        validateServerBaseUrl();
        loadDatasetFromFile();

        PsiAlgorithmParameterDTO psiAlgorithmParameterDTO = new PsiAlgorithmParameterDTO();
        switch(algorithm){
            case "BS":
                psiAlgorithmParameterDTO.setAlgorithm(PsiAlgorithmDTO.BS);
                break;
            case "DH":
                psiAlgorithmParameterDTO.setAlgorithm(PsiAlgorithmDTO.DH);
                break;
            default:
                throw new CommandLine.ParameterException(spec.commandLine(), "The input algorithm parameter is not supported");
        }
        psiAlgorithmParameterDTO.setKeySize(keySize);

        // Init the API client that calls the PSI server by passing the server base URL
        PsiServerApi psiServerApi = new PsiServerApi(serverBaseUrl);

        // Create the session by calling POST /psi passing the selected psiAlgorithmParameterDTO as body
        PsiSessionWrapperDTO psiSessionWrapperDTO = psiServerApi.postPsi(psiAlgorithmParameterDTO);

        // When creating the psiClient, if a key description file is passed as parameter, we use it for keys.
        // Similarly, if enabled, set up and validate the cache
        PsiClient psiClient;

        if(keyDescriptionFile == null){
            if(cache)
                psiClient = PsiClientFactory.loadSession(psiSessionWrapperDTO.getPsiSessionDTO(), new RedisPsiCacheProvider(cacheUrl, cachePort));
            else psiClient = PsiClientFactory.loadSession(psiSessionWrapperDTO.getPsiSessionDTO());
        }
        else{
            if(cache)
                psiClient = PsiClientFactory.loadSession(psiSessionWrapperDTO.getPsiSessionDTO(), readKeyDescriptionFromFile(keyDescriptionFile), new RedisPsiCacheProvider(cacheUrl, cachePort));
            else{
                psiClient = PsiClientFactory.loadSession(psiSessionWrapperDTO.getPsiSessionDTO(), readKeyDescriptionFromFile(keyDescriptionFile));
            }
        }

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
        writeResultFile(psiResult);

        // Save key description used during by the execution in the outputKeyDescriptionFile
        writeKeyDescriptionToFile(psiClient.getClientKeyDescription(), outputKeyDescriptionFile);

        System.out.println("PSI computed correctly. PSI result written on "+outputFile.getPath()+". The size of the intersection is  = " + psiResult.size());

        System.out.println("Printing execution statistics");
        for(StatisticsFactory statisticsFactory : psiClient.getStatisticList())
            System.out.println(statisticsFactory);
    }

    public void runList(){
        validateServerBaseUrl();
        PsiServerApi psiServerApi = new PsiServerApi(serverBaseUrl);

        List<PsiAlgorithmParameterDTO> psiAlgorithmParameterDTOList = psiServerApi.getPsiParameter().getContent();
        if(psiAlgorithmParameterDTOList.size() == 0){
            System.out.println("The server does not support any PSI algorithm");
        }else{
            System.out.println("Supported algorithm-keySize pairs:");
            for(PsiAlgorithmParameterDTO psiAlgorithmParameterDTO : psiAlgorithmParameterDTOList){
                System.out.println(psiAlgorithmParameterDTO.getAlgorithm().toString()+"-"+psiAlgorithmParameterDTO.getKeySize());
            }
        }
    }

    //////////////////////////////////////////////////////////////
    // HELPER FUNCTIONS
    //////////////////////////////////////////////////////////////

    private void loadDatasetFromFile(){
        if(inputDataset == null)
            throw new CommandLine.ParameterException(spec.commandLine(), "The option --inputDataset (-i) is required for the command compute");

        clientDataset = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputDataset))) {
            String line;
            while ((line = br.readLine()) != null) {
                clientDataset.add(line);
            }
        } catch (IOException e) {
            System.err.println("Cannot parse the input dataset");
            System.exit(1);
        }
        System.out.println(clientDataset.size()+ " entries loaded from dataset file");
    }

    private void writeResultFile(Set<String> set) {
        try {
            Files.write(Paths.get(outputFile.getPath()), set);
        } catch (IOException e) {
            throw new RuntimeException("Error writing the output file");
        }
    }

    private void validateServerBaseUrl(){
        // Remove any trailing slashes in the url
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
            System.err.println("Cannot find the input key description file" +keyDescriptionFile.getPath());
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
            System.err.println("Cannot write the output key description file "+outputYamlFile.getPath());
            System.exit(1);
        }
    }
}
