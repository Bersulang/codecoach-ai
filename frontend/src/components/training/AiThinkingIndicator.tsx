import "./AiThinkingIndicator.css";

interface AiThinkingIndicatorProps {
  stage?: string;
  content?: string;
}

function AiThinkingIndicator({ stage, content }: AiThinkingIndicatorProps) {
  return (
    <div className="ai-thinking">
      <div className="ai-thinking__dots" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>
      <div>
        <div className="ai-thinking__stage">{stage || "AI 正在思考..."}</div>
        {content ? <div className="ai-thinking__content">{content}</div> : null}
      </div>
    </div>
  );
}

export default AiThinkingIndicator;
