package vn.co.cake.vnpay.service;

import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.co.cake.vnpay.entity.VnpaySettings;
import vn.co.cake.vnpay.repository.VnpaySettingsRepository;

/** Cấu hình admin bật/tắt VNPAY trên storefront (độc lập env {@code vnpay.enabled}). */
@Service
public class VnpaySettingsService {

    private final VnpaySettingsRepository settingsRepository;

    public VnpaySettingsService(VnpaySettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Transactional(readOnly = true)
    public VnpaySettings getSettings() {
        return settingsRepository.findById(VnpaySettings.SINGLETON_ID)
                .orElseGet(this::createDefaultSettings);
    }

    @Transactional(readOnly = true)
    public boolean isFeatureEnabled() {
        return getSettings().isEnabled();
    }

    @Transactional
    public VnpaySettings saveEnabled(boolean enabled, String updater) {
        VnpaySettings settings = getSettings();
        settings.setEnabled(enabled);
        settings.setUpdated(new Date());
        settings.setUpdater(updater);
        return settingsRepository.save(settings);
    }

    private VnpaySettings createDefaultSettings() {
        VnpaySettings settings = new VnpaySettings();
        settings.setId(VnpaySettings.SINGLETON_ID);
        settings.setEnabled(true);
        settings.setVisibleToAll(false);
        return settingsRepository.save(settings);
    }
}
