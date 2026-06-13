import { Link, useLocation } from "react-router-dom";
import { useKeycloak } from "@react-keycloak/web";

const Navbar = () => {
  const { keycloak } = useKeycloak();
  const location = useLocation();

  const isAdmin = keycloak?.hasRealmRole("ADMIN");
  const username = keycloak?.tokenParsed?.preferred_username;
  const isActive = (path) => location.pathname === path;

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-dark sticky-top">
      <div className="container">
        <Link className="navbar-brand fw-bold" to="/">
          ✈️ Agencia de Viajes
        </Link>

        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarMain"
        >
          <span className="navbar-toggler-icon"></span>
        </button>

        <div className="collapse navbar-collapse" id="navbarMain">
          {keycloak?.authenticated && (
            <>
              <ul className="navbar-nav me-auto">
                <li className="nav-item">
                  <Link
                    className={`nav-link ${isActive("/") ? "active" : ""}`}
                    to="/"
                  >
                    🌍 Catálogo
                  </Link>
                </li>
                <li className="nav-item">
                  <Link
                    className={`nav-link ${isActive("/my-bookings") ? "active" : ""}`}
                    to="/my-bookings"
                  >
                    📋 Mis Reservas
                  </Link>
                </li>

                {isAdmin && (
                  <li className="nav-item dropdown">
                    <button
                      className="nav-link dropdown-toggle btn btn-link"
                      type="button"
                      data-bs-toggle="dropdown"
                      aria-expanded="false"
                    >
                      🔧 Admin
                    </button>
                    <ul className="dropdown-menu">
                      <li>
                        <Link className="dropdown-item" to="/admin/packages">
                          📦 Gestionar Paquetes
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item" to="/admin/discounts">
                          🎟️ Gestionar Descuentos
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item" to="/admin/bookings">
                          📊 Todas las Reservas
                        </Link>
                      </li>
                      <li>
                        <Link className="dropdown-item" to="/admin/users">
                          👥 Gestionar Usuarios
                        </Link>
                      </li>
                      <li>
                        <hr className="dropdown-divider" />
                      </li>
                      <li>
                        <Link className="dropdown-item" to="/admin/reports">
                          📈 Reportes
                        </Link>
                      </li>
                    </ul>
                  </li>
                )}
              </ul>

              <div className="d-flex align-items-center">
                <Link
                  to="/profile"
                  className="text-light me-3 text-decoration-none"
                >
                  👤 <b>{username}</b>
                </Link>
                <span
                  className={`badge ${isAdmin ? "bg-danger" : "bg-info"} me-3`}
                >
                  {isAdmin ? "ADMIN" : "CLIENT"}
                </span>
                <button
                  className="btn btn-outline-danger btn-sm"
                  onClick={() =>
                    keycloak.logout({ redirectUri: window.location.origin })
                  }
                >
                  Cerrar Sesión
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
