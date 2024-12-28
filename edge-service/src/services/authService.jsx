const API_URL = import.meta.env.VITE_BACKEND_URL;

class AuthService {
  async login(email, password) {
    const response = await fetch(`${API_URL}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    });

    const data = await response.json();
    
    if (!response.ok) {
      throw new Error(data.message || 'Login failed');
    }

    // Store user data and token in localStorage
    if (data.token) {
      localStorage.setItem('user', JSON.stringify({
        user: data.user,
        token: data.token
      }));
    }

    return data;
  }

  async register({ username, email, password }) {
    const response = await fetch(`${API_URL}/api/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, email, password }),
    });

    const data = await response.json();
    
    if (!response.ok) {
      throw new Error(data.message || 'Registration failed');
    }

    // Store user data and token in localStorage
    if (data.token) {
      localStorage.setItem('user', JSON.stringify({
        user: data.user,
        token: data.token
      }));
    }

    return data;
  }

  getToken() {
    const user = JSON.parse(localStorage.getItem('user'));
    return user?.token;
  }

  isAuthenticated() {
    return !!this.getToken();
  }

  logout() {
    localStorage.removeItem('user');
  }
}

export default new AuthService();