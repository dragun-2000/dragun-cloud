/**
 * AM009 - Province Management
 * Handles province active/inactive status updates
 */
(function() {
    'use strict';
    
    // Wait for jQuery to be available
    function waitForJQuery(callback) {
        if (typeof jQuery !== 'undefined' && typeof $ !== 'undefined') {
            callback();
        } else {
            setTimeout(function() { waitForJQuery(callback); }, 100);
        }
    }
    
    waitForJQuery(function() {
        $(document).ready(function() {
            
            // Helper function to get CSRF token
            function getCsrfToken() {
                const token = $('input[name="_csrf"]').val();
                if (!token) {
                    console.error('CSRF token NOT FOUND!');
                }
                return token;
            }

            // Helper function to update province status
            function updateProvinceStatus(provinceId, deleted, $element, revertCallback) {
                const csrfToken = getCsrfToken();
                if (!csrfToken) {
                    alert('Lỗi: Không tìm thấy CSRF token');
                    if (revertCallback) revertCallback();
                    if ($element) $element.prop('disabled', false);
                    return;
                }

                const params = new URLSearchParams();
                params.append('provinceId', provinceId);
                params.append('deleted', deleted);
                params.append('_csrf', csrfToken);
                
                $.ajax({
                    url: '/AM/AM009/update-status',
                    type: 'POST',
                    data: params.toString(),
                    contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
                    headers: {
                        'X-CSRF-TOKEN': csrfToken
                    },
                    success: function(response) {
                        location.reload();
                    },
                    error: function(xhr, status, error) {
                        console.error('Update error:', {
                            status: xhr.status,
                            statusText: xhr.statusText,
                            responseText: xhr.responseText,
                            error: error
                        });
                        const errorMsg = xhr.responseText || xhr.statusText || error || 'Có lỗi xảy ra';
                        alert('Cập nhật thất bại: ' + errorMsg);
                        if (revertCallback) revertCallback();
                        if ($element) $element.prop('disabled', false);
                    }
                });
            }

            // Toggle switch handler
            $(document).on('change', '.province-toggle', function() {
                const $toggle = $(this);
                const provinceId = $toggle.attr('data-province-id');
                const isActive = $toggle.is(':checked');
                const deleted = !isActive;
                
                if (!provinceId) {
                    alert('Lỗi: Không tìm thấy ID tỉnh/thành phố');
                    return;
                }
                
                $toggle.prop('disabled', true);
                updateProvinceStatus(provinceId, deleted, $toggle, function() {
                    $toggle.prop('checked', !isActive);
                });
            });

            // Action button handler
            $(document).on('click', '.province-action-btn', function(e) {
                e.preventDefault();
                e.stopPropagation();
                
                const $btn = $(this);
                const provinceId = $btn.attr('data-province-id');
                const currentDeletedAttr = $btn.attr('data-province-deleted');
                
                if (!provinceId) {
                    alert('Lỗi: Không tìm thấy ID tỉnh/thành phố');
                    return;
                }
                
                // Convert attribute value to boolean
                // Thymeleaf renders: data-province-deleted="true" or data-province-deleted="false"
                const currentDeleted = currentDeletedAttr === 'true' || currentDeletedAttr === true;
                const newDeleted = !currentDeleted;
                
                $btn.prop('disabled', true);
                updateProvinceStatus(provinceId, newDeleted, $btn);
            });

            // Toggle all buttons
            $('#toggleAllBtn').on('click', function() {
                if (confirm('Bạn có chắc muốn bật tất cả các tỉnh/thành phố?')) {
                    updateAllProvinces(false);
                }
            });

            $('#offAllBtn').on('click', function() {
                if (confirm('Bạn có chắc muốn tắt tất cả các tỉnh/thành phố? Khách hàng sẽ không thể đặt hàng.')) {
                    updateAllProvinces(true);
                }
            });

            function updateAllProvinces(deleted) {
                const csrfToken = getCsrfToken();
                if (!csrfToken) {
                    alert('Lỗi: Không tìm thấy CSRF token');
                    return;
                }
                
                $('#toggleAllBtn, #offAllBtn').prop('disabled', true);
                
                const params = new URLSearchParams();
                params.append('deleted', deleted);
                params.append('_csrf', csrfToken);
                
                $.ajax({
                    url: '/AM/AM009/update-all-status',
                    type: 'POST',
                    data: params.toString(),
                    contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
                    headers: {
                        'X-CSRF-TOKEN': csrfToken
                    },
                    success: function(response) {
                        location.reload();
                    },
                    error: function(xhr, status, error) {
                        console.error('Update all error:', {
                            status: xhr.status,
                            statusText: xhr.statusText,
                            responseText: xhr.responseText,
                            error: error
                        });
                        const errorMsg = xhr.responseText || xhr.statusText || error || 'Có lỗi xảy ra';
                        alert('Cập nhật thất bại: ' + errorMsg);
                        $('#toggleAllBtn, #offAllBtn').prop('disabled', false);
                    }
                });
            }
        });
    });
})();
