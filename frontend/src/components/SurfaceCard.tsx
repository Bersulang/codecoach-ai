import { Card } from "antd";
import type { CardProps } from "antd";

function SurfaceCard({ className, bordered = false, ...rest }: CardProps) {
  return (
    <Card
      {...rest}
      bordered={bordered}
      className={`cc-surface-card${className ? ` ${className}` : ""}`}
    />
  );
}

export default SurfaceCard;
