import { createContext, useContext, useState, useEffect } from 'react';
import { useCompanies } from './CompaniesContext';

const JobsDataContext = createContext();

export const useJobsData = () => {
  const context = useContext(JobsDataContext);
  if (!context) {
    throw new Error('useJobsData must be used within a JobsDataProvider');
  }
  return context;
};

/**
 * Jobs are nested inside companies in the API response, so this context does not call the API itself:
 * it derives the jobs from {@link CompaniesProvider}, which owns fetching, caching and refreshing.
 */
export const JobsDataProvider = ({ children }) => {
  const {
    companies,
    loading,
    error: companiesError,
    refetch,
    forceRefresh: refreshCompanies,
    getCacheAge,
    isCacheStale,
    lastFetchTime
  } = useCompanies();
  // Kept as state (not derived on every render) so local updates like applicationsCount survive until the next refresh
  const [jobs, setJobs] = useState([]);

  useEffect(() => {
    const allJobs = companies.flatMap(company => company.jobs || []);
    if (companies.length > 0) {
      console.log(`[JobsDataContext] Derived ${allJobs.length} jobs from ${companies.length} companies`);
    }
    setJobs(allJobs);
  }, [companies]);

  const error = companiesError ? 'Failed to load jobs. Please try again later.' : null;

  const getJobById = (id) => {
    return jobs.find(job => String(job.id) === String(id)) || null;
  };

  const getJobsByCompany = (companyId) => {
    return jobs.filter(job => job.companyId === companyId);
  };

  const getJobsByCategory = (category) => {
    return jobs.filter(job => job.category === category);
  };

  const forceRefresh = async () => {
    console.log('[JobsDataContext] Force refresh requested');
    await refreshCompanies();
  };

  const updateJobApplicationsCount = (jobId, increment = true) => {
    console.log(`[JobsDataContext] ${increment ? 'Incrementing' : 'Decrementing'} applications count for job ${jobId}`);
    setJobs(prevJobs =>
      prevJobs.map(job =>
        String(job.id) === String(jobId)
          ? {
              ...job,
              applicationsCount: increment
                ? (job.applicationsCount || 0) + 1
                : Math.max((job.applicationsCount || 0) - 1, 0)
            }
          : job
      )
    );
  };

  const value = {
    jobs,
    loading,
    error,
    getJobById,
    getJobsByCompany,
    getJobsByCategory,
    refetch,
    forceRefresh,
    updateJobApplicationsCount,
    getCacheAge,
    isCacheStale,
    lastFetchTime,
    totalJobs: jobs.length
  };

  return (
    <JobsDataContext.Provider value={value}>
      {children}
    </JobsDataContext.Provider>
  );
};
