import { Empty } from "antd";
import type { ReactNode } from "react";

interface EmptyStateProps {
  description?: ReactNode;
  action?: ReactNode;
}

function EmptyState({ description = "No data", action }: EmptyStateProps) {
  return (
    <div className="cc-empty-state">
      <Empty description={description}>{action}</Empty>
    </div>
  );
}

export default EmptyState;
