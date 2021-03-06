package it.lockless.psidemoclient;

import it.lockless.psidemoclient.cache.RedisPsiCacheProvider;

import java.util.function.BooleanSupplier;

/**
 * Allows running Redis-related tests only if it is available
 */
public class RedisChecker implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try{
            new RedisPsiCacheProvider("localhost", 6379);
            return true;
        }catch (Exception exception){
            return false;
        }
    }
}
