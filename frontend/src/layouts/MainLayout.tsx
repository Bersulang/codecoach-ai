import { Layout } from 'antd'
import { Outlet } from 'react-router-dom'
import './MainLayout.css'

const { Header, Content } = Layout

function MainLayout() {
  return (
    <Layout className="main-layout">
      <Header className="main-layout__header">
        <div className="main-layout__brand">CodeCoach AI</div>
      </Header>
      <Content className="main-layout__content">
        <Outlet />
      </Content>
    </Layout>
  )
}

export default MainLayout
