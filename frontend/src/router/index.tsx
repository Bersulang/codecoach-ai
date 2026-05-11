import { Navigate, createBrowserRouter } from "react-router-dom";
import RequireAuth from "../components/RequireAuth";
import PublicOnlyRoute from "../components/PublicOnlyRoute";
import MainLayout from "../layouts/MainLayout";
import DashboardPage from "../pages/Dashboard";
import HistoryPage from "../pages/History";
import InterviewPage from "../pages/Interview";
import LandingPage from "../pages/Landing";
import LoginPage from "../pages/Login";
import ProfilePage from "../pages/Profile";
import ProjectFormPage from "../pages/ProjectForm";
import ProjectsPage from "../pages/Projects";
import QuestionSessionPage from "../pages/QuestionSession";
import QuestionsPage from "../pages/Questions";
import RegisterPage from "../pages/Register";
import ReportPage from "../pages/Report";

const router = createBrowserRouter([
  {
    path: "/",
    children: [
      {
        index: true,
        element: <LandingPage />,
      },
      {
        element: (
          <PublicOnlyRoute>
            <LoginPage />
          </PublicOnlyRoute>
        ),
        path: "login",
      },
      {
        element: (
          <PublicOnlyRoute>
            <RegisterPage />
          </PublicOnlyRoute>
        ),
        path: "register",
      },
      {
        element: (
          <RequireAuth>
            <MainLayout />
          </RequireAuth>
        ),
        children: [
          {
            path: "dashboard",
            element: <DashboardPage />,
          },
          {
            path: "projects",
            element: <ProjectsPage />,
          },
          {
            path: "projects/new",
            element: <ProjectFormPage />,
          },
          {
            path: "projects/:id/edit",
            element: <ProjectFormPage />,
          },
          {
            path: "interviews/:sessionId",
            element: <InterviewPage />,
          },
          {
            path: "reports/:reportId",
            element: <ReportPage />,
          },
          {
            path: "history",
            element: <HistoryPage />,
          },
          {
            path: "questions",
            element: <QuestionsPage />,
          },
          {
            path: "question-sessions/:sessionId",
            element: <QuestionSessionPage />,
          },
          {
            path: "profile",
            element: <ProfilePage />,
          },
        ],
      },
      {
        path: "*",
        element: <Navigate to="/" replace />,
      },
    ],
  },
]);

export default router;
