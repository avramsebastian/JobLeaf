import httpClient from '../config/httpClient';
import { API_ENDPOINTS } from '../config/api';

/**
 * Apply for a job
 * @param {string} jobId - The job ID (UUID)
 * @param {string} coverLetter - Optional cover letter
 */
export const applyForJob = async (jobId, coverLetter = '') => {
  try {
    const response = await httpClient.post(API_ENDPOINTS.APPLY_JOB, {
      jobId,
      // The backend treats an empty cover letter as none
      coverLetter: coverLetter || null
    });
    return response.data;
  } catch (error) {
    console.error('Error applying for job:', error);
    throw error;
  }
};

/**
 * Withdraw an application
 * @param {string} jobId - The job ID (UUID)
 */
export const withdrawApplication = async (jobId) => {
  try {
    await httpClient.delete(API_ENDPOINTS.WITHDRAW_APPLICATION(jobId));
  } catch (error) {
    console.error('Error withdrawing application:', error);
    throw error;
  }
};

/**
 * Get all applications for the current user
 */
export const getMyApplications = async () => {
  try {
    const response = await httpClient.get(API_ENDPOINTS.MY_APPLICATIONS);
    return response.data;
  } catch (error) {
    console.error('Error fetching applications:', error);
    throw error;
  }
};

/**
 * Get all applied job IDs for the current user (lightweight)
 */

/**
 * Check if user has applied for a job
 * @param {string} jobId - The job ID (UUID)
 */

/**
 * Get all applications for a specific job (Employer)
 * @param {string} jobId - The job ID (UUID)
 */
export const getApplicationsByJob = async (jobId) => {
  try {
    const response = await httpClient.get(API_ENDPOINTS.APPLICATIONS_BY_JOB(jobId));
    return response.data;
  } catch (error) {
    console.error('Error fetching job applications:', error);
    throw error;
  }
};

/**
 * Change status and internal notes of a received application (Employer)
 * @param {string} applicationId - The application ID (UUID)
 * @param {string} status - REVIEWED, SHORTLISTED, INTERVIEWED, OFFERED or REJECTED
 * @param {string} [notes] - Left out: the stored notes stay. Empty text: the notes are removed.
 */
export const updateApplication = async (applicationId, status, notes) => {
  try {
    const response = await httpClient.patch(
      API_ENDPOINTS.UPDATE_JOB_APPLICATION(applicationId),
      // undefined is dropped by JSON.stringify, so the backend keeps the notes
      { status, notes }
    );
    return response.data;
  } catch (error) {
    console.error('Error updating application status:', error);
    throw error;
  }
};

/**
 * The applicant's profile picture as a blob URL for an img tag (Employer). Revoke it with URL.revokeObjectURL.
 * @param {string} applicationId - The application ID (UUID)
 * @returns the URL, or null when there is no picture
 */
export const getApplicantPictureUrl = async (applicationId) => {
  try {
    const response = await httpClient.get(API_ENDPOINTS.APPLICANT_PICTURE(applicationId), {
      responseType: 'blob',
      headers: { Accept: 'image/png, image/jpeg' },
    });
    return URL.createObjectURL(response.data);
  } catch (error) {
    console.error('Error fetching applicant picture:', error);
    return null;
  }
};

/**
 * The applicant's resume as a blob URL, to open it in a new tab (Employer).
 * @param {string} applicationId - The application ID (UUID)
 */
export const getApplicantResumeUrl = async (applicationId) => {
  const response = await httpClient.get(API_ENDPOINTS.APPLICANT_RESUME(applicationId), {
    responseType: 'blob',
    headers: { Accept: 'application/pdf' },
  });
  return URL.createObjectURL(response.data);
};
