import axios from 'axios';

/*
 * Bảo mật JWT: token KHÔNG còn được lưu ở localStorage phía FE nữa.
 * Trước đây, nếu site có bất kỳ lỗ XSS nào (kể cả từ thư viện bên thứ ba),
 * JS độc hại có thể đọc localStorage.getItem('gis_token') và đánh cắp toàn
 * bộ token. Giờ BE set JWT qua cookie HttpOnly + Secure + SameSite, nên JS
 * (kể cả JS độc hại chèn qua XSS) không thể đọc được token đó.
 *
 * `withCredentials: true` để trình duyệt tự động đính kèm cookie HttpOnly
 * trong mọi request tới BE — thay cho việc FE tự gắn header Authorization.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Response interceptor to handle global errors (like 401 Unauthorized)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Không còn token/user ở localStorage để xóa — cookie HttpOnly hết hạn
      // hoặc bị BE từ chối thì tự nhiên request sẽ 401. Chỉ cần điều hướng
      // về trang login; AuthContext sẽ tự cập nhật lại trạng thái qua /api/auth/me.

      // Only redirect if we are not already on the login page to prevent redirect loops
      if (!window.location.pathname.endsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
