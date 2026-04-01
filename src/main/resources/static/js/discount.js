
    document.addEventListener('DOMContentLoaded', function() {
    const discountTypeSelect = document.getElementById('discountType');
    const discountValueInput = document.getElementById('discountValue');
    const valueLabel = document.getElementById('valueLabel');
    const valueIcon = document.getElementById('valueIcon');

    function updateDiscountValueField() {
    const isFixed = discountTypeSelect.value === 'FIXED';
    if (isFixed) {
    valueLabel.textContent = 'Скидка (руб.) *';
    valueIcon.className = 'fas fa-ruble-sign';
    discountValueInput.removeAttribute('max');
    discountValueInput.placeholder = 'Введите сумму скидки';
} else {
    valueLabel.textContent = 'Скидка (%) *';
    valueIcon.className = 'fas fa-percent';
    discountValueInput.setAttribute('max', '100');
    discountValueInput.placeholder = 'Введите процент скидки';
}
    discountValueInput.setAttribute('min', '0');
}

    if (discountTypeSelect) {
    discountTypeSelect.addEventListener('change', updateDiscountValueField);
    updateDiscountValueField(); // вызвать при загрузке
}
});
