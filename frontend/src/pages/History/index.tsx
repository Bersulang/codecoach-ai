import {
  Button,
  Card,
  Empty,
  Pagination,
  Select,
  Space,
  Tag,
  Typography,
  message,
} from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getInterviewSessions } from '../../api/interview'
import type {
  InterviewSessionHistoryItem,
  InterviewStatus,
} from '../../types/interview'
import '../../styles/history.css'

const DEFAULT_PAGE_SIZE = 10

const STATUS_OPTIONS: Array<{ label: string; value: InterviewStatus }> = [
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '已结束', value: 'FINISHED' },
  { label: '失败', value: 'FAILED' },
]

function formatDate(value?: string | null) {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleDateString()
}

function statusColor(status: InterviewStatus) {
  if (status === 'FINISHED') {
    return 'default'
  }
  if (status === 'FAILED') {
    return 'red'
  }
  return 'blue'
}

function statusLabel(status: InterviewStatus) {
  if (status === 'FINISHED') {
    return '已结束'
  }
  if (status === 'FAILED') {
    return '失败'
  }
  return '进行中'
}

function HistoryPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<InterviewSessionHistoryItem[]>([])
  const [loading, setLoading] = useState(false)
  const [status, setStatus] = useState<InterviewStatus | undefined>(undefined)
  const [pageNum, setPageNum] = useState(1)
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE)
  const [total, setTotal] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    getInterviewSessions({
      pageNum,
      pageSize,
      status,
    })
      .then((data) => {
        if (!active) {
          return
        }
        setItems(data.records || [])
        setTotal(data.total || 0)
      })
      .catch(() => {
        if (!active) {
          return
        }
        message.error('训练历史加载失败')
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
  }, [pageNum, pageSize, status])

  const emptyState = useMemo(
    () => (
      <div className="history-empty">
        <Empty description="暂无训练历史" />
      </div>
    ),
    [],
  )

  const showPagination = total > pageSize

  const handlePageChange = (nextPage: number, nextSize: number) => {
    setPageNum(nextPage)
    setPageSize(nextSize)
  }

  const handleStatusChange = (value: InterviewStatus | undefined) => {
    setStatus(value)
    setPageNum(1)
  }

  return (
    <div className="history-page">
      <div className="history-header">
        <div>
          <Typography.Title level={3} className="history-title">
            训练历史
          </Typography.Title>
          <Typography.Text className="history-subtitle">
            查看已完成或进行中的训练记录
          </Typography.Text>
        </div>
        <div className="history-actions">
          <Select
            placeholder="筛选状态"
            allowClear
            style={{ width: 160 }}
            value={status}
            onChange={handleStatusChange}
            options={STATUS_OPTIONS}
          />
        </div>
      </div>

      <div className="history-list">
        {loading ? (
          <Card className="history-card" loading />
        ) : items.length === 0 ? (
          emptyState
        ) : (
          items.map((item) => (
            <Card key={item.id} className="history-card">
              <div className="history-card__header">
                <div>
                  <Typography.Title level={4} className="history-card__title">
                    {item.projectName}
                  </Typography.Title>
                  <Space size={8} wrap>
                    <Tag color={statusColor(item.status)}>
                      {statusLabel(item.status)}
                    </Tag>
                    <Typography.Text type="secondary">
                      {item.targetRole}
                    </Typography.Text>
                  </Space>
                </div>
                <Space className="history-actions-group" wrap>
                  {item.status === 'FINISHED' && item.reportId ? (
                    <Button
                      type="primary"
                      onClick={() => navigate(`/reports/${item.reportId}`)}
                    >
                      查看报告
                    </Button>
                  ) : null}
                  {item.status === 'IN_PROGRESS' ? (
                    <Button onClick={() => navigate(`/interviews/${item.id}`)}>
                      继续训练
                    </Button>
                  ) : null}
                </Space>
              </div>
              <div className="history-card__meta">
                <div>
                  <span className="history-card__label">难度</span>
                  <span className="history-card__value">{item.difficulty}</span>
                </div>
                <div>
                  <span className="history-card__label">进度</span>
                  <span className="history-card__value">
                    {item.currentRound}/{item.maxRound}
                  </span>
                </div>
                <div>
                  <span className="history-card__label">总评分</span>
                  <span className="history-card__value">
                    {item.totalScore ?? '—'}
                  </span>
                </div>
                <div>
                  <span className="history-card__label">开始时间</span>
                  <span className="history-card__value">
                    {formatDate(item.createdAt)}
                  </span>
                </div>
                <div>
                  <span className="history-card__label">结束时间</span>
                  <span className="history-card__value">
                    {formatDate(item.endedAt)}
                  </span>
                </div>
              </div>
            </Card>
          ))
        )}
      </div>

      {showPagination && (
        <div className="history-footer">
          <Pagination
            current={pageNum}
            pageSize={pageSize}
            total={total}
            showSizeChanger
            onChange={handlePageChange}
          />
        </div>
      )}
    </div>
  )
}

export default HistoryPage
