/**
 * API Configuration
 * Centralized configuration for all API endpoints
 */

// Base URL for the backend API
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/jobleaf";

// API Version Configuration
// Default version sent in Accept header for all REST API calls
// Format: application/vnd.jobleaf+json;v=1.0
export const DEFAULT_API_VERSION = "1.0";

// Supported API versions
export const SUPPORTED_API_VERSIONS = ["1.0", "2.0"];

// Helper to generate Accept header value for a specific version
export const getAcceptHeader = (version = DEFAULT_API_VERSION) =>
  `application/vnd.jobleaf+json;v=${version}`;

// API Endpoints
export const API_ENDPOINTS = {
  // Company endpoints
  COMPANIES: "/companies/public",
  COMPANY_BY_ID: (id) => `/companies/${id}`,
  // Admin only (ROLE_ADMIN): list without jobs (GET), create (POST), update (PUT), delete (DELETE)
  ADMIN_COMPANIES: "/companies/admin",
  ADMIN_COMPANY_BY_ID: (id) => `/companies/${id}/admin`,

  // Job endpoints (placeholder for future use)
  JOBS: "/jobs",
  JOB_BY_ID: (id) => `/jobs/${id}`,

  // Auth endpoints (placeholder for future use)
  LOGIN: "/auth/login/public",
  REGISTER: "/auth/register/public",
  LOGOUT: "/auth/logout/public",

  // User endpoints
  // Job seeker only (ROLE_JOB_SEEKER): the profile without files (GET), create or update it (PUT, multipart),
  // and the two files on their own, so the profile details never carry megabytes
  PROFILE: "/profile/jobseeker",
  UPDATE_PROFILE: "/profile/jobseeker",
  PROFILE_PICTURE: "/profile/picture/jobseeker",
  PROFILE_RESUME: "/profile/resume/jobseeker",

  // Contact endpoints
  CONTACTS: "/contacts/public",
  CONTACT_BY_ID: (id) => `/contacts/${id}`,
  // Admin only (ROLE_ADMIN): the backend puts the access level at the end of the path
  ADMIN_CONTACTS: "/contacts/admin",
  ADMIN_CONTACTS_SORT: "/contacts/sort/admin",
  ADMIN_CONTACTS_PAGE: "/contacts/page/admin",
  UPDATE_CONTACT_STATUS: (id) => `/contacts/${id}/status/admin`,

  // CSRF token endpoint
  CSRF_TOKEN: "/csrf-token/public",

  // Admin User Management endpoints
  // Admin only (ROLE_ADMIN). The two PATCH endpoints take everything from the path, no request body
  SEARCH_USER_BY_EMAIL: "/users/search/admin",
  ELEVATE_TO_EMPLOYER: (userId) => `/users/${userId}/role/employer/admin`,
  ASSIGN_COMPANY_TO_EMPLOYER: (userId, companyId) => `/users/${userId}/company/${companyId}/admin`,

  // Employer Job Management endpoints
  // Employer only (ROLE_EMPLOYER): list (GET) and create (POST) jobs of the employer's own company,
  // change a job's status (PATCH with {"status": "ACTIVE" | "CLOSED"})
  EMPLOYER_JOBS: "/jobs/employer",
  POST_JOB: "/jobs/employer",
  UPDATE_JOB_STATUS: (jobId) => `/jobs/${jobId}/status/employer`,

  // Saved Jobs endpoints
  // Job seeker only (ROLE_JOB_SEEKER): list (GET), save (POST) and unsave (DELETE) jobs of the logged-in user
  SAVED_JOBS: "/saved-jobs/jobseeker",
  SAVE_JOB: (jobId) => `/saved-jobs/${jobId}/jobseeker`,
  UNSAVE_JOB: (jobId) => `/saved-jobs/${jobId}/jobseeker`,

  // Job Application endpoints
  JOB_APPLICATIONS: "/job-applications",
  // Job seeker only (ROLE_JOB_SEEKER): apply (POST {jobId, coverLetter}), own applications (GET),
  // withdraw (DELETE, keeps the application with status WITHDRAWN)
  APPLY_JOB: "/job-applications/jobseeker",
  MY_APPLICATIONS: "/job-applications/jobseeker",
  WITHDRAW_APPLICATION: (jobId) => `/job-applications/${jobId}/jobseeker`,
  // Employer only (ROLE_EMPLOYER): applications received for a job of the own company (GET),
  // change status and internal notes of one (PATCH {status, notes?})
  APPLICATIONS_BY_JOB: (jobId) => `/jobs/${jobId}/applications/employer`,
  UPDATE_JOB_APPLICATION: (applicationId) => `/jobs/applications/${applicationId}/employer`,
  // Employer only: the applicant's files, read one at a time (GET, binary)
  APPLICANT_PICTURE: (applicationId) => `/jobs/applications/${applicationId}/picture/employer`,
  APPLICANT_RESUME: (applicationId) => `/jobs/applications/${applicationId}/resume/employer`,
};

// HTTP Headers
export const API_HEADERS = {
  "Content-Type": "application/json",
};

// Request timeout (in milliseconds)
export const API_TIMEOUT = 30000;

export default {
  API_BASE_URL,
  API_ENDPOINTS,
  API_HEADERS,
  API_TIMEOUT,
  DEFAULT_API_VERSION,
  SUPPORTED_API_VERSIONS,
};
