import { useKeycloak } from "@react-keycloak/web";
import { useLocation } from "react-router-dom";

const PrivateRoute = ({ children, role }) => {
  const { keycloak, initialized } = useKeycloak();
  const location = useLocation();

  if (!initialized) {
    return (
      <div className="text-center mt-5">
        <div className="spinner-border text-primary" role="status"></div>
        <p className="mt-2">Iniciando seguridad...</p>
      </div>
    );
  }

  if (!keycloak.authenticated) {
    if (location.pathname === "/register") {
      return null;
    }
    keycloak.login();
    return null;
  }

  if (role && !keycloak.hasRealmRole(role)) {
    return (
      <div className="container mt-5">
        <div className="alert alert-warning">
          ⚠️ Acceso restringido: Se requiere el rol <b>{role}</b>.
        </div>
      </div>
    );
  }

  return children;
};

export default PrivateRoute;
