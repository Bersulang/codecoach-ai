import { Navigate, createBrowserRouter } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import RequireAuth from "../components/RequireAuth";
import PublicOnlyRoute from "../components/PublicOnlyRoute";
import LoginPage from "../pages/Login";
import RegisterPage from "../pages/Register";
import ProjectsPage from "../pages/Projects";
import ProjectFormPage from "../pages/ProjectForm";
import InterviewPage from "../pages/Interview";
import ReportPage from "../pages/Report";
import HistoryPage from "../pages/History";

function RootRedirect() {
  const hasToken = Boolean(localStorage.getItem("token"));
  return <Navigate to={hasToken ? "/projects" : "/login"} replace />;
}

const router = createBrowserRouter([
  {
    path: "/",
    children: [
      {
        index: true,
        element: <RootRedirect />,
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
