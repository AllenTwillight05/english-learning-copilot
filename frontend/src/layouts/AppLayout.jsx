import { Layout } from "antd";
import { Outlet } from "react-router-dom";
import { AppHeader } from "../components/navigation/AppHeader";

const { Content } = Layout;

export function AppLayout() {
  return (
    <Layout className="app-shell">
      <AppHeader />
      <Content className="page-content">
        <Outlet />
      </Content>
    </Layout>
  );
}
