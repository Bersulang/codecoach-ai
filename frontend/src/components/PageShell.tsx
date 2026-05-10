import type { CSSProperties, PropsWithChildren } from "react";

interface PageShellProps extends PropsWithChildren {
  className?: string;
  style?: CSSProperties;
  maxWidth?: number | string;
}

function PageShell({
  children,
  className,
  style,
  maxWidth = "var(--cc-container-max)",
}: PageShellProps) {
  return (
    <div
      className={`cc-page-shell${className ? ` ${className}` : ""}`}
      style={{ maxWidth, ...style }}
    >
      {children}
    </div>
  );
}

export default PageShell;
