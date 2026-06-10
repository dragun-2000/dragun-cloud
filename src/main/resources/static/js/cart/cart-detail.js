let orderItemsString = document.getElementById('orderItems-default').value;
let orderItems = JSON.parse(orderItemsString);
const cartInfo = document.getElementById('cart-info');
const totalBill = document.getElementById('total-bill');
const totalBillTemp = document.getElementById('total-bill-temp');

const bagTotal = document.getElementById('bag-total');
const bagTotalMobile = document.getElementById('bag-total-mobile');

sessionStorage.setItem('orderItems', JSON.stringify(orderItems));

// if (orderItems.length === 0) {
//     let paymentMomoItem = document.getElementById('paymentMomo');
//     paymentMomoItem.setAttribute('disabled', 'disabled');
//     paymentMomoItem.setAttribute('background', '#505050');
// }

function showEmptyCart() {
    const emptyEl = document.getElementById('cart-empty');
    const checkoutEl = document.getElementById('cart-checkout');
    if (emptyEl) {
        emptyEl.style.display = 'block';
    }
    if (checkoutEl) {
        checkoutEl.style.display = 'none';
    }
    if (cartInfo) {
        cartInfo.innerHTML = '';
    }
    if (typeof updateHeaderBagCount === 'function') {
        updateHeaderBagCount(0);
    }
}

function hideEmptyCart() {
    const emptyEl = document.getElementById('cart-empty');
    const checkoutEl = document.getElementById('cart-checkout');
    if (emptyEl) {
        emptyEl.style.display = 'none';
    }
    if (checkoutEl) {
        checkoutEl.style.display = '';
    }
}

function renderCartItems() {
    let item = sessionStorage.getItem('orderItems');
    let orderItems = JSON.parse(item);
    if (orderItems.length === 0) {
        showEmptyCart();
        return;
    }
    hideEmptyCart();
    const itemElement = document.createElement('div');
    const tableItem = document.createElement('table');
    const bodyItem = document.createElement('tbody');
    itemElement.classList.add('cart-info');
    itemElement.appendChild(tableItem);
    tableItem.appendChild(bodyItem);
    orderItems.forEach(item => {
        // Build a safe key for DOM ids/classes (no spaces or special chars)
        const key = (item.option + '-' + item.id)
            .replace(/\s+/g, '_')
            .replace(/[^\w-]/g, '_');
        const trElement = document.createElement('tr');
        trElement.classList.add(key);
        trElement.innerHTML = `
            <td>
              <label class="custom-control custom-checkbox">
                <input class="custom-control-input" type="checkbox" checked id="${'checkbox_' + key}" disabled>
                <span class="custom-control-indicator"></span>
              </label>
            </td>
            <td>
              <div class="cart-pro">
                <div class="cart-thumb">
                  <img src="${item.image}" alt="product">
                </div>
                <div class="cart-content">
                  <h2 class="cart-title"><a href="@{/products(id=${item.id})}"></a>${item.name}</h2>
                  <p class="cart-text"></p>
                  <p class="cart-text"></p>
                </div>
              </div>
            </td>
            <td>
              <div class="input-group">
                <button id="${'minus_' + key}" type="button" class="btn btn-number btn-minus" disabled="disabled">
                  <img class="svg" src="/images/common/icon-minus.svg" alt="minus">
                </button>
                <input id="${key}" type="text" name="${item.option}" class="form-control input-number" value="${item.quantity}" min="1" max="30">
                <button id="${'plus_' + key}" type="button" class="btn btn-number btn-plus">
                  <img class="svg" src="/images/common/icon-plus.svg" alt="plus">
                </button>
              </div>
              <ul class="cart-price">
                <li class="discount"></li>
                <li id="${'itemPrice_' + key}" class="salePrice">${item.price * item.quantity}</li>
              </ul>
            </td>
            <td>
              <p class="cart-color"> [Option: ${item.option}] </p>
            </td>
            <td>
              <div class="cart-btn">
                <button class="btn btn-outline-secondary delete-btn">Xóa Sản Phẩm</button>
              </div>
            </td>
            <div hidden ><input id="${'price_' + key}" value="${item.price}"></div>
        `;
        bodyItem.appendChild(trElement);

        const decreaseBtn = trElement.querySelector('.btn-minus');
        const increaseBtn = trElement.querySelector('.btn-plus');
        const quantityInput = trElement.querySelector('.input-number');
        const checkboxBtn = trElement.querySelector('.custom-control-input');

        toggleDecreaseButton(decreaseBtn, item.quantity);
        toggleIncreaseButton(increaseBtn, item.quantity);

        checkboxBtn.addEventListener('click', function() {
            const id = 'checkbox_' + key;
            let checkboxItem = document.getElementById(id);
            if (checkboxItem.checked) {
                const idQuantity = key;
                let quantityItemDefault = document.getElementById(idQuantity);
                item.quantity = parseInt(quantityItemDefault.value);
            } else {
                item.quantity = 0;
            }

            reUpdateTotalCartDetail(orderItems);
        });

        decreaseBtn.addEventListener('click', function() {
            const id = key;
            let quantityItem = document.getElementById(id);
            const currentValue = parseInt(quantityItem.value);

            const itemPriceElement = document.getElementById('itemPrice_' + id);
            const priceElement = document.getElementById('price_' + id);

            if (currentValue > 1) {
                const quantity = currentValue - 1;
                trElement.value = quantity;
                quantityItem.value = quantity;
                
                let priceItem = parseInt(quantity) * parseInt(priceElement.value);
                itemPriceElement.textContent = formatMoney(priceItem);
                item.quantity = quantity;

                toggleDecreaseButton(decreaseBtn, quantity);
                toggleIncreaseButton(increaseBtn, quantity);
                reUpdateTotalCartDetail(orderItems);
                addToCart();
            }
        });

        increaseBtn.addEventListener('click', function() {
            const id = key;
            let quantityItem = document.getElementById(id);
            const currentValue = parseInt(quantityItem.value);

            const itemPriceElement = document.getElementById('itemPrice_' + id);
            const priceElement = document.getElementById('price_' + id);

            if (currentValue > 0 && currentValue <= 30) {
                const quantity = currentValue + 1;
                trElement.value = quantity;
                quantityItem.value = quantity;
                
                let priceItem = parseInt(quantity) * parseInt(priceElement.value);
                itemPriceElement.textContent = formatMoney(priceItem);
                item.quantity = quantity;

                toggleDecreaseButton(decreaseBtn, quantity);
                toggleIncreaseButton(increaseBtn, quantity);
                reUpdateTotalCartDetail(orderItems);
                addToCart();
            }
        });

        quantityInput.addEventListener('input', function() {
            let id = quantityInput.id;
            let quantity = parseInt(quantityInput.value);
            if (isNaN(quantity) || quantity < 1) {
                quantity = 1;
            }

            const itemPriceElement = document.getElementById('itemPrice_' + id);
            const priceElement = document.getElementById('price_' + id);

            let priceItem = parseInt(quantity) * parseInt(priceElement.value);
            itemPriceElement.textContent = formatMoney(priceItem);
            item.quantity = quantity;

            reUpdateTotalCartDetail(orderItems);
            toggleDecreaseButton(decreaseBtn, quantity);
            toggleIncreaseButton(increaseBtn, quantity);
            addToCart();
        });
        
        trElement.querySelector('.delete-btn').addEventListener('click', function() {
            orderItems = orderItems.filter(orderItem => orderItem !== item);
            sessionStorage.setItem('orderItems', JSON.stringify(orderItems));
            bodyItem.removeChild(trElement);
            if (orderItems.length === 0) {
                showEmptyCart();
            } else {
                updateTotalCartDetail();
            }
            addToCart();
        });

        function toggleDecreaseButton(decreaseBtn, quantity) {
            if (quantity <= 1) {
                decreaseBtn.setAttribute('disabled', 'disabled');
            } else {
                decreaseBtn.removeAttribute('disabled');
            }
        }

        function toggleIncreaseButton(increaseBtn, quantity) {
            if (quantity >= 30) {
                increaseBtn.setAttribute('disabled', 'disabled');
            } else {
                increaseBtn.removeAttribute('disabled');
            }
        }
    });

    cartInfo.append(itemElement);
}

function updateTotalCartDetail() {
    let items = sessionStorage.getItem('orderItems');
    let orderItems = JSON.parse(items);
    let totalCart = 0;
    orderItems.forEach(item => {
        let quantity = parseInt(item.quantity);
        let price = parseInt(item.price);
        totalCart += quantity * price;
    });
    
    totalBillTemp.textContent = formatMoney(totalCart) + " VND";
    totalBill.textContent = formatMoney(totalCart) + " VND";
}

function addToCart() {
    let orderItems = sessionStorage.getItem('orderItems');
    fetch('/update-to-cart', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: orderItems
    })
    .then(response => {
        if (response.ok) {
            setTimeout(() => {
                showNotification('success', 'Cập nhập giỏ hàng thành công!')
            }, 500);
        }
    })
    .catch((error) => {
        showNotification('error', 'Cập nhập giỏ hàng thất bại!')
        console.log("addToCartButton error = " + error)
    });
}

function handleOrderClick() {
    // Check if there are active provinces
    const hasActiveProvinces = document.getElementById('hasActiveProvinces')?.value === 'true';
    
    if (!hasActiveProvinces) {
        // Show popup notification
        showPopup('orderDisabled');
        return;
    }
    
    // Proceed with normal order flow
    order();
}

function order() {
    let items = sessionStorage.getItem('orderItems');
    fetch('/update-to-cart', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: items
    })
        .then(response => {
            if (response.ok) {
                window.location.href = '/payment-detail';
            }
        })
        .catch((error) => {
            showPopup('fail')
            console.log("order error = " + error)
        });
}

function reUpdateTotalCartDetail(orderItems) {
    sessionStorage.setItem('orderItems', JSON.stringify(orderItems));
    updateTotalCartDetail()
}

function showPopup(type) {
    if (type === 'success') {
        document.getElementById("removeToCardSuccess").style.display = "flex";
    } else if (type === 'orderDisabled') {
        document.getElementById("orderDisabledPopup").style.display = "flex";
    } else {
        document.getElementById("removeToCardFail").style.display = "flex";
    }
}

// Hàm đóng popup
function closePopup(popupId) {
    document.getElementById(popupId).style.display = "none";
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

function hideNotification() {
    const notification = document.getElementById('notification');
    notification.className = 'notification';
}

updateTotalCartDetail();
renderCartItems();
