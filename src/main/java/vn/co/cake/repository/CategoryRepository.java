package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Category findFirstByName(String name);
    List<Category> findAllByDeletedIsFalse();
}
