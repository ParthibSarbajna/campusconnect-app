# Changelog

## Unreleased (this change set; not yet built or tested by the assistant)
### Fixed
- `CompanyController`: `PATCH /admin/companies/{id}/active` with a body of `{}` used to deactivate the company (missing boolean defaulted to false). `active` is now `@NotNull` and the body is validated.
### Added
- Flyway **V5**: unique index on `lower(companies.name)` so the database enforces the case-insensitive rule the service already applied.
- Flyway **V6**: placement drives, drive branches, applications, application status history.
- Backend packages `drive` and `application`: entities, repositories, DTOs, `DriveService`, `EligibilityEvaluator`, `StudentDriveService`, `ApplicationService`, three controllers.
- `StudentProfileRepository.findByUserIdIn` (batch lookup for application lists).
- Frontend: `AdminCompanies`, `AdminDrives`, `StudentDrives`, `StudentApplications`, `ApplicationsBoard`, API modules, `StatusPill`, navigation and routes.
- Tests: 4 backend test classes and 5 frontend test files (see README).
### Changed
- README rewritten to match the code. `AdminHome` footer text updated.
