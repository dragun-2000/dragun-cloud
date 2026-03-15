function showTab(tabName) {
    // Ẩn tất cả các tab
    const tabs = document.querySelectorAll('.tab-content');
    tabs.forEach(tab => {
        tab.style.display = 'none';
    });

    const tabButtons = document.querySelectorAll('.tab-button');
    tabButtons.forEach(button => {
        button.classList.remove('active'); // Xóa class 'active' khỏi tất cả các nút
    });

    document.getElementById(tabName).style.display = 'block';

    const activeButton = document.querySelector(`.tab-button[data-tab="${tabName}"]`);
    activeButton.classList.add('active');
}

window.onload = () => {
    var hash = window.location.hash.slice(1); // e.g. "payment-policy"
    if (hash && document.getElementById(hash)) {
        showTab(hash);
    } else {
        showTab('privacy-policy');
    }
};