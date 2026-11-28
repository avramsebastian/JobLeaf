import httpClient from '../config/httpClient';
import { API_ENDPOINTS } from '../config/api';

/** Same limits as the backend (ProfileFile): checked here only to give a quick message, the backend checks again. */
export const MAX_FILE_SIZE = 10 * 1024 * 1024;
export const PICTURE_EXTENSIONS = ['png', 'jpg', 'jpeg'];
export const RESUME_EXTENSIONS = ['pdf'];

/**
 * @returns an error message, or null when the file looks fine
 */
export const checkFile = (file, allowedExtensions, label) => {
  const extension = file.name.split('.').pop().toLowerCase();
  if (!allowedExtensions.includes(extension)) {
    return `${label} must be one of: ${allowedExtensions.join(', ')}`;
  }
  if (file.size > MAX_FILE_SIZE) {
    return `${label} must be at most 10 MB`;
  }
  if (file.size === 0) {
    return `${label} is empty`;
  }
  return null;
};

/**
 * The logged-in job seeker's profile, without file contents (only names, types and has... flags).
 * @returns the profile, or null when none was saved yet (the backend answers 404)
 */
export const getProfile = async () => {
  try {
    const response = await httpClient.get(API_ENDPOINTS.PROFILE);
    return response.data;
  } catch (error) {
    if (error.response?.status === 404) {
      return null;
    }
    console.error('Error fetching profile:', error);
    throw error;
  }
};

/**
 * Create or update the profile. The details are always sent; a file that is null keeps the stored one, so the
 * files can be added later (step two).
 *
 * @param {Object} profileData - jobTitle, location, experienceLevel, professionalBio, portfolioWebsite
 * @param {File|null} profilePicture - png, jpg or jpeg
 * @param {File|null} resume - pdf
 */
export const updateProfile = async (profileData, profilePicture, resume) => {
  const formData = new FormData();

  // The backend reads this part as JSON (and validates it), so it must be sent with the JSON content type
  formData.append('profile', new Blob([JSON.stringify(profileData)], { type: 'application/json' }));
  if (profilePicture) {
    formData.append('profilePicture', profilePicture);
  }
  if (resume) {
    formData.append('resume', resume);
  }

  // No Content-Type here: for FormData the browser sets "multipart/form-data" with the boundary itself
  const response = await httpClient.put(API_ENDPOINTS.UPDATE_PROFILE, formData);
  return response.data;
};

/**
 * The profile picture as a blob URL for an img tag (the request needs the login cookie, which a plain img src to the
 * API would not send in production).
 * @returns the URL, or null when there is no picture
 */
export const getProfilePictureUrl = async () => {
  try {
    const response = await httpClient.get(API_ENDPOINTS.PROFILE_PICTURE, {
      responseType: 'blob',
      headers: { Accept: 'image/png, image/jpeg' },
    });
    return URL.createObjectURL(response.data);
  } catch (error) {
    console.error('Error fetching profile picture:', error);
    return null;
  }
};

const fetchResume = async () => {
  const response = await httpClient.get(API_ENDPOINTS.PROFILE_RESUME, {
    responseType: 'blob',
    headers: { Accept: 'application/pdf' },
  });
  return response.data;
};

/**
 * Downloads the resume under its stored name.
 */
export const downloadResume = async (fileName = 'resume.pdf') => {
  const url = URL.createObjectURL(await fetchResume());
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

/**
 * The resume as a blob URL, to open it in a new tab.
 * @returns the URL, or null when it could not be loaded
 */
export const getResumeUrl = async () => {
  try {
    return URL.createObjectURL(await fetchResume());
  } catch (error) {
    console.error('Error fetching resume:', error);
    return null;
  }
};
