import type { ReactNode } from "react";

interface SectionHeaderProps {
  title: ReactNode;
  description?: ReactNode;
  actions?: ReactNode;
}

function SectionHeader({ title, description, actions }: SectionHeaderProps) {
  return (
    <div className="cc-section-header">
      <div>
        <h2 className="cc-section-title">{title}</h2>
        {description ? (
          <div className="cc-section-description">{description}</div>
        ) : null}
      </div>
      {actions ? <div className="cc-section-actions">{actions}</div> : null}
    </div>
  );
}

export default SectionHeader;
