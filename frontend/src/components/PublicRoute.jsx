import { useKeycloak } from "@react-keycloak/web";
import { Navigate } from "react-router-dom";

const PublicRoute = ({ children }) => {
  const { keycloak, initialized } = useKeycloak();

  if (!initialized) {
    return (
      <div className="text-center mt-5">
        <div className="spinner-border text-primary"></div>
        <p className="mt-2">Cargando...</p>
      </div>
    );
  }

  // Si ya está autenticado, redirigir al home
  if (keycloak.authenticated) {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default PublicRoute;
