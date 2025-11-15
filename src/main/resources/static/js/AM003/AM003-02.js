$(function () {
    'use strict';

    $(function () {
        $("#update_product_form").on('submit', function (event) {
            // Validate category (must select at least one if required by UI)
            const categorySelect = $('#categorySelect');
            let categoryValues = [];
            
            // Handle both Select2 and regular select
            if (categorySelect.length > 0) {
                if ($.fn.select2 && categorySelect.data('select2')) {
                    // Select2 is initialized
                    categoryValues = categorySelect.val() || [];
                } else {
                    // Regular select
                    const selectedOptions = categorySelect.find('option:selected');
                    categoryValues = selectedOptions.map(function() { return $(this).val(); }).get();
                }
                
                // Category is required (UI shows ※), validate before submit
                if (categoryValues.length === 0) {
                    event.preventDefault();
                    event.stopPropagation();
                    categorySelect.addClass('is-invalid');
                    alert('Vui lòng chọn ít nhất một loại sản phẩm.');
                    return false;
                }
                categorySelect.removeClass('is-invalid');
            }
            
            if (this.checkValidity() === false) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                const form = $('#update_product_form')[0]; // Lấy form HTML
                const formData = new FormData(form); // Khởi tạo đối tượng FormData từ form
                
                // Đảm bảo CSRF token được thêm vào FormData
                const csrfToken = $("input[name='_csrf']").val();
                const csrfParamName = $("input[name='_csrf']").attr('name');
                if (csrfToken) {
                    // Thêm CSRF token vào FormData (Spring Security yêu cầu trong multipart)
                    if (!formData.has(csrfParamName)) {
                        formData.append(csrfParamName, csrfToken);
                    }
                }
                
                // Show loading indicator
                const submitButton = $('#update_product_form').find('button[type="submit"]');
                const originalButtonText = submitButton.text();
                submitButton.prop('disabled', true).text('Đang xử lý...');
                
                $.ajax({
                    type: "POST",
                    url: "/AM/AM003-02/update",
                    data: formData,
                    contentType: false,  // Quan trọng: để browser tự set Content-Type với boundary
                    processData: false, // Quan trọng: không xử lý data, giữ nguyên FormData
                    headers: {"X-CSRF-TOKEN": csrfToken}, // Thêm vào headers để đảm bảo
                    timeout: 600000, // 10 minutes timeout for large file uploads
                    xhr: function() {
                        var xhr = new window.XMLHttpRequest();
                        // Upload progress (optional, for future enhancement)
                        xhr.upload.addEventListener("progress", function(evt) {
                            if (evt.lengthComputable) {
                                var percentComplete = (evt.loaded / evt.total) * 100;
                                console.log('Upload progress: ' + percentComplete + '%');
                            }
                        }, false);
                        return xhr;
                    },
                    success: function (data) {
                        submitButton.prop('disabled', false).text(originalButtonText);
                        $('#message-success').text(data);
                        $('#update-product-success').modal('show');
                    },
                    error: function(error) {
                        submitButton.prop('disabled', false).text(originalButtonText);
                        console.log('error', error);
                        let errorMessage = 'Đã xảy ra lỗi khi cập nhật sản phẩm.';
                        
                        if (error.status === 0 || error.statusText === 'abort' || error.statusText === 'timeout') {
                            errorMessage = 'Kết nối bị ngắt hoặc timeout. ';
                            errorMessage += 'Có thể do file quá lớn hoặc mất kết nối mạng. ';
                            errorMessage += 'Vui lòng thử lại với file nhỏ hơn hoặc kiểm tra kết nối mạng.';
                        } else if (error.responseText) {
                            errorMessage = error.responseText;
                        } else if (error.status === 400) {
                            errorMessage = 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.';
                        } else if (error.status === 500) {
                            errorMessage = 'Lỗi server. Vui lòng thử lại sau.';
                        } else if (error.status === 413) {
                            errorMessage = 'File quá lớn. Vui lòng chọn file nhỏ hơn 100MB.';
                        }
                        
                        $('#message-error').text(errorMessage);
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
