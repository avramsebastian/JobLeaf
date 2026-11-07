import { createContext, useContext, useState, useEffect } from 'react';
import httpClient from '../config/httpClient';
import { API_ENDPOINTS } from '../config/api';
import { clearSession, getExpiresAt, isSessionExpired, saveSession } from '../utils/authSession';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Load user from localStorage on app start, unless the session (the cookie's lifetime) has ended
  useEffect(() => {
    const savedUser = localStorage.getItem('user');

    if (savedUser && !isSessionExpired()) {
      try {
        const parsedUser = JSON.parse(savedUser);

        // MIGRATION: Check if email field looks like it contains a name instead of email
        // This fixes old cached data from before the bug was fixed
        if (parsedUser.email && !parsedUser.email.includes('@')) {
          console.warn('[Auth] Detected invalid cached user data. Clearing localStorage...');
          clearSession();
          setIsLoading(false);
          return;
        }

        setUser(parsedUser);
      } catch (error) {
        console.error('Error loading saved user:', error);
        clearSession();
      }
    } else if (savedUser) {
      console.log('[Auth] Saved session has expired, user must log in again');
      clearSession();
    }
    setIsLoading(false);
  }, []);

  // Save user to localStorage whenever user changes. Clearing is done by logout, expiry and the 401 handler only:
  // user is also null on the first render after a reload, and clearing then would delete the token being restored.
  useEffect(() => {
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    }
  }, [user]);

  // Log out automatically when the token expires. ProtectedRoute then sends the user to /login.
  useEffect(() => {
    const expiresAt = user ? getExpiresAt() : null;
    if (expiresAt === null) {
      return undefined;
    }
    const logoutAtExpiry = () => {
      console.log('[Auth] Session expired, logging out');
      clearSession();
      setUser(null);
    };
    const timeoutId = setTimeout(logoutAtExpiry, Math.max(expiresAt - Date.now(), 0));
    return () => clearTimeout(timeoutId);
  }, [user]);

  /**
   * Logs the user in from a login or register response: stores the token, builds the user and,
   * for job seekers, checks whether the profile is complete.
   */
  const startSession = async (data) => {
    const { expiresAt, user: userData } = data;

    // The JWT itself arrived in an HttpOnly cookie; only remember when it stops being accepted
    saveSession(expiresAt);

    // Create user object with role from backend
    // Backend UserResponse contains: userId, name, email, mobileNumber, role
    // Role values from backend: ROLE_JOB_SEEKER, ROLE_EMPLOYER, ROLE_ADMIN
    const userWithRole = {
      ...userData,
      phone: userData.mobileNumber, // Map mobileNumber to phone for frontend consistency
      role: userData.role, // Use the role from the backend (ROLE_JOB_SEEKER, ROLE_EMPLOYER, ROLE_ADMIN)
      profileComplete: false // Default to false, will be checked for job seekers
    };

    console.log('[Auth] Login successful, user:', userWithRole);

    // For job seekers, check if profile is complete
    if (userData.role === 'ROLE_JOB_SEEKER') {
      try {
        const { getProfile } = await import('../services/profileService');
        const profileData = await getProfile();

        // Check if profile has all required fields
        const isComplete = !!(
          profileData &&
          profileData.jobTitle &&
          profileData.location &&
          profileData.experienceLevel &&
          profileData.professionalBio &&
          profileData.profilePictureName &&
          profileData.resumeName
        );

        userWithRole.profileComplete = isComplete;
        console.log('[Auth] Profile completeness checked:', isComplete);
      } catch (error) {
        console.error('[Auth] Error checking profile completeness:', error);
        // If error, keep profileComplete as false
      }
    } else {
      // Employers don't need profile completion
      userWithRole.profileComplete = true;
    }

    setUser(userWithRole);
    return userWithRole;
  };

  const login = async (email, password, userType) => {
    setIsLoading(true);

    try {
      console.log('[Auth] Attempting login with:', { email, userType });

      // Call backend authentication API
      // Field names match LoginRequest in the OpenAPI spec
      const response = await httpClient.post(API_ENDPOINTS.LOGIN, {
        email,
        password
      });

      console.log('[Auth] Login response:', response.data);

      if (response.data?.user) {
        const userWithRole = await startSession(response.data);
        setIsLoading(false);
        return { success: true, user: userWithRole };
      } else {
        console.error('[Auth] Invalid response - missing user');
        setIsLoading(false);
        return { success: false, error: 'Invalid response from server' };
      }
    } catch (error) {
      setIsLoading(false);
      console.error('[Auth] Login error:', error);

      if (error.response) {
        // Server responded with error; the backend sends RFC 9457 problem details, whose message is in "detail"
        const errorMessage = error.response.data?.detail
          || error.response.data?.message
          || error.response.data?.error
          || `Authentication failed (${error.response.status})`;

        console.error('[Auth] Server error:', {
          status: error.response.status,
          data: error.response.data
        });

        return {
          success: false,
          error: errorMessage
        };
      } else if (error.request) {
        // Request was made but no response received
        console.error('[Auth] No response from server');
        return {
          success: false,
          error: 'Cannot connect to server. Please check if backend is running on http://localhost:8080'
        };
      } else {
        // Something else happened
        console.error('[Auth] Unexpected error:', error.message);
        return {
          success: false,
          error: 'An unexpected error occurred: ' + error.message
        };
      }
    }
  };

  const register = async (userData) => {
    setIsLoading(true);

    try {
      console.log('[Auth] Attempting registration with:', {
        name: userData.name,
        email: userData.email,
        mobileNumber: userData.mobileNumber
      });

      // Field names match RegisterRequest in the OpenAPI spec. No role: every new account is a job seeker.
      const response = await httpClient.post(API_ENDPOINTS.REGISTER, {
        name: userData.name,
        email: userData.email,
        mobileNumber: userData.mobileNumber,
        password: userData.password
      });

      // The backend logs the user in on registration: same token and user as a login
      const userWithRole = await startSession(response.data);
      setIsLoading(false);
      return {
        success: true,
        message: response.data?.message,
        user: userWithRole
      };

    } catch (error) {
      setIsLoading(false);
      console.error('[Auth] Registration error:', error);

      if (error.response) {
        // Server responded with error
        console.error('[Auth] Server error:', {
          status: error.response.status,
          data: error.response.data
        });

        // The backend sends RFC 9457 problem details; field problems are listed in "errors"
        // (e.g. {email: "An account with this email already exists"} or a breached password)
        const errorData = error.response.data;
        if (errorData?.errors && typeof errorData.errors === 'object') {
          return {
            success: false,
            fieldErrors: errorData.errors
          };
        }

        // Otherwise, it's a general error message
        const errorMessage = errorData?.detail
          || errorData?.message
          || (typeof errorData === 'string' ? errorData : null)
          || `Registration failed (${error.response.status})`;

        return {
          success: false,
          error: errorMessage
        };
      } else if (error.request) {
        // Request was made but no response received
        console.error('[Auth] No response from server');
        return {
          success: false,
          error: 'Cannot connect to server. Please check if backend is running on http://localhost:8080'
        };
      } else {
        // Something else happened
        console.error('[Auth] Unexpected error:', error.message);
        return {
          success: false,
          error: 'An unexpected error occurred: ' + error.message
        };
      }
    }
  };

  const updateProfile = async (profileData) => {
    if (!user) {
      return { success: false, error: 'No user logged in' };
    }

    setIsLoading(true);

    try {
      // Simulate API call delay
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Update user data
      const updatedUser = {
        ...user,
        ...profileData,
        updatedAt: new Date().toISOString()
      };

      setUser(updatedUser);
      setIsLoading(false);
      return { success: true, user: updatedUser };
    } catch (error) {
      setIsLoading(false);
      return { success: false, error: 'Failed to update profile' };
    }
  };

  const updateProfileComplete = (isComplete) => {
    if (user) {
      const updatedUser = {
        ...user,
        profileComplete: isComplete
      };
      setUser(updatedUser);
      console.log('[Auth] Updated profileComplete to:', isComplete);
    }
  };

  const logout = async () => {
    setUser(null);
    clearSession();
    try {
      // The token cookie is HttpOnly, so only the backend can delete it
      await httpClient.post(API_ENDPOINTS.LOGOUT);
    } catch (error) {
      console.warn('[Auth] Logout request failed; the cookie expires on its own', error);
    }
  };

  const value = {
    user,
    isLoading,
    login,
    register,
    updateProfile,
    updateProfileComplete,
    logout,
    isAuthenticated: !!user,
    isEmployer: user?.role === 'ROLE_EMPLOYER',
    isJobSeeker: user?.role === 'ROLE_JOB_SEEKER',
    isAdmin: user?.role === 'ROLE_ADMIN'
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};