import type { ReactNode } from "react";

interface PageHeaderProps {
  title: ReactNode;
  description?: ReactNode;
  actions?: ReactNode;
}

function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <div className="cc-page-header">
      <div>
        <h1 className="cc-page-title">{title}</h1>
        {description ? (
          <div className="cc-page-description">{description}</div>
        ) : null}
      </div>
      {actions ? <div className="cc-page-actions">{actions}</div> : null}
    </div>
  );
}

export default PageHeader;
