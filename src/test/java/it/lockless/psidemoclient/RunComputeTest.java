package it.lockless.psidemoclient;

import it.lockless.psidemoclient.client.PsiServerApi;
import it.lockless.psidemoclient.dto.PsiClientSessionDTO;
import it.lockless.psidemoclient.dto.PsiDatasetMapDTO;
import it.lockless.psidemoclient.dto.PsiServerDatasetPageDTO;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psi.PsiServerFactory;
import psi.exception.UnsupportedKeySizeException;
import psi.model.PsiAlgorithm;
import psi.model.PsiAlgorithmParameter;
import psi.model.PsiClientSession;
import psi.model.PsiServerSession;
import psi.server.PsiServer;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunComputeTest {

    PsiClientCLI psiClientCLI;

    @Mock
    PsiServerApi psiServerApi;

    Set<String> serverDataset;

    private PsiServerSession mockPostSession(PsiAlgorithmParameter psiAlgorithmParameter) throws UnsupportedKeySizeException {
       PsiServerSession psiServerSession = PsiServerFactory.initSession(psiAlgorithmParameter);
       PsiClientSession psiClientSession = PsiClientSession.getFromServerSession(psiServerSession);
       PsiClientSessionDTO psiClientSessionDTO = new PsiClientSessionDTO();
        psiClientSessionDTO.setPsiClientSession(psiClientSession);
        psiClientSessionDTO.setSessionId(1L);
        psiClientSessionDTO.setExpiration(Instant.now().plus(1, ChronoUnit.HOURS));
        when(psiServerApi.postPsi(any())).thenReturn(psiClientSessionDTO);
        return psiServerSession;
    }

    private PsiDatasetMapDTO encryptClientDataset(PsiServerSession psiServerSession, PsiDatasetMapDTO psiDatasetMapDTO) throws UnsupportedKeySizeException {
        PsiServer psiServer = PsiServerFactory.loadSession(psiServerSession);
        Map<Long, String> doubleEncryptedMap = psiServer.encryptDatasetMap(psiDatasetMapDTO.getContent());
        return new PsiDatasetMapDTO(doubleEncryptedMap);
    }

    private void mockPostPsiClientSet(PsiServerSession psiServerSession){
        when(psiServerApi.postPsiClientSet(any(), any())).thenAnswer(
                invocation -> encryptClientDataset(psiServerSession, invocation.getArgument(1)));
    }

    private void mockGetPsiServerSetPage(PsiServerSession psiServerSession) throws UnsupportedKeySizeException {
        PsiServer psiServer = PsiServerFactory.loadSession(psiServerSession);
        Set<String> encryptedDataset = psiServer.encryptDataset(serverDataset);

        Set<String> encryptedDatasetPage1 = new HashSet<>();
        Set<String> encryptedDatasetPage2 = new HashSet<>();
        int cont = 0;
        for(String entry : encryptedDataset){
            if(cont < 10)
                encryptedDatasetPage1.add(entry);
            else encryptedDatasetPage2.add(entry);
            cont++;
        }

        PsiServerDatasetPageDTO psiServerDatasetPageDTO1 = new PsiServerDatasetPageDTO();
        psiServerDatasetPageDTO1.setContent(encryptedDatasetPage1);
        psiServerDatasetPageDTO1.setPage(0);
        psiServerDatasetPageDTO1.setSize(100);
        psiServerDatasetPageDTO1.setEntries(encryptedDatasetPage1.size());
        psiServerDatasetPageDTO1.setTotalPages(2);
        psiServerDatasetPageDTO1.setLast(false);
        when(psiServerApi.getPsiServerSetPage(1L, 0,    100)).thenReturn(psiServerDatasetPageDTO1);

        PsiServerDatasetPageDTO psiServerDatasetPageDTO2 = new PsiServerDatasetPageDTO();
        psiServerDatasetPageDTO2.setContent(encryptedDatasetPage2);
        psiServerDatasetPageDTO2.setPage(1);
        psiServerDatasetPageDTO2.setSize(100);
        psiServerDatasetPageDTO2.setEntries(encryptedDatasetPage1.size());
        psiServerDatasetPageDTO2.setTotalPages(2);
        psiServerDatasetPageDTO2.setLast(true);
        when(psiServerApi.getPsiServerSetPage(1L, 1,    100)).thenReturn(psiServerDatasetPageDTO2);
    }

    private void loadServerDataset(){
        Set<String> localServerDataset = new HashSet<>();
        for(long i = 0; i < 10; i ++){
            localServerDataset.add("COMMON-"+i);
        }

        for(long i = 0; i < 10; i ++){
            localServerDataset.add("SERVER"+i);
        }
        this.serverDataset = localServerDataset;
    }

    @BeforeEach
    void setup() throws IllegalAccessException {
        this.psiClientCLI = new PsiClientCLI();
        FieldUtils.writeField(psiClientCLI,"serverBaseUrl", "https://mock.com", true);
        FieldUtils.writeField(psiClientCLI,"inputDataset", new File("dummy-dataset.txt"), true);
        FieldUtils.writeField(psiClientCLI,"outputFile", new File("out.txt"), true);
        FieldUtils.writeField(psiClientCLI,"outputKeyDescriptionFile", new File("key.yaml"), true);
        loadServerDataset();
    }

    private void setupMock(PsiAlgorithm psiAlgorithm, int keySize ) throws IllegalAccessException, UnsupportedKeySizeException {
        FieldUtils.writeField(psiClientCLI,"algorithm", psiAlgorithm.toString(), true);
        FieldUtils.writeField(psiClientCLI,"keySize", keySize, true);
        PsiAlgorithmParameter psiAlgorithmParameter = new PsiAlgorithmParameter();
        psiAlgorithmParameter.setAlgorithm(psiAlgorithm);
        psiAlgorithmParameter.setKeySize(keySize);
        PsiServerSession psiServerSession = mockPostSession(psiAlgorithmParameter);
        mockPostPsiClientSet(psiServerSession);
        mockGetPsiServerSetPage(psiServerSession);
    }

    private void setupRedis() throws IllegalAccessException {
        FieldUtils.writeField(psiClientCLI,"cache", true, true);
        FieldUtils.writeField(psiClientCLI,"cacheUrl", "localhost", true);
        FieldUtils.writeField(psiClientCLI,"cachePort", 6379, true);
    }

    @Test
    void runBsComputeBasic() throws IllegalAccessException, UnsupportedKeySizeException {
        setupMock(PsiAlgorithm.BS, 2048);
        assertEquals(5, psiClientCLI.runCompute(psiServerApi).getPsiSize());
    }

    @Test
    void runDhComputeBasic() throws IllegalAccessException, UnsupportedKeySizeException {
        setupMock(PsiAlgorithm.DH, 2048);
        assertEquals(5, psiClientCLI.runCompute(psiServerApi).getPsiSize());
    }

    @Test
    void runEcbsComputeBasic() throws IllegalAccessException, UnsupportedKeySizeException {
        setupMock(PsiAlgorithm.ECBS, 256);
        assertEquals(5, psiClientCLI.runCompute(psiServerApi).getPsiSize());
    }

    @Test
    void runEcdhComputeBasic() throws IllegalAccessException, UnsupportedKeySizeException {
        setupMock(PsiAlgorithm.ECDH, 256);
        assertEquals(5, psiClientCLI.runCompute(psiServerApi).getPsiSize());
    }

    @Test
    @Tag("redis") // Expects a Redis server running at localhost:6379
    void runBsComputeCache() throws IllegalAccessException, UnsupportedKeySizeException {
        Assumptions.assumeTrue(new RedisChecker(), "Redis is not available at localhost:6379. Skipping test");
        setupMock(PsiAlgorithm.BS, 2048);
        setupRedis();

        PsiClientCLI.ProcessExecutionResult firstPsi = psiClientCLI.runCompute(psiServerApi);
        PsiClientCLI.ProcessExecutionResult secondPsi = psiClientCLI.runCompute(psiServerApi);
        assertEquals(firstPsi.getPsiSize(), secondPsi.getPsiSize());
        assertEquals(5, secondPsi.getPsiSize());
        assertTrue(secondPsi.getTotalCacheHit() > 0);
        assertEquals(20, secondPsi.getTotalCacheHit());
    }


    @Test
    @Tag("redis") // Expects a Redis server running at localhost:6379
    void runDhComputeCache() throws IllegalAccessException, UnsupportedKeySizeException {
        Assumptions.assumeTrue(new RedisChecker(), "Redis is not available at localhost:6379. Skipping test");
        setupMock(PsiAlgorithm.DH, 2048);
        setupRedis();

        // For DH and ECDH, we must save the key and re-use it in the next execution to get cache hits
        File file = new File("dhKey.yaml");
        FieldUtils.writeField(psiClientCLI,"outputKeyDescriptionFile", file, true);
        PsiClientCLI.ProcessExecutionResult firstPsi = psiClientCLI.runCompute(psiServerApi);
        FieldUtils.writeField(psiClientCLI,"keyDescriptionFile", file, true);
        PsiClientCLI.ProcessExecutionResult secondPsi = psiClientCLI.runCompute(psiServerApi);
        assertEquals(firstPsi.getPsiSize(), secondPsi.getPsiSize());
        assertEquals(5, secondPsi.getPsiSize());
        assertTrue(secondPsi.getTotalCacheHit() > 0);
        assertEquals(30, secondPsi.getTotalCacheHit());
        file.deleteOnExit();
    }

    @Test
    @Tag("redis") // Expects a Redis server running at localhost:6379
    void runEcbsComputeCache() throws IllegalAccessException, UnsupportedKeySizeException {
        Assumptions.assumeTrue(new RedisChecker(), "Redis is not available at localhost:6379. Skipping test");
        setupMock(PsiAlgorithm.ECBS, 256);
        setupRedis();

        PsiClientCLI.ProcessExecutionResult firstPsi = psiClientCLI.runCompute(psiServerApi);
        PsiClientCLI.ProcessExecutionResult secondPsi = psiClientCLI.runCompute(psiServerApi);
        assertEquals(firstPsi.getPsiSize(), secondPsi.getPsiSize());
        assertEquals(5, secondPsi.getPsiSize());
        assertTrue(secondPsi.getTotalCacheHit() > 0);
        assertEquals(20, secondPsi.getTotalCacheHit());
    }


    @Test
    @Tag("redis") // Expects a Redis server running at localhost:6379
    void runEcdhComputeCache() throws IllegalAccessException, UnsupportedKeySizeException {
        Assumptions.assumeTrue(new RedisChecker(), "Redis is not available at localhost:6379. Skipping test");
        setupMock(PsiAlgorithm.ECDH, 256);
        setupRedis();

        // For DH and ECDH, we must save the key and re-use it in the next execution to get cache hits
        File file = new File("ecdhKey.yaml");
        FieldUtils.writeField(psiClientCLI,"outputKeyDescriptionFile", file, true);

        PsiClientCLI.ProcessExecutionResult firstPsi = psiClientCLI.runCompute(psiServerApi);
        FieldUtils.writeField(psiClientCLI,"keyDescriptionFile", file, true);
        PsiClientCLI.ProcessExecutionResult secondPsi = psiClientCLI.runCompute(psiServerApi);
        assertEquals(firstPsi.getPsiSize(), secondPsi.getPsiSize());
        assertEquals(5, secondPsi.getPsiSize());
        assertTrue(secondPsi.getTotalCacheHit() > 0);
        assertEquals(30, secondPsi.getTotalCacheHit());
        file.deleteOnExit();
    }
}
