let currentPushSubscription = null;

async function subscribeToPush() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        alert('Ваш браузер не поддерживает push-уведомления');
        return;
    }

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
        alert('Разрешение на уведомления не получено. Вы можете изменить это в настройках браузера.');
        return;
    }

    try {
        const keyResponse = await fetch('/api/push/public-key');
        const { publicKey } = await keyResponse.json();

        const registration = await navigator.serviceWorker.ready;
        let subscription = await registration.pushManager.getSubscription();
        if (!subscription) {
            subscription = await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: urlB64ToUint8Array(publicKey)
            });
        }

        currentPushSubscription = subscription;

        const saveResponse = await fetch('/api/push/subscribe', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(subscription.toJSON())
        });

        if (saveResponse.ok) {
            alert('Уведомления включены!');
            document.getElementById('enable-notifications-btn').style.display = 'none';
        } else {
            alert('Ошибка сохранения подписки');
        }
    } catch (e) {
        console.error('Ошибка подписки:', e);
        alert('Не удалось включить уведомления');
    }
}

async function unsubscribeFromPush() {
    if (!currentPushSubscription) return;
    try {
        await fetch('/api/push/unsubscribe', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ endpoint: currentPushSubscription.endpoint })
        });
        currentPushSubscription = null;
        console.log('Подписка удалена');
    } catch (e) {
        console.error('Ошибка при удалении подписки:', e);
    }
}

function urlB64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) {
        outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
}