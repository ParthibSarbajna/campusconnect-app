-- V5: the service already rejects duplicate company names ignoring case; enforce the same rule in the database
-- so concurrent requests cannot create "Acme" and "ACME". Forward-only: V4 is untouched.
CREATE UNIQUE INDEX uq_companies_name_lower ON companies (lower(name));
