/**
 * Telegram Admin Panel - API Client Module
 */
const API = {
    baseUrl: '',
    token: localStorage.getItem('admin_token'),

    async request(method, path, body = null) {
        const headers = { 'Content-Type': 'application/json' };
        if (this.token) headers['Authorization'] = 'Bearer ' + this.token;

        const opts = { method, headers };
        if (body) opts.body = JSON.stringify(body);

        try {
            const res = await fetch(this.baseUrl + path, opts);

            if (res.status === 403 || res.status === 401) {
                this.logout();
                return null;
            }

            const contentType = res.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await res.json();
            }
            return null;
        } catch (e) {
            console.error('API request failed:', e);
            return null;
        }
    },

    get(path) { return this.request('GET', path); },
    post(path, body) { return this.request('POST', path, body); },
    put(path, body) { return this.request('PUT', path, body); },
    del(path) { return this.request('DELETE', path); },

    setToken(token) {
        this.token = token;
        localStorage.setItem('admin_token', token);
    },

    logout() {
        this.token = null;
        localStorage.removeItem('admin_token');
        localStorage.removeItem('admin_user');
        window.location.reload();
    },

    isAuthenticated() {
        return !!this.token;
    }
};
