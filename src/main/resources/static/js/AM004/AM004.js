$(function () {
    'use strict';

    $(".check-item").on('change', function (event) {
        disableButton()
    });

    const queryString = window.location.search;
    const urlParams = new URLSearchParams(queryString);
    const checked = urlParams.get('checked')

    const status = urlParams.get('status')
    if (status == 'success') {
         $('#upload-account-success').modal('show');
    }
    if (status == 'failure') {
         $('#upload-account-failure').modal('show');
    }

    if (checked == null || checked == 'true') {
        selectAllAccount(checked);
    } else if (checked == 'false') {
        const checkItemArray = [];
        var localStoreAccountName = $("input[name='_csrf']").val();
        var checkItemStore = JSON.parse(localStorage.getItem(localStoreAccountName));
        if (checkItemStore !== 'undefined') {
            for (var i in checkItemStore) {
                checkItemArray.push(checkItemStore[i]);
            }
        }

        var checkItems = document.getElementsByClassName("check-box-item-account");
        for (var i = 0; i < checkItems.length; i++) {
            let checkKey = checkItemArray.find(item => item['key'] == checkItems[i].id);
            if (checkKey && checkKey['value'] === true) {
                checkItems[i].checked = checkKey['value'];
            }
        }
    }

    $("#check-all-account").on('change', function (event) {
        var localStoreAccountName = $("input[name='_csrf']").val();
        localStorage.removeItem(localStoreAccountName);

        var localStoreGroupName = $("input[name='_csrf']").val() + '-group';
        localStorage.removeItem(localStoreGroupName);

        var localStoreDetailGroupName = $("input[name='_csrf']").val() + '-detail';
        localStorage.removeItem(localStoreDetailGroupName);

        var checkItems = document.getElementsByClassName("check-box-item-account");
        var checkAllElement = document.getElementById("check-all-account");
        var listAccountIds = $('#listAccountIds').val();
        var url = window.location.href;

        const queryString = window.location.search;
        const urlParams = new URLSearchParams(queryString);
        const checked = urlParams.get('checked')

        if (checkAllElement.checked === true) {
            for (var i = 0; i < checkItems.length; i++) {
                checkItems[i].checked = true;
            }

            var array = JSON.parse(listAccountIds);
            const checkItemArray = [];
            for (var i = 0; i < array.length; i++) {
                const item = {key: array[i], value: true};
                checkItemArray.push(item)
            }

            localStorage.setItem(localStoreAccountName, JSON.stringify(checkItemArray));

            if (url.includes('checked')) {
                var urlResponse = url;
                if (url.includes('checked=false')) {
                    urlResponse = url.replace('checked=false', 'checked=true');
                } else if (url.includes('checked=')) {
                    urlResponse = url.replace('checked=', 'checked=true');
                }
                window.history.pushState(url, 'debase', urlResponse);
            } else {
                var urlResponse = url;
                if (url.includes("?")) {
                    urlResponse = url + '&checked=true';
                } else {
                    urlResponse = url + '?checked=true';
                }
                window.history.pushState(url, 'debase', urlResponse);
            }

            $('.page-link').each(function(e) {
                let href = $(this).attr('href');
                if (href) {
                    if (checked && checked != null) {
                        href = href.replace('checked=false', 'checked=true');
                    } else {
                        href = href.replace('checked=', 'checked=true');
                    }
                    $(this).attr('href', href);
                }
            });
        } else {
            for (var i = 0; i < checkItems.length; i++) {
                checkItems[i].checked = false;
            }

            if (url.includes('checked')) {
                if (url.includes('checked=true')) {
                    urlResponse = url.replace('checked=true', 'checked=false');
                } else if (url.includes('checked=')) {
                    urlResponse = url.replace('checked=', 'checked=false');
                }
                window.history.pushState(url, 'debase', urlResponse);
            } else {
                var urlResponse = url;
                if (url.includes("?")) {
                    urlResponse = url + '&checked=false';
                } else {
                    urlResponse = url + '?checked=false';
                }
                window.history.pushState(url, 'debase', urlResponse);
            }

            $('.page-link').each(function(e) {
                let href = $(this).attr('href');
                if (href) {
                    if (checked && checked != null) {
                        href = href.replace('checked=true', 'checked=false');
                    } else {
                        href = href.replace('checked=', 'checked=false');
                    }
                    $(this).attr('href', href);
                }
            });
        }

        disableButton()
    });

    $("#confirm-delete-account").on('click', function(event) {
        event.preventDefault();
        var checkItems = document.getElementsByClassName("check-item");
        var checkItemIds = [];

        for(var i = 0; i < checkItems.length; i++)
        {
            if (checkItems[i].children[0].checked === true) {
                checkItemIds.push(checkItems[i].id)
            }
        }
        $.ajax({
            type: 'POST',
            contentType: "application/json",
            data: JSON.stringify(checkItemIds),
            url: '/AM/AM004/deleted',
            headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
            success: function (data) {
                $('#delete-account-success').modal('show');
                $('#delete-account-success').modal('hide');
                $('#delete-account-modal').modal('hide');
            },
            error: function () {
                $('#message-error').text(error.responseText);
                $('#delete-account-error').modal('show');
                $('#delete-account-error').modal('hide');
                $('#delete-account-modal').modal('hide');
            }
        });
    });
});

function selectAllAccount(checked) {
    var localStoreGroupName = $("input[name='_csrf']").val() + '-group'
    localStorage.removeItem(localStoreGroupName);

    var localStoreDetailGroupName = $("input[name='_csrf']").val() + '-detail'
    localStorage.removeItem(localStoreDetailGroupName);

    var checkItems = document.getElementsByClassName("check-box-item-account");
    var checkAllElement = document.getElementById("check-all-account");

    var localStoreAccountName = $("input[name='_csrf']").val();
    var checkItemStore = JSON.parse(localStorage.getItem(localStoreAccountName));

    const checkItemArray = [];
    if (checkItemStore !== 'undefined' && checkItemStore !== null) {
        for (var i in checkItemStore) {
            checkItemArray.push(checkItemStore[i]);
        }
    }

    let checkSelectAll = checkItemArray.find(item => item['value'] == false);
    if (checkAllElement != null) {
        if (checkSelectAll == null && checkItemArray.length > 0) {
            for (var i = 0; i < checkItems.length; i++) {
                checkItems[i].checked = true;
            }

            checkAllElement.checked = true;
            updateUrl(false, true);
        } else {
            for (var i = 0; i < checkItems.length; i++) {
                checkItems[i].checked = false;
            }

            checkAllElement.checked = false;
            updateUrl(true, false);
        }
    }

    disableButton()
}

//check selected items
$("input.check-box-item-account").click(function () {
    const checkItemArray = [];
    var localStoreAccountName = $("input[name='_csrf']").val();
    var checkItemStore = JSON.parse(localStorage.getItem(localStoreAccountName));
    var checkItems = document.getElementsByClassName("check-box-item-account");
    var checkAll = $("input#check-all-account")[0];

    if (checkItemStore !== 'undefined' && checkItemStore !== null) {
        for (var i in checkItemStore) {
            checkItemArray.push(checkItemStore[i]);
        }
    }

    for (var i = 0; i < checkItems.length; i++) {
        let checkKey = checkItemArray.find(item => item['key'] == checkItems[i].id);

        if (checkKey) {
            checkKey['value'] = checkItems[i].checked;
        } else {
            const item = {key: checkItems[i].id, value: checkItems[i].checked};
            checkItemArray.push(item)
        }
    }

    var totalAccountIds = $('#totalAccountIds').val();

    let checkSelectAll = checkItemArray.find(item => item['value'] == false);
    if (checkSelectAll == null && checkItemArray.length > 0 && checkItemArray.length == totalAccountIds) {
        checkAll.checked = true;
        updateUrl(false, true);
    } else {
        checkAll.checked = false;
        updateUrl(true, false);
    }

    localStorage.setItem(localStoreAccountName, JSON.stringify(checkItemArray));
});

function updateUrl(current, checked) {

    const queryString = window.location.search;
    const urlParams = new URLSearchParams(queryString);
    const checkedExist = urlParams.get('checked')

    var url = window.location.href;
    if (url.includes('checked')) {

        var urlResponse = url;
        if (url.includes('checked=true')) {
            urlResponse = url.replace('checked=' + current, 'checked=' + checked);
        } else if (url.includes('checked=false')) {
            urlResponse = url.replace('checked=' + current, 'checked=' + checked);
        } else if (url.includes('checked=')) {
            urlResponse = url.replace('checked=', 'checked=' + checked);
        }

        $('.page-link').each(function(e) {
            let href = $(this).attr('href');
            if (href) {
                if (checkedExist && checkedExist != null) {
                    href = href.replace('checked=' + current, 'checked=' + checked);
                } else {
                    href = href.replace('checked=', 'checked=' + checked);
                }
                $(this).attr('href', href);
            }
        });

        window.history.pushState(url, 'debase', urlResponse);
    } else {
        var urlResponse = url;
        if (url.includes("?")) {
            urlResponse = url + '&checked=' + checked;
        } else {
            urlResponse = url + '?checked=' + checked;
        }

        $('.page-link').each(function(e) {
            let href = $(this).attr('href');
            if (href) {
                if (checkedExist && checkedExist != null) {
                    href = href.replace('checked=' + current, 'checked=' + checked);
                } else {
                    href = href.replace('checked=', 'checked=' + checked);
                }
                $(this).attr('href', href);
            }
        });
        window.history.pushState(url, 'debase', urlResponse);
    }
}

function disableButton() {
    var checkbox = getCheckedItem();
    if (checkbox == null || checkbox == '') {
        $("#delete-account").attr('disabled', true);
    } else {
        $("#delete-account").attr('disabled', false);
    }
}

function getCheckedItem() { //TODO get item from localStore
     var checkItems = document.getElementsByClassName("check-item");
     var checkItemIds = '';

     for(var i = 0; i < checkItems.length; i++) {
         if (checkItems[i].children[0].checked === true) {
             checkItemIds += checkItems[i].id
             checkItemIds += "::,::"
         }
     }
     return checkItemIds;
}

function openImportCSV() {
    $('#import-email').modal('show');
}

$('#chooseFile').bind('change', function () {
  var filename = $("#chooseFile").val();
  if (/^\s*$/.test(filename)) {
    $(".file-upload").removeClass('active');
    $("#noFile").text("No file chosen...");
  }
  else {
    $(".file-upload").addClass('active');
    $("#noFile").text(filename);
    $("#noFile").text(filename.replace("C:\\fakepath\\", ""));
  }
});
//
//async function uploadFile() {
//  let formData = new FormData();
//  formData.append("file", chooseFile.files[0]);
//
//  var input = document.querySelector('input[type="file"]')
//  let response = await fetch('/AM/AM004-02/import', {
//    method: "POST",
//    body: formData
//  }).then(r => r.json())
//    .then(data => {
//      if (data.status == 200) {
//          $('#import-email').modal('hide');
//          $('#upload-account-success').modal('show');
//        }
//    });
//}


