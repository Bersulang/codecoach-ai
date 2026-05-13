import { useEffect, type DependencyList, type RefObject } from "react";

export function useChatAutoScroll(
  endRef: RefObject<HTMLElement | null>,
  dependencies: DependencyList,
) {
  useEffect(() => {
    window.requestAnimationFrame(() => {
      endRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "end",
      });
    });
  }, dependencies);
}
