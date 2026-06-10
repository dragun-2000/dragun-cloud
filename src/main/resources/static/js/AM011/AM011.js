$(function () {

    'use strict';



    var $hiddenVisible = $('#visibleToAll');

    var $toggleOn = $('#vietqr-toggle-on');

    var $toggleOff = $('#vietqr-toggle-off');

    var $hint = $('#vietqr-toggle-hint');



    function csrfHeaders() {

        var token = $('input[name="_csrf"]').val();

        return token ? { 'X-CSRF-TOKEN': token } : {};

    }



    function isHtmlResponse(text) {

        if (!text) {

            return false;

        }

        var trimmed = String(text).trim();

        return trimmed.indexOf('<!DOCTYPE') === 0 || trimmed.indexOf('<html') === 0;

    }



    function setVisibleToAll(visible) {

        $hiddenVisible.val(visible ? 'true' : 'false');

        $toggleOn.toggleClass('is-active', visible);

        $toggleOff.toggleClass('is-active', !visible);

        if (visible) {

            $hint.text('Bật — Tất cả user được dùng VietQR.');

            $('#pilot-password-group').hide();

        } else {

            $hint.text('Tắt — Chỉ những người có mật khẩu pilot mới dùng được.');

            $('#pilot-password-group').show();

        }

    }



    function getVisibleToAll() {

        return $hiddenVisible.val() === 'true';

    }



    $toggleOn.on('click', function () {

        setVisibleToAll(true);

    });



    $toggleOff.on('click', function () {

        setVisibleToAll(false);

    });



    setVisibleToAll(getVisibleToAll());



    $('#btn-save-vietqr-settings').on('click', function () {

        var $status = $('#save-status');

        var visibleToAll = getVisibleToAll();

        var pilotPassword = $('#pilotPassword').val();



        $status.removeClass('text-success text-danger').text('Đang lưu...');



        $.ajax({

            url: '/AM/AM011/save',

            type: 'POST',

            contentType: 'application/json; charset=UTF-8',

            dataType: 'text',

            headers: csrfHeaders(),

            data: JSON.stringify({

                visibleToAll: visibleToAll,

                pilotPassword: pilotPassword || ''

            }),

            success: function (msg) {

                if (isHtmlResponse(msg)) {

                    $status.addClass('text-danger').text('Phiên hết hạn hoặc lỗi bảo mật. Vui lòng tải lại trang.');

                    return;

                }

                $status.addClass('text-success').text(msg || 'Đã lưu');

                $('#pilotPassword').val('');

            },

            error: function (xhr) {

                var msg = xhr.responseText || 'Lỗi lưu cấu hình';

                if (isHtmlResponse(msg)) {

                    msg = xhr.status === 403

                        ? 'Không có quyền hoặc CSRF không hợp lệ. Vui lòng tải lại trang.'

                        : 'Lỗi máy chủ. Vui lòng thử lại.';

                }

                $status.addClass('text-danger').text(msg);

            }

        });

    });

});

