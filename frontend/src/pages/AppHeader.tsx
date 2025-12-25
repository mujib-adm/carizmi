import { Image, Layout } from "antd";
import "../themes/css/header-footer.css";
const { Header } = Layout;
const Logo = "/themes/images/main-logo.png";

export default function AppHeader() {
  return (
    <Header className="portal-header">
      <Image src={Logo} alt="Logo" className="portal-logo" />
      <h2 className="portal-title"> Sof'umar Portal </h2>
    </Header>
  );
}