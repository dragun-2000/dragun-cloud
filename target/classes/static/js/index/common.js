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