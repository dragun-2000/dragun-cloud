function openDeleteOneProductModal(id) {
    $('#delete-one-product-modal').modal('show');
    $('#product-id').val(id);
}

function openRestoreOneProductModal(id) {
    $('#restore-one-product-modal').modal('show');
    $('#restore-product-id').val(id);
}

function deleteProduct() {
    const id = $('#product-id').val();
    $.ajax({
        type: 'POST',
        contentType: "application/json",
        data: JSON.stringify(id),
        url: '/AM/AM003/deleted',
        headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
        success: function (data) {
            $('#delete-product-success').modal('show');
            $('#delete-one-product-modal').modal('hide');
        },
        error: function () {
        }
    });
}

function restoreProduct() {
    const id = $('#restore-product-id').val();
    $.ajax({
        type: 'POST',
        contentType: "application/json",
        data: JSON.stringify(id),
        url: '/AM/AM003/restore',
        headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
        success: function (data) {
            $('#restore-product-success').modal('show');
            $('#restore-one-product-modal').modal('hide');
        },
        error: function () {
        }
    });
}

function syncProducts() {
    $.ajax({
        type: 'POST',
        contentType: "application/json",
        url: '/api/v2/pancake/sync-products',
        headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
        success: function (data) {
            console.log(data)
            $('#sync-product-success').modal('show');
            $('#sync-product-modal').modal('hide');
        },
        error: function () {
        }
    });
}

function openSetting() {
    const name = $('#shipping-name').val();
    const fee = $('#shipping-fee').val();
    
    $('#shippingName').val(name);
    $('#shippingFee').val(fee);
    $('#open-setting-shipping').modal('show');
}

function updateSetting() {
    const name = $('#shippingName').val();
    const shippingFee = $('#shippingFee').val();
    const data = {name: name, shippingFee: shippingFee};
    $.ajax({
        type: 'POST',
        contentType: "application/json",
        data: JSON.stringify(data),
        url: '/AM/AM003/settings',
        headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
        success: function (data) {
            $('#setting-success').modal('show');
            $('#open-setting-shipping').modal('hide');
        },
        error: function () {
        }
    });
}

function openSettingDiscount() {
    const name = $('#discount-name').val();
    const discount = $('#discount-percent').val();
    const startDate = $('#discount-start').val();
    const endDate = $('#discount-end').val();
    
    $('#discountName').val(name);
    $('#discountPercent').val(discount);
    $('#startDate').val(startDate);
    $('#endDate').val(endDate);
    $('#open-setting-discount').modal('show');
}

function updateSettingDiscount() {
    const name = $('#discountName').val();
    const discount = $('#discountPercent').val();
    const startDate = $('#startDate').val();  
    const endDate = $('#endDate').val();  
    
    const data = {
        name: name,
        discountPercent: discount,
        startDate: startDate,
        endDate: endDate
    };
    
    $.ajax({
        type: 'POST',
        contentType: "application/json",
        data: JSON.stringify(data),
        url: '/AM/AM003/settings-discount',
        headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
        success: function (data) {
            $('#setting-discount-success').modal('show');
            $('#open-setting-discount').modal('hide');
        },
        error: function () {
            // Có thể thêm xử lý lỗi ở đây nếu cần
        }
    });
}

function validateDates() {
    const startDate = document.getElementById('startDate').value;
    const endDate = document.getElementById('endDate').value;
    const errorMessage = document.getElementById('error-message-date');
    const btnOkDiscount = document.getElementById('btn-ok-discount');

    // Kiểm tra nếu startDate lớn hơn hoặc bằng endDate
    if (startDate && endDate && startDate >= endDate) {
        // Hiển thị thông báo lỗi
        errorMessage.style.display = 'block';
        // Vô hiệu hóa nút OK nếu ngày không hợp lệ
        btnOkDiscount.disabled = true;
    } else {
        // Ẩn thông báo lỗi nếu ngày hợp lệ
        errorMessage.style.display = 'none';
        // Kích hoạt nút OK nếu ngày hợp lệ
        btnOkDiscount.disabled = false;
    }
}

// Lấy các phần tử input và nút OK cho Shipping và Discount
const shippingFeeInput = document.getElementById('shippingFee');
const discountPercentInput = document.getElementById('discountPercent');
const btnOkShipping = document.getElementById('btn-ok-shipping');
const btnOkDiscount = document.getElementById('btn-ok-discount');

// Lấy thông báo lỗi cho Shipping và Discount
const errorMessageShipping = document.getElementById('error-message-shipping');
const errorMessageDiscount = document.getElementById('error-message-discount');

// Hàm kiểm tra tính hợp lệ cho Shipping và Discount
function checkValidity() {
    const shippingFee = parseFloat(shippingFeeInput.value);
    const discountPercent = parseFloat(discountPercentInput.value);

    // Kiểm tra hợp lệ cho Shipping (0 - 100,000)
    const shippingFeeValid = shippingFee >= 0 && shippingFee <= 100000;

    // Kiểm tra hợp lệ cho Discount (0 - 99)
    const discountPercentValid = discountPercent >= 0 && discountPercent <= 99;

    // Hiển thị thông báo lỗi nếu giá trị không hợp lệ
    errorMessageShipping.style.display = (shippingFeeValid || shippingFee === '') ? 'none' : 'block';
    errorMessageDiscount.style.display = (discountPercentValid || discountPercent === '') ? 'none' : 'block';

    // Bật hoặc tắt nút OK cho Shipping
    btnOkShipping.disabled = !(shippingFeeValid && shippingFee !== '');

    // Bật hoặc tắt nút OK cho Discount
    btnOkDiscount.disabled = !(discountPercentValid && discountPercent !== '');
}

// Lắng nghe sự kiện khi người dùng thay đổi giá trị
shippingFeeInput.addEventListener('input', checkValidity);
// discountPercentInput.addEventListener('input', checkValidity);

// Hàm kiểm tra tính hợp lệ ban đầu khi mở modal
function initialCheckValidity() {
    checkValidity();
}

$('#open-setting-shipping').on('shown.bs.modal', initialCheckValidity);
$('#open-setting-discount').on('shown.bs.modal', initialCheckValidity);
