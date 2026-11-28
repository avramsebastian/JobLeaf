import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useJobsData } from '../contexts/JobsDataContext';
import * as jobApplicationService from '../services/jobApplicationService';

// Backend statuses (ApplicationStatus) and how they are shown. WITHDRAWN applications are not sent to the employer.
const STATUS_LABELS = {
  PENDING: 'Pending',
  REVIEWED: 'Reviewed',
  SHORTLISTED: 'Shortlisted',
  INTERVIEWED: 'Interviewed',
  OFFERED: 'Offered',
  REJECTED: 'Rejected'
};

// The statuses the employer may set
const EMPLOYER_STATUSES = ['REVIEWED', 'SHORTLISTED', 'INTERVIEWED', 'OFFERED', 'REJECTED'];

// The applicant's picture, or the first letter of the name when there is none (or it is still loading)
const ApplicantAvatar = ({ url, name, className, textClassName }) => (
  url ? (
    <img src={url} alt={name} className={`${className} rounded-full object-cover`} />
  ) : (
    <div className={`${className} bg-gradient-to-br from-primary-600 to-purple-600 dark:from-primary-500 dark:to-purple-500 rounded-full flex items-center justify-center`}>
      <span className={`text-white font-bold ${textClassName}`}>{name?.charAt(0).toUpperCase()}</span>
    </div>
  )
);

const JobApplicants = () => {
  const { jobId } = useParams();
  const { getJobById } = useJobsData();
  const { isEmployer, isAuthenticated, isLoading: authLoading } = useAuth();
  const [applications, setApplications] = useState([]);
  const [job, setJob] = useState(null);
  const [filter, setFilter] = useState('all');
  const [notification, setNotification] = useState(null);
  const [selectedApplicant, setSelectedApplicant] = useState(null);
  const [showContactModal, setShowContactModal] = useState(false);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [loading, setLoading] = useState(true);
  // Notes being typed, per application id, until they are saved
  const [notesDrafts, setNotesDrafts] = useState({});
  // Profile pictures as blob URLs, per application id (only applicants who uploaded one)
  const [pictureUrls, setPictureUrls] = useState({});
  const [openingResume, setOpeningResume] = useState(false);

  // Prevent body scroll when modal is open
  useEffect(() => {
    if (showProfileModal || showContactModal) {
      document.body.style.overflow = 'hidden';
      document.body.style.position = 'fixed';
      document.body.style.width = '100%';
    } else {
      document.body.style.overflow = '';
      document.body.style.position = '';
      document.body.style.width = '';
    }

    // Cleanup on unmount
    return () => {
      document.body.style.overflow = '';
      document.body.style.position = '';
      document.body.style.width = '';
    };
  }, [showProfileModal, showContactModal]);

  useEffect(() => {
    let cancelled = false;
    let loadedUrls = [];

    const loadPictures = async (applicationsList) => {
      const withPicture = applicationsList.filter(app => app.applicant.hasProfilePicture);
      // One small request per picture, all at the same time
      const urls = await Promise.all(
        withPicture.map(app => jobApplicationService.getApplicantPictureUrl(app.applicationId))
      );
      loadedUrls = urls.filter(Boolean);
      if (cancelled) {
        loadedUrls.forEach(url => URL.revokeObjectURL(url));
        return;
      }
      setPictureUrls(Object.fromEntries(
        withPicture.map((app, index) => [app.applicationId, urls[index]]).filter(([, url]) => url)
      ));
    };

    const loadJobAndApplications = async () => {
      if (!jobId || !isEmployer) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);

        // Get job details from JobsDataContext
        const foundJob = getJobById(jobId);
        setJob(foundJob);

        // Fetch applications from backend
        const applicationsData = await jobApplicationService.getApplicationsByJob(jobId);

        // The backend sends the applicant with profile details; the files are not part of the list
        const transformedApplications = applicationsData.map(app => ({
          applicationId: app.id,
          notes: app.notes || '',
          coverLetter: app.coverLetter || '',
          applicant: {
            name: app.applicant.name,
            email: app.applicant.email,
            title: app.applicant.jobTitle || 'Not specified',
            phone: app.applicant.mobileNumber || 'Not provided',
            location: app.applicant.location || 'Not specified',
            bio: app.applicant.professionalBio || 'No bio available',
            experience: app.applicant.experienceLevel || 'No experience provided',
            applicationId: app.id,
            hasResume: app.applicant.hasResume,
            hasProfilePicture: app.applicant.hasProfilePicture,
            portfolio: app.applicant.portfolioWebsite || null,
            appliedAt: app.appliedAt,
            status: app.status
          }
        }));

        if (cancelled) return;
        setApplications(transformedApplications);
        loadPictures(transformedApplications);
      } catch (error) {
        console.error('[JobApplicants] Error loading job applications:', error);
        showNotification('Failed to load applications', 'error');
      } finally {
        setLoading(false);
      }
    };

    loadJobAndApplications();

    // Free the pictures when leaving the page (or before loading again)
    return () => {
      cancelled = true;
      loadedUrls.forEach(url => URL.revokeObjectURL(url));
    };
  }, [jobId, isEmployer, getJobById]);

  const openResume = async () => {
    // Open the tab right away (still part of the click), otherwise the browser blocks it as a pop-up
    const tab = window.open('', '_blank');
    setOpeningResume(true);
    try {
      const url = await jobApplicationService.getApplicantResumeUrl(selectedApplicant.applicationId);
      if (tab) {
        tab.location.href = url;
      } else {
        window.location.href = url;
      }
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (error) {
      console.error('Error opening resume:', error);
      if (tab) tab.close();
      showNotification('Could not open the resume', 'error');
    } finally {
      setOpeningResume(false);
    }
  };

  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 3000);
  };

  const replaceApplication = (updated) => {
    setApplications(prev =>
      prev.map(app =>
        app.applicationId === updated.id
          ? { ...app, notes: updated.notes || '', applicant: { ...app.applicant, status: updated.status } }
          : app
      )
    );
  };

  const handleStatusChange = async (applicationId, newStatus) => {
    try {
      // Notes are left out, so the backend keeps the stored ones
      replaceApplication(await jobApplicationService.updateApplication(applicationId, newStatus));
      showNotification('Application status updated successfully!');
    } catch (error) {
      console.error('Error updating application status:', error);
      showNotification(error.response?.data?.detail || 'Failed to update application status', 'error');
    }
  };

  const handleSaveNotes = async (application) => {
    const draft = notesDrafts[application.applicationId] ?? application.notes;
    try {
      replaceApplication(
        await jobApplicationService.updateApplication(application.applicationId, application.applicant.status, draft)
      );
      setNotesDrafts(prev => {
        const next = { ...prev };
        delete next[application.applicationId];
        return next;
      });
      showNotification('Notes saved');
    } catch (error) {
      console.error('Error saving notes:', error);
      showNotification(error.response?.data?.detail || 'Failed to save notes', 'error');
    }
  };

  const getTimeAgo = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInHours = Math.floor((now - date) / (1000 * 60 * 60));
    
    if (diffInHours < 24) {
      return `${diffInHours}h ago`;
    } else {
      const diffInDays = Math.floor(diffInHours / 24);
      return `${diffInDays}d ago`;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'PENDING':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'REVIEWED':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'SHORTLISTED':
      case 'INTERVIEWED':
        return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'OFFERED':
        return 'bg-green-100 text-green-800 border-green-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const filteredApplications = applications.filter(app => filter === 'all' || app.applicant.status === filter);

  const statusCounts = Object.fromEntries([
    ['all', applications.length],
    ...Object.keys(STATUS_LABELS).map(status => [
      status,
      applications.filter(app => app.applicant.status === status).length
    ])
  ]);

  // Show loading state while auth is initializing
  if (authLoading) {
    return (
      <div className="min-h-[calc(100vh-5rem)] bg-gradient-to-br from-gray-50 via-white to-primary-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-400">Loading...</p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated || !isEmployer) {
    return (
      <div className="min-h-[calc(100vh-5rem)] bg-gradient-to-br from-gray-50 via-white to-primary-50 flex items-center justify-center px-4 py-12 relative">
        <div className="text-center">
          <div className="text-6xl mb-6">🚫</div>
          <h2 className="text-2xl font-bold text-gray-900 mb-4">{!isAuthenticated ? 'Please Log In' : 'Access Denied'}</h2>
          <p className="text-gray-600 mb-6">{!isAuthenticated ? 'You need to be logged in to view job applicants.' : 'This page is only available for employers.'}</p>
          <Link to="/" className="inline-flex items-center px-6 py-3 bg-primary-600 text-white rounded-xl font-semibold hover:bg-primary-700 transition-colors">
            Go Home
          </Link>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-[calc(100vh-5rem)] bg-gradient-to-br from-gray-50 via-white to-primary-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600 dark:text-gray-400">Loading applicants...</p>
        </div>
      </div>
    );
  }

  if (!job) {
    return (
      <div className="min-h-[calc(100vh-5rem)] bg-gradient-to-br from-gray-50 via-white to-primary-50 flex items-center justify-center px-4 py-12 relative">
        <div className="text-center">
          <div className="text-6xl mb-6">❓</div>
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Job Not Found</h2>
          <p className="text-gray-600 mb-6">The job you're looking for doesn't exist or you don't have permission to view it.</p>
          <Link to="/employer/jobs" className="inline-flex items-center px-6 py-3 bg-primary-600 text-white rounded-xl font-semibold hover:bg-primary-700 transition-colors">
            Back to My Jobs
          </Link>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="min-h-[calc(100vh-5rem)] bg-gradient-to-br from-gray-50 via-white to-primary-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900 py-12 relative">
      {/* Background Elements */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute top-1/4 right-1/4 w-96 h-96 bg-gradient-to-br from-purple-200/30 to-primary-200/30 dark:from-purple-900/20 dark:to-primary-900/20 rounded-full blur-3xl"></div>
        <div className="absolute bottom-1/4 left-1/4 w-80 h-80 bg-gradient-to-tl from-blue-200/30 to-purple-200/30 dark:from-blue-900/20 dark:to-purple-900/20 rounded-full blur-3xl"></div>
      </div>

      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8">
          <Link to="/employer/jobs" className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium mb-4 inline-flex items-center">
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to My Jobs
          </Link>
          <h1 className="text-4xl font-black bg-gradient-to-r from-primary-600 via-purple-600 to-primary-800 dark:from-primary-400 dark:via-purple-400 dark:to-primary-300 bg-clip-text text-transparent mb-2">
            Job Applicants
          </h1>
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            {job.title} at {job.company}
          </p>
          <div className="flex flex-wrap items-center text-gray-600 dark:text-gray-400 space-x-4">
            <span className="flex items-center">
              <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              {job.location}
            </span>
            <span>•</span>
            <span>{job.jobType}</span>
            <span>•</span>
            <span>Posted {getTimeAgo(job.postedDate)}</span>
          </div>
        </div>

        {/* Notification */}
        {notification && (
          <div className={`mb-6 p-4 rounded-xl border ${
            notification.type === 'success' 
              ? 'bg-green-50 border-green-200 text-green-800'
              : 'bg-red-50 border-red-200 text-red-800'
          }`}>
            <div className="flex items-center">
              <svg className="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {notification.type === 'success' ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L12.732 4.5c-.77-.833-2.186-.833-2.954 0L2.857 16.5c-.77.833.192 2.5 1.732 2.5z" />
                )}
              </svg>
              {notification.message}
            </div>
          </div>
        )}

        {/* Filter Tabs */}
        <div className="mb-8">
          <div className="flex flex-wrap gap-2">
            {[
              { key: 'all', label: 'All Applications' },
              ...Object.entries(STATUS_LABELS).map(([key, label]) => ({ key, label }))
            ].map(({ key, label }) => (
              <button
                key={key}
                onClick={() => setFilter(key)}
                className={`px-4 py-2 rounded-xl font-medium transition-colors ${
                  filter === key
                    ? 'bg-primary-600 dark:bg-primary-700 text-white'
                    : 'bg-white/80 dark:bg-gray-800/80 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
                } border border-white/20 dark:border-gray-700/20`}
              >
                {label} ({statusCounts[key]})
              </button>
            ))}
          </div>
        </div>

        {applications.length === 0 ? (
          <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-lg rounded-3xl shadow-lg border border-white/20 dark:border-gray-700/20 p-12 text-center">
            <div className="text-6xl mb-6">📭</div>
            <h3 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-4">No Applications Yet</h3>
            <p className="text-gray-600 dark:text-gray-400 mb-8 max-w-md mx-auto">
              Your job posting is live! Applications will appear here once candidates start applying for this position.
            </p>
            <Link
              to={`/jobs/${job.id}`}
              className="inline-flex items-center px-6 py-3 bg-primary-600 dark:bg-primary-700 text-white rounded-xl font-semibold hover:bg-primary-700 dark:hover:bg-primary-600 transition-colors"
            >
              View Job Posting
              <svg className="w-4 h-4 ml-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </Link>
          </div>
        ) : filteredApplications.length === 0 ? (
          <div className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-lg rounded-3xl shadow-lg border border-white/20 dark:border-gray-700/20 p-12 text-center">
            <div className="text-6xl mb-6">🔍</div>
            <h3 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-4">No Applications Found</h3>
            <p className="text-gray-600 dark:text-gray-400">
              No applications match the selected filter.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-6">
            {filteredApplications
              .sort((a, b) => new Date(b.applicant.appliedAt) - new Date(a.applicant.appliedAt))
              .map((application) => (
              <div
                key={application.applicationId}
                className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-lg rounded-3xl shadow-lg border border-white/20 dark:border-gray-700/20 p-8 hover:shadow-xl transition-all duration-300"
              >
                <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between">
                  <div className="flex-1">
                    <div className="flex items-start justify-between mb-4">
                      <div className="flex items-start space-x-4">
                        <ApplicantAvatar
                          url={pictureUrls[application.applicationId]}
                          name={application.applicant.name}
                          className="w-12 h-12 shrink-0"
                          textClassName="text-lg"
                        />
                        <div>
                          <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100 mb-1">
                            {application.applicant.name}
                          </h3>
                          <p className="text-gray-600 dark:text-gray-400 mb-2">{application.applicant.title}</p>
                          <p className="text-sm text-gray-500 dark:text-gray-500 mb-3">{application.applicant.email}</p>
                          <div className="text-sm text-gray-500 dark:text-gray-500">
                            Applied {getTimeAgo(application.applicant.appliedAt)}
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center space-x-3 mb-4">
                      <span className={`px-3 py-1 rounded-full text-sm font-medium border ${getStatusColor(application.applicant.status)}`}>
                        {STATUS_LABELS[application.applicant.status]}
                      </span>
                    </div>
                  </div>

                  <div className="flex-1 lg:max-w-md lg:ml-8 mb-4">
                    {application.coverLetter && (
                      <details className="mb-4">
                        <summary className="cursor-pointer text-sm font-semibold text-gray-700 dark:text-gray-300">Cover letter</summary>
                        <p className="mt-2 text-sm text-gray-600 dark:text-gray-400 whitespace-pre-line">{application.coverLetter}</p>
                      </details>
                    )}
                    <label htmlFor={`notes-${application.applicationId}`} className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                      Internal notes <span className="font-normal text-gray-500 dark:text-gray-400">(only your company sees them)</span>
                    </label>
                    <textarea
                      id={`notes-${application.applicationId}`}
                      rows={3}
                      maxLength={5000}
                      value={notesDrafts[application.applicationId] ?? application.notes}
                      onChange={(e) => setNotesDrafts(prev => ({ ...prev, [application.applicationId]: e.target.value }))}
                      className="w-full px-3 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-gray-100 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none text-sm"
                    />
                    {notesDrafts[application.applicationId] !== undefined && (
                      <button
                        onClick={() => handleSaveNotes(application)}
                        className="mt-2 px-4 py-1.5 bg-primary-600 dark:bg-primary-700 text-white rounded-lg hover:bg-primary-700 dark:hover:bg-primary-600 transition-colors font-semibold text-sm"
                      >
                        Save notes
                      </button>
                    )}
                  </div>

                  <div className="lg:ml-8 lg:text-right">
                    <div className="mb-4">
                      <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                        Update Status
                      </label>
                      <select
                        value={application.applicant.status}
                        onChange={(e) => handleStatusChange(application.applicationId, e.target.value)}
                        className="px-4 py-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-gray-100 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all duration-300 outline-none"
                      >
                        {/* PENDING is only shown while nothing was decided; the employer can not set it back */}
                        {application.applicant.status === 'PENDING' && <option value="PENDING" disabled>Pending</option>}
                        {EMPLOYER_STATUSES.map(status => (
                          <option key={status} value={status}>{STATUS_LABELS[status]}</option>
                        ))}
                      </select>
                    </div>

                    <div className="flex flex-col gap-2">
                      <button
                        onClick={() => {
                          setSelectedApplicant(application.applicant);
                          setShowContactModal(true);
                        }}
                        className="px-6 py-2 bg-primary-600 dark:bg-primary-700 text-white rounded-xl hover:bg-primary-700 dark:hover:bg-primary-600 transition-colors font-semibold text-center"
                      >
                        Contact Applicant
                      </button>

                      <button
                        onClick={() => {
                          setSelectedApplicant(application.applicant);
                          setShowProfileModal(true);
                        }}
                        className="px-6 py-2 border border-primary-600 dark:border-primary-500 text-primary-600 dark:text-primary-400 rounded-xl hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-colors font-semibold"
                      >
                        View Profile
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Contact Modal */}
        {showContactModal && selectedApplicant && (
          <div
            className="fixed inset-0 bg-black bg-opacity-50 dark:bg-black dark:bg-opacity-70 flex items-center justify-center p-4"
            style={{ zIndex: 10000 }}
            onClick={() => setShowContactModal(false)}
          >
            <div
              className="bg-white dark:bg-gray-800 rounded-3xl shadow-2xl max-w-md w-full p-8"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="text-center mb-6">
                <ApplicantAvatar
                  url={pictureUrls[selectedApplicant.applicationId]}
                  name={selectedApplicant.name}
                  className="w-16 h-16 mx-auto mb-4"
                  textClassName="text-2xl"
                />
                <h3 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-2">{selectedApplicant.name}</h3>
                <p className="text-gray-600 dark:text-gray-400">{selectedApplicant.title}</p>
              </div>

              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700/50 rounded-xl">
                  <div>
                    <div className="text-sm font-semibold text-gray-700 dark:text-gray-300">Email</div>
                    <div className="text-gray-900 dark:text-gray-100">{selectedApplicant.email}</div>
                  </div>
                  <a
                    href={`mailto:${selectedApplicant.email}?subject=Regarding your application for ${job.title}`}
                    className="px-4 py-2 bg-primary-600 dark:bg-primary-700 text-white rounded-lg hover:bg-primary-700 dark:hover:bg-primary-600 transition-colors text-sm font-semibold"
                  >
                    Send Email
                  </a>
                </div>

                <div className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700/50 rounded-xl">
                  <div>
                    <div className="text-sm font-semibold text-gray-700 dark:text-gray-300">Phone</div>
                    <div className="text-gray-900 dark:text-gray-100">{selectedApplicant.phone}</div>
                  </div>
                  <a
                    href={`tel:${selectedApplicant.phone}`}
                    className="px-4 py-2 border border-primary-600 dark:border-primary-500 text-primary-600 dark:text-primary-400 rounded-lg hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-colors text-sm font-semibold"
                  >
                    Call Now
                  </a>
                </div>
              </div>

              <div className="mt-8 flex justify-center">
                <button
                  onClick={() => setShowContactModal(false)}
                  className="px-6 py-2 bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors font-semibold"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
      </div>

      {/* Profile Modal - Render outside main container */}
      {showProfileModal && selectedApplicant && (
        <div
          className="fixed inset-0 bg-black bg-opacity-75 dark:bg-black dark:bg-opacity-85 flex items-center justify-center p-4"
          style={{ zIndex: 10000 }}
          onClick={() => setShowProfileModal(false)}
        >
          <div
            className="bg-white dark:bg-gray-800 rounded-3xl shadow-2xl max-w-5xl w-full max-h-[90vh] overflow-hidden relative"
            onClick={(e) => e.stopPropagation()}
          >
              {/* Close Button */}
              <button
                onClick={() => setShowProfileModal(false)}
                className="absolute top-4 right-4 z-10 p-2 rounded-full bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
              >
                <svg className="w-6 h-6 text-gray-600 dark:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>

            <div className="p-8 overflow-y-auto max-h-[85vh]">
              <div className="text-center mb-8">
                <div className="w-24 h-24 rounded-full overflow-hidden border-4 border-white dark:border-gray-700 shadow-lg mx-auto mb-4">
                  <ApplicantAvatar
                    url={pictureUrls[selectedApplicant.applicationId]}
                    name={selectedApplicant.name}
                    className="w-full h-full"
                    textClassName="text-3xl"
                  />
                </div>
                <h3 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">{selectedApplicant.name}</h3>
                <p className="text-xl text-primary-600 dark:text-primary-400 mb-2">{selectedApplicant.title}</p>
                <p className="text-gray-600 dark:text-gray-400">{selectedApplicant.location || 'Location not provided'}</p>
              </div>

              <div className="space-y-6">
                {/* Bio */}
                <div>
                  <h4 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">About</h4>
                  <p className="text-gray-700 dark:text-gray-300">{selectedApplicant.bio}</p>
                </div>

                {/* Experience */}
                <div>
                  <h4 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">Experience</h4>
                  <p className="text-gray-700 dark:text-gray-300">{selectedApplicant.experience}</p>
                </div>

                {/* Resume */}
                <div>
                  <h4 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-3">Resume</h4>
                  <div className="border border-gray-200 dark:border-gray-700 rounded-xl p-4 bg-gray-50 dark:bg-gray-700/50 text-gray-700 dark:text-gray-300 flex items-center justify-between gap-4">
                    <span>{selectedApplicant.hasResume ? '📄 Resume (PDF)' : 'No resume uploaded'}</span>
                    {selectedApplicant.hasResume && (
                      <button
                        onClick={openResume}
                        disabled={openingResume}
                        className="px-4 py-2 bg-primary-600 dark:bg-primary-700 text-white rounded-lg hover:bg-primary-700 dark:hover:bg-primary-600 transition-colors font-semibold text-sm disabled:opacity-60"
                      >
                        {openingResume ? 'Opening...' : 'View Resume'}
                      </button>
                    )}
                  </div>
                </div>

                {/* Work Experience */}
                {selectedApplicant.workHistory && selectedApplicant.workHistory.length > 0 && (
                  <div>
                    <h4 className="text-lg font-semibold text-gray-900 mb-3">Work Experience</h4>
                    <div className="space-y-4">
                      {selectedApplicant.workHistory.map((work, index) => (
                        <div key={index} className="border-l-4 border-primary-200 pl-4 pb-4">
                          <div className="flex justify-between items-start mb-2">
                            <div>
                              <h5 className="font-semibold text-gray-900">{work.position}</h5>
                              <p className="text-primary-600 font-medium">{work.company}</p>
                            </div>
                            <span className="text-sm text-gray-500">
                              {work.startDate} - {work.current ? 'Present' : work.endDate}
                            </span>
                          </div>
                          {work.description && (
                            <p className="text-gray-700 text-sm">{work.description}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Education */}
                {selectedApplicant.education && selectedApplicant.education.length > 0 && (
                  <div>
                    <h4 className="text-lg font-semibold text-gray-900 mb-3">Education</h4>
                    <div className="space-y-4">
                      {selectedApplicant.education.map((edu, index) => (
                        <div key={index} className="border-l-4 border-green-200 pl-4 pb-4">
                          <div className="flex justify-between items-start mb-2">
                            <div>
                              <h5 className="font-semibold text-gray-900">{edu.degree}</h5>
                              <p className="text-green-600 font-medium">{edu.institution}</p>
                            </div>
                            <span className="text-sm text-gray-500">{edu.year}</span>
                          </div>
                          {edu.description && (
                            <p className="text-gray-700 text-sm">{edu.description}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Portfolio */}
                {selectedApplicant.portfolio && (
                  <div>
                    <h4 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2">Portfolio</h4>
                    <a
                      href={selectedApplicant.portfolio}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium inline-flex items-center"
                    >
                      {selectedApplicant.portfolio}
                      <svg className="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                      </svg>
                    </a>
                  </div>
                )}

                {/* Contact Info */}
                <div className="bg-gray-50 dark:bg-gray-700/50 rounded-xl p-4">
                  <h4 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-3">Contact Information</h4>
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-gray-600 dark:text-gray-400">Email:</span>
                      <span className="text-gray-900 dark:text-gray-100">{selectedApplicant.email}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600 dark:text-gray-400">Phone:</span>
                      <span className="text-gray-900 dark:text-gray-100">{selectedApplicant.phone}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="mt-8 flex justify-center gap-3">
                <a
                  href={`mailto:${selectedApplicant.email}?subject=Regarding your application for ${job.title}`}
                  className="px-6 py-2 bg-primary-600 dark:bg-primary-700 text-white rounded-xl hover:bg-primary-700 dark:hover:bg-primary-600 transition-colors font-semibold"
                >
                  Send Email
                </a>
                <button
                  onClick={() => setShowProfileModal(false)}
                  className="px-6 py-2 bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-xl hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors font-semibold"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
        )}
    </>
  );
};

export default JobApplicants;