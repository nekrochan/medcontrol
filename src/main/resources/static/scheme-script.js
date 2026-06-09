const statusTranslations = {
    'ACTIVE': 'Активна',
    'COMPLETED': 'Завершена',
    'PAUSED': 'Приостановлена',
    'INACTIVE': 'Неактивна'
};

const schemeTypeTranslations = {
    'ALTERNATION': 'Чередование',
    'WEEKDAYS': 'Дни недели',
    'UNSPECIFIED': 'Не указан'
};

var existingAlarms = [];
var existingDays = '';

function translateSchemeStatuses() {
    const statusBadges = document.querySelectorAll('.status-badge');

    statusBadges.forEach(function(badge) {
        const originalStatus = badge.getAttribute('data-status');

        if (originalStatus && statusTranslations[originalStatus]) {
            const translatedText = statusTranslations[originalStatus];
            if (badge.textContent !== translatedText) {
                badge.textContent = translatedText;
            }

            if (originalStatus === 'COMPLETED') {
                if (badge.classList.contains('bg-info')) {
                    badge.classList.remove('bg-info');
                    badge.classList.add('bg-primary');
                }
            }
        }
    });
}

function translateSchemeTypes() {
    const schemeTypeElements = document.querySelectorAll('.scheme-type-value');
    schemeTypeElements.forEach(function(element) {
        const originalType = element.getAttribute('data-scheme-type');
        if (originalType && schemeTypeTranslations[originalType]) {
            const translatedText = schemeTypeTranslations[originalType];
            if (element.textContent !== translatedText) {
                element.textContent = translatedText;
            }
        }
    });
}

function toggleSchemeTypeFields() {
    const selector = document.getElementById('schemeTypeSelector');
    if (!selector) return;

    const selectedType = selector.value;
    const hiddenType = document.getElementById('schemeTypeHidden');

    if (hiddenType) {
        hiddenType.value = selectedType;
    }

    const alternationFields = document.getElementById('alternationFields');
    const daysOfWeekFields = document.getElementById('daysOfWeekFields');

    if (alternationFields) {
        alternationFields.style.display = selectedType === 'ALTERNATION' ? 'block' : 'none';
    }

    if (daysOfWeekFields) {
        daysOfWeekFields.style.display = selectedType === 'WEEKDAYS' ? 'block' : 'none';
    }

    if (selectedType === 'WEEKDAYS') {
        initDayChips();
    }
}

function initDayChips() {
    const chipsContainer = document.getElementById('dayChips');
    if (!chipsContainer) return;

    const chips = chipsContainer.querySelectorAll('.day-chip');

    chips.forEach(chip => {
        chip.removeEventListener('click', dayChipClickHandler);
        chip.addEventListener('click', dayChipClickHandler);
        chip.style.cursor = 'pointer';
        chip.style.userSelect = 'none';
    });

    updateSelectedDays();
}

function dayChipClickHandler(event) {
    event.stopPropagation();
    this.classList.toggle('selected');
    updateSelectedDays();
}

function updateSelectedDays() {
    const selectedChips = document.querySelectorAll('#dayChips .day-chip.selected');
    const selectedDays = Array.from(selectedChips).map(chip => chip.getAttribute('data-day'));

    let hiddenInput = document.getElementById('selectedDays');

    if (!hiddenInput) {
        hiddenInput = document.createElement('input');
        hiddenInput.type = 'hidden';
        hiddenInput.name = 'selectedDays';
        hiddenInput.id = 'selectedDays';
        const form = document.getElementById('scheduleForm');
        if (form) form.appendChild(hiddenInput);
    }

    hiddenInput.value = selectedDays.join(',');
    console.log('Selected days updated:', hiddenInput.value);
}

function addTimeInput(value) {
    const timesList = document.getElementById('timesList');
    if (!timesList) return;

    const timeRow = document.createElement('div');
    timeRow.className = 'time-input-row';
    timeRow.style.display = 'flex';
    timeRow.style.gap = '10px';
    timeRow.style.marginBottom = '10px';

    timeRow.innerHTML = `
        <input type="time" name="notificationTimes" class="form-control" required style="flex-grow: 1;" value="${value || ''}">
        <button type="button" class="btn btn-outline-danger" onclick="this.parentElement.remove()" title="Удалить">
            <i class="bi bi-trash"></i>
        </button>
    `;
    timesList.appendChild(timeRow);
}

function loadExistingTimes() {
    const timesList = document.getElementById('timesList');
    if (!timesList) return;

    timesList.innerHTML = '';

    const existingTimes = existingAlarms || [];

    if (existingTimes.length > 0) {
        existingTimes.forEach(time => {
            addTimeInput(time);
        });
    } else {
        addTimeInput('');
    }
}

function updateCompositeProgressBar(takenPercent, cancelledPercent) {
    const takenBar = document.querySelector('.progress-stacked .bg-success');
    const cancelledBar = document.querySelector('.progress-stacked .bg-danger');

    if (takenBar && cancelledBar) {
        takenBar.style.width = takenPercent + '%';
        cancelledBar.style.width = cancelledPercent + '%';

        const takenLabel = document.querySelector('.stat-legend div:first-child strong');
        const cancelledLabel = document.querySelector('.stat-legend div:nth-child(2) strong');
        const remainingLabel = document.querySelector('.stat-legend div:nth-child(3) strong');

        if (takenLabel) takenLabel.textContent = takenPercent.toFixed(1) + '%';
        if (cancelledLabel) cancelledLabel.textContent = cancelledPercent.toFixed(1) + '%';
        if (remainingLabel) {
            const remaining = (100 - takenPercent - cancelledPercent).toFixed(1);
            remainingLabel.textContent = remaining + '%';
        }

        takenBar.title = 'Выполнено: ' + takenPercent.toFixed(1) + '%';
        cancelledBar.title = 'Отменено: ' + cancelledPercent.toFixed(1) + '%';
    }
}

function validateProgressBar() {
    const container = document.querySelector('.progress-stacked');
    if (!container) return;

    const takenBar = container.querySelector('.bg-success');
    const cancelledBar = container.querySelector('.bg-danger');

    if (takenBar && cancelledBar) {
        const takenWidth = parseFloat(takenBar.style.width) || 0;
        const cancelledWidth = parseFloat(cancelledBar.style.width) || 0;

        if (takenWidth + cancelledWidth > 100) {
            const newCancelledWidth = 100 - takenWidth;
            cancelledBar.style.width = Math.max(0, newCancelledWidth) + '%';
            console.warn('Сумма процентов превысила 100%, cancelled обрезан до', newCancelledWidth);
        }
    }
}

function highlightExistingDays() {
    if (!existingDays) return;

    const allChips = document.querySelectorAll('.day-chip');
    allChips.forEach(chip => {
        chip.classList.remove('selected');
    });

    var days = existingDays.split(',');
    var chips = document.querySelectorAll('.day-chip');
    chips.forEach(function(chip) {
        if (days.includes(chip.dataset.day)) {
            chip.classList.add('selected');
        }
    });
}

function showGenericError(message) {
    const alertDiv = document.getElementById('duplicateErrorAlert');
    if (alertDiv) {
        alertDiv.innerHTML = `
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            <strong>Ошибка!</strong> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;
        alertDiv.style.display = 'block';
        alertDiv.classList.add('show');
        alertDiv.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } else {
        alert(message);
    }
}

document.addEventListener('DOMContentLoaded', function() {

    const form = document.getElementById('scheduleForm');

    if (form) {
        const notificationTimesAttr = form.getAttribute('data-notification-times');
        console.log('Raw notification times attribute:', notificationTimesAttr);

        const notificationTimesInput = document.getElementById('notificationTimes');
        console.log('Notification times hidden input value:', notificationTimesInput ? notificationTimesInput.value : 'not found');

        if (notificationTimesAttr && notificationTimesAttr !== '') {
            existingAlarms = notificationTimesAttr.split(',').filter(t => t && t.trim() !== '');
            console.log('Parsed notification times from data attribute:', existingAlarms);
        } else if (notificationTimesInput && notificationTimesInput.value && notificationTimesInput.value !== '') {
            existingAlarms = notificationTimesInput.value.split(',').filter(t => t && t.trim() !== '');
            console.log('Parsed notification times from hidden input:', existingAlarms);
        } else {
            existingAlarms = [];
            console.log('No notification times found');
        }

        const selectedDaysAttr = form.getAttribute('data-selected-days');
        const selectedDaysInput = document.getElementById('selectedDays');

        console.log('Selected days attribute:', selectedDaysAttr);
        console.log('Selected days input value:', selectedDaysInput ? selectedDaysInput.value : 'not found');

        if (selectedDaysAttr && selectedDaysAttr !== '') {
            existingDays = selectedDaysAttr;
            console.log('Selected days from attribute:', existingDays);
        } else if (selectedDaysInput && selectedDaysInput.value && selectedDaysInput.value !== '') {
            existingDays = selectedDaysInput.value;
            console.log('Selected days from input:', existingDays);
        } else {
            existingDays = '';
        }
    } else {
        existingAlarms = [];
        existingDays = '';
    }

    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');

    if (startDateInput && !startDateInput.value) {
        const today = new Date().toISOString().split('T')[0];
        startDateInput.value = today;

        if (endDateInput && !endDateInput.value) {
            const nextMonth = new Date();
            nextMonth.setMonth(nextMonth.getMonth() + 1);
            endDateInput.value = nextMonth.toISOString().split('T')[0];
        }
    }

    const timesList = document.getElementById('timesList');
    if (timesList && (!existingAlarms || existingAlarms.length === 0)) {
        const now = new Date();
        now.setMinutes(now.getMinutes() + 1);
        const defaultTime = now.toTimeString().slice(0, 5);
        existingAlarms = [defaultTime];
        console.log('Добавлено время уведомления по умолчанию:', defaultTime);
    }

    loadExistingTimes();

    const hiddenType = document.getElementById('schemeTypeHidden');
    if (hiddenType && hiddenType.value === 'WEEKDAYS') {
        initDayChips();
        highlightExistingDays();
    } else {
        highlightExistingDays();
    }

    translateSchemeStatuses();
    translateSchemeTypes();

    if (window.MutationObserver) {
        const schemeGrid = document.querySelector('.scheme-grid');
        if (schemeGrid) {
            let translationTimeout;
            const observer = new MutationObserver(function(mutations) {
                let hasNewSchemes = false;
                mutations.forEach(function(mutation) {
                    if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeType === Node.ELEMENT_NODE) {
                                if (node.classList && node.classList.contains('scheme-card')) {
                                    hasNewSchemes = true;
                                } else if (node.querySelectorAll) {
                                    if (node.querySelectorAll('.scheme-card').length > 0) {
                                        hasNewSchemes = true;
                                    }
                                }
                            }
                        });
                    }
                });

                if (hasNewSchemes) {
                    if (translationTimeout) clearTimeout(translationTimeout);
                    translationTimeout = setTimeout(translateSchemeStatuses, 50);
                }
            });

            observer.observe(schemeGrid, { childList: true, subtree: true });
        }
    }

    const selector = document.getElementById('schemeTypeSelector');

    if (selector && hiddenType) {
        if (!hiddenType.value || hiddenType.value === 'UNSPECIFIED') {
            hiddenType.value = 'ALTERNATION';
            selector.value = 'ALTERNATION';
        } else {
            selector.value = hiddenType.value;
        }
    }

    toggleSchemeTypeFields();

    validateProgressBar();

    const progressBars = document.querySelectorAll('.progress-stacked .progress-bar');
    if (progressBars.length > 0 && window.MutationObserver) {
        const observer2 = new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
                if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
                    validateProgressBar();
                }
            });
        });

        progressBars.forEach(bar => {
            observer2.observe(bar, { attributes: true, attributeFilter: ['style'] });
        });
    }

    const schemeForm = document.getElementById('schemeForm');
    if (!schemeForm) return;

    let alertContainer = document.querySelector('.form-card');
    if (alertContainer && !document.getElementById('duplicateErrorAlert')) {
        const alertDiv = document.createElement('div');
        alertDiv.id = 'duplicateErrorAlert';
        alertDiv.className = 'alert alert-danger alert-dismissible fade';
        alertDiv.role = 'alert';
        alertDiv.style.display = 'none';
        alertDiv.innerHTML = `
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            <strong>Ошибка!</strong> Схема с таким названием уже существует в профиле.
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;
        alertContainer.insertBefore(alertDiv, schemeForm);
    }

    schemeForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        const formData = new FormData(schemeForm);
        const url = schemeForm.action;

        const submitBtn = schemeForm.querySelector('button[type="submit"]');
        const originalBtnText = submitBtn.innerHTML;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Сохранение...';

        const alertDiv = document.getElementById('duplicateErrorAlert');
        if (alertDiv) {
            alertDiv.style.display = 'none';
            alertDiv.classList.remove('show');
        }

        try {
            const response = await fetch(url, {
                method: 'POST',
                body: formData,
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (response.ok) {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const result = await response.json();
                    if (result.redirectUrl) {
                        window.location.href = result.redirectUrl;
                    } else if (result.success && result.schemeId) {
                        window.location.href = `/schemes/${result.schemeId}/schedule`;
                    } else {
                        window.location.href = '/schemes/all';
                    }
                } else {
                    if (response.redirected) {
                        window.location.href = response.url;
                    } else {
                        await response.text();
                        window.location.href = '/schemes/all';
                    }
                }
            } else if (response.status === 400) {
                const errorData = await response.json().catch(() => null);

                if (errorData && errorData.message && errorData.message.includes('Схема с таким названием уже существует')) {
                    if (alertDiv) {
                        alertDiv.style.display = 'block';
                        alertDiv.classList.add('show');
                    }

                    const nameInput = document.getElementById('name');
                    if (nameInput) {
                        nameInput.classList.add('is-invalid');

                        let errorSpan = document.getElementById('nameDuplicateError');
                        if (!errorSpan) {
                            errorSpan = document.createElement('span');
                            errorSpan.id = 'nameDuplicateError';
                            errorSpan.className = 'error-msg';
                            nameInput.parentNode.appendChild(errorSpan);
                        }
                        errorSpan.textContent = 'Схема с таким названием уже существует в профиле';
                        errorSpan.style.display = 'block';

                        nameInput.addEventListener('input', function() {
                            nameInput.classList.remove('is-invalid');
                            if (errorSpan) errorSpan.style.display = 'none';
                        }, { once: true });
                    }

                    alertDiv.scrollIntoView({ behavior: 'smooth', block: 'center' });
                } else {
                    showGenericError('Ошибка валидации формы. Проверьте правильность заполнения полей.');
                }
            } else {
                showGenericError('Произошла ошибка при создании схемы. Пожалуйста, попробуйте позже.');
            }
        } catch (error) {
            console.error('Ошибка при отправке формы:', error);
            showGenericError('Сетевая ошибка. Проверьте соединение и попробуйте снова.');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnText;
        }
    });
});

window.toggleSchemeTypeFields = toggleSchemeTypeFields;
window.initDayChips = initDayChips;
window.addTimeInput = addTimeInput;
window.updateSelectedDays = updateSelectedDays;
window.updateCompositeProgressBar = updateCompositeProgressBar;
window.validateProgressBar = validateProgressBar;
window.translateSchemeStatuses = translateSchemeStatuses;
window.highlightExistingDays = highlightExistingDays;
