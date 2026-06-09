package vn.co.cake.security.repository;

import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory store for security context backup and admin bookmark URLs.
 */
@Repository
public class ContextRepository {

    private final ConcurrentMap<String, Object> values = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> contextExpiresAtMs = new ConcurrentHashMap<>();

    public void saveContext(String id, String context, int timeoutDays) {
        values.put(id, context);
        long expiresAt = System.currentTimeMillis() + (timeoutDays * 24L * 60L * 60L * 1000L);
        contextExpiresAtMs.put(id, expiresAt);
    }

    public Object findContextById(String id) {
        Long expiresAt = contextExpiresAtMs.get(id);
        if (expiresAt != null && System.currentTimeMillis() > expiresAt) {
            deleteKey(id);
            return null;
        }
        return values.get(id);
    }

    public Boolean deleteKey(String id) {
        contextExpiresAtMs.remove(id);
        return values.remove(id) != null;
    }

    public void saveBookmarkUrl(String key, String url) {
        values.put(key, url);
    }

    public Object getBookmarkUrl(String key) {
        return values.get(key);
    }
}
