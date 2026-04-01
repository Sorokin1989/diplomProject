document.addEventListener('DOMContentLoaded', function() {
    const discountTypeSelect = document.getElementById('discountType');
    const valueInput = document.getElementById('value');
    const valueLabel = document.getElementById('valueLabel');
    const valueIcon = document.getElementById('valueIcon');

    function updateValueField() {
        const isFixed = discountTypeSelect.value === 'FIXED';
        if (isFixed) {
            valueLabel.textContent = 'Скидка (руб.) *';
            valueIcon.className = 'fas fa-ruble-sign';
            valueInput.removeAttribute('max');
            valueInput.placeholder = 'Введите сумму скидки';
        } else {
            valueLabel.textContent = 'Скидка (%) *';
            valueIcon.className = 'fas fa-percent';
            valueInput.setAttribute('max', '100');
            valueInput.placeholder = 'Введите процент скидки';
        }
        // min всегда 0
        valueInput.setAttribute('min', '0');
    }

    if (discountTypeSelect) {
        discountTypeSelect.addEventListener('change', updateValueField);
        updateValueField(); // вызвать при загрузке
    }
});