/**
 * Cập nhật số lượng giỏ trên header (desktop + mobile).
 * Gọi sau khi đặt hàng / thanh toán thành công — HTML server render không tự đổi khi AJAX.
 */
function updateHeaderBagCount(count) {
	var total = parseInt(count, 10);
	if (isNaN(total) || total < 0) {
		total = 0;
	}
	var desktop = document.getElementById('bag-count');
	if (desktop) {
		desktop.textContent = total;
	}
	var mobile = document.getElementById('bag-count-mobile');
	if (mobile) {
		mobile.textContent = total;
	}
	if (total === 0) {
		try {
			sessionStorage.setItem('orderItems', JSON.stringify([]));
		} catch (e) { /* ignore */ }
		if (typeof $ !== 'undefined' && $.ajax) {
			var headers = {};
			if (typeof $ === 'function') {
				var csrf = $('input[name="_csrf"]').val();
				if (csrf) {
					headers['X-CSRF-TOKEN'] = csrf;
				}
			}
			$.ajax({
				url: '/cart/sync-session',
				type: 'POST',
				contentType: 'application/json',
				data: '{}',
				headers: headers,
				xhrFields: { withCredentials: true }
			});
		}
	}
}

$(function(){
	//gNav
	$("body").on("click", ".btn-menu", function(){
		$(".hamber-menu").addClass("hamber-menu--show");
	});
	$("body").on("click", ".item-toggle .btn--close-nav", function(){
		$(".hamber-menu").removeClass("hamber-menu--show");
		$(".navigation-sub").slideUp();
		$(".navigation > li > a").removeClass("active");
	});
	//
	$("body").on("click", ".navigation > li > a", function(){
		if($(this).hasClass("active")){
			$(this).removeClass("active");
			$(this).removeClass("active").siblings(".navigation-sub").slideUp();
		}
		else {
			$(this).addClass("active").siblings(".navigation-sub").slideDown();
		}
	});

});