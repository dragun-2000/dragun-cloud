(function () {
  var history = [];
  var productsById = {};
  var bodyProfile = { height: null, weight: null };
  var lastProductQuery = '';

  function looksLikeBodyOnly(text) {
    var t = (text || '').toLowerCase();
    var hasBody = /\d\s*m\s*\d/i.test(t) || /\d{2,3}\s*cm/.test(t) || /\d{2,3}\s*kg/.test(t);
    var hasProduct = /áo|quần|váy|sale|jean|hoodie|sơ mi|new in|best/.test(t);
    return hasBody && !hasProduct;
  }

  function optionButtons(values, kind, productId, selected) {
    if (!values || !values.length) return '';
    var selectedNorm = (selected || '').toString().toUpperCase();
    return '<div class="db-chat-opts" data-kind="' + kind + '">' +
      values.map(function (v) {
        var isSelected = selectedNorm && v.toString().toUpperCase() === selectedNorm;
        var on = isSelected ? ' is-on' : '';
        return '<button type="button" class="db-chat-opt' + on + '" data-id="' + productId + '" data-kind="' + kind + '" data-value="' + escapeHtml(v) + '">' + escapeHtml(v) + '</button>';
      }).join('') + '</div>';
  }

  function el(html) {
    var wrap = document.createElement('div');
    wrap.innerHTML = html.trim();
    return wrap.firstChild;
  }

  function escapeHtml(text) {
    return String(text || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function bodyEl() {
    return document.getElementById('dbChatBody');
  }

  function scrollBottom() {
    var body = bodyEl();
    if (body) body.scrollTop = body.scrollHeight;
  }

  function appendMsg(text, type) {
    var node = document.createElement('div');
    node.className = 'db-chat-msg ' + type;
    node.textContent = text;
    bodyEl().appendChild(node);
    scrollBottom();
    return node;
  }

  function appendTyping() {
    var node = el('<div class="db-chat-msg bot db-chat-typing" id="dbChatTyping"><span></span><span></span><span></span></div>');
    bodyEl().appendChild(node);
    scrollBottom();
  }

  function removeTyping() {
    var t = document.getElementById('dbChatTyping');
    if (t) t.remove();
  }

  function optionButtons(values, kind, productId) {
    if (!values || !values.length) return '';
    return '<div class="db-chat-opts" data-kind="' + kind + '">' +
      values.map(function (v, i) {
        var on = i === 0 ? ' is-on' : '';
        return '<button type="button" class="db-chat-opt' + on + '" data-id="' + productId + '" data-kind="' + kind + '" data-value="' + escapeHtml(v) + '">' + escapeHtml(v) + '</button>';
      }).join('') + '</div>';
  }

  function renderCard(p) {
    productsById[p.id] = p;
    var priceHtml = p.hasDiscount
      ? '<s>' + escapeHtml(p.priceDisplay) + '</s><strong>' + escapeHtml(p.discountPriceDisplay) + ' VND</strong>'
      : '<strong>' + escapeHtml(p.discountPriceDisplay || p.priceDisplay) + ' VND</strong>';
    var disabled = p.outStock ? ' disabled' : '';
    return el(
      '<div class="db-chat-card" data-product-id="' + p.id + '">' +
        '<div class="db-chat-card-main">' +
          '<img src="' + escapeHtml(p.image || '') + '" alt="">' +
          '<div class="db-chat-card-info">' +
            '<p class="db-chat-card-name">' + escapeHtml(p.name) + '</p>' +
            '<div class="db-chat-card-price">' + priceHtml + '</div>' +
            optionButtons(p.colors, 'color', p.id, p.colors && p.colors[0]) +
            optionButtons(p.sizes, 'size', p.id, p.recommendedSize) +
            optionButtons(p.types, 'type', p.id, p.types && p.types[0]) +
            (p.recommendedSize ? '<div class="db-chat-toast" style="display:block">Size gợi ý: ' + escapeHtml(p.recommendedSize) + '</div>' : '') +
            '<div class="db-chat-actions">' +
              '<a class="db-chat-btn-view" href="' + escapeHtml(p.detailUrl) + '">Xem</a>' +
              '<button type="button" class="db-chat-btn-cart"' + disabled + ' data-add="' + p.id + '">Thêm giỏ</button>' +
            '</div>' +
            '<div class="db-chat-toast" hidden></div>' +
          '</div>' +
        '</div>' +
      '</div>'
    );
  }

  function selectedOpt(card, kind) {
    var on = card.querySelector('.db-chat-opt.is-on[data-kind="' + kind + '"]');
    return on ? on.getAttribute('data-value') : null;
  }

  function findVariation(product, card) {
    var vars = product.variations || [];
    if (!vars.length) return null;
    var color = selectedOpt(card, 'color');
    var size = selectedOpt(card, 'size');
    var type = selectedOpt(card, 'type');
    var match = vars.filter(function (v) {
      if (color && v.color && v.color !== color) return false;
      if (size && v.size && v.size !== size) return false;
      if (type && v.type && v.type !== type) return false;
      return !v.remainQuantity || v.remainQuantity > 0;
    });
    return match[0] || vars[0];
  }

  function bumpBagCount() {
    ['bag-count', 'bag-count-mobile'].forEach(function (id) {
      var n = document.getElementById(id);
      if (!n) return;
      var current = parseInt(n.textContent, 10);
      n.textContent = (isNaN(current) ? 0 : current) + 1;
    });
  }

  function addToCart(productId, card) {
    var product = productsById[productId];
    if (!product) return;
    if (product.sizes && product.sizes.length && !selectedOpt(card, 'size')) {
      var toastNeed = card.querySelector('.db-chat-toast');
      if (toastNeed) {
        toastNeed.hidden = false;
        toastNeed.textContent = 'Chọn size trước khi thêm giỏ, hoặc gửi chiều cao/cân nặng để mình gợi ý.';
      }
      return;
    }
    var variation = findVariation(product, card);
    if (!variation || !variation.variationId) {
      window.location.href = product.detailUrl;
      return;
    }
    var optionParts = [selectedOpt(card, 'color'), selectedOpt(card, 'type'), selectedOpt(card, 'size')].filter(Boolean);
    var payload = [{
      id: product.id,
      variationId: variation.variationId,
      name: product.name,
      quantity: 1,
      price: product.finalPrice,
      image: variation.image || product.image,
      option: optionParts.join('/')
    }];
    var btn = card.querySelector('[data-add]');
    if (btn) btn.disabled = true;
    fetch('/add-to-cart', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(function (res) {
      var toast = card.querySelector('.db-chat-toast');
      if (res.ok) {
        bumpBagCount();
        if (toast) {
          toast.hidden = false;
          toast.textContent = 'Đã thêm vào giỏ';
        }
      } else if (toast) {
        toast.hidden = false;
        toast.textContent = 'Không thêm được. Mở chi tiết để chọn đủ màu/size.';
      }
    }).catch(function () {
      window.location.href = product.detailUrl;
    }).finally(function () {
      if (btn && !product.outStock) btn.disabled = false;
    });
  }

  function send(text) {
    var message = (text || '').trim();
    if (!message) return;
    if (!looksLikeBodyOnly(message)) {
      lastProductQuery = message;
    } else if (lastProductQuery) {
      message = lastProductQuery + '. ' + message;
    }
    appendMsg(text, 'user');
    history.push({ role: 'user', content: message });
    appendTyping();
    var input = document.getElementById('dbChatInput');
    var sendBtn = document.getElementById('dbChatSend');
    if (input) input.value = '';
    if (sendBtn) sendBtn.disabled = true;

    fetch('/api/store-chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        message: message,
        height: bodyProfile.height,
        weight: bodyProfile.weight,
        conversationHistory: history.slice(-12)
      })
    }).then(function (res) { return res.json(); })
      .then(function (data) {
        removeTyping();
        var reply = (data && data.message) || 'Mình chưa nghe rõ, bạn nói lại giúp nhé.';
        appendMsg(reply, 'bot');
        history.push({ role: 'assistant', content: reply });
        if (data.height) bodyProfile.height = data.height;
        if (data.weight) bodyProfile.weight = data.weight;
        if (data.similarSuggestion && data.products && data.products.length) {
          var hint = document.createElement('div');
          hint.className = 'db-chat-similar-hint';
          hint.textContent = 'Gợi ý sản phẩm tương tự';
          bodyEl().appendChild(hint);
        }
        (data.products || []).forEach(function (p) {
          bodyEl().appendChild(renderCard(p));
        });
        scrollBottom();
      })
      .catch(function () {
        removeTyping();
        appendMsg('Kết nối lỗi. Bạn thử lại giúp mình.', 'bot');
      })
      .finally(function () {
        if (sendBtn) sendBtn.disabled = false;
        if (input) input.focus();
      });
  }

  function openPanel() {
    document.getElementById('dbChatPanel').classList.add('is-open');
    document.getElementById('dbChatInput').focus();
  }

  function closePanel() {
    document.getElementById('dbChatPanel').classList.remove('is-open');
  }

  function init() {
    var fab = document.getElementById('dbChatFab');
    if (!fab) return;
    fab.addEventListener('click', function () {
      var panel = document.getElementById('dbChatPanel');
      if (panel.classList.contains('is-open')) closePanel();
      else openPanel();
    });
    document.getElementById('dbChatClose').addEventListener('click', closePanel);
    document.getElementById('dbChatForm').addEventListener('submit', function (e) {
      e.preventDefault();
      send(document.getElementById('dbChatInput').value);
    });
    document.getElementById('dbChatBody').addEventListener('click', function (e) {
      var chip = e.target.closest('.db-chat-chip');
      if (chip) {
        send(chip.getAttribute('data-q'));
        return;
      }
      var opt = e.target.closest('.db-chat-opt');
      if (opt) {
        var group = opt.parentNode;
        group.querySelectorAll('.db-chat-opt').forEach(function (b) { b.classList.remove('is-on'); });
        opt.classList.add('is-on');
        return;
      }
      var addBtn = e.target.closest('[data-add]');
      if (addBtn) {
        var card = addBtn.closest('.db-chat-card');
        addToCart(parseInt(addBtn.getAttribute('data-add'), 10), card);
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
