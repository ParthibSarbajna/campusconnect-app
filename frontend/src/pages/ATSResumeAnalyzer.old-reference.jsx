import { useState } from 'react';
import { jsPDF } from 'jspdf';
import { downloadStudentResume } from '../api/studentProfile.js';
import * as pdfjsLib from 'pdfjs-dist';

pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.min.mjs',
  import.meta.url,
).toString();

const DOMAINS = [
  'Auto Detect',
  'CSE',
  'CSE(AI & ML)',
  'CSIT',
  'Information Technology',
  'Data Science',
  'Cybersecurity',
  'ECE',
  'EEE',
  'Electrical Engineering',
  'Mechanical Engineering',
  'Civil Engineering',
  'Chemical Engineering',
  'Automobile Engineering',
  'Robotics & Automation',
  'Biotechnology',
  'Aerospace Engineering',
  'MBA',
  'BBA',
  'Finance',
  'Marketing',
  'Human Resources',
  'Business Analytics',
  'Operations Management',
  'Other Engineering',
  'Other',
];

const KEYWORD_GROUPS = {
  'Programming Languages': [
    { name: 'Java', terms: ['java'] },
    { name: 'Python', terms: ['python'] },
    { name: 'C', terms: ['c programming', 'programming in c', 'language c'] },
    { name: 'C++', terms: ['c++', 'cpp'] },
    { name: 'JavaScript', terms: ['javascript'] },
    { name: 'TypeScript', terms: ['typescript'] },
    { name: 'SQL', terms: ['sql'] },
  ],

  'Frameworks & Software': [
    { name: 'React', terms: ['react', 'react.js', 'reactjs'] },
    { name: 'Spring Boot', terms: ['spring boot'] },
    { name: 'Node.js', terms: ['node.js', 'nodejs'] },
    { name: 'Django', terms: ['django'] },
    { name: 'Flask', terms: ['flask'] },
    { name: 'REST APIs', terms: ['rest api', 'restful api', 'rest apis'] },
    { name: 'Software Testing', terms: ['software testing', 'unit testing'] },
    { name: 'Agile', terms: ['agile', 'scrum'] },
  ],

  'Core CS Concepts': [
    { name: 'Data Structures', terms: ['data structures'] },
    { name: 'Algorithms', terms: ['algorithms'] },
    {
      name: 'Object-Oriented Programming',
      terms: ['object-oriented programming', 'object oriented programming', 'oop'],
    },
    { name: 'DBMS', terms: ['dbms', 'database management system'] },
    { name: 'Operating Systems', terms: ['operating systems', 'operating system'] },
    { name: 'Computer Networks', terms: ['computer networks'] },
    {
      name: 'Problem Solving',
      terms: ['problem-solving', 'problem solving', 'problem solving skills'],
    },
    { name: 'Software Development', terms: ['software development'] },
  ],

  'Tools & Platforms': [
    { name: 'Git', terms: ['git'] },
    { name: 'GitHub', terms: ['github'] },
    { name: 'Docker', terms: ['docker'] },
    { name: 'AWS', terms: ['aws', 'amazon web services'] },
    { name: 'Azure', terms: ['azure'] },
    { name: 'Linux', terms: ['linux'] },
    { name: 'PostgreSQL', terms: ['postgresql', 'postgres'] },
    { name: 'MySQL', terms: ['mysql'] },
  ],

  'AI & Data': [
    { name: 'Machine Learning', terms: ['machine learning', 'ml'] },
    { name: 'Deep Learning', terms: ['deep learning'] },
    { name: 'Artificial Intelligence', terms: ['artificial intelligence', 'ai'] },
    {
      name: 'Natural Language Processing',
      terms: ['natural language processing', 'nlp'],
    },
    { name: 'Computer Vision', terms: ['computer vision'] },
    { name: 'Data Analysis', terms: ['data analysis', 'data analytics'] },
    { name: 'Generative AI', terms: ['generative ai'] },
    {
      name: 'Large Language Models',
      terms: ['large language models', 'large language model', 'llm', 'llms'],
    },
    { name: 'Statistics', terms: ['statistics', 'statistical analysis'] },
  ],

  'Electronics & Electrical': [
    { name: 'VLSI', terms: ['vlsi'] },
    { name: 'Verilog', terms: ['verilog'] },
    { name: 'VHDL', terms: ['vhdl'] },
    { name: 'Digital Electronics', terms: ['digital electronics'] },
    { name: 'Analog Electronics', terms: ['analog electronics'] },
    { name: 'PCB Design', terms: ['pcb design'] },
    { name: 'Circuit Design', terms: ['circuit design'] },
    { name: 'MATLAB', terms: ['matlab'] },
    { name: 'Embedded Systems', terms: ['embedded systems', 'embedded system'] },
    { name: 'Microcontrollers', terms: ['microcontrollers', 'microcontroller'] },
    { name: 'Arduino', terms: ['arduino'] },
    { name: 'Raspberry Pi', terms: ['raspberry pi'] },
    { name: 'Sensor Integration', terms: ['sensor integration'] },
    { name: 'ECG', terms: ['ecg'] },
  ],

  'Mechanical Engineering': [
    { name: 'AutoCAD', terms: ['autocad'] },
    { name: 'SolidWorks', terms: ['solidworks'] },
    { name: 'CATIA', terms: ['catia'] },
    { name: 'ANSYS', terms: ['ansys'] },
    { name: 'CAD', terms: ['cad'] },
    { name: 'CAM', terms: ['cam'] },
    {
      name: 'Finite Element Analysis',
      terms: ['finite element analysis', 'fea'],
    },
    { name: 'Thermodynamics', terms: ['thermodynamics'] },
    { name: 'Manufacturing', terms: ['manufacturing'] },
    { name: 'Mechanical Design', terms: ['mechanical design'] },
    { name: 'CNC', terms: ['cnc'] },
  ],

  'Civil Engineering': [
    { name: 'Civil 3D', terms: ['civil 3d'] },
    { name: 'STAAD.Pro', terms: ['staad pro', 'staad.pro'] },
    { name: 'Revit', terms: ['revit'] },
    { name: 'Structural Analysis', terms: ['structural analysis'] },
    { name: 'Construction Management', terms: ['construction management'] },
    { name: 'Surveying', terms: ['surveying'] },
    { name: 'BIM', terms: ['building information modeling', 'bim'] },
  ],

  'Business & Management': [
    { name: 'Business Administration', terms: ['business administration'] },
    { name: 'Business Strategy', terms: ['business strategy'] },
    { name: 'Business Development', terms: ['business development'] },
    { name: 'Project Management', terms: ['project management'] },
    { name: 'Operations Management', terms: ['operations management'] },
    { name: 'Supply Chain Management', terms: ['supply chain management'] },
    { name: 'Market Research', terms: ['market research'] },
    { name: 'Leadership', terms: ['leadership'] },
    { name: 'Negotiation', terms: ['negotiation'] },
  ],

  'Finance & Accounting': [
    { name: 'Financial Analysis', terms: ['financial analysis'] },
    { name: 'Financial Modeling', terms: ['financial modeling', 'financial modelling'] },
    { name: 'Accounting', terms: ['accounting'] },
    { name: 'Excel', terms: ['excel', 'microsoft excel'] },
    { name: 'Financial Reporting', terms: ['financial reporting'] },
    { name: 'Budgeting', terms: ['budgeting'] },
    { name: 'Risk Management', terms: ['risk management'] },
    { name: 'Investment Analysis', terms: ['investment analysis'] },
  ],

  'Marketing & Sales': [
    { name: 'Digital Marketing', terms: ['digital marketing'] },
    { name: 'SEO', terms: ['seo', 'search engine optimization'] },
    { name: 'Social Media Marketing', terms: ['social media marketing'] },
    { name: 'Content Marketing', terms: ['content marketing'] },
    { name: 'Sales', terms: ['sales'] },
    { name: 'CRM', terms: ['crm', 'customer relationship management'] },
    { name: 'Brand Management', terms: ['brand management'] },
    { name: 'Customer Acquisition', terms: ['customer acquisition'] },
  ],

  'Human Resources': [
    { name: 'Recruitment', terms: ['recruitment'] },
    { name: 'Talent Acquisition', terms: ['talent acquisition'] },
    { name: 'HR Management', terms: ['human resources', 'hr management'] },
    { name: 'Employee Engagement', terms: ['employee engagement'] },
    { name: 'Performance Management', terms: ['performance management'] },
    { name: 'Payroll', terms: ['payroll'] },
    { name: 'Training & Development', terms: ['training and development'] },
  ],

  'Biotechnology & Chemical': [
    { name: 'Biotechnology', terms: ['biotechnology'] },
    { name: 'Molecular Biology', terms: ['molecular biology'] },
    { name: 'Genetic Engineering', terms: ['genetic engineering'] },
    { name: 'Bioinformatics', terms: ['bioinformatics'] },
    { name: 'Chemical Process', terms: ['chemical process'] },
    { name: 'Process Engineering', terms: ['process engineering'] },
    { name: 'Laboratory Techniques', terms: ['laboratory techniques'] },
  ],

  'General Professional Skills': [
    { name: 'Teamwork', terms: ['teamwork', 'team work'] },
    { name: 'Analytical Skills', terms: ['analytical skills', 'analytical thinking'] },
    { name: 'Communication Skills', terms: ['communication skills'] },
    { name: 'Time Management', terms: ['time management'] },
    { name: 'Critical Thinking', terms: ['critical thinking'] },
    { name: 'Research', terms: ['research'] },
  ],
};

const DOMAIN_HINTS = {
  CSE: [
    'software engineer',
    'software developer',
    'programming',
    'java',
    'software development',
  ],

  'CSE(AI & ML)': [
    'machine learning',
    'artificial intelligence',
    'deep learning',
    'nlp',
    'ai engineer',
  ],

  CSIT: ['information technology', 'it support', 'systems administrator'],
  'Information Technology': ['information technology', 'it support', 'network administration'],
  'Data Science': ['data scientist', 'data science', 'data analytics', 'statistics'],
  Cybersecurity: ['cybersecurity', 'cyber security', 'penetration testing', 'security analyst'],
  ECE: ['electronics', 'vlsi', 'verilog', 'embedded', 'communication systems'],
  EEE: ['electrical', 'power systems', 'electrical machines'],
  'Electrical Engineering': ['electrical', 'power systems', 'electrical machines'],
  'Mechanical Engineering': ['mechanical', 'solidworks', 'autocad', 'thermodynamics', 'manufacturing'],
  'Civil Engineering': ['civil engineer', 'structural engineering', 'construction', 'staad'],
  'Chemical Engineering': ['chemical engineering', 'chemical process', 'process engineering'],
  'Automobile Engineering': ['automobile', 'automotive', 'vehicle design'],
  'Robotics & Automation': ['robotics', 'automation', 'robot', 'control systems'],
  Biotechnology: ['biotechnology', 'bioinformatics', 'molecular biology'],
  'Aerospace Engineering': ['aerospace', 'aeronautical', 'flight mechanics'],
  MBA: ['business administration', 'management trainee', 'business strategy'],
  BBA: ['business administration', 'business development', 'management trainee'],
  Finance: ['finance', 'financial analysis', 'accounting', 'investment'],
  Marketing: ['marketing', 'digital marketing', 'brand management', 'sales'],
  'Human Resources': ['human resources', 'recruitment', 'talent acquisition'],
  'Business Analytics': ['business analytics', 'business intelligence', 'data analytics'],
  'Operations Management': ['operations management', 'supply chain', 'logistics'],
};

// Synonyms/abbreviations used only for matching; display labels remain consistent.
const KEYWORD_SYNONYMS = {
  'Object-Oriented Programming': ['OOP', 'OOPs', 'object oriented', 'object-oriented'],
  'Data Structures': ['DS', 'data structure'],
  Algorithms: ['algo', 'algorithm'],
  DBMS: ['database management', 'database management systems'],
  'Operating Systems': ['OS', 'operating system'],
  'Computer Networks': ['CN', 'computer network', 'networking'],
  'REST APIs': ['REST', 'RESTful', 'REST API', 'RESTful APIs', 'web APIs'],
  'Spring Boot': ['Spring framework', 'Spring'],
  'Node.js': ['Node', 'NodeJS'],
  JavaScript: ['JS', 'ECMAScript'],
  TypeScript: ['TS'],
  'Machine Learning': ['ML', 'machine-learning'],
  'Artificial Intelligence': ['AI'],
  'Natural Language Processing': ['NLP'],
  'Deep Learning': ['DL'],
  'Computer Vision': ['CV'],
  'Large Language Models': ['LLM', 'LLMs', 'large-language model'],
  'Generative AI': ['GenAI', 'generative artificial intelligence'],
  PostgreSQL: ['Postgres', 'Postgre'],
  GitHub: ['Git hub'],
  AWS: ['Amazon Web Services'],
  Azure: ['Microsoft Azure'],
  'Software Testing': ['QA testing', 'quality assurance', 'unit tests', 'testing'],
  Agile: ['Scrum', 'Agile methodology', 'Agile methodologies'],
  'Problem Solving': ['problem-solving', 'problem solving skills'],
  'Communication Skills': ['communication', 'verbal communication', 'written communication'],
  'Teamwork': ['team work', 'collaboration', 'collaborative'],
  'Analytical Skills': ['analytical thinking', 'analysis skills'],
  'Project Management': ['project coordination'],
  'Business Administration': ['business management'],
  'Financial Analysis': ['finance analysis'],
  'Digital Marketing': ['online marketing'],
  'Human Resources': ['HR', 'human resource'],
};

function normalize(text) {
  return String(text ?? '')
    .toLowerCase()
    .replace(/&/g, ' and ')
    .replace(/[’']/g, '')
    .replace(/[^a-z0-9+#.]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

// Match whole normalized phrases, not substrings (e.g. "C" must not match "Java").
function containsKeyword(text, keyword) {
  const normalizedText = normalize(text);
  const normalizedKeyword = normalize(keyword);

  if (!normalizedText || !normalizedKeyword) return false;

  const escaped = normalizedKeyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`(^|\\s)${escaped}(?=\\s|$)`, 'i').test(normalizedText);
}

function containsAnyKeyword(text, terms = []) {
  return terms.some((term) => containsKeyword(text, term));
}

function detectDomain(jobDescription) {
  const text = normalize(jobDescription);
  let bestDomain = 'Other';
  let bestScore = 0;

  Object.entries(DOMAIN_HINTS).forEach(([domain, hints]) => {
    const score = hints.reduce(
      (total, hint) => total + (containsKeyword(text, hint) ? 1 : 0),
      0,
    );

    if (score > bestScore) {
      bestScore = score;
      bestDomain = domain;
    }
  });

  return bestScore > 0 ? bestDomain : 'Other';
}

function extractKeywords(jobDescription) {
  const groups = [];

  Object.entries(KEYWORD_GROUPS).forEach(([category, entries]) => {
    const matchingEntries = entries.filter((entry) =>
      containsAnyKeyword(jobDescription, [
        ...entry.terms,
        ...(KEYWORD_SYNONYMS[entry.name] || []),
      ]),
    );

    if (matchingEntries.length) {
      const termsByKeyword = {};
      matchingEntries.forEach((entry) => {
        // Add common spelling/abbreviation variants for resume-side matching.
        termsByKeyword[entry.name] = [...new Set([
          entry.name,
          ...entry.terms,
          ...(KEYWORD_SYNONYMS[entry.name] || []),
        ])];
      });

      groups.push({
        category,
        keywords: matchingEntries.map((entry) => entry.name),
        termsByKeyword,
      });
    }
  });

  return groups;
}

async function extractPdfText(file) {
  const buffer = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data: buffer }).promise;

  let text = '';

  for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber += 1) {
    const page = await pdf.getPage(pageNumber);
    const content = await page.getTextContent();

    text += `${content.items.map((item) => item.str).join(' ')} `;
  }

  return text.trim();
}

function downloadReport(result, domain, filename) {
  const doc = new jsPDF();
  const pageWidth = doc.internal.pageSize.getWidth();
  const pageHeight = doc.internal.pageSize.getHeight();
  const margin = 13;
  const contentWidth = pageWidth - margin * 2;
  let y = 16;

  const colors = {
    navy: [24, 35, 65],
    blue: [59, 103, 210],
    green: [22, 150, 100],
    red: [210, 75, 75],
    gray: [100, 110, 125],
    light: [243, 246, 250],
    dark: [35, 42, 55],
    white: [255, 255, 255],
  };

  function checkPage(heightNeeded = 12) {
    if (y + heightNeeded > pageHeight - 17) {
      doc.addPage();
      y = 16;
    }
  }

  function addWrappedText(text, fontSize = 10, color = colors.dark, options = {}) {
    const { bold = false, indent = 0, spacing = 2 } = options;
    doc.setFont('helvetica', bold ? 'bold' : 'normal');
    doc.setFontSize(fontSize);
    doc.setTextColor(...color);

    const lines = doc.splitTextToSize(String(text), contentWidth - indent);
    const lineHeight = fontSize * 0.42 + 1.5;
    checkPage(lines.length * lineHeight + spacing);
    doc.text(lines, margin + indent, y);
    y += lines.length * lineHeight + spacing;
  }

  function addSectionTitle(title, color = colors.navy) {
    checkPage(14);
    doc.setFillColor(...colors.light);
    doc.roundedRect(margin, y - 4, contentWidth, 9, 2, 2, 'F');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.setTextColor(...color);
    doc.text(title, margin + 3, y + 1.5);
    y += 11;
  }

  function addKeywordGroups(groups, color) {
    if (!groups.length) {
      addWrappedText('None identified.', 10, colors.gray);
      return;
    }

    groups.forEach((group) => {
      addWrappedText(group.category, 8, colors.dark, { bold: true, spacing: 1 });
      addWrappedText(group.keywords.join('  |  '), 9, color, { indent: 2, spacing: 2 });
    });
  }

  // Header
  doc.setFillColor(...colors.navy);
  doc.rect(0, 0, pageWidth, 38, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(19);
  doc.setTextColor(...colors.white);
  doc.text('CampusConnect', margin, 17);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  doc.setTextColor(220, 230, 245);
  doc.text('ATS Resume Analysis Report', margin, 26);
  doc.setFontSize(7);
  doc.text('Resume insights for your next opportunity', margin, 32);
  y = 45;

  // Report details
  addWrappedText(`Job Domain: ${domain}`, 10, colors.dark, { bold: true, spacing: 1 });
  addWrappedText(`Resume: ${filename}`, 9, colors.gray, { spacing: 1 });
  addWrappedText(`Generated: ${new Date().toLocaleString()}`, 9, colors.gray, { spacing: 4 });

  // Score card
  checkPage(36);
  doc.setFillColor(...colors.light);
  doc.roundedRect(margin, y, contentWidth, 27, 3, 3, 'F');
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(10);
  doc.setTextColor(...colors.navy);
  doc.text('ESTIMATED KEYWORD MATCH', margin + 5, y + 8);

  const scoreColor = result.score >= 70 ? colors.green : result.score >= 40 ? colors.blue : colors.red;
  doc.setFontSize(22);
  doc.setTextColor(...scoreColor);
  doc.text(`${result.score}%`, margin + 5, y + 19);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  doc.setTextColor(...colors.gray);
  doc.text(`${result.matched} of ${result.total} identified keywords matched`, margin + 32, y + 18);
  y += 32;

  addWrappedText(
    'This score estimates keyword coverage only. It does not guarantee ranking or selection by any specific Applicant Tracking System (ATS).',
    8,
    colors.gray,
    { spacing: 4 },
  );

  addSectionTitle('MATCHED KEYWORDS', colors.green);
  addKeywordGroups(result.matchedGroups, colors.green);
  y += 1;

  addSectionTitle('MISSING KEYWORDS', colors.red);
  addKeywordGroups(result.missingGroups, colors.red);
  y += 1;

  addSectionTitle('SUGGESTIONS TO IMPROVE');
  result.suggestions.forEach((suggestion, index) => {
    addWrappedText(`${index + 1}. ${suggestion}`, 8, colors.dark, { indent: 2, spacing: 2 });
  });
  y += 3;

  addWrappedText(
    'Important: Add missing keywords only when they genuinely reflect your skills, coursework, projects, or experience. Do not misrepresent your qualifications.',
    8,
    colors.gray,
    { spacing: 3 },
  );

  // Footer on every page
  const pageCount = doc.getNumberOfPages();
  for (let page = 1; page <= pageCount; page++) {
    doc.setPage(page);
    doc.setDrawColor(220, 225, 235);
    doc.line(margin, pageHeight - 13, pageWidth - margin, pageHeight - 13);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.setTextColor(...colors.gray);
    doc.text('CampusConnect | ATS Resume Analyzer', margin, pageHeight - 8);
    doc.text(`Page ${page} of ${pageCount}`, pageWidth - margin, pageHeight - 8, { align: 'right' });
  }

  doc.save('CampusConnect-ATS-Report.pdf');
}

export default function ATSResumeAnalyzer() {
  const [resumeFile, setResumeFile] = useState(null);
  const [resumeText, setResumeText] = useState('');
  const [jobDescription, setJobDescription] = useState('');
  const [domain, setDomain] = useState('Auto Detect');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function handleResumeChange(event) {
    const file = event.target.files?.[0];
    if (!file) return;

    setError('');
    setNotice('');
    setResult(null);

    if (file.type !== 'application/pdf') {
      setError('Please upload a PDF resume.');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setError('Resume must be smaller than 5 MB.');
      return;
    }

    setLoading(true);

    try {
      const text = await extractPdfText(file);

      if (!text) {
        throw new Error('No readable text found. This PDF may be scanned.');
      }

      setResumeFile(file);
      setResumeText(text);
      setNotice(`Resume loaded: ${file.name}`);
    } catch (e) {
      setError(e.message || 'Could not read this PDF.');
    } finally {
      setLoading(false);
    }
  }

  async function loadSavedResume() {
    setError('');
    setNotice('');
    setResult(null);
    setLoading(true);

    try {
      const blob = await downloadStudentResume();

      const file = new File([blob], 'CampusConnect-resume.pdf', {
        type: 'application/pdf',
      });

      const text = await extractPdfText(file);

      if (!text) {
        throw new Error('No readable text found in your saved resume.');
      }

      setResumeFile(file);
      setResumeText(text);
      setNotice('Your saved CampusConnect resume has been loaded.');
    } catch (e) {
      setError(e.message || 'Could not load your saved resume.');
    } finally {
      setLoading(false);
    }
  }

  function analyzeResume() {
    setError('');
    setResult(null);

    if (!resumeText) {
      setError('Please upload or load your resume first.');
      return;
    }

    if (jobDescription.trim().length < 50) {
      setError('Please paste a complete job description (at least 50 characters).');
      return;
    }

    const groups = extractKeywords(jobDescription);

    if (!groups.length) {
      setError('No recognized keywords found in the job description.');
      return;
    }

    const detectedDomain = detectDomain(jobDescription);
    const selectedDomain = domain === 'Auto Detect' ? detectedDomain : domain;

    const matchedGroups = [];
    const missingGroups = [];

    groups.forEach((group) => {
      const matched = group.keywords.filter((keyword) =>
        containsAnyKeyword(resumeText, group.termsByKeyword[keyword] || [keyword]),
      );

      const missing = group.keywords.filter(
        (keyword) => !containsAnyKeyword(resumeText, group.termsByKeyword[keyword] || [keyword]),
      );

      if (matched.length) {
        matchedGroups.push({
          category: group.category,
          keywords: matched,
        });
      }

      if (missing.length) {
        missingGroups.push({
          category: group.category,
          keywords: missing,
        });
      }
    });

    const allKeywords = groups.flatMap((group) => group.keywords);
    const matchedKeywords = matchedGroups.flatMap((group) => group.keywords);

    const score = allKeywords.length
      ? Math.round((matchedKeywords.length / allKeywords.length) * 100)
      : 0;

    const suggestions = [];

    if (missingGroups.length) {
      suggestions.push(
        'Review missing keywords and include them only if they genuinely reflect your skills, coursework, or experience.',
      );
    }

    if (!/experience|internship/i.test(resumeText)) {
      suggestions.push('Consider adding an Experience or Internship section if applicable.');
    }

    if (!/project/i.test(resumeText)) {
      suggestions.push('Add relevant academic or personal projects with technologies and outcomes.');
    }

    if (!/github|linkedin/i.test(resumeText)) {
      suggestions.push('Consider including your GitHub and LinkedIn profile links.');
    }

    suggestions.push(
      'Customize your resume summary and project descriptions to emphasize relevant achievements for this role.',
    );

    const analysis = {
      score,
      total: allKeywords.length,
      matched: matchedKeywords.length,
      matchedGroups,
      missingGroups,
      suggestions,
      domain: selectedDomain,
    };

    setResult(analysis);

    if (domain === 'Auto Detect') {
      setNotice(`Detected job domain: ${selectedDomain}`);
    }
  }

  return (
    <main className="mx-auto grid max-w-4xl gap-5 px-4 py-8">
      <section>
        <h1 className="text-3xl font-extrabold">ATS Resume Analyzer</h1>
        <p className="mt-2 text-sm text-mute">
          Analyze resumes for engineering, IT, management, business, and other career domains.
        </p>
      </section>

      <section className="card grid gap-4">
        <h2 className="text-xl font-bold">1. Select your domain</h2>

        <select
          className="w-full rounded-lg border border-line bg-surf p-3"
          value={domain}
          onChange={(event) => {
            setDomain(event.target.value);
            setResult(null);
          }}
        >
          {DOMAINS.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>

        <p className="text-xs text-mute">
          Auto Detect estimates the domain from the job description.
          The score is based on keywords found in the job description.
        </p>
      </section>

      <section className="card grid gap-4">
        <h2 className="text-xl font-bold">2. Upload your resume</h2>

        <div className="flex flex-wrap gap-3">
          <label className="btn cursor-pointer">
            Choose PDF
            <input
              type="file"
              accept="application/pdf,.pdf"
              className="sr-only"
              onChange={handleResumeChange}
            />
          </label>

          <button
            type="button"
            className="btn-ghost"
            onClick={loadSavedResume}
            disabled={loading}
          >
            Use my saved CampusConnect resume
          </button>
        </div>

        {resumeFile && (
          <p className="text-sm text-mute">
            Selected: {resumeFile.name}
          </p>
        )}

        <p className="text-xs text-mute">
          PDF only · Maximum 5 MB · Text extraction happens in your browser.
          Scanned PDFs may require OCR.
        </p>
      </section>

      <section className="card grid gap-3">
        <h2 className="text-xl font-bold">3. Paste the job description</h2>

        <textarea
          className="min-h-48 w-full rounded-lg border border-line bg-surf p-3"
          value={jobDescription}
          onChange={(event) => {
            setJobDescription(event.target.value);
            setResult(null);
          }}
          placeholder="Paste the complete job description here..."
        />

        <button
          type="button"
          className="btn"
          onClick={analyzeResume}
          disabled={loading}
        >
          {loading ? 'Reading resume…' : 'Analyze Resume'}
        </button>
      </section>

      {notice && (
        <p role="status" className="rounded-lg bg-okt p-3 text-sm text-ok">
          {notice}
        </p>
      )}

      {error && (
        <p role="alert" className="rounded-lg bg-badt p-3 text-sm text-bad">
          {error}
        </p>
      )}

      {result && (
        <section className="grid gap-4">
          <article className="card">
            <h2 className="text-xl font-bold">ATS Keyword Match Score</h2>

            <p className="mt-2 text-sm text-mute">
              Analyzed domain: <strong>{result.domain}</strong>
            </p>

            <div className="mt-3 flex items-end gap-2">
              <span className="text-5xl font-extrabold text-pri">
                {result.score}%
              </span>

              <span className="mb-1 text-sm text-mute">
                {result.matched} of {result.total} keywords matched
              </span>
            </div>

            <div className="mt-4 h-3 overflow-hidden rounded-full bg-line">
              <div
                className="h-full rounded-full bg-pri"
                style={{ width: `${result.score}%` }}
              />
            </div>

            <p className="mt-3 text-xs text-mute">
              This is an estimated keyword coverage score, not a guarantee
              of ranking in any specific ATS.
            </p>

            <button
              type="button"
              className="btn mt-4"
              onClick={() =>
                downloadReport(result, result.domain, resumeFile?.name || 'Resume')
              }
            >
              Download ATS Analysis Report (PDF)
            </button>
          </article>

          <article className="card">
            <h2 className="text-xl font-bold text-ok">Matched Keywords</h2>

            {result.matchedGroups.length ? (
              result.matchedGroups.map((group) => (
                <div key={group.category} className="mt-3">
                  <h3 className="font-bold">{group.category}</h3>

                  <div className="mt-1 flex flex-wrap gap-2">
                    {group.keywords.map((keyword) => (
                      <span key={keyword} className="pill bg-okt text-ok">
                        {keyword}
                      </span>
                    ))}
                  </div>
                </div>
              ))
            ) : (
              <p className="mt-2 text-sm text-mute">No keywords matched.</p>
            )}
          </article>

          <article className="card">
            <h2 className="text-xl font-bold text-bad">Missing Keywords</h2>

            {result.missingGroups.length ? (
              result.missingGroups.map((group) => (
                <div key={group.category} className="mt-3">
                  <h3 className="font-bold">{group.category}</h3>

                  <div className="mt-1 flex flex-wrap gap-2">
                    {group.keywords.map((keyword) => (
                      <span key={keyword} className="pill bg-badt text-bad">
                        {keyword}
                      </span>
                    ))}
                  </div>
                </div>
              ))
            ) : (
              <p className="mt-2 text-sm text-mute">
                All identified keywords matched!
              </p>
            )}
          </article>

          <article className="card">
            <h2 className="text-xl font-bold">Suggestions to Improve</h2>

            <ul className="mt-2 list-disc space-y-2 pl-5 text-sm">
              {result.suggestions.map((suggestion) => (
                <li key={suggestion}>{suggestion}</li>
              ))}
            </ul>
          </article>
        </section>
      )}
    </main>
  );
}