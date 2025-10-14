(function () {
    'use strict';

    // textarea event handler to add support for maxlength attribute
    $(document).on('input keyup', 'textarea[maxlength]', function (e) {
        // maxlength attribute does not in IE prior to IE10
        // http://stackoverflow.com/q/4717168/740639
        var $this = $(this);
        var maxlength = $this.attr('maxlength');
        if (!!maxlength) {
            var text = $this.val();

            if (text.length > maxlength) {
                // truncate excess text (in the case of a paste)
                $this.val(text.substring(0, maxlength));
                e.preventDefault();
            }
        }
    });

    window.addEventListener('load', function () {
        // カスタムブートストラップ検証スタイルを適用するすべてのフォームを取得
        var forms = document.getElementsByClassName('needs-validation');
        var isValidTime = true;
        // ループして帰順を防ぐ
        var validation = Array.prototype.filter.call(forms, function (form) {
            form.addEventListener('submit', function (event) {
                if (!isValidTime || form.checkValidity() === false) {
                    event.preventDefault();
                    event.stopPropagation();
                } else {
                    var confirm = form.elements['confirm-submit'];
                    if (typeof confirm !== 'undefined') {
                        if (typeof confirmMsgId !== 'undefined') {
                            var msg = getMsg(confirmMsgId);
                            var buttonCancelTitle;
                            var buttonOKTitle;
                            if (typeof buttonCancel !== 'undefined') {
                                buttonCancelTitle = buttonCancel;
                            }
                            if (typeof buttonOK !== 'undefined') {
                                buttonOKTitle = buttonOK;
                            }
                            modalConfirm({
                                message: msg,
                                buttonCancel: buttonCancelTitle,
                                buttonOK: buttonOKTitle,
                                default: 'buttonCancel',
                                done: function(body) {
                                    form.submit();
                                }
                            });
                            event.preventDefault();
                            event.stopPropagation();
                        }
                    }
                }
                form.classList.add('was-validated');
            }, false);
        });

    }, false);
})();
