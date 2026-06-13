import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import authService from "../services/auth.service";

const Register = () => {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    fullName: "",
    phoneNumber: "",
    idDocument: "",
    nationality: "Chilena",
    district: "",
  });

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const handleChange = (field, value) => {
    setForm({ ...form, [field]: value });
    setError(null);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setError(null);

    // Validaciones del frontend
    if (form.password.length < 6) {
      setError("La contraseña debe tener al menos 6 caracteres");
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError("Las contraseñas no coinciden");
      return;
    }
    if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/.test(form.email)) {
      setError("Formato de email inválido");
      return;
    }

    setSubmitting(true);

    authService
      .register({
        email: form.email,
        password: form.password,
        fullName: form.fullName,
        phoneNumber: form.phoneNumber,
        idDocument: form.idDocument,
        nationality: form.nationality,
        district: form.district,
      })
      .then(() => {
        setSuccess(true);
        setSubmitting(false);
      })
      .catch((err) => {
        console.error(err);
        setError(
          err.response?.data?.message ||
            err.response?.data ||
            "Error al registrarse. Intenta de nuevo.",
        );
        setSubmitting(false);
      });
  };

  // Pantalla de exito
  if (success) {
    return (
      <div
        className="container"
        style={{ maxWidth: "500px", marginTop: "60px" }}
      >
        <div className="card shadow text-center">
          <div className="card-body p-5">
            <div style={{ fontSize: "4rem" }}>✅</div>
            <h3 className="mt-3">¡Cuenta creada!</h3>
            <p className="text-muted">
              Tu cuenta fue registrada exitosamente. Ahora puedes iniciar
              sesión.
            </p>
            <button
              className="btn btn-primary btn-lg mt-3"
              onClick={() => {
                // Recarga forzando login en Keycloak
                window.location.href = "/";
              }}
            >
              🚪 Ir a iniciar sesión
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: "700px", marginTop: "40px" }}>
      <div className="card shadow">
        <div className="card-header bg-primary text-white">
          <h4 className="mb-0">✈️ Crea tu cuenta en Travel Agency</h4>
        </div>
        <div className="card-body">
          <p className="text-muted small">
            Regístrate gratis para empezar a reservar tus viajes. Los campos
            marcados con * son obligatorios.
          </p>

          {error && (
            <div className="alert alert-danger">
              <strong>⚠️</strong> {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <h6 className="text-primary mt-2 mb-3">
              🔐 Credenciales de acceso
            </h6>
            <div className="row g-3">
              <div className="col-md-12">
                <label className="form-label fw-bold">Email *</label>
                <input
                  type="email"
                  className="form-control"
                  placeholder="tu@correo.com"
                  value={form.email}
                  onChange={(e) => handleChange("email", e.target.value)}
                  required
                />
                <small className="text-muted">
                  Lo usarás para iniciar sesión
                </small>
              </div>
              <div className="col-md-6">
                <label className="form-label fw-bold">Contraseña *</label>
                <input
                  type="password"
                  className="form-control"
                  placeholder="Mínimo 6 caracteres"
                  value={form.password}
                  onChange={(e) => handleChange("password", e.target.value)}
                  required
                  minLength="6"
                />
              </div>
              <div className="col-md-6">
                <label className="form-label fw-bold">
                  Confirmar contraseña *
                </label>
                <input
                  type="password"
                  className="form-control"
                  placeholder="Repite la contraseña"
                  value={form.confirmPassword}
                  onChange={(e) =>
                    handleChange("confirmPassword", e.target.value)
                  }
                  required
                />
              </div>
            </div>

            <hr className="my-4" />

            <h6 className="text-primary mb-3">👤 Información personal</h6>
            <div className="row g-3">
              <div className="col-md-12">
                <label className="form-label fw-bold">Nombre completo *</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Ej: Juan Pérez González"
                  value={form.fullName}
                  onChange={(e) => handleChange("fullName", e.target.value)}
                  required
                />
              </div>
              <div className="col-md-6">
                <label className="form-label fw-bold">Documento (RUT)</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="12345678-9"
                  value={form.idDocument}
                  onChange={(e) => handleChange("idDocument", e.target.value)}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label fw-bold">Teléfono</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="+56912345678"
                  value={form.phoneNumber}
                  onChange={(e) => handleChange("phoneNumber", e.target.value)}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label fw-bold">Nacionalidad</label>
                <input
                  type="text"
                  className="form-control"
                  value={form.nationality}
                  onChange={(e) => handleChange("nationality", e.target.value)}
                />
              </div>
              <div className="col-md-6">
                <label className="form-label fw-bold">Comuna / Distrito</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Ej: Las Condes"
                  value={form.district}
                  onChange={(e) => handleChange("district", e.target.value)}
                />
              </div>
            </div>

            <hr className="my-4" />

            <div className="d-flex gap-2">
              <Link to="/" className="btn btn-outline-secondary flex-grow-1">
                ← Cancelar
              </Link>
              <button
                type="submit"
                className="btn btn-success flex-grow-1"
                disabled={submitting}
              >
                {submitting ? "Creando cuenta..." : "✅ Crear mi cuenta"}
              </button>
            </div>
          </form>

          <hr className="my-4" />

          <p className="text-center mb-0 small text-muted">
            ¿Ya tienes una cuenta?{" "}
            <a href="/" className="text-decoration-none">
              Iniciar sesión
            </a>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;
