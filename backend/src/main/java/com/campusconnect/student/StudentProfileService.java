package com.campusconnect.student;

import com.campusconnect.auth.AuthUser;
import com.campusconnect.common.ApiException;
import java.io.IOException;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static com.campusconnect.student.StudentProfileDtos.*;

@Service
public class StudentProfileService {

    private static final long MAX_RESUME_SIZE = 5 * 1024 * 1024;

    private final StudentProfileRepository profiles;

    public StudentProfileService(StudentProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getMyProfile(AuthUser me) {
        return profiles.findByUserId(me.id())
                .map(StudentProfileResponse::from)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Student profile not found. Save your profile first."));
    }

    @Transactional
    public StudentProfileResponse updateMyProfile(
            AuthUser me, UpdateStudentProfileRequest request) {

        StudentProfile profile = profiles.findByUserId(me.id())
                .orElseGet(() -> new StudentProfile(me.id()));

        String rollNumber = normalize(request.rollNumber());

        if (rollNumber != null
                && profiles.existsByRollNumberIgnoreCase(rollNumber)
                && !rollNumber.equalsIgnoreCase(profile.getRollNumber())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "That roll number is already in use.");
        }

        profile.updateDetails(
                rollNumber,
                normalize(request.phone()),
                request.dateOfBirth(),
                normalize(request.degree()),
                normalize(request.branch()),
                request.graduationYear(),
                request.cgpa(),
                request.activeBacklogs(),
                normalize(request.resumeUrl()));

        try {
            return StudentProfileResponse.from(
                    profiles.saveAndFlush(profile));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "The profile conflicts with an existing record.");
        }
    }

    @Transactional
    public void uploadResume(AuthUser me, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "Please select a PDF resume.");
        }

        if (file.getSize() > MAX_RESUME_SIZE) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "Resume must be 5 MB or smaller.");
        }

        String contentType = file.getContentType();
        if (!"application/pdf".equalsIgnoreCase(contentType)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "Only PDF resumes are allowed.");
        }

        try {
            byte[] data = file.getBytes();

            // Validate that the uploaded file is actually a readable PDF.
            try (PDDocument document = Loader.loadPDF(data)) {
                if (document.isEncrypted()) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "Password-protected PDFs are not supported.");
                }
            }

            StudentProfile profile = profiles.findByUserId(me.id())
                    .orElseGet(() -> new StudentProfile(me.id()));

            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                fileName = "resume.pdf";
            }

            profile.storeResume(fileName, "application/pdf", data);
            profiles.save(profile);

        } catch (IOException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "The uploaded PDF could not be read.");
        }
    }

    @Transactional(readOnly = true)
    public ResumeMetadata getResumeMetadata(AuthUser me) {
        StudentProfile profile = profiles.findByUserId(me.id()).orElse(null);
        return profile == null || !profile.hasResume()
                ? new ResumeMetadata(false, null, null)
                : new ResumeMetadata(true, profile.getResumeFileName(), profile.getResumeUploadedAt());
    }

    @Transactional(readOnly = true)
    public ResumeFile getMyResume(AuthUser me) {
        StudentProfile profile = profiles.findByUserId(me.id())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Resume not found."));

        if (!profile.hasResume()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "Resume not found. Upload a PDF first.");
        }

        return new ResumeFile(
                profile.getResumeFileName(),
                profile.getResumeData());
    }

    @Transactional
    public void deleteMyResume(AuthUser me) {
        StudentProfile profile = profiles.findByUserId(me.id())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Student profile not found."));

        profile.removeResume();
        profiles.save(profile);
    }

    @Transactional(readOnly = true)
    public ResumeAnalysis analyzeResume(AuthUser me, String jobDescription) {
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please provide a job description.");
        }
        if (jobDescription.length() > 20000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Job description must be 20,000 characters or fewer.");
        }

        StudentProfile profile = profiles.findByUserId(me.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Student profile not found."));
        if (!profile.hasResume()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload your PDF resume before analyzing it.");
        }

        try (PDDocument document = Loader.loadPDF(profile.getResumeData())) {
            String resumeText = new PDFTextStripper().getText(document);
            if (resumeText.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "No readable text was found in the PDF. Please upload a text-based PDF.");
            }

            String resume = normalizeSkillText(resumeText);
            String jd = normalizeSkillText(jobDescription);

            // Skills are scored as concepts/phrases, not arbitrary words. This prevents
            // common words (e.g. "India", "year", "academic") inflating or deflating scores.
            var skillCatalog = java.util.List.of(
                "Java", "Python", "C", "C++", "C#", "JavaScript", "TypeScript", "SQL",
                "HTML", "CSS", "React", "Angular", "Node.js", "Spring Boot", "REST API",
                "Microservices", "Git", "GitHub", "Linux", "Docker", "Kubernetes",
                "Data Structures and Algorithms", "Object-Oriented Programming",
                "DBMS", "Operating Systems", "Computer Networks", "Unit Testing",
                "Debugging", "Problem Solving", "Communication", "Teamwork",
                "Machine Learning", "Deep Learning", "Artificial Intelligence",
                "Natural Language Processing", "Computer Vision", "TensorFlow",
                "PyTorch", "Scikit-learn", "Pandas", "NumPy", "Data Analysis",
                "Statistics", "Power BI", "Tableau", "Excel", "Cloud Computing",
                "AWS", "Azure", "GCP", "CI/CD", "Agile", "Embedded Systems",
                "Arduino", "Raspberry Pi", "MATLAB", "Verilog", "VLSI",
                "Circuit Design", "Mechanical Design", "AutoCAD", "SolidWorks",
                "Thermodynamics", "Manufacturing", "Civil Engineering",
                "Structural Design", "Project Management", "Business Analysis",
                "Financial Analysis", "Marketing", "Sales", "Supply Chain"
            );

            var jdSkills = skillCatalog.stream()
                    .filter(skill -> containsSkill(jd, skill))
                    .toList();
            if (jdSkills.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "The job description does not contain recognizable skills. Add responsibilities or required skills and try again.");
            }

            var matched = jdSkills.stream()
                    .filter(skill -> containsSkill(resume, skill))
                    .toList();
            var missing = jdSkills.stream()
                    .filter(skill -> !containsSkill(resume, skill))
                    .toList();

            // Required/essential terms receive more weight than preferred/nice-to-have terms.
            double totalWeight = 0.0;
            double earnedWeight = 0.0;
            for (String skill : jdSkills) {
                boolean preferred = isPreferredSkill(jobDescription, skill);
                double weight = preferred ? 0.65 : 1.0;
                totalWeight += weight;
                if (containsSkill(resume, skill)) earnedWeight += weight;
            }
            int score = totalWeight == 0 ? 0 : (int)Math.round(100.0 * earnedWeight / totalWeight);

            String branch = profile.getBranch() == null ? "not specified" : profile.getBranch().trim();
            String disclaimer = "Estimated skill overlap: " + matched.size() + " of " + jdSkills.size()
                    + " recognized JD skills. Student branch: " + branch
                    + ". Branch is shown as context and does not automatically penalize unrelated skills. "
                    + "This heuristic is not a real ATS score or hiring prediction.";

            return new ResumeAnalysis(score, matched, missing, disclaimer);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read the stored PDF resume.");
        }
    }

    private boolean containsSkill(String normalizedText, String skill) {
        String normalizedSkill = normalizeSkillText(skill);
        // Word-boundary-like matching prevents C matching inside unrelated words.
        String regex = "(^|[^a-z0-9+#])" + java.util.regex.Pattern.quote(normalizedSkill)
                + "($|[^a-z0-9+#])";
        return java.util.regex.Pattern.compile(regex).matcher(normalizedText).find();
    }

    private boolean isPreferredSkill(String jd, String skill) {
        int index = jd.indexOf(normalizeSkillText(skill));
        if (index < 0) return false;
        String before = jd.substring(Math.max(0, index - 100), index);
        return before.matches("(?s).*(preferred|preferably|nice to have|bonus|plus|optional|desirable)\\s*[:,-]?\\s*$");
    }

    private String normalizeSkillText(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT)
                .replace("node.js", "nodejs")
                .replace("node js", "nodejs")
                .replace("springboot", "spring boot")
                .replace("restful", "rest")
                .replaceAll("[^a-z0-9+#.]", " ")
                .replaceAll("\\\\s+", " ")
                .trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ResumeFile(String fileName, byte[] data) {
    }

    public record ResumeMetadata(boolean hasResume, String fileName, java.time.Instant uploadedAt) {
    }

    public record ResumeAnalysis(
            int score,
            java.util.List<String> matchedKeywords,
            java.util.List<String> missingKeywords,
            String disclaimer) {
    }
}
