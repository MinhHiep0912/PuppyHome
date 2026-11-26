import type { ReactNode } from "react";
import "./ErrorBadge.css";

interface ErrorBadgeProps {
  message: string;
  children?: ReactNode;
}

export function ErrorBadge({ message, children }: ErrorBadgeProps) {
  return (
    <div className="error-badge" role="alert">
      <span>{message}</span>
      {children}
    </div>
  );
}
