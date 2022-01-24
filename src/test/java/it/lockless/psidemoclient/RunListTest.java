package it.lockless.psidemoclient;

import it.lockless.psidemoclient.client.PsiServerApi;
import it.lockless.psidemoclient.dto.PsiAlgorithmParameterDTO;
import it.lockless.psidemoclient.dto.PsiAlgorithmParameterListDTO;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import psi.model.PsiAlgorithm;
import psi.model.PsiAlgorithmParameter;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RunListTest {

    PsiClientCLI psiClientCLI;

    @Mock
    PsiServerApi psiServerApi;

    @BeforeEach
    void setup() throws IllegalAccessException {
        this.psiClientCLI = new PsiClientCLI();
        FieldUtils.writeField(psiClientCLI,"serverBaseUrl", "https://mock.com", true);

        PsiAlgorithmParameter psiAlgorithmParameter2048 = new PsiAlgorithmParameter();
        psiAlgorithmParameter2048.setAlgorithm(PsiAlgorithm.BS);
        psiAlgorithmParameter2048.setKeySize(2048);
        PsiAlgorithmParameterDTO psiAlgorithmParameter2048DTO = new PsiAlgorithmParameterDTO(psiAlgorithmParameter2048);

        PsiAlgorithmParameter psiAlgorithmParameter4096 = new PsiAlgorithmParameter();
        psiAlgorithmParameter4096.setAlgorithm(PsiAlgorithm.BS);
        psiAlgorithmParameter4096.setKeySize(4096);
        PsiAlgorithmParameterDTO psiAlgorithmParameter4096DTO = new PsiAlgorithmParameterDTO(psiAlgorithmParameter4096);

        List<PsiAlgorithmParameterDTO> psiAlgorithmParameterDTOList = new LinkedList<>();
        psiAlgorithmParameterDTOList.add(psiAlgorithmParameter2048DTO);
        psiAlgorithmParameterDTOList.add(psiAlgorithmParameter4096DTO);

        PsiAlgorithmParameterListDTO psiAlgorithmParameterListDTO = new PsiAlgorithmParameterListDTO(psiAlgorithmParameterDTOList);
        when(psiServerApi.getPsiAlgorithmParameterList()).thenReturn(psiAlgorithmParameterListDTO);
    }

    @Test
    void runList(){
        assertTrue(psiClientCLI.runList(psiServerApi));
    }
}
