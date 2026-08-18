export interface AdminUser {
  id: number;
  username: string;
  fullName: string;
  role: 'ADMIN' | 'VIEWER';
}
