# CampusConnect

Centralized campus placement management with AI-driven resume optimization (final-year B.Tech project).

**Status: partially complete.** Accounts, roles, student profiles, companies, placement drives, eligibility and applications are implemented. Recruitment rounds and results, offers and the "placed once per batch" rule, analytics, imports and the resume optimizer are **not implemented yet** (see "Not implemented"). Nothing in this README claims a check passed unless it says so under "Verification status".

## What exists

| Area | Implemented |
|---|---|
| Accounts and roles | Student registration, session sign-in/out, `STUDENT` / `DEPARTMENT_SPOC` / `PLACEMENT_ADMIN`, admin account console, department-scoped SPOC, lockout, CSRF |
| Student profile | Roll number, branch, graduation year, CGPA, active backlogs (`/students/me`) |
| Companies (admin) | Create, list, view, update, activate/deactivate; names unique ignoring case (DB index in V5) |
| Placement drives (admin) | Create/edit; lifecycle DRAFT, OPEN, CLOSED, CANCELLED; criteria: batch year, min CGPA, max active backlogs, eligible branches, deadline, CTC, location, description; paged, filterable list |
| Eligibility | `EligibilityEvaluator`: every criterion comes from the drive, every failure gives a plain-language reason |
| Applications | Students browse OPEN drives with their eligibility verdict, apply (deadline, eligibility and duplicate checks on the server), withdraw; admin shortlists/selects/rejects; SPOC sees only their department's students; every status change is stored in `application_status_history` |
| Frontend | Role-aware navigation; screens for companies, drives, student drives, student applications, applications board (admin read-write, SPOC read-only) |

The batch (cohort) of a student is their `graduation_year`; a drive is open only to students of the drive's `graduation_year`.

## Not implemented

- Recruitment rounds and per-round results (an application can currently only be APPLIED, SHORTLISTED, SELECTED, REJECTED or WITHDRAWN).
- Offers, offer acceptance, placement history, and the rule "a student may be placed only once per batch". Eligibility does not yet consider prior placement.
- Placement analytics and reports, CSV/Excel export, historical imports.
- AI resume optimizer (and therefore no external AI service or credential is used anywhere).
- Student-facing notifications, email, password reset.

## Security design

Server-side session in an HttpOnly `CC_SESSION` cookie (`SameSite=Lax`; set `SESSION_COOKIE_SECURE=true` over HTTPS), CSRF token required on every state-changing request (`GET /api/v1/auth/csrf`), BCrypt passwords, generic login errors, per-account lockout. Authorization is enforced on the server: URL rules, `@PreAuthorize`, ownership checks (a student's `withdraw` query includes their own id, so another student's application id is a 404) and department scoping for SPOCs. Nothing sensitive is kept in `localStorage`.

## API (all under `/api/v1`)

Authentication and accounts: see `AuthController`, `AccountController`, `AdminController` (`/auth/*`, `/users/*`, `/admin/users`, `/admin/departments`, `/departments/{id}/users`).

| Method and path | Who | Notes |
|---|---|---|
| `GET/PUT /students/me` | student | own profile |
| `GET/POST /admin/companies`, `GET/PUT /admin/companies/{id}`, `PATCH /admin/companies/{id}/active` | admin | `{"active": true|false}` is required; `{}` is 400 |
| `GET /admin/drives?status&companyId&graduationYear&page&size` | admin | paged (size capped at 100) |
| `POST /admin/drives`, `PUT /admin/drives/{id}`, `GET /admin/drives/{id}` | admin | new drives start as DRAFT; company must be active; deadline must be in the future |
| `POST /admin/drives/{id}/status` | admin | `{"status":"OPEN"|"CLOSED"|"CANCELLED"}`; illegal moves are 409. After OPEN only description, location, CTC and deadline can change |
| `GET /drives?page&size` | student | OPEN drives whose deadline has not passed, each with `eligibility {eligible, reasons[]}` and the student's own application status |
| `GET /drives/{id}` | student | same view for one drive |
| `POST /drives/{id}/applications` | student | 201; 404 not open, 409 deadline passed or duplicate, 422 not eligible (reasons in the message) |
| `GET /applications/me`, `POST /applications/{id}/withdraw` | student | withdraw only while APPLIED or SHORTLISTED |
| `GET /applications?driveId&status&page&size` | SPOC (own department only) or admin | student name, email, branch, batch, CGPA |
| `POST /admin/applications/{id}/status` | admin | `{"status", "note"}`: APPLIED to SHORTLISTED or REJECTED; SHORTLISTED to SELECTED or REJECTED; others final (409) |

Status codes: 401 not signed in, 403 forbidden or missing CSRF, 404 not found, 409 conflict, 422 not eligible, 400 invalid input.

## Database migrations (forward-only)

V1 baseline, V2 users, V3 student profiles, V4 companies (unchanged), **V5** case-insensitive unique company name index, **V6** `placement_drives`, `drive_branches`, `applications`, `application_status_history`. V6 constraints: unique `(drive_id, student_user_id)`, status check constraints, indexes for drive status/deadline, student applications and drive/status lookups.

## Required software

Java 21 (17+ works), Maven 3.9+, Node.js 18+ (20+ recommended), PostgreSQL 14+ (18 in use). Docker is not required.

## Windows setup (PowerShell)

```powershell
# 1) Database (once, in psql as a superuser)
#    CREATE USER campusconnect WITH PASSWORD 'choose-a-local-password';
#    CREATE DATABASE campusconnect OWNER campusconnect;

# 2) Configuration
Copy-Item .env.example .env      # edit DB_URL, DB_USER, DB_PASSWORD (and the BOOTSTRAP_ADMIN_* values for the first admin)

# 3) Backend (terminal 1). Flyway applies V1..V6 on startup.
cd backend
mvn spring-boot:run

# 4) Frontend (terminal 2)
cd frontend
npm.cmd ci
npm.cmd run dev                  # http://localhost:5173
```
After the first successful start delete `BOOTSTRAP_ADMIN_PASSWORD` from `.env`. `.env` is git-ignored: never zip or share it.

## Demonstration flow

1. Sign in as the admin. **Companies**: add a company. **Drives**: create a drive for a batch (for example 2027, CGPA 7.5, branches CSE, IT) and Publish it.
2. Register a student, sign in, open **My account** and save the profile (batch, branch, CGPA, backlogs).
3. As the student open **Drives**. Eligible: Apply works. Not eligible: the reasons are listed and there is no Apply button.
4. As the admin open **Applications**: Shortlist, then Select or Reject. As the student, **Applications** shows the new status.
5. As a SPOC (created by the admin for a department; students must be assigned to that department on the admin accounts page) open **Applications**: only that department's students appear.

## Tests

```powershell
cd backend;  mvn test            # JUnit 5 and Spring MockMvc; repositories are mocked, no database needed
cd backend;  mvn package
cd frontend; npm.cmd test        # Vitest
cd frontend; npm.cmd run build
```
New backend tests: `EligibilityEvaluatorTest`, `DriveServiceTest`, `ApplicationServiceTest`, `PlacementAuthorizationTest`. New frontend tests: `StudentDrives`, `StudentApplications`, `ApplicationsBoard`, `AdminDrives`, `AdminCompanies`.

## Verification status

**Historical (from the surefire reports packaged in the verification ZIP, run on the project owner's machine):** 68 backend tests, 0 failures, 0 errors, before the work described below. Frontend tests and the production build were reported as passing in an earlier run.

**Not executed anywhere yet:** everything added in this change: V5 and V6 migrations (never applied to PostgreSQL), Hibernate schema validation of the new entities, the new Java code (only a syntax-only `javac` pass was possible), the new backend and frontend tests (only a JavaScript parse check was possible), the frontend production build, and the live endpoints. Run the Tests commands above and then start the backend; Flyway and Hibernate `validate` will report any migration or mapping mismatch at startup.

**SPOC department scoping** is a SQL subquery in `ApplicationService.filter`. The unit tests cover who may call the endpoint but not the SQL itself, so verify it manually (demonstration flow step 5).

## Known limitations

- Sessions are in server memory (lost on restart, single instance).
- Lockout is per account, not per IP. No password reset or email verification.
- Company list is not paginated (fine for hundreds of companies).
- A student's department (used for SPOC scoping) is assigned by an admin; it is separate from the profile's free-text `branch` used for eligibility.
- Drives cannot be deleted, only cancelled. No Maven wrapper. No database-backed integration tests. No 40,000-student load test has been run.

## Layout

```
backend/    Spring Boot (auth/, account/, student/, company/, drive/, application/, department/, config/, common/, db/migration/)
frontend/   React + Vite (api/, auth/, components/, pages/, lib/)
reference/demo/   original single-file HTML demo
```
