document.addEventListener('DOMContentLoaded', function() {

    const addBtn = document.getElementById('addProfileBottomBtn');
    if (addBtn) {
        addBtn.addEventListener('click', function() {
            const modalEl = document.getElementById('createProfileModalUser');
            if (modalEl) {
                const modal = new bootstrap.Modal(modalEl);
                modal.show();
            }
        });
    }

});

function toggleUsernameEdit() {
    const container = document.getElementById('username-edit-container');
    const btn = document.getElementById('username-edit-btn');
    if (!container || !btn) return;
    const isHidden = container.style.display === 'none' || container.style.display === '';
    if (isHidden) {
        const passContainer = document.getElementById('password-edit-container');
        const passBtn = document.getElementById('password-edit-btn');
        if (passContainer) passContainer.style.display = 'none';
        if (passBtn) passBtn.style.display = '';
    }
    container.style.display = isHidden ? 'block' : 'none';
    btn.style.display = isHidden ? 'none' : '';
    if (isHidden) {
        const input = container.querySelector('input');
        if (input) input.focus();
    }
}

function togglePasswordEdit() {
    const container = document.getElementById('password-edit-container');
    const btn = document.getElementById('password-edit-btn');
    if (!container || !btn) return;
    const isHidden = container.style.display === 'none' || container.style.display === '';
    if (isHidden) {
        const userContainer = document.getElementById('username-edit-container');
        const userBtn = document.getElementById('username-edit-btn');
        if (userContainer) userContainer.style.display = 'none';
        if (userBtn) userBtn.style.display = '';
    }
    container.style.display = isHidden ? 'block' : 'none';
    btn.style.display = isHidden ? 'none' : '';
    if (isHidden) {
        const input = container.querySelector('input');
        if (input) input.focus();
    }
}

function openCreateProfileModal() {
    const modalEl = document.getElementById('createProfileModalUser');
    if (modalEl) {
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }
}

function openRenameModal(btn) {
    const profileId = btn.getAttribute('data-profile-id');
    const profileName = btn.getAttribute('data-profile-name');
    const modalEl = document.getElementById('renameProfileModal');
    const form = document.getElementById('renameProfileForm');
    const input = document.getElementById('renameProfileName');

    if (modalEl && form && input && profileId) {
        form.action = '/profiles/' + profileId + '/rename';
        input.value = profileName || '';
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }
}

    function toggleUsernameEdit() {
        const container = document.getElementById('username-edit-container');
        const btn = document.getElementById('username-edit-btn');
        const isHidden = container.style.display === 'none' || container.style.display === '';
        if (isHidden) {
            document.getElementById('password-edit-container').style.display = 'none';
            document.getElementById('password-edit-btn').style.display = '';
        }
        container.style.display = isHidden ? 'block' : 'none';
        btn.style.display = isHidden ? 'none' : '';
        if (isHidden) container.querySelector('input').focus();
    }

    function togglePasswordEdit() {
        const container = document.getElementById('password-edit-container');
        const btn = document.getElementById('password-edit-btn');
        const isHidden = container.style.display === 'none' || container.style.display === '';
        if (isHidden) {
            document.getElementById('username-edit-container').style.display = 'none';
            document.getElementById('username-edit-btn').style.display = '';
        }
        container.style.display = isHidden ? 'block' : 'none';
        btn.style.display = isHidden ? 'none' : '';
        if (isHidden) container.querySelector('input').focus();
    }

    function openCreateProfileModal() {
        var modal = new bootstrap.Modal(document.getElementById('createProfileModalUser'));
        modal.show();
    }

    function openRenameModal(btn) {
        var profileId = btn.getAttribute('data-profile-id');
        var profileName = btn.getAttribute('data-profile-name');
        var form = document.getElementById('renameProfileForm');
        form.action = '/profiles/' + profileId + '/rename';
        document.getElementById('renameProfileName').value = profileName;
        var modal = new bootstrap.Modal(document.getElementById('renameProfileModal'));
        modal.show();
    }

    function openDeleteAccountModal() {
        const deleteModal = new bootstrap.Modal(document.getElementById('deleteAccountModal'));
        deleteModal.show();

        setTimeout(() => {
            document.getElementById('deletePassword').focus();
        }, 500);
    }