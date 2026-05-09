import {
  Button,
  Card,
  Empty,
  Input,
  Popconfirm,
  Result,
  Space,
  Tag,
  Typography,
  message,
} from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  finishInterview,
  getInterviewSession,
  submitInterviewAnswer,
} from '../../api/interview'
import type {
  AnswerResponse,
  InterviewMessage,
  InterviewMessageType,
  InterviewSessionDetail,
} from '../../types/interview'
import '../../styles/interview.css'

const { TextArea } = Input

const MESSAGE_TYPE_LABELS: Record<InterviewMessageType, string> = {
  AI_QUESTION: 'AI 问题',
  USER_ANSWER: '我的回答',
  AI_FEEDBACK: 'AI 反馈',
  AI_FOLLOW_UP: 'AI 追问',
}

function normalizeMessages(messages: InterviewMessage[]) {
  return messages.map((message, index) => ({
    ...message,
    messageId: message.messageId ?? message.id ?? `msg-${index}`,
  }))
}

function formatStatus(status?: string) {
  if (!status) {
    return '未知'
  }
  if (status === 'FINISHED') {
    return '已结束'
  }
  if (status === 'RUNNING') {
    return '进行中'
  }
  return status
}

function getMessageClass(type: InterviewMessageType) {
  if (type === 'USER_ANSWER') {
    return 'interview-message interview-message--user'
  }
  if (type === 'AI_FEEDBACK') {
    return 'interview-message interview-message--feedback'
  }
  if (type === 'AI_FOLLOW_UP') {
    return 'interview-message interview-message--followup'
  }
  return 'interview-message interview-message--question'
}

function InterviewPage() {
  const navigate = useNavigate()
  const { sessionId } = useParams()
  const [detail, setDetail] = useState<InterviewSessionDetail | null>(null)
  const [messages, setMessages] = useState<InterviewMessage[]>([])
  const [loading, setLoading] = useState(false)
  const [sending, setSending] = useState(false)
  const [finishing, setFinishing] = useState(false)
  const [loadError, setLoadError] = useState(false)
  const [answer, setAnswer] = useState('')

  const isFinished = detail?.status === 'FINISHED'

  useEffect(() => {
    if (!sessionId) {
      return
    }

    let active = true
    setLoading(true)
    setLoadError(false)
    getInterviewSession(sessionId)
      .then((data) => {
        if (!active) {
          return
        }
        setDetail(data)
        setMessages(normalizeMessages(data.messages || []))
      })
      .catch(() => {
        if (!active) {
          return
        }
        setLoadError(true)
        message.error('训练会话加载失败')
      })
      .finally(() => {
        if (!active) {
          return
        }
        setLoading(false)
      })

    return () => {
      active = false
    }
  }, [sessionId])

  const handleAppend = (payload: AnswerResponse) => {
    setMessages((prev) => {
      const next = [...prev]
      const baseRound = detail?.currentRound ?? 0
      if (payload.userAnswer) {
        next.push({
          messageId: `user-${Date.now()}`,
          messageType: 'USER_ANSWER',
          content: payload.userAnswer,
          roundNo: baseRound,
        })
      }
      if (payload.aiFeedback) {
        next.push({
          messageId: `feedback-${Date.now()}`,
          messageType: 'AI_FEEDBACK',
          content: payload.aiFeedback,
          roundNo: baseRound,
        })
      }
      if (payload.nextQuestion) {
        next.push({
          messageId: `question-${Date.now()}`,
          messageType: 'AI_FOLLOW_UP',
          content: payload.nextQuestion,
          roundNo: baseRound + 1,
        })
      }
      return next
    })

    setDetail((prev) => {
      if (!prev) {
        return prev
      }
      if (payload.finished) {
        return { ...prev, status: 'FINISHED' }
      }
      if (payload.nextQuestion) {
        return {
          ...prev,
          currentRound: Math.min(prev.currentRound + 1, prev.maxRound),
        }
      }
      return prev
    })
  }

  const handleSubmitAnswer = async () => {
    const trimmed = answer.trim()
    if (!trimmed || !sessionId) {
      return
    }
    setSending(true)
    try {
      const data = await submitInterviewAnswer(sessionId, { answer: trimmed })
      handleAppend(data)
      setAnswer('')
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setSending(false)
    }
  }

  const handleFinish = async () => {
    if (!sessionId) {
      return
    }
    setFinishing(true)
    try {
      const data = await finishInterview(sessionId)
      if (typeof data?.reportId === 'number') {
        navigate(`/reports/${data.reportId}`)
      } else {
        message.error('结束训练失败，请稍后重试')
      }
    } catch {
      // Errors are handled by the request interceptor.
    } finally {
      setFinishing(false)
    }
  }

  const metaItems = useMemo(
    () =>
      detail
        ? [
            { label: '项目名称', value: detail.projectName },
            { label: '目标岗位', value: detail.targetRole },
            { label: '难度', value: detail.difficulty },
            {
              label: '进度',
              value: `${detail.currentRound}/${detail.maxRound}`,
            },
            { label: '状态', value: formatStatus(detail.status) },
          ]
        : [],
    [detail],
  )

  if (!sessionId || (Number.isNaN(Number(sessionId)) && !sessionId)) {
    return (
      <div className="interview-page">
        <Result
          status="error"
          title="训练会话不存在"
          subTitle="请从项目列表重新进入训练。"
          extra={
            <Button type="primary" onClick={() => navigate('/projects')}>
              返回项目列表
            </Button>
          }
        />
      </div>
    )
  }

  if (loadError) {
    return (
      <div className="interview-page">
        <Result
          status="error"
          title="训练会话加载失败"
          subTitle="训练会话不存在或无权限访问。"
          extra={
            <Button type="primary" onClick={() => navigate('/projects')}>
              返回项目列表
            </Button>
          }
        />
      </div>
    )
  }

  const trimmedAnswer = answer.trim()

  return (
    <div className="interview-page">
      <div className="interview-header">
        <div>
          <Typography.Title level={3} className="interview-title">
            项目拷打训练
          </Typography.Title>
          <Typography.Text className="interview-subtitle">
            回答问题，获取针对性的追问与反馈
          </Typography.Text>
        </div>
        <Space>
          <Tag color={isFinished ? 'default' : 'blue'}>
            {formatStatus(detail?.status)}
          </Tag>
          <Popconfirm
            title="确认结束训练吗？"
            okText="结束"
            cancelText="取消"
            onConfirm={handleFinish}
            disabled={finishing}
          >
            <Button danger loading={finishing} disabled={!detail || isFinished}>
              结束训练
            </Button>
          </Popconfirm>
        </Space>
      </div>

      <Card className="interview-meta-card" loading={loading}>
        <div className="interview-meta">
          {metaItems.map((item) => (
            <div key={item.label}>
              <span className="interview-meta-label">{item.label}</span>
              <span className="interview-meta-value">{item.value}</span>
            </div>
          ))}
        </div>
      </Card>

      <div className="interview-messages">
        {loading ? (
          <Card className="interview-message" loading />
        ) : messages.length === 0 ? (
          <div className="interview-empty">
            <Empty description="暂无训练消息" />
          </div>
        ) : (
          messages.map((messageItem) => (
            <div
              key={String(messageItem.messageId)}
              className={getMessageClass(messageItem.messageType)}
            >
              <div className="interview-message__header">
                <span className="interview-message__type">
                  {MESSAGE_TYPE_LABELS[messageItem.messageType]}
                </span>
                {messageItem.roundNo ? (
                  <span className="interview-message__type">
                    第 {messageItem.roundNo} 轮
                  </span>
                ) : null}
              </div>
              <div className="interview-message__content">
                {messageItem.content}
              </div>
            </div>
          ))
        )}
      </div>

      <Card className="interview-answer-card" bordered={false}>
        <Typography.Text className="interview-status">
          {isFinished
            ? '训练已结束，你可以查看报告或返回项目列表。'
            : '请输入你的回答，AI 将生成反馈与下一道问题。'}
        </Typography.Text>
        <TextArea
          value={answer}
          onChange={(event) => setAnswer(event.target.value)}
          rows={4}
          placeholder="请填写你的回答"
          disabled={isFinished || loading || sending}
          style={{ marginTop: 12 }}
        />
        <div className="interview-answer-actions">
          <Space>
            <Button
              type="primary"
              onClick={handleSubmitAnswer}
              loading={sending}
              disabled={
                sending || loading || isFinished || trimmedAnswer.length === 0
              }
            >
              提交回答
            </Button>
            <Button onClick={() => setAnswer('')} disabled={!answer || sending}>
              清空
            </Button>
          </Space>
          {detail ? (
            <span className="interview-status">
              当前进度 {detail.currentRound}/{detail.maxRound}
            </span>
          ) : null}
        </div>
      </Card>
    </div>
  )
}

export default InterviewPage
