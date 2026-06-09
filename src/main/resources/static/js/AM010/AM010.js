$(document).on('click', '.log-detail-btn', function (e) {
    e.preventDefault();
    e.stopPropagation();

    var $btn = $(this);
    var groupKey = $btn.attr('data-group-key') || $btn.data('groupKey');
    if (groupKey) {
        $.get('/AM/AM010/order-logs', { groupKey: groupKey }, function (data) {
            $('#log-detail-body').text(JSON.stringify(data, null, 2));
            $('#logDetailModal').modal('show');
        }).fail(function () {
            alert('Không tải được chi tiết log');
        });
        return;
    }

    var logId = $btn.attr('data-detail-log-id') || $btn.data('detailLogId');
    if (logId) {
        $.get('/AM/AM010/detail/' + logId, function (data) {
            $('#log-detail-body').text(JSON.stringify(data, null, 2));
            $('#logDetailModal').modal('show');
        }).fail(function () {
            alert('Không tải được chi tiết log');
        });
        return;
    }

    alert('Thiếu mã đơn');
});
