package vn.co.cake.vnpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.co.cake.vnpay.entity.VnpaySettings;

public interface VnpaySettingsRepository extends JpaRepository<VnpaySettings, Short> {
}
