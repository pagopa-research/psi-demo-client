package it.lockless.psidemoclient.util;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import psi.exception.CustomRuntimeException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class BloomFilterHelper {

    private BloomFilterHelper() {
    }

    /**
     * Creates a BloomFilter object from its serialized representation
     * @param bloomFilterByteArray array of bytes that represent the serialized Bloom Filter
     * @return BloomFilter<CharSequence> object which can be used to filter the client dataset
     */
    public static BloomFilter<CharSequence> getBloomFilterFromByteArray(byte[] bloomFilterByteArray){
        InputStream inputStream = new ByteArrayInputStream(bloomFilterByteArray);
        try {
            return  BloomFilter.readFrom(inputStream, Funnels.stringFunnel(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CustomRuntimeException("Cannot deserialize the Bloom Filter");
        }
    }

    /**
     * Filters the client dataset with the input Bloom Filter
     *
     * @param inputDataset the client dataset
     * @param bloomFilter the Bloom Filter received from the server
     * @return a filtered client dataset which only contains the entries that are likely to also be part of
     * the server dataset. Due to Bloom Filter semantics, we are sure that all the entries of the client dataset
     * which are not in the result, are also not part of the server dataset.
     */
    public static Set<String> filterSet(Set<String> inputDataset, BloomFilter<CharSequence> bloomFilter){
        Set<String> resultSet = new HashSet<>();
        for(String s : inputDataset){
            if(bloomFilter.mightContain(s))
                resultSet.add(s);
        }
        return resultSet;
    }
}
