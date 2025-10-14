$(function(){
	$('.navLink a[href^="#"]').on('click', function (e) {
			e.preventDefault();
			$(document).off("scroll");
			
			$('.navLink a[href^="#"]').each(function () {
				$(this).removeClass('active');
			})
			$(this).addClass('active');
		
			var target = this.hash,
					menu = target;
			$target = $(target);
			$('html, body').stop().animate({
					'scrollTop': $target.offset().top+2
			}, 500, 'swing', function () {
					window.location.hash = target;
					$(document).on("scroll", onScroll);
			});
	});
})