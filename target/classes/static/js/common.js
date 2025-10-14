var modalDone = false;
var clickedOK = false;

/* --------------------------------------------------------------------------------------------------
modalConfirm({
  message: ,							// メッセージ文字列
  buttonCancel: ,						// キャンセルボタン文字列
  buttonOK: ,							// OKボタン文字列
  default: ,							// デフォルトボタン 'buttonCancel', 'buttonOK'
     done: function(body) {},			// OKボタン押下時のコールバック
   cancel: function() {}				// キャンセルボタン押下時のコールバック
});
-------------------------------------------------------------------------------------------------- */
function modalConfirm(arg) {

    // modalConfirmの存在チェック
    if (document.getElementById("modalConfirm") == null) {

        /* 無なければ追加 */
        $('body').append('\
        <div class="modal fade" id="modalConfirm" tabindex="-1" role="dialog" aria-labelledby="modalConfirmLabel" aria-hidden="true">\
            <div class="modal-dialog modal-dialog-centered w-370" role="document">\
                <div class="modal-content">\
                    <div class="modal-body text-center" id="confirmMessage"></div>\
                    <div class="modal-footer justify-content-center border-top-0">\
                        <button type="button" class="btn btn-primary btn-80 mr-4" id="confirmButtonOK">はい</button>\
                        <button type="button" class="btn btn-secondary btn-80" id="confirmButtonCancel" data-dismiss="modal" aria-label="Close">いいえ</button>\
                    </div>\
                </div>\
            </div>\
        </div>\
        ');
    }

    // イベントクリア
    $('#modalConfirm').off('show.bs.modal');
    $('#modalConfirm').off('shown.bs.modal');
    $('#modalConfirm').off('hidden.bs.modal');
    $('#modalConfirm').off('click', '#confirmButtonCancel');
    $('#modalConfirm').off('click', '#confirmButtonOK');
    modalDone = false;
    clickedOK = false;

    /* 引数を設定 */
    $('#modalConfirm').on('show.bs.modal', function (event) {
        var modal = $(this);
        if (arg.message) {
            modal.find('#confirmMessage').html(arg.message);
        }
        if (arg.buttonCancel) {
            modal.find('#confirmButtonCancel').text(arg.buttonCancel);
        }
        if (arg.buttonOK) {
            modal.find('#confirmButtonOK').text(arg.buttonOK);
        }
    });

    // ダイアログ表示直後にフォーカスを設定する
    $('#modalConfirm').on('shown.bs.modal', function (event) {
        if (arg.default == 'buttonCancel') {
            $(this).find('#confirmButtonCancel').focus();
        } else {
            $(this).find('#confirmButtonOK').focus();
        }
    });

    // ダイアログを表示
    $('#modalConfirm').modal();

    $('#modalConfirm').on('click', '#confirmButtonCancel', function () {
        if (arg.cancel) {
            arg.cancel();
        }
        $('#modalConfirm').modal('hide');
    });

    $('#modalConfirm').on('click', '#confirmButtonOK', function () {
        if (arg.done) {
            arg.done($('#modalConfirm').find('#confirmMessage'));
        }
        clickedOK = true;
        $('#modalConfirm').modal('hide');
    });

    $('#modalConfirm').on('hidden.bs.modal', function (event) {
        if (arg.cancel) {
            arg.cancel();
        }
        modalDone = true;
    });

}

/**
 * Reset data form
 * @param form
 */
function resetForm(form) {
    if (form !== undefined) {
        $(form).removeClass('was-validated');
        form.reset();
    }
}

$(function () {
    /**
     * @Close_modal
     * Reset form
     */
    $('.modal').on('hidden.bs.modal', function () {
        resetForm($(this).find('form')[0]);
        $(this).find('#confirm-submit').attr('disabled', true);
        $(this).find('#send-mail').attr('disabled', true);
        $(this).find('.cc-item').each(function () {
            $(this).remove();
        });
        $(this).find('.default-cc-item td:first-child').each(function () {
            $(this).html('');
        });
        $(this).find('.selectpicker').selectpicker("refresh");
    });
});

function back(){
    history.back();
}

