package it.lockless.psidemoclient.dto;

import java.time.Instant;
import java.util.Arrays;

public class BloomFilterDTO {

    private byte[] serializedBloomFilter;
    private Instant bloomFilterCreationDate;

    public BloomFilterDTO() {
        // Constructor with no arguments is used by Jackson in serialization/deserialization
    }

    public byte[] getSerializedBloomFilter() {
        return serializedBloomFilter;
    }

    public void setSerializedBloomFilter(byte[] serializedBloomFilter) {
        this.serializedBloomFilter = serializedBloomFilter;
    }

    public Instant getBloomFilterCreationDate() {
        return bloomFilterCreationDate;
    }

    public void setBloomFilterCreationDate(Instant bloomFilterCreationDate) {
        this.bloomFilterCreationDate = bloomFilterCreationDate;
    }

    @Override
    public String toString() {
        return "BloomFilterDTO{" +
                "serializedBloomFilter=" + Arrays.toString(serializedBloomFilter) +
                ", bloomFilterCreationDate=" + bloomFilterCreationDate +
                '}';
    }
}
