$(function () {
    'use strict';

    $(function () {
        $("#register_product_form").on('submit', function (event) {
            if (this.checkValidity() === false) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                const form = $('#register_product_form')[0]; // Lấy form HTML
                const formData = new FormData(form); // Khởi tạo đối tượng FormData từ form
                $.ajax({
                   type: "POST",
                   // contentType: "application/json",
                   url: "/AM/AM003-01/register",
                   data: formData,
                   contentType: false, 
                   processData: false, 
                   headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
                   success: function (data) {
                      $('#message-success').text(data);
                      $('#register-product-success').modal('show');
                   },
                   error: function(error) {
                      console.log('error', error);
                      $('#message-error').text(error.responseText);
                      $('#register-product-error').modal('show');
                   }
               });
               event.preventDefault();
            }
            this.classList.add('was-validated');
        });
    });
});

function previewImage(event, previewId) {
    var file = event.target.files[0]; // Lấy file đã chọn
    var reader = new FileReader(); // Tạo một FileReader mới để đọc file ảnh

    reader.onload = function(e) {
        // Khi file ảnh đã được đọc xong, thay đổi thuộc tính src của img để hiển thị ảnh
        var imagePreview = document.getElementById(previewId);
        imagePreview.src = e.target.result; // Đặt ảnh đã chọn làm nguồn cho img
    };

    if (file) {
        reader.readAsDataURL(file); // Đọc file ảnh dưới dạng base64
    }
}

window.addEventListener('load', function (ev) {
});
