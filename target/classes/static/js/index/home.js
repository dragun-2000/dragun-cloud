$(function(){
  $('.slider--marquee').slick({
    speed: 15000,
    autoplay: true,
    autoplaySpeed: false,
    cssEase: 'linear',
    // slidesToShow: 1,
    // slidesToScroll: 1,
    variableWidth: true,
    infinite: true,
    //initialSlide: 1,
    arrows: false,
    buttons: false,
  });
  //
  $('.slider-news').slick({
    dots: false,
    arrows: true,
    infinite: true,
    speed: 500,
    fade: true,
    cssEase: 'linear',
    autoplay: true,
  });
  //
  $('.slider-thumb').slick({
    dots: true,
    arrows: false,
    infinite: true,
    speed: 500,
    fade: true,
    cssEase: 'linear',
    autoplay: true,
    slidesToShow: 1,
  });
  //
  $('.slider-products').slick({
    slidesToShow: 3,
    slidesToScroll: 1,
    autoplay: true,
    autoplaySpeed: 3000,
    arrows: false,
    dot: false,
    responsive: [
      {
        breakpoint: 991,
        settings: {
          slidesToShow: 2,
        }
      }
    ]
  });
  //
  // sliderPhotoUser
  $('.sliderPhoto').slick({
    infinite: true,
    slidesToShow: 8,
    slidesToScroll: 8,
    dots: false,
    arrows: false,
    responsive: [
      {
        breakpoint: 767,
        settings: {
          slidesToShow: 5,
          slidesToScroll: 5,
        }
      }
    ]
  });
  //
  $("body").on("click", ".xans-color li", function(){
    $('.xans-color li').removeClass();
    $(this).addClass('active');
  });
  $("body").on("click", ".xans-size li", function(){
    // $('.xans-color li').removeClass();
    // $(this).addClass('active');
    $(this).toggleClass('active');
  });
  $("body").on("click", ".xans-size .size-s", function(){
    $(".beige-s").toggleClass('beige-show');
  });
  $("body").on("click", ".xans-size .size-m", function(){
    $(".beige-m").toggleClass('beige-show');
  });
  $("body").on("click", ".xans-size .size-l", function(){
    $(".beige-l").toggleClass('beige-show');
  });
  //
  $('.btn-number').click(function(e){
    e.preventDefault();
    
    fieldName = $(this).attr('data-field');
    type      = $(this).attr('data-type');
    var input = $("input[name='"+fieldName+"']");
    var currentVal = parseInt(input.val());
    if (!isNaN(currentVal)) {
      if(type == 'minus') {
          
        if(currentVal > input.attr('min')) {
          input.val(currentVal - 1).change();
        } 
        if(parseInt(input.val()) == input.attr('min')) {
          $(this).attr('disabled', true);
        }

        } else if(type == 'plus') {

          if(currentVal < input.attr('max')) {
            input.val(currentVal + 1).change();
          }
          if(parseInt(input.val()) == input.attr('max')) {
            $(this).attr('disabled', true);
          }
        }
    } else {
      input.val(0);
    }
  });
  $('.input-number').focusin(function(){
    $(this).data('oldValue', $(this).val());
  });
  $('.input-number').change(function() {
    minValue =  parseInt($(this).attr('min'));
    maxValue =  parseInt($(this).attr('max'));
    valueCurrent = parseInt($(this).val());
    
    name = $(this).attr('name');
    if(valueCurrent >= minValue) {
        $(".btn-number[data-type='minus'][data-field='"+name+"']").removeAttr('disabled')
    } else {
        alert('Sorry, the minimum value was reached');
        $(this).val($(this).data('oldValue'));
    }
    if(valueCurrent <= maxValue) {
        $(".btn-number[data-type='plus'][data-field='"+name+"']").removeAttr('disabled')
    } else {
        alert('Sorry, the maximum value was reached');
        $(this).val($(this).data('oldValue'));
    }
      
      
  });
  $(".input-number").keydown(function (e) {
    // Allow: backspace, delete, tab, escape, enter and .
    if ($.inArray(e.keyCode, [46, 8, 9, 27, 13, 190]) !== -1 ||
       // Allow: Ctrl+A
      (e.keyCode == 65 && e.ctrlKey === true) || 
       // Allow: home, end, left, right
      (e.keyCode >= 35 && e.keyCode <= 39)) {
           // let it happen, don't do anything
           return;
    }
    // Ensure that it is a number and stop the keypress
    if ((e.shiftKey || (e.keyCode < 48 || e.keyCode > 57)) && (e.keyCode < 96 || e.keyCode > 105)) {
      e.preventDefault();
    }
  });
});
function ratingStar(star){
  star.click(function(){
    var stars = $('.star-list--is').find('li')
    stars.removeClass('checked');
    var thisIndex = $(this).parents('li').index();
    for(var i=0; i <= thisIndex; i++){
      stars.eq(i).addClass('checked');
    }
    putScoreNow(thisIndex+1);
  });
}

$(function(){
  if($('.star-list--is').length > 0){
      ratingStar($('.star-list--is li span'));
  }
});