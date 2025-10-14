// $(function () {
//     'use strict';
//
//     $(function () {
//         $("#register_account_form").on('submit', function (event) {
//             var self = this;
//             var isValid = checkFormRegistrationAccount();
//             if (this.checkValidity() === false || !isValid) {
//                 event.preventDefault();
//                 event.stopPropagation();
//             } else {
//                 var fullName = $('input[name=fullName]').val();
//                 var mailAddress = $('input[name=mailAddress]').val();
//                 var password = $('input[name=password]').val();
//                 var confirmPassword = $('input[name=confirmPassword]').val();
//                 var authorities = $('select[name=authorities]').val();
//
//                 var data = {fullName: fullName, mailAddress: mailAddress, password: password, confirmPassword: confirmPassword, authorities: authorities};
//                 $.ajax({
//                    type: "POST",
//                    contentType: "application/json",
//                    url: "/AM/AM004/register",
//                    data: JSON.stringify(data),
//                    headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
//                    success: function (data) {
//                       $('#message-success').text(data);
//                       $('#register-account-success').modal('show');
//                    },
//                    error: function(error) {
//                       console.log('error', error);
//                       $('#message-error').text(error.responseText);
//                       $('#register-account-error').modal('show');
//                    }
//                });
//                event.preventDefault();
//             }
//             this.classList.add('was-validated');
//         });
//     });
// });
//
// window.addEventListener('load', function (ev) {
//     addPatternAttr('mailAddress', emailRegex);
//     addPatternAttr('password', passwordUserRegex);
//     addPatternAttr('confirmPassword', passwordUserRegex);
// });
