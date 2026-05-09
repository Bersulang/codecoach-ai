import type { PropsWithChildren } from "react";
import { Navigate } from "react-router-dom";

function PublicOnlyRoute({ children }: PropsWithChildren) {
  const hasToken = Boolean(localStorage.getItem("token"));

  if (hasToken) {
    return <Navigate to="/projects" replace />;
  }

  return <>{children}</>;
}

export default PublicOnlyRoute;
