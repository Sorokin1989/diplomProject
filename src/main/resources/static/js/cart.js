// cart.js

// Функция для получения CSRF-токена
function getCsrfToken() {
    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
    if (tokenMeta && headerMeta) {
        return {
            token: tokenMeta.getAttribute('content'),
            header: headerMeta.getAttribute('content')
        };
    }
    return null;
}

// Удаление позиции из корзины
function removeItem(itemId) {
    if (!confirm('Удалить этот курс из корзины?')) return;

    const csrf = getCsrfToken();
    const headers = {};
    if (csrf) {
        headers[csrf.header] = csrf.token;
    }

    fetch('/cart/remove/' + itemId, {
        method: 'POST',
        headers: headers
    })
        .then(response => {
            if (response.ok) {
                location.reload();
            } else {
                alert('Ошибка удаления');
            }
        })
        .catch(() => alert('Ошибка сети. Попробуйте позже.'));
}

// Очистка всей корзины
function clearCart() {
    if (!confirm('Очистить всю корзину?')) return;

    const csrf = getCsrfToken();
    const headers = {};
    if (csrf) {
        headers[csrf.header] = csrf.token;
    }

    fetch('/cart/clear', {
        method: 'POST',
        headers: headers
    })
        .then(response => {
            if (response.ok) {
                location.reload();
            } else {
                alert('Ошибка очистки корзины');
            }
        })
        .catch(() => alert('Ошибка сети. Попробуйте позже.'));
}

function checkout() {
    window.location.href = '/orders/checkout';   // было '/checkout'
}