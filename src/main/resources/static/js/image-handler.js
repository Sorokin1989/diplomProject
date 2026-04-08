// document.addEventListener('DOMContentLoaded', function() {
//     const deleteButtons = document.querySelectorAll('.delete-image-btn');
//
//     deleteButtons.forEach(btn => {
//         btn.addEventListener('click', function(e) {
//             if (confirm('Удалить текущее изображение?')) {
//                 const url = this.dataset.url;
//                 const csrfToken = document.querySelector('input[name="_csrf"]').value;
//
//                 fetch(url, {
//                     method: 'POST',
//                     headers: {
//                         'Content-Type': 'application/x-www-form-urlencoded',
//                         'X-CSRF-TOKEN': csrfToken
//                     },
//                     body: new URLSearchParams({
//                         '_csrf': csrfToken
//                     })
//                 }).then(response => {
//                     if (response.redirected) {
//                         window.location.href = response.url;
//                     } else {
//                         window.location.reload();
//                     }
//                 }).catch(error => {
//                     console.error('Ошибка:', error);
//                     alert('Не удалось удалить изображение');
//                 });
//             }
//         });
//     });
// });