package it.lockless.psidemoclient;

import it.lockless.psidemoclient.cache.RedisPsiCacheProvider;
import psi.cache.PsiCacheProvider;

import java.util.function.BooleanSupplier;

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
