package it.lockless.psidemoclient.cache;

/**
 Exception thrown whenever the user is attempting to save a key that already exist, which is
 a behavior not admitted by the PsiCacheProvider interface offered by the PSI SDK.
 */
public class RedisKeyAlreadyWrittenException extends RuntimeException{

    public RedisKeyAlreadyWrittenException() {
        super("The key you are attempting to save already exists in the cache. This behavior is not admissible for implementations of PsiCacheProvider");
    }
}
