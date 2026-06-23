package com.digitrans.erp_service.security;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InMemoryUserRepository {
    private final ConcurrentMap<String, UserEntity> users = new ConcurrentHashMap<>();

    public void save(UserEntity user) {
        users.put(user.getUsername(), user);
    }

    public UserEntity findByUsername(String username) {
        return users.get(username);
    }
}
