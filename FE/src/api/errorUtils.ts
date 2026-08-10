/**
 * Trích xuất thông báo lỗi hiển thị được cho người dùng từ một lỗi axios,
 * gộp cả các lỗi validate theo từng field (`ErrorResponse.details` ở BE -
 * xem `GlobalExceptionHandler#handleValidationErrors`) vào cùng thông báo.
 *
 * TRƯỚC ĐÂY: AddUserModal/EditUserModal chỉ đọc `response.data.message` và
 * bỏ qua hẳn `details`. Với lỗi validate (vd. mật khẩu ngắn hơn 6 ký tự),
 * BE trả `message: "Lỗi xác thực dữ liệu"` (chung chung) kèm
 * `details: { password: "Password must be at least 6..." }` (cụ thể theo
 * field) - nhưng người dùng chỉ từng thấy phần chung chung, không biết
 * chính xác field nào sai và sai vì sao.
 */

interface ApiErrorResponseBody {
  message?: string;
  details?: Record<string, string> | null;
}

export function extractErrorMessage(err: unknown, fallback: string): string {
  if (!err || typeof err !== 'object' || !('response' in err)) {
    return fallback;
  }

  const data = (err as { response?: { data?: ApiErrorResponseBody } }).response?.data;
  if (!data) {
    return fallback;
  }

  const baseMessage = data.message || fallback;
  const details = data.details;
  if (details && Object.keys(details).length > 0) {
    const detailText = Object.values(details).join(' ');
    return `${baseMessage}: ${detailText}`;
  }

  return baseMessage;
}
