import { useEffect, useState } from "react";
import type { CurrentUser } from "../types/auth";

interface UserAvatarProps {
  user?: CurrentUser | null;
  size?: "sm" | "lg";
  className?: string;
}

function getDisplayName(user?: CurrentUser | null) {
  return user?.nickname || user?.username || "用户";
}

function UserAvatar({ user, size = "sm", className = "" }: UserAvatarProps) {
  const [imageFailed, setImageFailed] = useState(false);
  const displayName = getDisplayName(user);
  const initial = displayName.slice(0, 1).toUpperCase();

  useEffect(() => {
    setImageFailed(false);
  }, [user?.avatarUrl]);

  return (
    <span className={`cc-user-avatar cc-user-avatar--${size} ${className}`}>
      {user?.avatarUrl && !imageFailed ? (
        <img
          src={user.avatarUrl}
          alt={displayName}
          onError={() => setImageFailed(true)}
        />
      ) : (
        initial
      )}
    </span>
  );
}

export default UserAvatar;
