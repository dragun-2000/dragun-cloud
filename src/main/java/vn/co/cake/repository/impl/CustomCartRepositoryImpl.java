package vn.co.cake.repository.impl;

import org.springframework.stereotype.Repository;
import vn.co.cake.repository.BaseRepository;
import vn.co.cake.repository.CustomCartRepository;

import javax.persistence.EntityManager;

@Repository
public class CustomCartRepositoryImpl extends BaseRepository implements CustomCartRepository {
    private final EntityManager entityManager;

    public CustomCartRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


}
