// Same limit as the backend (JobApplication: cover letter max 5000 characters)
export const COVER_LETTER_MAX = 5000;

const CoverLetterField = ({ value, onChange }) => (
  <div>
    <label htmlFor="coverLetter" className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
      Cover letter <span className="font-normal text-gray-500 dark:text-gray-400">(optional)</span>
    </label>
    <textarea
      id="coverLetter"
      rows={5}
      maxLength={COVER_LETTER_MAX}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder="Tell the employer why you are a good fit..."
      className="w-full px-4 py-3 border-2 border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:border-primary-500 resize-y"
    />
    <p className="mt-1 text-xs text-right text-gray-500 dark:text-gray-400">
      {value.length}/{COVER_LETTER_MAX}
    </p>
  </div>
);

export default CoverLetterField;
