/**
 * Telegram Admin Panel - Main Application
 */
let currentPage = 'dashboard';
let adminUser = JSON.parse(localStorage.getItem('admin_user') || 'null');

// ====== INITIALIZATION ======
document.addEventListener('DOMContentLoaded', () => {
    if (API.isAuthenticated() && adminUser) {
        showApp();
        navigateTo('dashboard');
    } else {
        showLogin();
    }
});

// ====== AUTH ======
async function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;
    const errorEl = document.getElementById('login-error');

    try {
        const res = await API.post('/api/auth/login', { username, password });
        if (res && res.success) {
            API.setToken(res.data.token);
            adminUser = { username: res.data.username, role: res.data.role, email: res.data.email };
            localStorage.setItem('admin_user', JSON.stringify(adminUser));
            showApp();
            navigateTo('dashboard');
        } else {
            errorEl.textContent = res ? res.message : 'Connection failed';
            errorEl.style.display = 'block';
        }
    } catch (err) {
        errorEl.textContent = 'Connection error. Please check the server.';
        errorEl.style.display = 'block';
    }
}

function showLogin() {
    document.getElementById('login-page').style.display = 'flex';
    document.getElementById('app-layout').style.display = 'none';
}

function showApp() {
    document.getElementById('login-page').style.display = 'none';
    document.getElementById('app-layout').style.display = 'flex';
    document.getElementById('admin-name').textContent = adminUser.username;
    document.getElementById('admin-role').textContent = adminUser.role;
    document.getElementById('admin-avatar').textContent = adminUser.username.charAt(0).toUpperCase();
}

// ====== NAVIGATION ======
function navigateTo(page) {
    currentPage = page;
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const pageEl = document.getElementById('page-' + page);
    const navEl = document.querySelector(`[data-page="${page}"]`);
    if (pageEl) pageEl.classList.add('active');
    if (navEl) navEl.classList.add('active');

    document.getElementById('page-title').textContent = getPageTitle(page);

    // Load page data
    switch (page) {
        case 'dashboard': loadDashboard(); break;
        case 'users': loadUsers(); break;
        case 'channels': loadChannels(); break;
        case 'groups': loadGroups(); break;
        case 'announcements': loadAnnouncements(); break;
        case 'reports': loadReports(); break;
        case 'configs': loadConfigs(); break;
        case 'audit-logs': loadAuditLogs(); break;
    }
}

function getPageTitle(page) {
    const titles = {
        'dashboard': '📊 Dashboard Overview',
        'users': '👥 User Management',
        'channels': '📢 Channel Management',
        'groups': '👨‍👩‍👧‍👦 Group Management',
        'announcements': '📣 Announcements',
        'reports': '🛡️ Content Moderation',
        'configs': '⚙️ System Configuration',
        'audit-logs': '📋 Audit Logs'
    };
    return titles[page] || page;
}

// ====== TOAST ======
function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

// ====== DASHBOARD ======
async function loadDashboard() {
    const res = await API.get('/api/dashboard/stats');
    if (!res || !res.success) return;
    const s = res.data;

    document.getElementById('dashboard-content').innerHTML = `
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-icon blue">👥</div>
                <div><div class="stat-value">${s.totalUsers}</div><div class="stat-label">Total Users</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon green">✅</div>
                <div><div class="stat-value">${s.activeUsers}</div><div class="stat-label">Active (24h)</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon red">🚫</div>
                <div><div class="stat-value">${s.bannedUsers}</div><div class="stat-label">Banned Users</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon orange">🆕</div>
                <div><div class="stat-value">${s.newUsersToday}</div><div class="stat-label">New Today</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon purple">⭐</div>
                <div><div class="stat-value">${s.premiumUsers}</div><div class="stat-label">Premium Users</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon blue">📢</div>
                <div><div class="stat-value">${s.totalChannels}</div><div class="stat-label">Total Channels</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon green">👨‍👩‍👧‍👦</div>
                <div><div class="stat-value">${s.totalGroups}</div><div class="stat-label">Total Groups</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon red">⚠️</div>
                <div><div class="stat-value">${s.pendingReports}</div><div class="stat-label">Pending Reports</div></div>
            </div>
        </div>
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-icon blue">📣</div>
                <div><div class="stat-value">${s.activeAnnouncements}</div><div class="stat-label">Active Announcements</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon green">🤖</div>
                <div><div class="stat-value">${s.botUsers}</div><div class="stat-label">Bot Users</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon orange">📝</div>
                <div><div class="stat-value">${s.todayAuditActions}</div><div class="stat-label">Today's Actions</div></div>
            </div>
            <div class="stat-card">
                <div class="stat-icon purple">📢</div>
                <div><div class="stat-value">${s.activeChannels}</div><div class="stat-label">Active Channels</div></div>
            </div>
        </div>`;
}

// ====== USERS ======
let userPage = 0;
async function loadUsers(page = 0) {
    userPage = page;
    const keyword = document.getElementById('user-search')?.value || '';
    const status = document.getElementById('user-status-filter')?.value || '';
    const res = await API.get(`/api/users?page=${page}&size=10&keyword=${keyword}&status=${status}`);
    if (!res || !res.success) return;

    const users = res.data.content;
    const totalPages = res.data.totalPages;

    let html = `<table class="data-table"><thead><tr>
        <th>ID</th><th>Telegram ID</th><th>Username</th><th>Name</th><th>Phone</th><th>Status</th><th>Messages</th><th>Last Active</th><th>Actions</th>
    </tr></thead><tbody>`;

    users.forEach(u => {
        html += `<tr>
            <td>${u.id}</td>
            <td>${u.telegramId || '-'}</td>
            <td>@${escapeHtml(u.username) || '-'}</td>
            <td>${escapeHtml(u.firstName) || ''} ${escapeHtml(u.lastName) || ''}</td>
            <td>${escapeHtml(u.phoneNumber) || '-'}</td>
            <td><span class="badge badge-${u.status.toLowerCase()}">${escapeHtml(u.status)}</span></td>
            <td>${u.messagesCount}</td>
            <td>${formatDate(u.lastActiveAt)}</td>
            <td class="btn-group">
                ${u.status === 'BANNED' ?
                    `<button class="btn btn-sm btn-success" onclick="unbanUser(${u.id})">Unban</button>` :
                    `<button class="btn btn-sm btn-danger" onclick="showBanModal(${u.id}, '${escapeHtml(u.username)}')">Ban</button>`}
                <button class="btn btn-sm btn-info" onclick="showEditUser(${u.id})">Edit</button>
                <button class="btn btn-sm btn-warning" onclick="showUserDetail(${u.id})">Detail</button>
            </td>
        </tr>`;
    });
    html += '</tbody></table>';
    html += renderPagination(page, totalPages, 'loadUsers');

    document.getElementById('users-table').innerHTML = html;
}

function showBanModal(userId, username) {
    document.getElementById('modal-container').innerHTML = `
        <div class="modal-overlay" onclick="closeModal(event)">
            <div class="modal" onclick="event.stopPropagation()">
                <h2>🚫 Ban User: @${username}</h2>
                <div class="form-group">
                    <label>Ban Reason</label>
                    <textarea id="ban-reason" placeholder="Enter the reason for banning..."></textarea>
                </div>
                <div class="form-group">
                    <label>Expires At (optional)</label>
                    <input type="datetime-local" id="ban-expires">
                </div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-danger" onclick="banUser(${userId})">Confirm Ban</button>
                </div>
            </div>
        </div>`;
}

async function banUser(id) {
    const reason = document.getElementById('ban-reason').value;
    const expiresAt = document.getElementById('ban-expires').value || null;
    const res = await API.post(`/api/users/${id}/ban`, { reason, expiresAt });
    if (res && res.success) {
        showToast('User banned successfully');
        closeModal();
        loadUsers(userPage);
    } else {
        showToast(res?.message || 'Failed', 'error');
    }
}

async function unbanUser(id) {
    const res = await API.post(`/api/users/${id}/unban`, {});
    if (res && res.success) {
        showToast('User unbanned successfully');
        loadUsers(userPage);
    } else {
        showToast(res?.message || 'Failed', 'error');
    }
}

async function showUserDetail(id) {
    const res = await API.get(`/api/users/${id}`);
    if (!res || !res.success) return;
    const u = res.data;

    document.getElementById('modal-container').innerHTML = `
        <div class="modal-overlay" onclick="closeModal(event)">
            <div class="modal" onclick="event.stopPropagation()">
                <h2>👤 User Detail</h2>
                <table class="data-table">
                    <tr><td><strong>ID</strong></td><td>${u.id}</td></tr>
                    <tr><td><strong>Telegram ID</strong></td><td>${u.telegramId || '-'}</td></tr>
                    <tr><td><strong>Username</strong></td><td>@${u.username || '-'}</td></tr>
                    <tr><td><strong>Name</strong></td><td>${u.firstName || ''} ${u.lastName || ''}</td></tr>
                    <tr><td><strong>Phone</strong></td><td>${u.phoneNumber || '-'}</td></tr>
                    <tr><td><strong>Status</strong></td><td><span class="badge badge-${u.status.toLowerCase()}">${u.status}</span></td></tr>
                    <tr><td><strong>Premium</strong></td><td>${u.premium ? '⭐ Yes' : 'No'}</td></tr>
                    <tr><td><strong>Bot</strong></td><td>${u.bot ? '🤖 Yes' : 'No'}</td></tr>
                    <tr><td><strong>Messages</strong></td><td>${u.messagesCount}</td></tr>
                    <tr><td><strong>Groups</strong></td><td>${u.groupsCount}</td></tr>
                    <tr><td><strong>Channels</strong></td><td>${u.channelsCount}</td></tr>
                    <tr><td><strong>Device</strong></td><td>${u.deviceInfo || '-'}</td></tr>
                    <tr><td><strong>App Version</strong></td><td>${u.appVersion || '-'}</td></tr>
                    <tr><td><strong>Registered</strong></td><td>${formatDate(u.registeredAt)}</td></tr>
                    <tr><td><strong>Last Active</strong></td><td>${formatDate(u.lastActiveAt)}</td></tr>
                    ${u.banReason ? `<tr><td><strong>Ban Reason</strong></td><td>${u.banReason}</td></tr>` : ''}
                </table>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">Close</button>
                    <button class="btn btn-primary" onclick="closeModal(); showEditUser(${u.id})">✏️ Edit</button>
                </div>
            </div>
        </div>`;
}

function showCreateUser() {
    document.getElementById('modal-container').innerHTML = `
        <div class="modal-overlay" onclick="closeModal(event)">
            <div class="modal" onclick="event.stopPropagation()">
                <h2>➕ Add New User</h2>
                <div class="form-group"><label>Telegram ID <small>(required, must be unique)</small></label><input id="new-user-tid" type="number" placeholder="e.g. 100001"></div>
                <div class="form-group"><label>First Name</label><input id="new-user-fname" placeholder="First name"></div>
                <div class="form-group"><label>Last Name</label><input id="new-user-lname" placeholder="Last name"></div>
                <div class="form-group"><label>Username</label><input id="new-user-uname" placeholder="Username (no @)"></div>
                <div class="form-group"><label>Phone Number</label><input id="new-user-phone" placeholder="+8613800001111"></div>
                <div class="form-group"><label>Premium</label><select id="new-user-premium">
                    <option value="false">No</option><option value="true">Yes ⭐</option>
                </select></div>
                <div class="form-group"><label>Bot</label><select id="new-user-bot">
                    <option value="false">No</option><option value="true">Yes 🤖</option>
                </select></div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-primary" onclick="createUser()">Create User</button>
                </div>
            </div>
        </div>`;
}

async function createUser() {
    const telegramId = document.getElementById('new-user-tid').value;
    if (!telegramId) { showToast('Telegram ID is required', 'error'); return; }
    const body = {
        telegramId: parseInt(telegramId),
        firstName: document.getElementById('new-user-fname').value || null,
        lastName: document.getElementById('new-user-lname').value || null,
        username: document.getElementById('new-user-uname').value || null,
        phoneNumber: document.getElementById('new-user-phone').value || null,
        premium: document.getElementById('new-user-premium').value === 'true',
        bot: document.getElementById('new-user-bot').value === 'true',
        status: 'ACTIVE'
    };
    const res = await API.post('/api/users', body);
    if (res?.success) { showToast('User created successfully'); closeModal(); loadUsers(userPage); }
    else showToast(res?.message || 'Failed to create user', 'error');
}

async function showEditUser(id) {
    const res = await API.get(`/api/users/${id}`);
    if (!res?.success) return;
    const u = res.data;

    document.getElementById('modal-container').innerHTML = `
        <div class="modal-overlay" onclick="closeModal(event)">
            <div class="modal" onclick="event.stopPropagation()">
                <h2>✏️ Edit User</h2>
                <div class="form-group"><label>Telegram ID</label><input id="edit-user-tid" type="number" value="${u.telegramId || ''}"></div>
                <div class="form-group"><label>First Name</label><input id="edit-user-fname" value="${escapeHtml(u.firstName) || ''}"></div>
                <div class="form-group"><label>Last Name</label><input id="edit-user-lname" value="${escapeHtml(u.lastName) || ''}"></div>
                <div class="form-group"><label>Username</label><input id="edit-user-uname" value="${escapeHtml(u.username) || ''}"></div>
                <div class="form-group"><label>Phone Number</label><input id="edit-user-phone" value="${escapeHtml(u.phoneNumber) || ''}"></div>
                <div class="form-group"><label>Premium</label><select id="edit-user-premium">
                    <option value="false" ${!u.premium ? 'selected' : ''}>No</option>
                    <option value="true" ${u.premium ? 'selected' : ''}>Yes ⭐</option>
                </select></div>
                <div class="form-group"><label>Bot</label><select id="edit-user-bot">
                    <option value="false" ${!u.bot ? 'selected' : ''}>No</option>
                    <option value="true" ${u.bot ? 'selected' : ''}>Yes 🤖</option>
                </select></div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-primary" onclick="updateUser(${id})">Save Changes</button>
                </div>
            </div>
        </div>`;
}

async function updateUser(id) {
    const body = {
        telegramId: parseInt(document.getElementById('edit-user-tid').value) || null,
        firstName: document.getElementById('edit-user-fname').value || null,
        lastName: document.getElementById('edit-user-lname').value || null,
        username: document.getElementById('edit-user-uname').value || null,
        phoneNumber: document.getElementById('edit-user-phone').value || null,
        premium: document.getElementById('edit-user-premium').value === 'true',
        bot: document.getElementById('edit-user-bot').value === 'true'
    };
    const res = await API.put(`/api/users/${id}`, body);
    if (res?.success) { showToast('User updated successfully'); closeModal(); loadUsers(userPage); }
    else showToast(res?.message || 'Failed to update user', 'error');
}

// ====== CHANNELS ======
let channelPage = 0;
async function loadChannels(page = 0) {
    channelPage = page;
    const keyword = document.getElementById('channel-search')?.value || '';
    const status = document.getElementById('channel-status-filter')?.value || '';
    const res = await API.get(`/api/channels?page=${page}&size=10&keyword=${keyword}&status=${status}`);
    if (!res || !res.success) return;

    const items = res.data.content;
    const totalPages = res.data.totalPages;

    let html = `<table class="data-table"><thead><tr>
        <th>ID</th><th>Title</th><th>Username</th><th>Members</th><th>Messages</th><th>Status</th><th>Public</th><th>Actions</th>
    </tr></thead><tbody>`;

    items.forEach(c => {
        html += `<tr>
            <td>${c.id}</td>
            <td>${escapeHtml(c.title)}</td>
            <td>@${escapeHtml(c.username) || '-'}</td>
            <td>${c.memberCount}</td>
            <td>${c.messagesCount}</td>
            <td><span class="badge badge-${c.status.toLowerCase()}">${escapeHtml(c.status)}</span></td>
            <td>${c.public ? '✅' : '🔒'}</td>
            <td class="btn-group">
                ${c.status === 'ACTIVE' ?
                    `<button class="btn btn-sm btn-warning" onclick="suspendChannel(${c.id})">Suspend</button>` :
                    `<button class="btn btn-sm btn-success" onclick="activateChannel(${c.id})">Activate</button>`}
                <button class="btn btn-sm btn-info" onclick="editChannel(${c.id})">Edit</button>
                <button class="btn btn-sm btn-danger" onclick="deleteChannel(${c.id})">Delete</button>
            </td>
        </tr>`;
    });
    html += '</tbody></table>';
    html += renderPagination(page, totalPages, 'loadChannels');
    document.getElementById('channels-table').innerHTML = html;
}

async function suspendChannel(id) {
    const res = await API.post(`/api/channels/${id}/suspend`, {});
    if (res?.success) { showToast('Channel suspended'); loadChannels(channelPage); }
    else showToast(res?.message || 'Failed', 'error');
}

async function activateChannel(id) {
    const res = await API.post(`/api/channels/${id}/activate`, {});
    if (res?.success) { showToast('Channel activated'); loadChannels(channelPage); }
    else showToast(res?.message || 'Failed', 'error');
}

async function deleteChannel(id) {
    if (!confirm('Are you sure you want to delete this channel?')) return;
    const res = await API.del(`/api/channels/${id}`);
    if (res?.success) { showToast('Channel deleted'); loadChannels(channelPage); }
    else showToast(res?.message || 'Failed', 'error');
}

async function editChannel(id) {
    const res = await API.get(`/api/channels/${id}`);
    if (!res?.success) return;
    const c = res.data;

    document.getElementById('modal-container').innerHTML = `
        <div class="modal-overlay" onclick="closeModal(event)">
            <div class="modal" onclick="event.stopPropagation()">
                <h2>✏️ Edit Channel</h2>
                <div class="form-group"><label>Title</label><input id="edit-ch-title" value="${c.title}"></div>
                <div class="form-group"><label>Description</label><textarea id="edit-ch-desc">${c.description || ''}</textarea></div>
                <div class="form-group"><label>Username</label><input id="edit-ch-username" value="${c.username || ''}"></div>
                <div class="form-group"><label>Public</label><select id="edit-ch-public">
                    <option value="true" ${c.public ? 'selected' : ''}>Yes</option>
                    <option value="false" ${!c.public ? 'selected' : ''}>No</option>
                </select></div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-primary" onclick="saveChannel(${id})">Save</button>
                </div>
            </div>
        </div>`;
}

async function saveChannel(id) {
    const body = {
        title: document.getElementById('edit-ch-title').value,
        description: document.getElementById('edit-ch-desc').value,
        username: document.getElementById('edit-ch-username').value,
        public: document.getElementById('edit-ch-public').value === 'true'
    };
    const res = await API.put(`/api/channels/${id}`, body);
    if (res?.success) { showToast('Channel updated'); closeModal(); loadChannels(channelPage); }
    else showToast(res?.message || 'Failed', 'error');
}

// ====== GROUPS ======
let groupPage = 0;
async function loadGroups(page = 0) {
    groupPage = page;
    const keyword = document.getElementById('group-search')?.value || '';
    const status = document.getElementById('group-status-filter')?.value || '';
    const res = await API.get(`/api/groups?page=${page}&size=10&keyword=${keyword}&status=${status}`);
    if (!res || !res.success) return;

    const items = res.data.content;
    const totalPages = res.data.totalPages;

    let html = `<table class="data-table"><thead><tr>
        <th>ID</th><th>Title</th><th>Type</th><th>Members</th><th>Messages</th><th>Status</th><th>Actions</th>
    </tr></thead><tbody>`;

    items.forEach(g => {
        html += `<tr>
            <td>${g.id}</td>
            <td>${g.title}</td>
            <td>${g.groupType || '-'}</td>
            <td>${g.memberCount}</td>
            <td>${g.messagesCount}</td>
            <td><span class="badge badge-${g.status.toLowerCase()}">${g.status}</span></td>
            <td class="btn-group">
                ${g.status === 'ACTIVE' ?
                    `<button class="btn btn-sm btn-warning" onclick="suspendGroup(${g.id})">Suspend</button>` :
                    `<button class="btn btn-sm btn-success" onclick="activateGroup(${g.id})">Activate</button>`}
                <button class="btn btn-sm btn-danger" onclick="deleteGroup(${g.id})">Delete</button>
            </td>
        </tr>`;
    });
    html += '</tbody></table>';
    html += renderPagination(page, totalPages, 'loadGroups');
    document.getElementById('groups-table').innerHTML = html;
}

async function suspendGroup(id) {
    const res = await API.post(`/api/groups/${id}/suspend`, {});
    if (res?.success) { showToast('Group suspended'); loadGroups(groupPage); }
    else showToast(res?.message || 'Failed', 'error');
}

async function activateGroup(id) {
    const res = await API.post(`/api/groups/${id}/activate`, {});
    if (res?.success) { showToast('Group activated'); loadGroups(groupPage); }
    else showToast(res?.message || 'Failed', 'error');
}

async function deleteGroup(id) {
    if (!confirm('Are you sure?')) return;
    const res = await API.del(`/api/groups/${id}`);
    if (res?.success) { showToast('Group deleted'); loadGroups(groupPage); }
    else showToast(res?.message || 'Failed', 'error');
}

// ====== ANNOUNCEMENTS ======
let announcementPage = 0;
async function loadAnnouncements(page = 0) {
    announcementPage = page;
    const res = await API.get(`/api/announcements?page=${page}&size=10`);
    if (!res || !res.success) return;

    const items = res.data.content;
    const totalPages = res.data.totalPages;

    let html = `<table class="data-table"><thead><tr>
        <th>ID</th><th>Title</th><th>Type</th><th>Priority</th><th>Active</th><th>Created</th><th>Actions</th>
    </tr></thead><tbody>`;

    items.forEach(a => {
        html += `<tr>
            <td>${a.id}</td>
            <td>${a.title}</td>
            <td>${a.type || '-'}</td>
            <td>${a.priority || '-'}</td>
            <td>${a.active ? '<span class="badge badge-active">Active</span>' : '<span class="badge badge-deleted">Inactive</span>'}</td>
            <td>${formatDate(a.createdAt)}</td>
            <td class="btn-group">
                <button class="btn btn-sm btn-info" onclick="editAnnouncement(${a.id})">Edit</button>
                ${a.active ?
                    `<button class="btn btn-sm btn-warning" onclick="toggleAnnouncement(${a.id}, false)">Deactivate</button>` :
                    `<button class="btn btn-sm btn-success" onclick="toggleAnnouncement(${a.id}, true)">Activate</button>`}
            </td>
        </tr>`;
    });
    html += '</tbody></table>';
    html += renderPagination(page, totalPages, 'loadAnnouncements');
    document.getElementById('announcements-table').innerHTML = html;
}

function showCreateAnnouncement() {
    document.getElementById('modal-container').innerHTML = `
        <div class="modal-overlay" onclick="closeModal(event)">
            <div class="modal" onclick="event.stopPropagation()">
                <h2>📣 New Announcement</h2>
                <div class="form-group"><label>Title</label><input id="ann-title" placeholder="Announcement title"></div>
                <div class="form-group"><label>Content</label><textarea id="ann-content" placeholder="Announcement content"></textarea></div>
                <div class="form-group"><label>Type</label><select id="ann-type">
                    <option value="SYSTEM">System</option>
                    <option value="UPDATE">Update</option>
                    <option value="MAINTENANCE">Maintenance</option>
                    <option value="PROMOTION">Promotion</option>
                </select></div>
                <div class="form-group"><label>Priority</label><select id="ann-priority">
                    <option value="NORMAL">Normal</option>
                    <option value="LOW">Low</option>
                    <option value="HIGH">High</option>
                    <option value="URGENT">Urgent</option>
                </select></div>
                <div class="form-group"><label>Target Audience</label><select id="ann-audience">
                    <option value="ALL">All Users</option>
                    <option value="PREMIUM">Premium Users</option>
                </select></div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-primary" onclick="createAnnouncement()">Publish</button>
                </div>
            </div>
        </div>`;
}

async function createAnnouncement() {
    const body = {
        title: document.getElementById('ann-title').value,
        content: document.getElementById('ann-content').value,
        type: document.getElementById('ann-type').value,
        priority: document.getElementById('ann-priority').value,
        targetAudience: document.getElementById('ann-audience').value,
        active: true
    };
    const res = await API.post('/api/announcements', body);
    if (res?.success) { showToast('Announcement published'); closeModal(); loadAnnouncements(); }
    else showToast(res?.message || 'Failed', 'error');
}

async function editAnnouncement(id) {
    const res = await API.get(`/api/announcements/${id}`);
    if (!res?.success) return;
    const a = res.data;

    document.getElementById('modal-container').innerHTML = `
        <div class="modal-overlay" onclick="closeModal(event)">
            <div class="modal" onclick="event.stopPropagation()">
                <h2>✏️ Edit Announcement</h2>
                <div class="form-group"><label>Title</label><input id="edit-ann-title" value="${a.title}"></div>
                <div class="form-group"><label>Content</label><textarea id="edit-ann-content">${a.content}</textarea></div>
                <div class="form-group"><label>Type</label><select id="edit-ann-type">
                    <option value="SYSTEM" ${a.type==='SYSTEM'?'selected':''}>System</option>
                    <option value="UPDATE" ${a.type==='UPDATE'?'selected':''}>Update</option>
                    <option value="MAINTENANCE" ${a.type==='MAINTENANCE'?'selected':''}>Maintenance</option>
                    <option value="PROMOTION" ${a.type==='PROMOTION'?'selected':''}>Promotion</option>
                </select></div>
                <div class="form-group"><label>Priority</label><select id="edit-ann-priority">
                    <option value="LOW" ${a.priority==='LOW'?'selected':''}>Low</option>
                    <option value="NORMAL" ${a.priority==='NORMAL'?'selected':''}>Normal</option>
                    <option value="HIGH" ${a.priority==='HIGH'?'selected':''}>High</option>
                    <option value="URGENT" ${a.priority==='URGENT'?'selected':''}>Urgent</option>
                </select></div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-primary" onclick="updateAnnouncement(${id})">Save</button>
                </div>
            </div>
        </div>`;
}

async function updateAnnouncement(id) {
    const body = {
        title: document.getElementById('edit-ann-title').value,
        content: document.getElementById('edit-ann-content').value,
        type: document.getElementById('edit-ann-type').value,
        priority: document.getElementById('edit-ann-priority').value
    };
    const res = await API.put(`/api/announcements/${id}`, body);
    if (res?.success) { showToast('Announcement updated'); closeModal(); loadAnnouncements(); }
    else showToast(res?.message || 'Failed', 'error');
}

async function toggleAnnouncement(id, activate) {
    const endpoint = activate ? 'activate' : 'deactivate';
    const res = await API.post(`/api/announcements/${id}/${endpoint}`, {});
    if (res?.success) { showToast('Announcement ' + endpoint + 'd'); loadAnnouncements(); }
    else showToast(res?.message || 'Failed', 'error');
}

// ====== REPORTS ======
let reportPage = 0;
async function loadReports(page = 0) {
    reportPage = page;
    const status = document.getElementById('report-status-filter')?.value || '';
    const url = status ? `/api/reports?status=${status}&page=${page}&size=10` : `/api/reports?page=${page}&size=10`;
    const res = await API.get(url);
    if (!res || !res.success) return;

    const items = res.data.content;
    const totalPages = res.data.totalPages;

    let html = `<table class="data-table"><thead><tr>
        <th>ID</th><th>Reporter</th><th>Reported User</th><th>Type</th><th>Description</th><th>Status</th><th>Created</th><th>Actions</th>
    </tr></thead><tbody>`;

    items.forEach(r => {
        html += `<tr>
            <td>${r.id}</td>
            <td>${escapeHtml(r.reporterName) || '-'}</td>
            <td>${escapeHtml(r.reportedUserName) || '-'}</td>
            <td>${escapeHtml(r.reportType)}</td>
            <td title="${escapeHtml(r.description)}">${escapeHtml((r.description || '').substring(0, 50))}${r.description && r.description.length > 50 ? '...' : ''}</td>
            <td><span class="badge badge-${r.status.toLowerCase()}">${escapeHtml(r.status)}</span></td>
            <td>${formatDate(r.createdAt)}</td>
            <td class="btn-group">
                ${r.status === 'PENDING' || r.status === 'REVIEWING' ? `
                    <button class="btn btn-sm btn-danger" onclick="showResolveModal(${r.id})">Resolve</button>
                    <button class="btn btn-sm btn-secondary" onclick="dismissReport(${r.id})">Dismiss</button>
                ` : `<span class="badge badge-${r.status.toLowerCase()}">${r.actionTaken || '-'}</span>`}
            </td>
        </tr>`;
    });
    html += '</tbody></table>';
    html += renderPagination(page, totalPages, 'loadReports');
    document.getElementById('reports-table').innerHTML = html;
}

function showResolveModal(id) {
    document.getElementById('modal-container').innerHTML = `
        <div class="modal-overlay" onclick="closeModal(event)">
            <div class="modal" onclick="event.stopPropagation()">
                <h2>🛡️ Resolve Report #${id}</h2>
                <div class="form-group"><label>Action</label><select id="resolve-action">
                    <option value="WARNING">Warning</option>
                    <option value="MUTE">Mute User</option>
                    <option value="BAN">Ban User</option>
                    <option value="DELETE_CONTENT">Delete Content</option>
                    <option value="NONE">No Action</option>
                </select></div>
                <div class="form-group"><label>Note</label><textarea id="resolve-note" placeholder="Resolution details..."></textarea></div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-danger" onclick="resolveReport(${id})">Confirm</button>
                </div>
            </div>
        </div>`;
}

async function resolveReport(id) {
    const body = {
        action: document.getElementById('resolve-action').value,
        note: document.getElementById('resolve-note').value
    };
    const res = await API.post(`/api/reports/${id}/resolve`, body);
    if (res?.success) { showToast('Report resolved'); closeModal(); loadReports(reportPage); }
    else showToast(res?.message || 'Failed', 'error');
}

async function dismissReport(id) {
    const res = await API.post(`/api/reports/${id}/dismiss`, { note: 'Dismissed by admin' });
    if (res?.success) { showToast('Report dismissed'); loadReports(reportPage); }
    else showToast(res?.message || 'Failed', 'error');
}

// ====== CONFIGS ======
async function loadConfigs() {
    const res = await API.get('/api/configs');
    if (!res || !res.success) return;

    const configs = res.data;
    let html = `<table class="data-table"><thead><tr>
        <th>Key</th><th>Value</th><th>Type</th><th>Category</th><th>Description</th><th>Actions</th>
    </tr></thead><tbody>`;

    configs.forEach(c => {
        html += `<tr>
            <td><code>${escapeHtml(c.configKey)}</code></td>
            <td><strong>${escapeHtml(c.configValue)}</strong></td>
            <td>${escapeHtml(c.configType)}</td>
            <td>${escapeHtml(c.category)}</td>
            <td>${escapeHtml(c.description) || '-'}</td>
            <td class="btn-group">
                <button class="btn btn-sm btn-info" onclick="showEditConfig(${c.id}, '${escapeHtml(c.configKey)}', '${escapeHtml(c.configValue)}', '${escapeHtml(c.configType)}')">Edit</button>
                <button class="btn btn-sm btn-danger" onclick="deleteConfig(${c.id})">Delete</button>
            </td>
        </tr>`;
    });
    html += '</tbody></table>';
    document.getElementById('configs-table').innerHTML = html;
}

function showEditConfig(id, key, value, type) {
    document.getElementById('modal-container').innerHTML = `
        <div class="modal-overlay" onclick="closeModal(event)">
            <div class="modal" onclick="event.stopPropagation()">
                <h2>⚙️ Edit Configuration</h2>
                <div class="form-group"><label>Key</label><input value="${key}" disabled></div>
                <div class="form-group"><label>Value</label>
                    ${type === 'BOOLEAN' ?
                        `<select id="config-value"><option value="true" ${value==='true'?'selected':''}>true</option><option value="false" ${value==='false'?'selected':''}>false</option></select>` :
                        `<input id="config-value" value="${value}">`}
                </div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-primary" onclick="saveConfig(${id})">Save</button>
                </div>
            </div>
        </div>`;
}

async function saveConfig(id) {
    const value = document.getElementById('config-value').value;
    const res = await API.put(`/api/configs/${id}`, { configValue: value });
    if (res?.success) { showToast('Configuration saved'); closeModal(); loadConfigs(); }
    else showToast(res?.message || 'Failed', 'error');
}

async function deleteConfig(id) {
    if (!confirm('Delete this configuration?')) return;
    const res = await API.del(`/api/configs/${id}`);
    if (res?.success) { showToast('Config deleted'); loadConfigs(); }
    else showToast(res?.message || 'Failed', 'error');
}

function showCreateConfig() {
    document.getElementById('modal-container').innerHTML = `
        <div class="modal-overlay" onclick="closeModal(event)">
            <div class="modal" onclick="event.stopPropagation()">
                <h2>➕ Add Configuration</h2>
                <div class="form-group"><label>Key</label><input id="new-cfg-key" placeholder="config_key_name"></div>
                <div class="form-group"><label>Value</label><input id="new-cfg-value" placeholder="value"></div>
                <div class="form-group"><label>Type</label><select id="new-cfg-type">
                    <option value="STRING">String</option><option value="NUMBER">Number</option>
                    <option value="BOOLEAN">Boolean</option><option value="JSON">JSON</option>
                </select></div>
                <div class="form-group"><label>Category</label><select id="new-cfg-category">
                    <option value="GENERAL">General</option><option value="SECURITY">Security</option>
                    <option value="MESSAGING">Messaging</option><option value="STORAGE">Storage</option>
                    <option value="NOTIFICATION">Notification</option><option value="CLIENT">Client</option>
                </select></div>
                <div class="form-group"><label>Description</label><input id="new-cfg-desc" placeholder="Description"></div>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-primary" onclick="createConfig()">Create</button>
                </div>
            </div>
        </div>`;
}

async function createConfig() {
    const body = {
        configKey: document.getElementById('new-cfg-key').value,
        configValue: document.getElementById('new-cfg-value').value,
        configType: document.getElementById('new-cfg-type').value,
        category: document.getElementById('new-cfg-category').value,
        description: document.getElementById('new-cfg-desc').value
    };
    const res = await API.post('/api/configs', body);
    if (res?.success) { showToast('Config created'); closeModal(); loadConfigs(); }
    else showToast(res?.message || 'Failed', 'error');
}

// ====== AUDIT LOGS ======
let auditPage = 0;
async function loadAuditLogs(page = 0) {
    auditPage = page;
    const res = await API.get(`/api/audit-logs?page=${page}&size=20`);
    if (!res || !res.success) return;

    const items = res.data.content;
    const totalPages = res.data.totalPages;

    let html = `<table class="data-table"><thead><tr>
        <th>Time</th><th>Admin</th><th>Action</th><th>Target</th><th>Details</th><th>IP</th>
    </tr></thead><tbody>`;

    items.forEach(l => {
        html += `<tr>
            <td>${formatDate(l.createdAt)}</td>
            <td>${l.adminUsername}</td>
            <td><span class="badge badge-active">${l.action}</span></td>
            <td>${l.targetType || '-'}#${l.targetId || '-'}</td>
            <td title="${l.details || ''}">${(l.details || '').substring(0, 60)}${l.details && l.details.length > 60 ? '...' : ''}</td>
            <td>${l.ipAddress || '-'}</td>
        </tr>`;
    });
    html += '</tbody></table>';
    html += renderPagination(page, totalPages, 'loadAuditLogs');
    document.getElementById('audit-logs-table').innerHTML = html;
}

// ====== UTILS ======
function escapeHtml(str) {
    if (str == null) return '';
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleString();
}

function renderPagination(current, total, func) {
    if (total <= 1) return '';
    let html = '<div class="pagination">';
    html += `<button ${current === 0 ? 'disabled' : ''} onclick="${func}(${current - 1})">‹ Prev</button>`;
    const start = Math.max(0, current - 2);
    const end = Math.min(total, start + 5);
    for (let i = start; i < end; i++) {
        html += `<button class="${i === current ? 'active' : ''}" onclick="${func}(${i})">${i + 1}</button>`;
    }
    html += `<span>of ${total}</span>`;
    html += `<button ${current >= total - 1 ? 'disabled' : ''} onclick="${func}(${current + 1})">Next ›</button>`;
    html += '</div>';
    return html;
}

function closeModal(event) {
    if (event && event.target !== event.currentTarget) return;
    document.getElementById('modal-container').innerHTML = '';
}
