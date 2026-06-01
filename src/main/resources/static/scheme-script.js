const statusTranslations = {
    'ACTIVE': 'Активна',
    'COMPLETED': 'Завершена',
    'PAUSED': 'Приостановлена',
    'INACTIVE': 'Неактивна'
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

    const existingTimes = window.existingNotificationTimes || [];

    if (existingTimes.length > 0) {
        existingTimes.forEach(time => {
            addTimeInput(time);
        });
    } else {
        if (timesList.children.length === 0) {
             addTimeInput('08:00');
        }
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
    var days = existingDays.split(',');
    var chips = document.querySelectorAll('.day-chip');
    chips.forEach(function(chip) {
        if (days.includes(chip.dataset.day)) {
            chip.classList.add('selected');
        }
    });
}

document.addEventListener('DOMContentLoaded', function() {

    existingAlarms = [];
    existingDays = '';
    window.existingNotificationTimes = existingAlarms.map(function(time) {
        return typeof time === 'string' ? time : time.toString();
    });
    highlightExistingDays();

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
    const hiddenType = document.getElementById('schemeTypeHidden');

    if (selector && hiddenType) {
        if (!hiddenType.value || hiddenType.value === 'UNSPECIFIED') {
            hiddenType.value = 'ALTERNATION';
            selector.value = 'ALTERNATION';
        }
    }

    toggleSchemeTypeFields();

    loadExistingTimes();

    if (hiddenType && hiddenType.value === 'WEEKDAYS') {
        initDayChips();
    }

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
});

const schemeTypeTranslations = {
    'ALTERNATION': 'Чередование',
    'WEEKDAYS': 'Дни недели',
    'UNSPECIFIED': 'Не указан'
};

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

window.toggleSchemeTypeFields = toggleSchemeTypeFields;
window.initDayChips = initDayChips;
window.addTimeInput = addTimeInput;
window.updateSelectedDays = updateSelectedDays;
window.updateCompositeProgressBar = updateCompositeProgressBar;
window.validateProgressBar = validateProgressBar;
window.translateSchemeStatuses = translateSchemeStatuses;
window.highlightExistingDays = highlightExistingDays;
