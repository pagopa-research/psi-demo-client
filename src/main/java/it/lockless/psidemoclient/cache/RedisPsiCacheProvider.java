package it.lockless.psidemoclient.cache;

import psi.cache.PsiCacheProvider;
import redis.clients.jedis.Jedis;

import java.util.Optional;

// Create redis docker: docker run --name redis -p 6379:6379 -d redis
// Connect to cli: docker exec -it redis redis-cli
public class RedisPsiCacheProvider implements PsiCacheProvider {

    private final Jedis jedis;

    public RedisPsiCacheProvider(String host, int port) {
        this.jedis = new Jedis(host,port);
    }

    @Override
    public Optional<String> get(String s) {
        String cachedResponse = jedis.get(s);
        if(cachedResponse == null)
            return Optional.empty();
        else return Optional.of(cachedResponse);
    }

    @Override
    public void put(String s, String s1) {
        if(jedis.setnx(s, s1) == 0)
            throw new RedisKeyAlreadyWrittenException();
    }
}
