package it.lockless.psidemoclient.util;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import psi.exception.CustomRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class BloomFilterHelper {

    public static BloomFilter<CharSequence> getBloomFilterFromByteArray(byte[] bloomFilterByteArray){
        InputStream inputStream = new ByteArrayInputStream(bloomFilterByteArray);
        try {
            return  BloomFilter.readFrom(inputStream, Funnels.stringFunnel(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CustomRuntimeException("Cannot deserialize the Bloom Filter");
        }
    }

    public static Set<String> filterSet(Set<String> inputDataset, BloomFilter<CharSequence> bloomFilter){
        Set<String> resultSet = new HashSet<>();
        for(String s : inputDataset){
            if(bloomFilter.mightContain(s))
                resultSet.add(s);
        }
        return resultSet;
    }
}
