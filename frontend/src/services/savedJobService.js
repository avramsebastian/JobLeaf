import httpClient from '../config/httpClient';
import { API_ENDPOINTS } from '../config/api';

/**
 * The logged-in job seeker's saved jobs (plain job objects, newest posted first).
 */
export const getSavedJobs = async () => {
  const response = await httpClient.get(API_ENDPOINTS.SAVED_JOBS);
  return response.data;
};

/**
 * Saves a job. Saving it again is fine (nothing changes).
 * @returns the saved job
 */
export const saveJob = async (jobId) => {
  const response = await httpClient.post(API_ENDPOINTS.SAVE_JOB(jobId));
  return response.data;
};

/**
 * Removes a job from the saved jobs. Removing a job that is not saved is fine (nothing changes).
 */
export const unsaveJob = async (jobId) => {
  await httpClient.delete(API_ENDPOINTS.UNSAVE_JOB(jobId));
};
