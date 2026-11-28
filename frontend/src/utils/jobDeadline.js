// The backend refuses applications after the deadline (409), so the UI hides the Apply button in that case.

export const isDeadlinePassed = (job) =>
  Boolean(job?.applicationDeadline) && new Date(job.applicationDeadline) <= new Date();

export const formatDeadline = (job) =>
  job?.applicationDeadline
    ? new Date(job.applicationDeadline).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
    : null;
