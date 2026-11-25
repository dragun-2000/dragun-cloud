package vn.co.cake.storefront.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import vn.co.cake.storefront.entity.SaleMenuStyle;
import vn.co.cake.storefront.entity.StorefrontSettings;
import vn.co.cake.storefront.repository.StorefrontSettingsRepository;

class StorefrontSettingsServiceTest {

    private StorefrontSettingsRepository repository;
    private AtomicLong now;
    private StorefrontSettingsService service;

    @BeforeEach
    void setUp() {
        repository = mock(StorefrontSettingsRepository.class);
        now = new AtomicLong(1_000_000L);
        service = new StorefrontSettingsService(repository, now::get);
        when(repository.save(any(StorefrontSettings.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static StorefrontSettings settingsWith(SaleMenuStyle style) {
        StorefrontSettings settings = new StorefrontSettings();
        settings.setSaleMenuStyle(style);
        return settings;
    }

    @Test
    void noRowYet_showsSaleWithoutWritingOnRead() {
        when(repository.findById(StorefrontSettings.SINGLETON_ID)).thenReturn(Optional.empty());

        assertEquals(SaleMenuStyle.SALE, service.getSaleMenuStyle());
        verify(repository, never()).save(any());
    }

    @Test
    void storedBlackFriday_isReturned() {
        when(repository.findById(StorefrontSettings.SINGLETON_ID))
                .thenReturn(Optional.of(settingsWith(SaleMenuStyle.BLACK_FRIDAY)));

        assertEquals(SaleMenuStyle.BLACK_FRIDAY, service.getSaleMenuStyle());
    }

    @Test
    void nullStoredStyle_fallsBackToSale() {
        when(repository.findById(StorefrontSettings.SINGLETON_ID)).thenReturn(Optional.of(settingsWith(null)));

        assertEquals(SaleMenuStyle.SALE, service.getSaleMenuStyle());
    }

    @Test
    void repositoryFailure_fallsBackToSale() {
        when(repository.findById(StorefrontSettings.SINGLETON_ID)).thenThrow(new IllegalStateException("db down"));

        assertEquals(SaleMenuStyle.SALE, service.getSaleMenuStyle());
    }

    @Test
    void readsAreCachedUntilTtlExpires() {
        when(repository.findById(StorefrontSettings.SINGLETON_ID))
                .thenReturn(Optional.of(settingsWith(SaleMenuStyle.BLACK_FRIDAY)));

        service.getSaleMenuStyle();
        now.addAndGet(StorefrontSettingsService.CACHE_TTL_MILLIS - 1);
        service.getSaleMenuStyle();
        verify(repository, times(1)).findById(StorefrontSettings.SINGLETON_ID);

        now.addAndGet(1);
        service.getSaleMenuStyle();
        verify(repository, times(2)).findById(StorefrontSettings.SINGLETON_ID);
    }

    @Test
    void save_persistsStyleWithAuditAndRefreshesCacheImmediately() {
        when(repository.findById(StorefrontSettings.SINGLETON_ID)).thenReturn(Optional.empty());
        assertEquals(SaleMenuStyle.SALE, service.getSaleMenuStyle());

        service.saveSaleMenuStyle(SaleMenuStyle.BLACK_FRIDAY, "admin01");

        ArgumentCaptor<StorefrontSettings> saved = ArgumentCaptor.forClass(StorefrontSettings.class);
        verify(repository).save(saved.capture());
        assertEquals(StorefrontSettings.SINGLETON_ID, saved.getValue().getId());
        assertEquals(SaleMenuStyle.BLACK_FRIDAY, saved.getValue().getSaleMenuStyle());
        assertEquals("admin01", saved.getValue().getUpdater());
        assertNotNull(saved.getValue().getUpdated());

        when(repository.findById(StorefrontSettings.SINGLETON_ID)).thenReturn(Optional.of(saved.getValue()));
        assertEquals(SaleMenuStyle.BLACK_FRIDAY, service.getSaleMenuStyle());
    }

    @Test
    void save_rejectsNullStyle() {
        assertThrows(IllegalArgumentException.class, () -> service.saveSaleMenuStyle(null, "admin01"));
        verify(repository, never()).save(any());
    }

    @Test
    void parseStyle_acceptsKnownNamesOnly() {
        assertEquals(SaleMenuStyle.BLACK_FRIDAY, SaleMenuStyle.parse("BLACK_FRIDAY"));
        assertEquals(SaleMenuStyle.SALE, SaleMenuStyle.parse("SALE"));
        assertThrows(IllegalArgumentException.class, () -> SaleMenuStyle.parse("<script>"));
        assertThrows(IllegalArgumentException.class, () -> SaleMenuStyle.parse(null));
    }
}
