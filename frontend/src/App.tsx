import { Image, Layout } from "antd";
import { BrowserRouter } from "react-router-dom";
import Router from "./pages/Router";
import { AuthProvider } from "./context/AuthContext";
import Logo from "./images/logo.png";
import { NotificationProvider } from "./context/NotificationContext";

const { Header, Content, Footer } = Layout;
const year = new Date().getFullYear();

export default function App() {
  return (
    <AuthProvider>
      <Layout className="app-wrapper">
        <BrowserRouter>
          <NotificationProvider>
            <Header className="header">
              <Image src={Logo} alt="Logo" preview={false} height="68px" width="68px" className="header-logo" />
              <h2 className="header-title"> Sof'umar Portal </h2>
            </Header>

            <Layout className="main-layout">
              <Content className="app-main">
                <Router />
              </Content>
              <Footer className="copyright">
                <p>© {year} Sof'umar Community of Minnesota.</p>
              </Footer>
            </Layout>
          </NotificationProvider>
        </BrowserRouter>
      </Layout>
    </AuthProvider>
  );
}


// import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
// import Dashboard from "./pages/Dashboard";
// import Members from "./pages/Members";
// // import Payments from "./pages/Payments";
// // import Expenses from "./pages/Expenses";
// // import Reporting from "./pages/Reporting";
// // import Settings from "./pages/Settings";
// // import Users from "./pages/Users";
// import Login from "./pages/Login";
// import { useAuth } from "./state/auth";
//
// export default function App() {
//   const { token, role } = useAuth();
//   return (
//     <BrowserRouter>
//       {token ? (
//         <>
//           <Nav role={role} />
//           <Routes>
//             <Route path="/" element={<Dashboard />} />
//             <Route path="/members" element={<Members />} />
// //             <Route path="/payments" element={<Payments />} />
// //             <Route path="/expenses" element={<Expenses />} />
// //             <Route path="/reporting" element={<Reporting />} />
// //             {role === "Admin" && <Route path="/settings" element={<Settings />} />}
// //             {role === "Admin" && <Route path="/users" element={<Users />} />}
//             <Route path="*" element={<Navigate to="/" />} />
//           </Routes>
//         </>
//       ) : (
//         <Routes>
//           <Route path="/login" element={<Login />} />
//           <Route path="*" element={<Navigate to="/login" />} />
//         </Routes>
//       )}
//     </BrowserRouter>
//   );
// }