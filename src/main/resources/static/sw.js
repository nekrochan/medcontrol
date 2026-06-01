self.addEventListener('push', event => {
    console.log('=== PUSH EVENT RECEIVED ===', event);

    let title = 'Напоминание о лекарстве';
    let body = 'Время принять лекарство';

    if (event.data) {
        const text = event.data.text();
        console.log('Push data text:', text);

        try {
            const data = JSON.parse(text);
            title = data.title || title;
            body = data.body || body;
        } catch (e) {
            const parts = text.split('|||');
            if (parts.length >= 2) {
                title = parts[0];
                body = parts[1];
            } else {
                body = text;
            }
        }
    }

    const options = {
        body: body,
        icon: '/icons/pill-icon.png',
        badge: '/icons/badge-icon.png',
        vibrate: [200, 100, 200],
        tag: 'medication-reminder',
        renotify: true,
        requireInteraction: true,
        data: {
            url: '/intakes/date/' + new Date().toISOString().slice(0, 10)
        }
    };

    console.log('Showing notification:', title, options);

    event.waitUntil(
        self.registration.showNotification(title, options)
            .then(() => console.log('Notification shown successfully'))
            .catch(err => console.error('Error showing notification:', err))
    );
});

self.addEventListener('notificationclick', event => {
    console.log('Notification clicked');
    event.notification.close();
    const url = event.notification.data?.url || '/';
    event.waitUntil(
        clients.openWindow(url)
    );
});