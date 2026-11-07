export const ROLES = { STUDENT: 'STUDENT', SPOC: 'DEPARTMENT_SPOC', ADMIN: 'PLACEMENT_ADMIN' };
export const ROLE_LABELS = {
  STUDENT: 'Student',
  DEPARTMENT_SPOC: 'Department SPOC',
  PLACEMENT_ADMIN: 'Placement Admin',
};
const HOME = { STUDENT: '/student', DEPARTMENT_SPOC: '/spoc', PLACEMENT_ADMIN: '/admin' };
export const homePathFor = (role) => HOME[role] || '/';
