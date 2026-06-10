package vn.co.cake.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.co.cake.payment.entity.VietQrPilotSettings;

public interface VietQrPilotSettingsRepository extends JpaRepository<VietQrPilotSettings, Short> {
}
