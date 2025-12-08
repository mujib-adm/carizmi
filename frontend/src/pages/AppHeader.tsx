import "../themes/css/header-footer.css";
import Logo from "../themes/images/main-logo.png";
import {Image, Layout} from "antd";
const {Header, Content, Footer} = Layout;

export default function AppHeader() {
  return (
    <Header className="portal-header">
      <Imag src={Logo} alt="Logo" className="portal-logo" />
      <h2 className="portal-title"> Sof'umar Portal </h2>
    </Header>
  );
}