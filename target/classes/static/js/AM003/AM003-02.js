$(function () {
    'use strict';

    $(function () {
        $("#update_product_form").on('submit', function (event) {
            if (this.checkValidity() === false) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                const form = $('#update_product_form')[0]; // Lấy form HTML
                const formData = new FormData(form); // Khởi tạo đối tượng FormData từ form
                $.ajax({
                    type: "POST",
                    url: "/AM/AM003-02/update",
                    data: formData,
                    contentType: false,
                    processData: false,
                    headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
                    success: function (data) {
                        $('#message-success').text(data);
                        $('#update-product-success').modal('show');
                    },
                    error: function(error) {
                        console.log('error', error);
                        $('#message-error').text(error.responseText);
                        $('#update-product-error').modal('show');
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

function openImageModal(button) {
    const variationId = button.getAttribute('data-id');
    document.getElementById('modalVariationId').value = variationId;
    $('#update-image-modal').modal('show'); // mở modal Bootstrap
}

function closeImageModal() {
    $('#update-image-modal').modal('hide');
    document.getElementById('updateResult').innerText = '';
    document.getElementById('updateImageForm').reset();
}

document.getElementById('updateImageForm').addEventListener('submit', function (event) {
    event.preventDefault();

    const variationId = document.getElementById('modalVariationId').value;
    const imageFile = document.getElementById('newImageInput').files[0];
    const resultDiv = document.getElementById('updateResult');

    if (!imageFile) {
        resultDiv.innerText = "Vui lòng chọn ảnh.";
        return;
    }

    const formData = new FormData();
    formData.append("image", imageFile);

    fetch(`/variations/${variationId}`, {
        method: "PUT",
        body: formData
    })
        .then(response => {
            if (response.ok) {
                resultDiv.style.color = "green";
                resultDiv.innerText = "Cập nhật thành công!";
                setTimeout(() => {
                    closeImageModal();
                    location.reload(); // hoặc cập nhật ảnh ngay trên giao diện
                }, 1000);
            } else {
                return response.text().then(text => { throw new Error(text) });
            }
        })
        .catch(error => {
            resultDiv.style.color = "red";
            resultDiv.innerText = "Lỗi khi cập nhật ảnh: " + error.message;
        });
});

window.addEventListener('load', function (ev) {
});
