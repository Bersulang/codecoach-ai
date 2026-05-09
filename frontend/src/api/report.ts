import request from "./request";
import type { InterviewReport } from "../types/report";

export const getReportDetail = (reportId: number | string) =>
  request.get<InterviewReport>(`/api/reports/${reportId}`);
