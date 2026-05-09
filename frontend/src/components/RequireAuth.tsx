import type { PropsWithChildren } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

function RequireAuth({ children }: PropsWithChildren) {
  const location = useLocation()
  const hasToken = Boolean(localStorage.getItem('token'))

  if (!hasToken) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return <>{children}</>
}

export default RequireAuth
