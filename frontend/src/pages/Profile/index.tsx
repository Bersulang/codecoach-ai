import { Button, Popconfirm, message } from "antd";
import type { ChangeEvent } from "react";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { deleteCurrentUser, getCurrentUser, uploadAvatar } from "../../api/auth";
import type { CurrentUser } from "../../types/auth";
import "../Workspace/workspace.css";

const MAX_AVATAR_SIZE = 2 * 1024 * 1024;
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"];

function readStoredUser(): CurrentUser | null {
  const stored = localStorage.getItem("user");
  if (!stored) {
    return null;
  }
  try {
    return JSON.parse(stored) as CurrentUser;
  } catch {
    localStorage.removeItem("user");
    return null;
  }
}

function formatDateTime(value?: string) {
  if (!value) {
    return "暂无";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function persistUser(user: CurrentUser) {
  localStorage.setItem("user", JSON.stringify(user));
  window.dispatchEvent(new Event("codecoach:user-updated"));
}

function ProfilePage() {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [user, setUser] = useState<CurrentUser | null>(() => readStoredUser());
  const [uploading, setUploading] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const displayName = user?.nickname || user?.username || "CodeCoach User";

  useEffect(() => {
    let active = true;
    getCurrentUser()
      .then((data) => {
        if (!active) {
          return;
        }
        setUser(data);
        persistUser(data);
      })
      .catch(() => {
        message.error("用户信息加载失败");
      });
    return () => {
      active = false;
    };
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    window.dispatchEvent(new Event("codecoach:user-updated"));
    navigate("/", { replace: true });
  };

  const handleDeleteAccount = async () => {
    setDeleting(true);
    try {
      await deleteCurrentUser();
      message.success("账号已注销");
      handleLogout();
    } catch {
      message.error("账号注销失败，请稍后重试");
    } finally {
      setDeleting(false);
    }
  };

  const handleSelectFile = () => {
    inputRef.current?.click();
  };

  const handleAvatarChange = async (
    event: ChangeEvent<HTMLInputElement>,
  ) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) {
      return;
    }
    if (!ALLOWED_TYPES.includes(file.type)) {
      message.error("仅支持 JPG、PNG、WEBP 图片");
      return;
    }
    if (file.size > MAX_AVATAR_SIZE) {
      message.error("头像图片不能超过 2MB");
      return;
    }

    try {
      setUploading(true);
      const result = await uploadAvatar(file);
      const nextUser = {
        ...(user || {}),
        avatarUrl: result.avatarUrl,
      };
      setUser(nextUser);
      persistUser(nextUser);
      message.success("头像已更新");
    } catch {
      message.error("头像上传失败，请稍后重试");
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="workspace-page profile-page">
      <section className="profile-hero-card">
        <div className="profile-hero-card__body">
          <p className="workspace-kicker">个人中心</p>
          <h1>{displayName}</h1>
          <p>管理你的账号资料、头像和训练偏好。</p>
          <span className="profile-status-pill">
            {user?.loginStatus || "已登录"}
          </span>
        </div>
      </section>

      <section className="profile-grid">
        <div className="profile-panel profile-avatar-panel">
          <div>
            <span className="workspace-kicker">头像</span>
            <h2>更新右上角头像</h2>
            <p>上传后会同步显示在导航栏头像。支持 JPG、PNG、WEBP，文件大小不超过 2MB。</p>
          </div>
          <div className="profile-avatar-panel__actions">
            <input
              ref={inputRef}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              hidden
              onChange={handleAvatarChange}
            />
            <Button
              type="primary"
              loading={uploading}
              onClick={handleSelectFile}
            >
              上传头像
            </Button>
          </div>
        </div>

        <div className="profile-panel">
          <span className="workspace-kicker">账号信息</span>
          <dl className="profile-info-list">
            <div>
              <dt>用户名</dt>
              <dd>{user?.username || "暂无"}</dd>
            </div>
            <div>
              <dt>昵称</dt>
              <dd>{user?.nickname || user?.username || "暂无"}</dd>
            </div>
            <div>
              <dt>账号状态</dt>
              <dd>{user?.loginStatus || "已登录"}</dd>
            </div>
            <div>
              <dt>注册时间</dt>
              <dd>{formatDateTime(user?.createdAt)}</dd>
            </div>
          </dl>
        </div>

        <div className="profile-panel">
          <span className="workspace-kicker">训练偏好</span>
          <div className="profile-preference-list">
            <div>
              <strong>目标岗位</strong>
              <span>Java 后端实习</span>
            </div>
            <div>
              <strong>默认难度</strong>
              <span>NORMAL · 常规面试</span>
            </div>
            <p>偏好设置后续会支持保存，现在先用于展示默认训练方向。</p>
          </div>
        </div>
      </section>

      <section className="profile-danger-panel">
        <div>
          <span className="workspace-kicker">账号操作</span>
          <h2>注销账号</h2>
          <p>注销后账号将不可继续使用，当前登录状态会立即清除。</p>
        </div>
        <Popconfirm
          title="确认注销账号吗？"
          description="该操作会停用并注销当前账号。"
          okText="确认注销"
          cancelText="取消"
          okButtonProps={{ danger: true, loading: deleting }}
          onConfirm={handleDeleteAccount}
        >
          <Button danger loading={deleting}>
            注销账号
          </Button>
        </Popconfirm>
      </section>
    </div>
  );
}

export default ProfilePage;
