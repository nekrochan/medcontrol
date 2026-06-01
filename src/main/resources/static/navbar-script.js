document.addEventListener('DOMContentLoaded', function() {

    const createBtn = document.getElementById('createProfileBtn');
    if (createBtn) {
        createBtn.addEventListener('click', function() {
            const modalEl = document.getElementById('createProfileModalUser')
                         || document.getElementById('create-profile-modal');
            if (modalEl) {
                const modal = new bootstrap.Modal(modalEl);
                modal.show();
            }
        });
    }

    document.querySelectorAll('.modal').forEach(function(modalEl) {
        modalEl.addEventListener('shown.bs.modal', function() {
            const input = modalEl.querySelector('input[type="text"], input:not([type="hidden"])');
            if (input) setTimeout(() => input.focus(), 150);
        });
    });


});

    document.getElementById('profileToggle').addEventListener('click', function(e) {
        e.stopPropagation();
        document.getElementById('profileMenu').classList.toggle('open');
    });
    document.addEventListener('click', function() {
        document.getElementById('profileMenu').classList.remove('open');
    });

    if ('serviceWorker' in navigator) {
        window.addEventListener('load', () => {
            navigator.serviceWorker.register('/sw.js')
                .then(reg => console.log('SW зарегистрирован'))
                .catch(err => console.error('Ошибка SW:', err));
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        const btn = document.getElementById('enable-notifications-btn');
        if (btn && 'serviceWorker' in navigator && 'PushManager' in window) {
            navigator.serviceWorker.ready.then(async (reg) => {
                const subscription = await reg.pushManager.getSubscription();
                btn.style.display = (Notification.permission === 'granted' && subscription) ? 'none' : 'block';
            });
        }

            const menuToggle = document.getElementById('mobileMenuToggle');
            const sidebar = document.querySelector('.sidebar');

            if (menuToggle && sidebar) {
                menuToggle.addEventListener('click', function(e) {
                    e.stopPropagation();
                    sidebar.classList.toggle('mobile-open');
                    const icon = menuToggle.querySelector('i');
                    if (sidebar.classList.contains('mobile-open')) {
                        icon.classList.replace('bi-list', 'bi-x');
                        document.body.style.overflow = 'hidden';
                    } else {
                        icon.classList.replace('bi-x', 'bi-list');
                        document.body.style.overflow = '';
                    }
                });

                sidebar.querySelectorAll('.sidebar-link, .profile-item').forEach(link => {
                    link.addEventListener('click', function() {
                        sidebar.classList.remove('mobile-open');
                        const icon = menuToggle.querySelector('i');
                        if (icon) icon.classList.replace('bi-x', 'bi-list');
                        document.body.style.overflow = '';
                    });
                });
            }
    });