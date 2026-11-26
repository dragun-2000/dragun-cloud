package vn.co.cake.storefront.service;

import java.util.Date;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.storefront.entity.SaleMenuStyle;
import vn.co.cake.storefront.entity.StorefrontSettings;
import vn.co.cake.storefront.repository.StorefrontSettingsRepository;

/** Storefront display settings, edited on AM011. */
@Slf4j
@Service
public class StorefrontSettingsService {

    /** The header renders on every page; another app instance picks up a change within this window. */
    static final long CACHE_TTL_MILLIS = 60_000L;

    private final StorefrontSettingsRepository settingsRepository;
    private final LongSupplier clock;

    private volatile CachedStyle cache;

    @Autowired
    public StorefrontSettingsService(StorefrontSettingsRepository settingsRepository) {
        this(settingsRepository, System::currentTimeMillis);
    }

    StorefrontSettingsService(StorefrontSettingsRepository settingsRepository, LongSupplier clock) {
        this.settingsRepository = settingsRepository;
        this.clock = clock;
    }

    public SaleMenuStyle getSaleMenuStyle() {
        long now = clock.getAsLong();
        CachedStyle current = cache;
        if (current != null && now - current.loadedAt < CACHE_TTL_MILLIS) {
            return current.style;
        }
        SaleMenuStyle style = loadSaleMenuStyle();
        cache = new CachedStyle(style, now);
        return style;
    }

    @Transactional(readOnly = true)
    public StorefrontSettings getSettings() {
        return settingsRepository.findById(StorefrontSettings.SINGLETON_ID).orElseGet(StorefrontSettings::new);
    }

    @Transactional
    public StorefrontSettings saveSaleMenuStyle(SaleMenuStyle style, String updater) {
        if (style == null) {
            throw new IllegalArgumentException("Kiểu hiển thị menu SALE không hợp lệ");
        }
        StorefrontSettings settings = getSettings();
        settings.setId(StorefrontSettings.SINGLETON_ID);
        settings.setSaleMenuStyle(style);
        settings.setUpdated(new Date());
        settings.setUpdater(updater);
        StorefrontSettings saved = settingsRepository.save(settings);
        cache = null;
        return saved;
    }

    private SaleMenuStyle loadSaleMenuStyle() {
        try {
            SaleMenuStyle style = getSettings().getSaleMenuStyle();
            return style != null ? style : SaleMenuStyle.SALE;
        } catch (RuntimeException ex) {
            log.warn("Storefront settings lookup failed, showing SALE: {}", ex.getMessage());
            return SaleMenuStyle.SALE;
        }
    }

    private static final class CachedStyle {
        private final SaleMenuStyle style;
        private final long loadedAt;

        private CachedStyle(SaleMenuStyle style, long loadedAt) {
            this.style = style;
            this.loadedAt = loadedAt;
        }
    }
}
