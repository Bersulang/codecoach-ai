export type ResumeAnalysisStatus =
  | "PENDING"
  | "ANALYZING"
  | "ANALYZED"
  | "FAILED";

export interface ResumeSkill {
  name: string;
  category?: string;
  riskLevel?: "LOW" | "MEDIUM" | "HIGH" | string;
  reason?: string;
}

export interface ResumeRiskPoint {
  type: string;
  level?: "LOW" | "MEDIUM" | "HIGH" | string;
  evidence?: string;
  suggestion?: string;
}

export interface ResumeAnalysisProject {
  projectName: string;
  description?: string;
  techStack?: string[];
  role?: string;
  highlights?: string[];
  riskPoints?: string[];
  recommendedQuestions?: string[];
  possibleProjectName?: string;
}

export interface ResumeAnalysisResult {
  summary?: string;
  skills?: ResumeSkill[];
  projectExperiences?: ResumeAnalysisProject[];
  riskPoints?: ResumeRiskPoint[];
  interviewQuestions?: string[];
  optimizationSuggestions?: string[];
}

export interface ResumeProjectExperience {
  id: number;
  projectId?: number | null;
  projectName: string;
  description?: string;
  techStack?: string[];
  role?: string;
  highlights?: string[];
  riskPoints?: string[];
  recommendedQuestions?: string[];
  matchReason?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ResumeListItem {
  id: number;
  documentId: number;
  documentTitle?: string;
  title: string;
  targetRole: string;
  analysisStatus: ResumeAnalysisStatus;
  summary?: string | null;
  errorMessage?: string | null;
  analyzedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ResumeProfile extends ResumeListItem {
  analysisResult?: ResumeAnalysisResult | null;
  projectExperiences?: ResumeProjectExperience[];
}

export interface CreateResumeRequest {
  documentId: number;
  title?: string;
  targetRole?: string;
}

export interface ResumeProjectDraft {
  resumeId: number;
  resumeProjectId: number;
  name: string;
  description: string;
  techStack: string;
  role?: string;
  highlights?: string;
  difficulties?: string;
  riskPoints?: string[];
  pendingItems?: string[];
  safetyNotice?: string;
}

export interface ResumeProjectSaveRequest {
  name: string;
  description: string;
  techStack: string;
  role?: string;
  highlights?: string;
  difficulties?: string;
}

export interface ResumeProjectSaveResponse {
  projectId: number;
  resume: ResumeProfile;
}
