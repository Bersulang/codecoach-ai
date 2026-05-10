import { Empty } from "antd";
import type { ReactNode } from "react";

interface EmptyStateProps {
  description?: ReactNode;
  action?: ReactNode;
}

function EmptyState({ description = "暂无数据", action }: EmptyStateProps) {
  return (
    <div className="cc-empty-state">
      <Empty description={description}>{action}</Empty>
    </div>
  );
}

export default EmptyState;
