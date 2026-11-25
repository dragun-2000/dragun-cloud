package vn.co.cake.storefront.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.co.cake.storefront.entity.StorefrontSettings;

public interface StorefrontSettingsRepository extends JpaRepository<StorefrontSettings, Short> {
}
