let outStock = document.getElementById('out-stock').value;
let accessory = document.getElementById('product-accessory').value;
let productPrice = document.getElementById('final-price').value;
let productIdInput = document.getElementById('product-id').value;
let productId = parseInt(productIdInput);

let productImage = document.getElementById('product-image').value;
let productName = document.getElementById('product-name').value;
let orderItems = [];
let orderItemByProducts = [];

let variations = JSON.parse(document.getElementById('variations').value);

// Mobile
let selectedColorMobile = null;
let selectedSizeMobile = null;
let selectedTypeMobile = null;

// PC
let selectedColor = null;
let selectedSize = null;
let selectedType = null;

function hasTypeOptions() {
    return document.querySelectorAll('.type-button, .type-button-mobile').length > 0;
}

function hasColorOptions() {
    return document.querySelectorAll('.color-button, .color-button-mobile').length > 0;
}

function hasSizeOptions() {
    return document.querySelectorAll('.size-button, .size-button-mobile').length > 0;
}

function requireSelectionsDesktop() {
    if (accessory === 'false') {
        if (hasColorOptions() && !selectedColor) {
            showWarningPopup('fail');
            return false;
        }
        if (hasTypeOptions() && !selectedType) {
            showWarningPopup('fail');
            return false;
        }
        if (hasSizeOptions() && !selectedSize) {
            showWarningPopup('fail');
            return false;
        }
    }
    return true;
}

function requireSelectionsMobile() {
    if (accessory === 'false') {
        if (hasColorOptions() && !selectedColorMobile) {
            showWarningPopup('fail');
            return false;
        }
        if (hasTypeOptions() && !selectedTypeMobile) {
            showWarningPopup('fail');
            return false;
        }
        if (hasSizeOptions() && !selectedSizeMobile) {
            showWarningPopup('fail');
            return false;
        }
    }
    return true;
}

// ====================================== PC Thread =========================================

document.addEventListener('DOMContentLoaded', () => {
    const colorButtons = document.querySelectorAll('.color-button');
    const sizeButtons = document.querySelectorAll('.size-button');
    const typeButtons = document.querySelectorAll('.type-button');
    const slider = document.getElementById('product-slider');
    const colorImageWrapper = document.getElementById('color-image-wrapper');
    const colorImage = document.getElementById('color-image');

    colorButtons.forEach(button => {
        button.addEventListener('click', function() {
            selectedColor = button.innerText;
            highlightButton(colorButtons, button);

            let matchingVariationPC = variations.filter(v =>  v.color === selectedColor);
            if (selectedSize != null) {
                matchingVariationPC = variations.filter(v =>  v.color === selectedColor && v.size === selectedSize);
            }
            if (hasTypeOptions() && selectedType != null) {
                matchingVariationPC = matchingVariationPC.filter(v => v.type === selectedType);
            }

            if (matchingVariationPC.length > 0) {
                if (matchingVariationPC[0].image !== null) {
                    slider.style.display = 'none';
                    colorImageWrapper.style.display = 'block';
                    colorImage.src = matchingVariationPC[0].image;
                }
            }

            setTimeout(() => {
                onColorClick(button.innerText);
            }, 100);
        });
    });

    sizeButtons.forEach(button => {
        button.addEventListener('click', function() {
            selectedSize = button.innerText;
            highlightButtonSize(sizeButtons, button);

            setTimeout(() => {
                onSizeClick(button.innerText);

                let matchingVariations = variations.filter(v => v.size === selectedSize && v.color === selectedColor);
                if (hasTypeOptions() && selectedType != null) {
                    matchingVariations = matchingVariations.filter(v => v.type === selectedType);
                }
                if (matchingVariations.length > 0) {
                    if (matchingVariations[0].image !== null) {
                        slider.style.display = 'none';
                        colorImageWrapper.style.display = 'block';
                        colorImage.src = matchingVariations[0].image;
                    }
                }
            }, 100);
        });
    });

    // PC type buttons
    typeButtons.forEach(button => {
        button.addEventListener('click', function() {
            selectedType = button.innerText;
            typeButtons.forEach(b => b.classList.remove('active'));
            button.classList.add('active');

            // Update display image based on selected type too
            let matching = variations.filter(v => (!selectedColor || v.color === selectedColor) && (!selectedSize || v.size === selectedSize));
            matching = matching.filter(v => v.type === selectedType);
            if (matching.length > 0 && matching[0].image !== null) {
                const slider = document.getElementById('product-slider');
                const colorImageWrapper = document.getElementById('color-image-wrapper');
                const colorImage = document.getElementById('color-image');
                slider.style.display = 'none';
                colorImageWrapper.style.display = 'block';
                colorImage.src = matching[0].image;
            }
        });
    });

    function highlightButton(buttons, selectedButton) {
        buttons.forEach(button => button.classList.remove('active'));
        selectedButton.classList.add('active');
    }

    function highlightButtonSize(buttons, selectedButton) {
        sizeButtons.forEach(button => button.classList.remove('active'));
        buttons.forEach(button => {
            button.style.backgroundColor = '';
            button.style.color = '';
        });

        selectedButton.style.backgroundColor = '#6c757d';
        selectedButton.style.color = 'white';
    }

    updateVariationDisplay(variations);
});

function updateVariationDisplay(variations) {
    // Lưu trữ các màu sắc, kích thước, kiểu còn hàng
    const enabledColors = new Set();
    const enabledSizesByColor = {}; // Lưu trữ kích thước hợp lệ theo màu sắc
    const enabledSizes = new Set(); // Lưu trữ các size có remainQuantity > 0
    const enabledTypes = new Set();

    // Duyệt qua variations để xác định màu và kích thước còn hàng
    variations.forEach(variation => {
        if (variation.remainQuantity > 0) {
            enabledColors.add(variation.color); // Thêm màu vào Set nếu còn hàng

            // Kiểm tra và thêm kích thước vào danh sách của màu sắc tương ứng
            if (!enabledSizesByColor[variation.color]) {
                enabledSizesByColor[variation.color] = new Set();
            }
            enabledSizesByColor[variation.color].add(variation.size);

            enabledSizes.add(variation.size); // Thêm kích thước có sẵn vào Set
            if (variation.type) {
                enabledTypes.add(variation.type);
            }
        }
    });

    // Cập nhật trạng thái của tất cả các button màu sắc và kích thước
    const allColorElements = document.querySelectorAll('[id^="color-pc-"]');
    allColorElements.forEach(colorElement => {
        const color = colorElement.id.replace('color-pc-', '');
        
        if (enabledColors.has(color)) {
            colorElement.classList.remove('disabled'); // Bỏ disabled cho màu
        } else {
            colorElement.classList.add('disabled'); // Thêm disabled cho màu
        }
    });

    const allSizeElements = document.querySelectorAll('[id^="size-pc-"]');
    allSizeElements.forEach(sizeElement => {
        const size = sizeElement.id.replace('size-pc-', '');
        
        if (enabledSizes.has(size)) {
            sizeElement.classList.remove('disabled'); // Bỏ disabled cho kích thước
        } else {
            sizeElement.classList.add('disabled'); // Thêm disabled cho kích thước
        }
    });

    // Cập nhật trạng thái của tất cả các button kiểu nếu có
    const allTypeElements = document.querySelectorAll('.type-button');
    if (allTypeElements && allTypeElements.length > 0) {
        allTypeElements.forEach(typeElement => {
            const typeVal = typeElement.innerText;
            if (enabledTypes.has(typeVal)) {
                typeElement.classList.remove('disabled');
            } else {
                typeElement.classList.add('disabled');
            }
        });
    }
}

function onSizeClick(size) {
    // Ẩn hoặc vô hiệu hóa các button màu sắc không thuộc kích thước đã chọn
    const allColorElements = document.querySelectorAll('[id^="color-pc-"]');
    allColorElements.forEach(colorElement => {
        const color = colorElement.id.replace('color-pc-', '');
        
        // Lọc các variation có size đã chọn và màu phù hợp
        let matchingVariations = variations.filter(v => v.size === size && v.color === color);
        if (hasTypeOptions() && selectedType != null) {
            matchingVariations = matchingVariations.filter(v => v.type === selectedType);
        }
        
        if (matchingVariations.length === 0 || matchingVariations[0].remainQuantity <= 0) {
            colorElement.classList.add('disabled'); // Nếu không có variation hợp lệ thì vô hiệu hóa màu
        } else {
            colorElement.classList.remove('disabled'); // Hiển thị màu nếu có variation hợp lệ
        }
    });

    // Cập nhật khả dụng cho type theo size đã chọn và (nếu có) màu đã chọn
    const allTypeElements = document.querySelectorAll('.type-button');
    if (allTypeElements && allTypeElements.length > 0) {
        allTypeElements.forEach(typeElement => {
            const typeVal = typeElement.innerText;
            let matching = variations.filter(v => v.size === size);
            if (selectedColor) matching = matching.filter(v => v.color === selectedColor);
            const hasStock = matching.some(v => v.type === typeVal && v.remainQuantity > 0);
            if (hasStock) {
                typeElement.classList.remove('disabled');
            } else {
                typeElement.classList.add('disabled');
                typeElement.classList.remove('active');
                if (selectedType === typeVal) selectedType = null;
            }
        });
    }
}

function onColorClick(color) {
    // Ẩn hoặc vô hiệu hóa các button kích thước không thuộc màu đã chọn
    const allSizeElements = document.querySelectorAll('[id^="size-pc-"]');
    allSizeElements.forEach(sizeElement => {
        const size = sizeElement.id.replace('size-pc-', '');
        let matchingVariations = variations.filter(v => v.size === size && v.color === color);
        if (hasTypeOptions() && selectedType != null) {
            matchingVariations = matchingVariations.filter(v => v.type === selectedType);
        }
        
        if (matchingVariations.length === 0 || matchingVariations[0].remainQuantity <= 0) {
            sizeElement.classList.add('disabled'); // Nếu không có variation hợp lệ thì vô hiệu hóa kích thước
        } else {
            sizeElement.classList.remove('disabled'); // Hiển thị kích thước nếu có variation hợp lệ
        }
    });

    // Cập nhật khả dụng cho type theo màu đã chọn và (nếu có) size đã chọn
    const allTypeElements = document.querySelectorAll('.type-button');
    if (allTypeElements && allTypeElements.length > 0) {
        allTypeElements.forEach(typeElement => {
            const typeVal = typeElement.innerText;
            let matching = variations.filter(v => v.color === color);
            if (selectedSize) matching = matching.filter(v => v.size === selectedSize);
            const hasStock = matching.some(v => v.type === typeVal && v.remainQuantity > 0);
            if (hasStock) {
                typeElement.classList.remove('disabled');
            } else {
                typeElement.classList.add('disabled');
                typeElement.classList.remove('active');
                if (selectedType === typeVal) selectedType = null;
            }
        });
    }
}

function updateOrder() {
    if (!selectedColor) {
        selectedColor = 'blank';
    }
    if (!selectedSize) {
        selectedSize = 'blank';
    }
    let selectedColorNew = selectedColor.replace(" ", "");
    let selectedSizeNew = selectedSize.replace(" ", "");
    let option;
    if (hasTypeOptions() && selectedType) {
        let selectedTypeNew = selectedType.replace(" ", "");
        option = `${selectedColorNew}/${selectedTypeNew}/${selectedSizeNew}`;
    } else {
        option = `${selectedColorNew}/${selectedSizeNew}`;
    }
    const existingItem = orderItemByProducts.find(item => item.option === option);
    if (existingItem) {
        existingItem.quantity++;
        updateItemQuantity(existingItem);
    } else {
        let variation;
        if (accessory === 'true') {
            variation = variations[0];
        } else if (selectedColor !== 'blank' && selectedSize !== 'blank') {
            if (hasTypeOptions() && selectedType) {
                variation = variations.find(variation => variation.size === selectedSize && variation.color === selectedColor && variation.type === selectedType);
            } else {
                variation = variations.find(variation => variation.size === selectedSize && variation.color === selectedColor);
            }
        } else if (selectedColor !== 'blank') {
            if (hasTypeOptions() && selectedType) {
                variation = variations.find(variation => variation.color === selectedColor && variation.type === selectedType);
            } else {
                variation = variations.find(variation => variation.color === selectedColor);
            }
        } else {
            if (hasTypeOptions() && selectedType) {
                variation = variations.find(variation => variation.size === selectedSize && variation.type === selectedType);
            } else {
                variation = variations.find(variation => variation.size === selectedSize);
            }
        }

        const newItem = {
            option: option,
            quantity: 1,
            price: productPrice,
            image: productImage,
            name: productName,
            variationId: variation.variationId,
            id: productId
        };
        orderItemByProducts.push(newItem);
        orderItems.push(newItem);

        if (window.fbq) {
            fbq('track', 'AddToCart', {
                content_name: productName,
                content_ids: [productId], // ID sản phẩm
                content_type: 'product',
                value: productPrice, // Giá sản phẩm
                currency: 'VND'
            });
        } else {
            console.warn("Facebook Pixel chưa được tải.");
        }
    }
}

function addToCart() {
    if (outStock === 'true') {
        showOutStockPopup('fail');
        return;
    }

    if (!requireSelectionsDesktop()) return;

    updateOrder();
    fetch('/add-to-cart', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(orderItemByProducts)
    })
    .then(response => {
        if (response.ok) {
            setTimeout(() => {
                showPopup('success')
            }, 500);
        }
    })
    .catch((error) => {
        showPopup('fail')
        console.log("addToCartButton error = " + error)
    });
}

function buyNow() {
    if (outStock === 'true') {
        showOutStockPopup('fail');
        return;
    }

    if (!requireSelectionsDesktop()) return;

    updateOrder();
    fetch('/add-to-cart', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(orderItemByProducts)
    })
    .then(response => {
        if (response.ok) {
            window.location.href = '/cart-detail';
        }
    })
    .catch((error) => {
        console.error('Lỗi:', error);
    });
}

function updateItemQuantity(orderItem) {
    const itemElements = document.querySelectorAll('.order-item');
    itemElements.forEach(itemElement => {
        const itemText = itemElement.querySelector('.input-number');
        if (itemText != null) {
            if (itemText.id === orderItem.option) {
                const quantityInput = itemElement.querySelector('.input-number');
                orderItem.quantity = quantityInput.value;
            }
        }
    });
}
// ====================================== Mobile Thread =========================================

document.addEventListener('DOMContentLoaded', () => {
    const colorButtonMobiles = document.querySelectorAll('.color-button-mobile');
    const sizeButtonMobiles = document.querySelectorAll('.size-button-mobile');
    const typeButtonMobiles = document.querySelectorAll('.type-button-mobile');
    const slider = document.getElementById('product-slider');
    const colorImageWrapper = document.getElementById('color-image-wrapper');
    const colorImage = document.getElementById('color-image');

    // Xử lý sự kiện click cho nút màu
    colorButtonMobiles.forEach(button => {
        button.addEventListener('click', function () {
            selectedColorMobile = button.innerText;
            highlightButtonMobile(colorButtonMobiles, button);

            let matchingVariations = variations.filter(v =>  v.color === selectedColorMobile);
            if (selectedSizeMobile != null) {
                matchingVariations = variations.filter(v =>  v.color === selectedColorMobile && v.size === selectedSizeMobile);
            }
            if (hasTypeOptions() && selectedTypeMobile != null) {
                matchingVariations = matchingVariations.filter(v => v.type === selectedTypeMobile);
            }

            if (matchingVariations.length > 0) {
                if (matchingVariations[0].image !== null) {
                    slider.style.display = 'none';
                    colorImageWrapper.style.display = 'block';
                    colorImage.src = matchingVariations[0].image;
                }

            }

            setTimeout(() => {
                onColorClickMobile(button.innerText);
            }, 100);
        });
    });

    // Xử lý sự kiện click cho nút kích thước
    sizeButtonMobiles.forEach(button => {
        button.addEventListener('click', function () {
            selectedSizeMobile = button.innerText;
            highlightButtonSizeMobile(sizeButtonMobiles, button);

            setTimeout(() => {
                onSizeClickMobile(button.innerText);

                let matchingVariations = variations.filter(v => v.size === selectedSizeMobile && v.color === selectedColorMobile);
                if (hasTypeOptions() && selectedTypeMobile != null) {
                    matchingVariations = matchingVariations.filter(v => v.type === selectedTypeMobile);
                }
                if (matchingVariations.length > 0) {
                    if (matchingVariations[0].image !== null) {
                        slider.style.display = 'none';
                        colorImageWrapper.style.display = 'block';
                        colorImage.src = matchingVariations[0].image;
                    }
                }
            }, 100);
        });
    });

    // Mobile type buttons
    typeButtonMobiles.forEach(button => {
        button.addEventListener('click', function () {
            selectedTypeMobile = button.innerText;
            typeButtonMobiles.forEach(b => b.classList.remove('active'));
            button.classList.add('active');

            let matching = variations.filter(v => (!selectedColorMobile || v.color === selectedColorMobile) && (!selectedSizeMobile || v.size === selectedSizeMobile));
            matching = matching.filter(v => v.type === selectedTypeMobile);
            if (matching.length > 0 && matching[0].image !== null) {
                const slider = document.getElementById('product-slider');
                const colorImageWrapper = document.getElementById('color-image-wrapper');
                const colorImage = document.getElementById('color-image');
                slider.style.display = 'none';
                colorImageWrapper.style.display = 'block';
                colorImage.src = matching[0].image;
            }
        });
    });

    // Hàm làm nổi bật nút màu khi chọn
    function highlightButtonMobile(buttons, selectedButton) {
        buttons.forEach(button => button.classList.remove('active'));
        selectedButton.classList.add('active');
    }

    // Hàm làm nổi bật nút kích thước khi chọn
    function highlightButtonSizeMobile(buttons, selectedButton) {
        buttons.forEach(button => button.classList.remove('active'));
        buttons.forEach(button => {
            button.style.backgroundColor = '';
            button.style.color = '';
        });

        selectedButton.style.backgroundColor = '#6c757d';
        selectedButton.style.color = 'white';
    }

    // Cập nhật trạng thái cho các variations ban đầu
    updateVariationMobileDisplay(variations);
});

// Cập nhật hiển thị các variation trên mobile
function updateVariationMobileDisplay(variations) {
    // Lưu trữ các màu sắc, kích thước, kiểu còn hàng
    const enabledColors = new Set();
    const enabledSizesByColor = {}; // Lưu trữ kích thước hợp lệ theo màu sắc
    const enabledSizes = new Set(); // Lưu trữ các size có remainQuantity > 0
    const enabledTypes = new Set();

    // Duyệt qua variations để xác định màu và kích thước còn hàng
    variations.forEach(variation => {
        if (variation.remainQuantity > 0) {
            enabledColors.add(variation.color); // Thêm màu vào Set nếu còn hàng

            // Kiểm tra và thêm kích thước vào danh sách của màu sắc tương ứng
            if (!enabledSizesByColor[variation.color]) {
                enabledSizesByColor[variation.color] = new Set();
            }
            enabledSizesByColor[variation.color].add(variation.size);

            enabledSizes.add(variation.size); // Thêm kích thước có sẵn vào Set
            if (variation.type) {
                enabledTypes.add(variation.type);
            }
        }
    });

    // Cập nhật trạng thái của tất cả các button màu sắc và kích thước
    variations.forEach(variation => {
        const colorElement = document.getElementById(`color-${variation.color}`);
        const sizeElement = document.getElementById(`size-${variation.size}`);

        // Nếu màu sắc này có tồn tại trong enabledColors
        if (enabledColors.has(variation.color)) {
            if (colorElement) colorElement.classList.remove('disabled'); // Bỏ disabled cho màu
        } else {
            if (colorElement) colorElement.classList.add('disabled'); // Thêm disabled cho màu
        }

        // Nếu kích thước này có tồn tại trong enabledSizes
        if (enabledSizes.has(variation.size)) {
            if (sizeElement) sizeElement.classList.remove('disabled'); // Bỏ disabled cho kích thước
        } else {
            if (sizeElement) sizeElement.classList.add('disabled'); // Thêm disabled cho kích thước
        }
    });

    // Cập nhật trạng thái cho type trên mobile nếu có
    const allTypeElements = document.querySelectorAll('.type-button-mobile');
    if (allTypeElements && allTypeElements.length > 0) {
        allTypeElements.forEach(typeElement => {
            const typeVal = typeElement.innerText;
            if (enabledTypes.has(typeVal)) {
                typeElement.classList.remove('disabled');
            } else {
                typeElement.classList.add('disabled');
            }
        });
    }
}

// Xử lý khi click vào kích thước trên mobile
function onSizeClickMobile(size) {
    // Ẩn hoặc vô hiệu hóa các button màu sắc không thuộc kích thước đã chọn
    const allColorElements = document.querySelectorAll('[id^="color-"]');
    allColorElements.forEach(colorElement => {
        const color = colorElement.id.replace('color-', '');
        
        // Lọc các variation có size đã chọn và màu phù hợp
        let matchingVariations = variations.filter(v => v.size === size && v.color === color);
        if (hasTypeOptions() && selectedTypeMobile != null) {
            matchingVariations = matchingVariations.filter(v => v.type === selectedTypeMobile);
        }
        
        if (matchingVariations.length === 0 || matchingVariations[0].remainQuantity <= 0) {
            colorElement.classList.add('disabled'); // Nếu không có variation hợp lệ thì vô hiệu hóa màu
        } else {
            colorElement.classList.remove('disabled'); // Hiển thị màu nếu có variation hợp lệ
        }
    });

    // Cập nhật khả dụng cho type theo size đã chọn và (nếu có) màu đã chọn trên mobile
    const allTypeElements = document.querySelectorAll('.type-button-mobile');
    if (allTypeElements && allTypeElements.length > 0) {
        allTypeElements.forEach(typeElement => {
            const typeVal = typeElement.innerText;
            let matching = variations.filter(v => v.size === size);
            if (selectedColorMobile) matching = matching.filter(v => v.color === selectedColorMobile);
            const hasStock = matching.some(v => v.type === typeVal && v.remainQuantity > 0);
            if (hasStock) {
                typeElement.classList.remove('disabled');
            } else {
                typeElement.classList.add('disabled');
                typeElement.classList.remove('active');
                if (selectedTypeMobile === typeVal) selectedTypeMobile = null;
            }
        });
    }
}

// Xử lý khi click vào màu trên mobile
function onColorClickMobile(color) {
    // Ẩn hoặc vô hiệu hóa các button kích thước không thuộc màu đã chọn
    const allSizeElements = document.querySelectorAll('[id^="size-"]');
    allSizeElements.forEach(sizeElement => {
        const size = sizeElement.id.replace('size-', '');
        let matchingVariations = variations.filter(v => v.size === size && v.color === color);
        if (hasTypeOptions() && selectedTypeMobile != null) {
            matchingVariations = matchingVariations.filter(v => v.type === selectedTypeMobile);
        }
        
        if (matchingVariations.length === 0 || matchingVariations[0].remainQuantity <= 0) {
            sizeElement.classList.add('disabled'); // Nếu không có variation hợp lệ thì vô hiệu hóa kích thước
        } else {
            sizeElement.classList.remove('disabled'); // Hiển thị kích thước nếu có variation hợp lệ
        }
    });

    // Cập nhật khả dụng cho type theo màu đã chọn và (nếu có) size đã chọn trên mobile
    const allTypeElements = document.querySelectorAll('.type-button-mobile');
    if (allTypeElements && allTypeElements.length > 0) {
        allTypeElements.forEach(typeElement => {
            const typeVal = typeElement.innerText;
            let matching = variations.filter(v => v.color === color);
            if (selectedSizeMobile) matching = matching.filter(v => v.size === selectedSizeMobile);
            const hasStock = matching.some(v => v.type === typeVal && v.remainQuantity > 0);
            if (hasStock) {
                typeElement.classList.remove('disabled');
            } else {
                typeElement.classList.add('disabled');
                typeElement.classList.remove('active');
                if (selectedTypeMobile === typeVal) selectedTypeMobile = null;
            }
        });
    }
}

function updateOrderMobile() {
    if (!selectedColorMobile) {
        selectedColorMobile = 'blank';
    }
    if (!selectedSizeMobile) {
        selectedSizeMobile = 'blank';
    }

    let selectedColorNew = selectedColorMobile.replace(" ", "");
    let selectedSizeNew = selectedSizeMobile.replace(" ", "");
    let option;
    if (hasTypeOptions() && selectedTypeMobile) {
        let selectedTypeNew = selectedTypeMobile.replace(" ", "");
        option = `${selectedColorNew}/${selectedTypeNew}/${selectedSizeNew}`;
    } else {
        option = `${selectedColorNew}/${selectedSizeNew}`;
    }
    const existingItem = orderItemByProducts.find(item => item.option === option);
    if (existingItem) {
        existingItem.quantity++;
        updateItemQuantityMobile(existingItem);
    } else {
        let variation;
        if (accessory === 'true') {
            variation = variations[0];
        } else if (selectedColorMobile !== 'blank' && selectedSizeMobile !== 'blank') {
            if (hasTypeOptions() && selectedTypeMobile) {
                variation = variations.find(variation => variation.size === selectedSizeMobile && variation.color === selectedColorMobile && variation.type === selectedTypeMobile);
            } else {
                variation = variations.find(variation => variation.size === selectedSizeMobile && variation.color === selectedColorMobile);
            }
        } else if (selectedColorMobile !== 'blank') {
            if (hasTypeOptions() && selectedTypeMobile) {
                variation = variations.find(variation => variation.color === selectedColorMobile && variation.type === selectedTypeMobile);
            } else {
                variation = variations.find(variation => variation.color === selectedColorMobile);
            }
        } else {
            if (hasTypeOptions() && selectedTypeMobile) {
                variation = variations.find(variation => variation.size === selectedSizeMobile && variation.type === selectedTypeMobile);
            } else {
                variation = variations.find(variation => variation.size === selectedSizeMobile);
            }
        }

        const newItem = {
            option: option,
            quantity: 1,
            price: productPrice,
            image: productImage,
            name: productName,
            variationId: variation.variationId,
            id: productId
        };
        orderItemByProducts.push(newItem);
        orderItems.push(newItem);

        if (window.fbq) {
            fbq('track', 'AddToCart', {
                content_name: productName,
                content_ids: [productId], // ID sản phẩm
                content_type: 'product',
                value: productPrice, // Giá sản phẩm
                currency: 'VND'
            });
        } else {
            console.warn("Facebook Pixel chưa được tải.");
        }
    }
}

function updateItemQuantityMobile(orderItem) {
    const itemElements = document.querySelectorAll('.order-item-mobile');
    itemElements.forEach(itemElement => {
        const itemText = itemElement.querySelector('.input-number-mobile');
        if (itemText != null) {
            const keyMobile = 'mobile-' + orderItem.option;
            if (itemText.id === keyMobile) {
                const quantityInput = itemElement.querySelector('.input-number-mobile');
                orderItem.quantity = quantityInput.value;
            }
        }
    });
}

function addToCartMobile() {
    if (outStock === 'true') {
        showOutStockPopup('fail');
        return;
    }

    if (!requireSelectionsMobile()) return;

    updateOrderMobile();
    fetch('/add-to-cart', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(orderItemByProducts)
    })
    .then(response => {
        if (response.ok) {
            setTimeout(() => {
                showPopup('success')
            }, 500);
        }
    })
    .catch((error) => {
        showPopup('fail')
        console.log("addToCartButton error = " + error)
    });
}

function buyNowMobile() {
    if (outStock === 'true') {
        showOutStockPopup('fail');
        return;
    }

    if (!requireSelectionsMobile()) return;

    updateOrderMobile();
    fetch('/add-to-cart', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(orderItemByProducts)
    })
    .then(response => {
        if (response.ok) {
            window.location.href = '/cart-detail';
        }
    })
    .catch((error) => {
        console.error('Lỗi:', error);
    });
}

// ========================================== Common Thread =================================================

function showPopup(type) {
    if (type === 'success') {
        document.getElementById("addToCardSuccess").style.display = "flex";
    } else {
        document.getElementById("addToCardFail").style.display = "flex";
    }
}

function showWarningPopup() {
    document.getElementById("showWarningPopup").style.display = "flex";
}

function showOutStockPopup() {
    document.getElementById("showOutStockPopup").style.display = "flex";
}

// Hàm đóng popup
function closePopup(popupId) {
    reloadPage();
    document.getElementById(popupId).style.display = "none";
}

function reloadPage() {
    location.reload();
}

window.onload = function() {
    updateVariationDisplay(variations);
    updateVariationMobileDisplay(variations);
};