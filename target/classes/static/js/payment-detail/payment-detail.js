const shippingPrice = document.getElementsByClassName('shipping-price');
const totalBill = document.getElementById('totalPriceBill');
const discountPrice = document.getElementById('final-price');
const totalBillDisplay = document.getElementById('final-price');

$(function () {
    'use strict';

    $(function () {
        $("#create-order").on('submit', function (event) {
            if (this.checkValidity() === false) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                const orderId = $('input[name=orderId]').val();
                const fullName = $('input[name=fullName]').val();
                const email = $('input[name=email]').val();
                const phone = $('input[name=phone]').val();
                const province = $('select[name=province]').val();
                const district = $('select[name=district]').val();
                const ward = $('select[name=ward]').val();
                const address = $('input[name=address]').val();
                const note = $('textarea[name=note]').val();
                const voucher = $('input[name=voucher]').val();
                const paymentMethod = document.querySelector('input[name="radio"]:checked').value;

                const data = {
                                orderId: orderId, fullName: fullName, email: email, phone: phone, paymentMethod: paymentMethod,
                                province: province, district: district, ward: ward, address: address, note:note, voucher:voucher
                            };
                $.ajax({
                   type: "POST",
                   contentType: "application/json",
                   url: "/api/payment/create",
                   data: JSON.stringify(data),
                   headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
                   success: function (data) {
                      showPopup('success');
                   },
                   error: function(error) {
                      console.log('error', error);
                      showPopup('fail', error.responseText);
                   }
               });
               event.preventDefault();
            }
            this.classList.add('was-validated');
        });
    });
});

$(document).ready(function() {
    $('#provinceSelect').change(function() {
      const provinceCode = $(this).val();
      $('#districtSelect').empty().append('<option value="">Chọn Quận/Huyện</option>');
        if (provinceCode) {
            $.get('/districts/' + provinceCode, function(districts) {
                $.each(districts, function(index, district) {
                    $('#districtSelect').append('<option value="' + district.code + '">' + district.name + '</option>');
                });
            });
        }
    });
    
    $('#districtSelect').change(function() {
      const districtCode = $(this).val();
      $('#wardSelect').empty().append('<option value="">Chọn Phường/Xã</option>');
        if (districtCode) {
            $.get('/wards/' + districtCode, function(districts) {
                $.each(districts, function(index, district) {
                    $('#wardSelect').append('<option value="' + district.code + '">' + district.name + '</option>');
                });
            });
        }
    });
});

function showPopup(type, message) {
    if (type === 'success') {
        document.getElementById("orderSuccessPopup").style.display = "flex";
    } else if (type === 'fail') {
        setErrorMessage(message)
        document.getElementById("orderFailedPopup").style.display = "flex";
    }
}

function setErrorMessage(message) {
    const errorMessageElement = document.querySelector('#orderFailedPopup .error-message');
    if (errorMessageElement) {
        errorMessageElement.textContent = message;
    }
}

// Hàm đóng popup
function closePopup(popupId) {
    document.getElementById(popupId).style.display = "none";
    if (popupId === 'orderSuccessPopup') {
        window.location.href = '/order-history';
        // window.location.href = '/';
    } else {
        window.location.href = '/cart-detail';
    }
}

document.getElementById('apply-btn').addEventListener('click', function() {
    let voucherCode = document.getElementById('voucher').value;
    fetch('/payment/voucher?code=' + voucherCode, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        },
    })
    .then(response => {
        if (!response.ok) {
            showNotification('error', 'Mã giảm giá đã hết hạn')
            return 0;
        } else {
            showNotification('success', 'Sử dụng mã giảm giá thành công')
            return response.text();
        }
    })
    .then(discountPrice => {
        updateBill(discountPrice);
    })
    .catch((error) => {
        console.error('Lỗi:', error);
    });
});

function updateBill(discount) {
    if (discount === 0) return;
    let tempPrice = totalBill.value;
    discountPrice.textContent = formatMoney(parseInt(discount)) + ' VND';
    totalBillDisplay.textContent = formatMoney(parseInt(tempPrice) - parseInt(discount) + 35000) + ' VND';
}

function formatMoney(value) {
    return value.toLocaleString('vi-VN');
}

function showNotification(type, message) {
    const notification = document.getElementById('notification');
    const notificationMessage = document.getElementById('notification-message');

    notificationMessage.textContent = message;
    notification.className = `notification ${type} show`;

    setTimeout(() => {
        hideNotification();
    }, 2000);
}

const modal = document.getElementById("imageModal");
const modalImg = document.getElementById("fullImage");
const thumbnail = document.getElementById("thumbnail");

thumbnail.onclick = function () {
    modal.classList.add("show");
    modalImg.src = "/images/QR.jpg"; // ảnh to
};

function closeModal() {
    modal.classList.remove("show");
}

window.addEventListener('load', function (ev) {
});
