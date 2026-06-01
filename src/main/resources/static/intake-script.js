function translateIntakeStatus(status) {
    const statusMap = {
        'SCHEDULED': 'Запланировано',
        'TAKEN': 'Принято',
        'MOVED': 'Перенесено',
        'CANCELLED': 'Отменено',
        'PAUSED': 'Приостановлено'
    };
    
    return statusMap[status] || status;
}

function translateAllStatuses() {
    const statusBadges = document.querySelectorAll('.badge-status');
    statusBadges.forEach(badge => {
        const originalStatus = badge.textContent.trim();
        if (originalStatus) {
            badge.textContent = translateIntakeStatus(originalStatus);
        }
    });
    
    const intakeStatusBadges = document.querySelectorAll('.intake-status-badge');
    intakeStatusBadges.forEach(badge => {
        const originalStatus = badge.textContent.trim();
        if (originalStatus) {
            badge.textContent = translateIntakeStatus(originalStatus);
        }
    });
    
    const actionChips = document.querySelectorAll('.action-chip');
    actionChips.forEach(chip => {
        const originalText = chip.textContent.trim();
        if (originalText === 'Принято') {
        } else if (originalText === 'Отменено') {
        } else if (originalText === 'Перенести') {
        }
    });
}

function initDetailsPage() {
    const editBtn = document.getElementById('commentEditBtn');
    const cancelBtn = document.getElementById('cancelEditBtn');
    const commentDisplay = document.getElementById('commentDisplay');
    const commentEdit = document.getElementById('commentEdit');
    const commentTextarea = document.getElementById('commentTextarea');
    const commentForm = document.getElementById('commentForm');

    if (editBtn) {
        editBtn.addEventListener('click', function(e) {
            e.preventDefault();
            if (commentDisplay && commentEdit) {
                commentDisplay.style.display = 'none';
                commentEdit.style.display = 'block';
                if (commentTextarea) {
                    commentTextarea.focus();
                    const length = commentTextarea.value.length;
                    commentTextarea.setSelectionRange(length, length);
                }
            }
        });
    }

    if (cancelBtn) {
        cancelBtn.addEventListener('click', function(e) {
            e.preventDefault();

            const returnDate = commentForm ? commentForm.getAttribute('data-return-date') : '';

            let url = `/intakes/${intakeId}`;
            if (returnDate && returnDate !== 'null') {
                url += `?returnDate=${encodeURIComponent(returnDate)}&editMode=false`;
            } else {
                url += `?editMode=false`;
            }

            window.location.href = url;
        });
    }

    if (commentTextarea) {
        function autoResize() {
            this.style.height = 'auto';
            this.style.height = this.scrollHeight + 'px';
        }

        commentTextarea.addEventListener('input', autoResize);
        autoResize.call(commentTextarea);
    }

    if (commentForm) {
        commentForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const formData = new FormData(this);
            const actionUrl = this.action;

            fetch(actionUrl, {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(response => {
                if (response.ok) {
                    return response.text();
                }
                throw new Error('Ошибка при сохранении комментария');
            })
            .then(() => {
                const newComment = commentTextarea.value.trim();
                const commentTextElement = document.getElementById('commentText');
                if (commentTextElement) {
                    commentTextElement.textContent = newComment || 'Нет комментария';
                }

                commentDisplay.style.display = 'block';
                commentEdit.style.display = 'none';

                showTemporaryMessage('Комментарий сохранен', 'success');
            })
            .catch(error => {
                console.error('Error:', error);
                showTemporaryMessage(error.message, 'error');
            });
        });
    }
}

let intakeId = null;
const commentForm = document.getElementById('commentForm');
if (commentForm) {
    const match = commentForm.action.match(/\/intakes\/(\d+)\/comment/);
    if (match) {
        intakeId = match[1];
    }
}

document.addEventListener('DOMContentLoaded', function() {
    if (document.getElementById('commentEditBtn') || document.getElementById('commentDisplay')) {
        initDetailsPage();
    }
    initCalendar();
    
    translateAllStatuses();
});

function initCalendar() {
    const calendarBtn = document.getElementById('calendarBtn');
    if (!calendarBtn) return;

    const modalElement = document.getElementById('calendarModal');
    if (!modalElement) return;

    const calendarModal = new bootstrap.Modal(modalElement);
    const calendarGoBtn = document.getElementById('calendarGoBtn');
    const calendarDatePicker = document.getElementById('calendarDatePicker');

    if (calendarBtn) {
        calendarBtn.addEventListener('click', function() {
            calendarModal.show();
        });
    }

    if (calendarGoBtn && calendarDatePicker) {
        calendarGoBtn.addEventListener('click', function() {
            const selectedDate = calendarDatePicker.value;
            if (selectedDate) {
                window.location.href = `/intakes/date/${selectedDate}`;
            }
        });
    }

    if (calendarDatePicker) {
        calendarDatePicker.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                const selectedDate = calendarDatePicker.value;
                if (selectedDate) {
                    window.location.href = `/intakes/date/${selectedDate}`;
                }
            }
        });
    }
}