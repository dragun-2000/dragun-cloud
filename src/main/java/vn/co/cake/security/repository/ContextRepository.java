package vn.co.cake.security.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * ContextRepository
 */
@Repository
public class ContextRepository {

    // context key , context
    private ValueOperations<String, Object> contextValueOperations;

    private RedisTemplate<String, Object> redisTemplate;

    public ContextRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    private void init() {
        contextValueOperations = redisTemplate.opsForValue();
    }

    // save context
    public void saveContext(String id, String context, int timeout) {
        contextValueOperations.set(id, context, timeout, TimeUnit.DAYS);
    }

    // find by context key
    public Object findContextById(String id) {
        return contextValueOperations.get(id);
    }

    // delete key
    public Boolean deleteKey(String id) {
        return contextValueOperations.getOperations().delete(id);
    }

    public void saveBookmarkUrl(String key, String url) {
        contextValueOperations.set(key, url);
    }

    public Object getBookmarkUrl(String key) {
        return contextValueOperations.get(key);
    }
}
