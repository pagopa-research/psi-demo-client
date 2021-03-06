package it.lockless.psidemoclient;
import com.google.common.hash.BloomFilter;
import it.lockless.psidemoclient.cache.RedisPsiCacheProvider;
import it.lockless.psidemoclient.client.PsiServerApi;
import it.lockless.psidemoclient.dto.*;
import it.lockless.psidemoclient.util.BloomFilterHelper;
import it.lockless.psidemoclient.util.PsiClientKeyDescriptionYaml;
import it.lockless.psidemoclient.util.PsiDemoClientRuntimeException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import picocli.CommandLine;
import picocli.CommandLine.*;
import psi.PsiClientFactory;
import psi.client.PsiClient;
import psi.PsiClientKeyDescription;
import psi.PsiClientKeyDescriptionFactory;
import psi.exception.InvalidPsiClientKeyDescriptionException;
import psi.exception.UnsupportedKeySizeException;
import psi.model.PsiAlgorithm;
import psi.model.PsiAlgorithmParameter;
import psi.model.PsiPhaseStatistics;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/*
Command to create an executable JAR:
mvn clean compile assembly:single

Command to run the JAR located in /target:
java -jar psi-demo-client-1.0-jar-with-dependencies.jar [arguments]
 */

@Command(name = "psiClientCLI", mixinStandardHelpOptions = true, version = "psiClientCLI 1.0", description = "Demo implementation of a PSI client using the psi-sdk.")
public class PsiClientCLI implements Runnable{

    private Set<String> clientDataset;

    @CommandLine.Parameters(description = "Should either be list or compute.")
    private String command;

    @Spec
    private Model.CommandSpec spec;

    @Option(names = { "-url", "--serverUrl" }, paramLabel = "URL", required = true, description = "URL of the server offering the PSI server API")
    private String serverBaseUrl;

    @Option(names = { "-i", "--inputDataset" }, paramLabel = "FILE", description = "File containing the client dataset. Each line of the file is interpreted as an entry of the dataset. Required if command is compute")
    private File inputDataset;

    @Option(names = { "-o", "--output" }, paramLabel = "FILE", defaultValue = "out.txt", description = "Output file containing the result of the PSI")
    private File outputFile;

    @Option(names = { "-a", "--algorithm" }, paramLabel = "String", defaultValue = "BS", description = "Algorithm used for the PSI computation. Should be compliant with the values provided by the list command")
    private String algorithm;

    @Option(names = { "-k", "--keysize" }, paramLabel = "Integer", defaultValue = "2048", description = "Key size used for the PSI computation. Should be compliant with the values provided by the list command")
    private int keySize;

    @Option(names = { "-key", "--keyDescription" }, paramLabel = "FILE", description = "Yaml file containing the key description for the specific algorithm")
    private File keyDescriptionFile;

    @Option(names = { "-outkey", "--outputKeyDescription" }, paramLabel = "FILE", defaultValue = "key.yaml", description = "Output file on which the key description used by the algorithm is printed at the end of the execution")
    private File outputKeyDescriptionFile;

    @Option(names = { "-c", "--cache" }, paramLabel = "Boolean", description = "Defines whether the client-side PSI calculation should use the Redis cache. If not modified with --cacheUrl and --cachePort, attempts to connect to Redis on localhost:6379")
    private boolean cache;

    @Option(names = { "-curl", "--cacheUrl" }, paramLabel = "URL", defaultValue = "localhost", description = "Defines the url of the Redis cache. Default value is localhost")
    private String cacheUrl;

    @Option(names = { "-cport", "--cachePort" }, paramLabel = "Integer", defaultValue = "6379", description = "Defines the port of the Redis cache. Default value is 6379")
    private Integer cachePort;

    @Option(names = { "-bf", "--bloomFilterMaxAge" }, paramLabel = "Integer", description = "If set, defines the max minutes since the Bloom Filter creation to consider it valid. If the server sends an older Bloom Filter, the Bloom Filter is not applied. If this parameter is not set, the Bloom Filter is not applied")
    private Integer bloomFilterMaxAge;

    public static void main(String... args) {
        int exitCode = new CommandLine(new it.lockless.psidemoclient.PsiClientCLI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        validateServerBaseUrl();
        PsiServerApi psiServerApi = new PsiServerApi(serverBaseUrl);

        switch (command) {
            case "list":
                int listResultSize = runList(psiServerApi);
                if(listResultSize < 0)
                    System.exit(1);
                break;

            case "compute":
                ProcessExecutionResult processExecutionResult = runCompute(psiServerApi);
                if(!processExecutionResult.isSuccessful())
                    System.exit(1);
                break;

            default:
                throw new CommandLine.ParameterException(spec.commandLine(), "The first parameter should either be list or compute");
        }
    }

    /**
     * Performs the complete PSI computation which comprises both local computations (different calls to the psi-sdk)
     * and API calls to the server.
     * Code executed when passing compute as the first argument (command).
     *
     * @param psiServerApi psiServerApi object which performs the API client towards the server
     * @return ProcessExecutionResult object, which provides some information on the outcome of the execution
     */
    public ProcessExecutionResult runCompute(PsiServerApi psiServerApi) {
        System.out.println("PSI Client started. Running algorithm "+algorithm+" with keySize "+keySize);
        loadDatasetFromFile();
        PsiAlgorithmParameter psiAlgorithmParameter = new PsiAlgorithmParameter();
        switch(algorithm){
            case "BS":
                psiAlgorithmParameter.setAlgorithm(PsiAlgorithm.BS);
                break;
            case "DH":
                psiAlgorithmParameter.setAlgorithm(PsiAlgorithm.DH);
                break;
            case "ECBS":
                psiAlgorithmParameter.setAlgorithm(PsiAlgorithm.ECBS);
                break;
            case "ECDH":
                psiAlgorithmParameter.setAlgorithm(PsiAlgorithm.ECDH);
                break;
            default:
                throw new CommandLine.ParameterException(spec.commandLine(), "The input algorithm parameter is not supported");
        }
        psiAlgorithmParameter.setKeySize(keySize);

        // Create the session by calling POST /psi passing the selected psiAlgorithmParameter as body
        PsiClientSessionDTO psiClientSessionDTO = psiServerApi.postPsi(new PsiAlgorithmParameterDTO(psiAlgorithmParameter));

        // If the server sent a Bloom Filter was created less than bloomFilterMaxAge minutes ago
        // then filter the input dataset with the Bloom Filter
        if(this.bloomFilterMaxAge != null && psiClientSessionDTO.getBloomFilterDTO() != null){
            if(psiClientSessionDTO.getBloomFilterDTO().getSerializedBloomFilter() == null
                    || psiClientSessionDTO.getExpiration() == null)
                throw new CommandLine.ParameterException(spec.commandLine(), "Error reading the BloomFilterDTO in the PsiClientSessionDTO");

            if(psiClientSessionDTO.getBloomFilterDTO().getBloomFilterCreationDate().
                    isAfter(Instant.now().minus(bloomFilterMaxAge, ChronoUnit.MINUTES))){
                this.clientDataset = getFilteredDatasetWithBloomFilter(psiClientSessionDTO.getBloomFilterDTO());
                System.out.println("Dataset filtered to " + clientDataset.size() + " entries by applying the Bloom Filter");
            } else System.out.println("The Bloom Filter sent by the server is stale");
        }

        // When creating the psiClient, if a key description file is passed as parameter, we use it for keys.
        // Similarly, if enabled, set up and validate the cache
        PsiClient psiClient;
        if(!cache) {
            try{
                if (keyDescriptionFile == null)
                    psiClient = PsiClientFactory.loadSession(psiClientSessionDTO.getPsiClientSession());
                else
                    psiClient = PsiClientFactory.loadSession(psiClientSessionDTO.getPsiClientSession(), readKeyDescriptionFromFile(keyDescriptionFile));
            } catch (UnsupportedKeySizeException unsupportedKeySizeException){
                throw new CommandLine.ParameterException(spec.commandLine(), unsupportedKeySizeException.getMessage());
            }
        } else{
            RedisPsiCacheProvider redisPsiCacheProvider;
            try{
                redisPsiCacheProvider = new RedisPsiCacheProvider(cacheUrl, cachePort);
            } catch (JedisConnectionException jedisConnectionException){
                throw new CommandLine.ParameterException(spec.commandLine(), "Cannot connect to the Redis server at "+cacheUrl+":"+cachePort);
            }
            try {
                if (keyDescriptionFile == null)
                    psiClient = PsiClientFactory.loadSession(psiClientSessionDTO.getPsiClientSession(), redisPsiCacheProvider);
                else
                    psiClient = PsiClientFactory.loadSession(psiClientSessionDTO.getPsiClientSession(), readKeyDescriptionFromFile(keyDescriptionFile), redisPsiCacheProvider);
            } catch (UnsupportedKeySizeException unsupportedKeySizeException){
                throw new CommandLine.ParameterException(spec.commandLine(), unsupportedKeySizeException.getMessage());
            }
        }

        // Send the encrypted client dataset and load the returned entries (double encrypted client dataset)
        Map<Long, String> encryptedMap = psiClient.loadAndEncryptClientDataset(clientDataset);
        PsiDatasetMapDTO doubleEncryptedMapWrapped = psiServerApi.postPsiClientSet(psiClientSessionDTO.getSessionId(), new PsiDatasetMapDTO(encryptedMap));
        psiClient.loadDoubleEncryptedClientDataset(doubleEncryptedMapWrapped.getContent());

        // Read the encrypted server dataset
        int page = 0;
        int size = 100;
        PsiServerDatasetPageDTO serverDatasetPageDTO;
        do{
            serverDatasetPageDTO = psiServerApi.getPsiServerSetPage(psiClientSessionDTO.getSessionId(), page++, size);
            psiClient.loadAndProcessServerDataset(serverDatasetPageDTO.getContent());
        } while(Boolean.FALSE.equals(serverDatasetPageDTO.getLast()));

        // Compute PSI and write the result on the output file
        Set<String> psiResult = psiClient.computePsi();
        writeResultFile(psiResult);

        // Save key description used during by the execution in the outputKeyDescriptionFile
        writeKeyDescriptionToFile(psiClient.getClientKeyDescription(), outputKeyDescriptionFile);

        System.out.println("PSI computed correctly. PSI result written on "+outputFile.getPath()+". The size of the intersection is = " + psiResult.size());

        // Fill up the ProcessExecutionResult which summarizes the outcome of the execution
        ProcessExecutionResult processExecutionResult = new ProcessExecutionResult();
        processExecutionResult.psiSize = psiResult.size();
        processExecutionResult.successful = true;
        System.out.println("\nPrinting execution statistics");
        for(PsiPhaseStatistics psiPhaseStatistics : psiClient.getStatisticList()){
            System.out.println(psiPhaseStatistics);
            processExecutionResult.totalCacheHit += psiPhaseStatistics.getCacheHit();
            processExecutionResult.totalCacheMiss += psiPhaseStatistics.getCacheMiss();
        }
        System.out.println();

        return processExecutionResult;
    }

    /**
     * Returns a list of pairs of algorithms and key sizes (PsiAlgorithmParameterDTO) supported by the server for PSI calculations.
     * Code executed when passing list as the first argument (command)
     *
     * @param psiServerApi psiServerApi object which performs the API client towards the server
     * @return the number of psiAlgorithmParameter supported by the server
     */
    public int runList(PsiServerApi psiServerApi){
        List<PsiAlgorithmParameter> psiAlgorithmParameterDTOList = psiServerApi.getPsiAlgorithmParameterList().getContent();
        if(psiAlgorithmParameterDTOList.isEmpty()){
            System.out.println("The server does not support any PSI algorithm");
            return -1;
        }else{
            System.out.println("Supported algorithm-keySize pairs:");
            for(PsiAlgorithmParameter psiAlgorithmParameter : psiAlgorithmParameterDTOList){
                System.out.println(psiAlgorithmParameter);
                System.out.println(psiAlgorithmParameter.getAlgorithm().toString()+"-"+psiAlgorithmParameter.getKeySize());
            }
            return psiAlgorithmParameterDTOList.size();
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
            throw new CommandLine.ParameterException(spec.commandLine(), "Cannot parse the input dataset");
        }
        System.out.println(clientDataset.size()+ " entries loaded from dataset file");
    }

    private void writeResultFile(Set<String> set) {
        try {
            Files.write(Paths.get(outputFile.getPath()), set);
        } catch (IOException e) {
            throw new PsiDemoClientRuntimeException("Error writing the output file");
        }
    }

    private void validateServerBaseUrl(){
        // Remove any trailing slashes in the url
        while(serverBaseUrl.endsWith("/"))
            serverBaseUrl = serverBaseUrl.substring(0, serverBaseUrl.length()-1);
        try {
            new URL(serverBaseUrl);
        } catch (MalformedURLException e) {
            throw new CommandLine.ParameterException(spec.commandLine(), "The input serverBaseUrl is not a valid URL");
        }
    }

    private PsiClientKeyDescription readKeyDescriptionFromFile(File keyDescriptionFile){
        if(keyDescriptionFile == null)
            return null;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(keyDescriptionFile);
        } catch (FileNotFoundException e) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Cannot find the input key description file" +keyDescriptionFile.getPath());
        }
        Yaml yaml = new Yaml(new Constructor(PsiClientKeyDescriptionYaml.class));
        PsiClientKeyDescriptionYaml psiClientKeyDescriptionYaml = yaml.load(inputStream);
        System.out.println("Key description read from file "+keyDescriptionFile);

        try {
            return PsiClientKeyDescriptionFactory.createGenericPsiClientKeyDescription(
                    psiClientKeyDescriptionYaml.getClientPrivateExponent(),
                    psiClientKeyDescriptionYaml.getServerPublicExponent(),
                    psiClientKeyDescriptionYaml.getModulus(),
                    psiClientKeyDescriptionYaml.getEcClientPrivateD(),
                    psiClientKeyDescriptionYaml.getEcServerPublicQ());
        } catch (InvalidPsiClientKeyDescriptionException e){
            throw new CommandLine.ParameterException(spec.commandLine(), "The input key description file is invalid");
        }
    }

    private void writeKeyDescriptionToFile(PsiClientKeyDescription psiClientKeyDescription, File outputYamlFile){
        PsiClientKeyDescriptionYaml psiClientKeyDescriptionYaml = new PsiClientKeyDescriptionYaml(psiClientKeyDescription);
        Yaml yaml = new Yaml();
        String yamlString = yaml.dumpAs(psiClientKeyDescriptionYaml, Tag.MAP, DumperOptions.FlowStyle.BLOCK);
        try {
            Files.write(outputYamlFile.toPath(), yamlString.getBytes());
        } catch (IOException e) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Cannot write to the output key description file "+outputYamlFile.getPath());
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private Set<String> getFilteredDatasetWithBloomFilter(BloomFilterDTO bloomFilterDTO) {
        BloomFilter<CharSequence> bloomFilter = BloomFilterHelper.getBloomFilterFromByteArray(bloomFilterDTO.getSerializedBloomFilter());
        return BloomFilterHelper.filterSet(this.clientDataset, bloomFilter);
    }

    /**
     * Static class used to wrap the result of the compute command to enable in-depth unit testing
     */
    public static class ProcessExecutionResult {
         private boolean successful;
         private int psiSize;
         private int totalCacheHit;
         private int totalCacheMiss;

        public boolean isSuccessful() {
            return successful;
        }

        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }

        public int getPsiSize() {
            return psiSize;
        }

        public void setPsiSize(int psiSize) {
            this.psiSize = psiSize;
        }

        public int getTotalCacheHit() {
            return totalCacheHit;
        }

        public void setTotalCacheHit(int totalCacheHit) {
            this.totalCacheHit = totalCacheHit;
        }

        public int getTotalCacheMiss() {
            return totalCacheMiss;
        }

        public void setTotalCacheMiss(int totalCacheMiss) {
            this.totalCacheMiss = totalCacheMiss;
        }

        @Override
        public String toString() {
            return "ProcessExecutionResult{" +
                    "successful=" + successful +
                    ", psiSize=" + psiSize +
                    ", totalCacheHit=" + totalCacheHit +
                    ", totalCacheMiss=" + totalCacheMiss +
                    '}';
        }
    }
}
