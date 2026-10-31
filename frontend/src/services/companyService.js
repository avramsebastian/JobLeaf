import httpClient, { withApiVersion } from "../config/httpClient";
import { API_ENDPOINTS } from "../config/api";

/**
 * The backend sends requirements/benefits as JSON arrays; older payloads sent them as JSON strings.
 */
const toStringList = (value, field, jobId) => {
  if (Array.isArray(value)) return value;
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    console.warn(`Failed to parse ${field} for job:`, jobId);
    return [];
  }
};

/**
 * Transform job data from backend to frontend structure.
 * Jobs are nested inside their company, so the company name and logo come from the parent company.
 */
const transformJob = (job, company) => {
  const requirements = toStringList(job.requirements, "requirements", job.id);
  const benefits = toStringList(job.benefits, "benefits", job.id);

  return {
    id: job.id,
    title: job.title,
    company: company.name,
    companyLogo: company.logo,
    companyId: job.companyId,
    location: job.location,
    workType: job.workType,
    jobType: job.jobType,
    category: job.category,
    experienceLevel: job.experienceLevel,
    salary: {
      min: parseFloat(job.salaryMin),
      max: parseFloat(job.salaryMax),
      currency: job.salaryCurrency,
      period: job.salaryPeriod,
    },
    description: job.description,
    requirements,
    skills: requirements,
    benefits,
    postedDate: job.postedDate,
    applicationDeadline: job.applicationDeadline,
    applicationsCount: job.applicationsCount || 0,
    featured: job.featured || false,
    urgent: job.urgent || false,
    remote: job.remote || false,
    status: job.status,
  };
};

/**
 * Fetch all companies from the backend
 * Uses default API version (1.0) via Accept header in httpClient interceptor
 * Accept: application/vnd.jobleaf+json;v=1.0
 */
export const fetchCompanies = async () => {
  try {
    const response = await httpClient.get(API_ENDPOINTS.COMPANIES);
    const data = response.data;

    // Transform the backend data to match frontend structure
    return data.map((company) => ({
      id: company.id,
      name: company.name,
      logo: company.logo,
      industry: company.industry,
      size: company.size,
      rating: company.rating,
      locations: company.locations ? company.locations.split(",") : [],
      founded: company.founded,
      description: company.description,
      employees: company.employees,
      website: company.website,
      jobs: company.jobs ? company.jobs.map((job) => transformJob(job, company)) : [],
    }));
  } catch (error) {
    console.error("Error fetching companies:", error);
    throw error;
  }
};

/**
 * Fetch a single company by ID
 * Uses default API version (1.0) via Accept header in httpClient interceptor
 * Accept: application/vnd.jobleaf+json;v=1.0
 */
export const fetchCompanyById = async (id) => {
  try {
    const response = await httpClient.get(API_ENDPOINTS.COMPANY_BY_ID(id));
    const company = response.data;

    // Transform the backend data to match frontend structure
    return {
      id: company.id,
      name: company.name,
      logo: company.logo,
      industry: company.industry,
      size: company.size,
      rating: company.rating,
      locations: company.locations ? company.locations.split(",") : [],
      founded: company.founded,
      description: company.description,
      employees: company.employees,
      website: company.website,
    };
  } catch (error) {
    console.error("Error fetching company:", error);
    throw error;
  }
};

/**
 * Fetch a company by name (for detail pages that use company name in URL)
 */
export const fetchCompanyByName = async (name) => {
  try {
    const companies = await fetchCompanies();
    const company = companies.find(
      (c) =>
        c.name.toLowerCase().replace(/[^a-z0-9]/g, "") ===
        name.toLowerCase().replace(/[^a-z0-9]/g, "")
    );
    return company || null;
  } catch (error) {
    console.error("Error fetching company by name:", error);
    throw error;
  }
};
