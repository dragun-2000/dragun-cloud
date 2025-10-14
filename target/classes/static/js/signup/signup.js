$(function () {
    'use strict';

    $(function () {
        $("#register_account_form").on('submit', function (event) {
            const self = this;
            // const isValid = checkFormRegistrationAccount();
            if (this.checkValidity() === false) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                const accountId = $('input[name=accountId]').val();
                const fullName = $('input[name=fullName]').val();
                const mailAddress = $('input[name=mailAddress]').val();
                const password = $('input[name=password]').val();
                const confirmPassword = $('input[name=confirmPassword]').val();
                const phone = $('input[name=phone]').val();
                const province = $('select[name=province]').val();
                const district = $('select[name=district]').val();
                const ward = $('select[name=ward]').val();
                const address = $('input[name=address]').val();

                const data = {
                                fullName: fullName, mailAddress: mailAddress, password: password, confirmPassword: confirmPassword,
                                province: province, district: district, ward: ward, address: address, phone: phone, accountId: accountId
                            };
                $.ajax({
                   type: "POST",
                   contentType: "application/json",
                   url: "/SA/SA001/register",
                   data: JSON.stringify(data),
                   headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
                   success: function (data) {
                      $('#message-success').text(data);
                      $('#register-account-success').modal('show');
                   },
                   error: function(error) {
                      console.log('error', error);
                      $('#message-error').text(error.responseText);
                      $('#register-account-error').modal('show');
                   }
               });
               event.preventDefault();
            }
            this.classList.add('was-validated');
        });
        
        $("#update_account_form").on('submit', function (event) {
            const self = this;
            // const isValid = checkFormRegistrationAccount();
            if (this.checkValidity() === false) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                const accountId = $('input[name=accountId]').val();
                const fullName = $('input[name=fullName]').val();
                const mailAddress = $('input[name=mailAddress]').val();
                const password = $('input[name=password]').val();
                const confirmPassword = $('input[name=confirmPassword]').val();
                const phone = $('input[name=phone]').val();
                const province = $('select[name=province]').val();
                const district = $('select[name=district]').val();
                const ward = $('select[name=ward]').val();
                const address = $('input[name=address]').val();

                const data = {
                                fullName: fullName, mailAddress: mailAddress, password: password, confirmPassword: confirmPassword,
                                province: province, district: district, ward: ward, address: address, phone: phone, accountId: accountId
                            };
                $.ajax({
                   type: "POST",
                   contentType: "application/json",
                   url: "/SA/SA001/register",
                   data: JSON.stringify(data),
                   headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
                   success: function (data) {
                      $('#message-success').text(data);
                      $('#update-account-success').modal('show');
                   },
                   error: function(error) {
                      console.log('error', error);
                      $('#message-error').text(error.responseText);
                      $('#register-account-error').modal('show');
                   }
               });
               event.preventDefault();
            }
            this.classList.add('was-validated');
        });
    });
});

$(document).ready(function() {
    $('#provinceSelect').change(function() {
      const provinceCode = $(this).val();
      $('#districtSelect').empty().append('<option value="">-- Select District --</option>');
        if (provinceCode) {
            $.get('/districts/' + provinceCode, function(districts) {
                $.each(districts, function(index, district) {
                    $('#districtSelect').append('<option value="' + district.code + '">' + district.name + '</option>');
                });
            });
        }
    });
    
    $('#districtSelect').change(function() {
      const districtCode = $(this).val();
      $('#wardSelect').empty().append('<option value="">-- Select Ward --</option>');
        if (districtCode) {
            $.get('/wards/' + districtCode, function(districts) {
                $.each(districts, function(index, district) {
                    $('#wardSelect').append('<option value="' + district.code + '">' + district.name + '</option>');
                });
            });
        }
    });
});

window.addEventListener('load', function (ev) {
//     addPatternAttr('mailAddress', emailRegex);
//     addPatternAttr('password', passwordUserRegex);
//     addPatternAttr('confirmPassword', passwordUserRegex);
});
