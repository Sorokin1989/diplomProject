
    function updateQuantity(itemId, newQuantity) {
    if (newQuantity < 1) return;
    fetch('/cart/update/' + itemId, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: 'quantity=' + newQuantity
}).then(() => location.reload());
}

    function removeItem(itemId) {
    fetch('/cart/remove/' + itemId, { method: 'POST' })
        .then(() => location.reload());
}
