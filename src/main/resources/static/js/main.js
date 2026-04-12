document.addEventListener('DOMContentLoaded', function() {
    // Обработка для data-confirm (универсальное сообщение)
    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', (e) => {
            const message = form.getAttribute('data-confirm');
            if (message && !confirm(message)) {
                e.preventDefault();
            }
        });
    });

    // Обработка для data-code (специальный формат для промокодов)
    document.querySelectorAll('form[data-code]').forEach(form => {
        form.addEventListener('submit', (e) => {
            const code = form.getAttribute('data-code');
            if (code && !confirm(`Удалить промокод «${code}»?`)) {
                e.preventDefault();
            }
        });
    });
});