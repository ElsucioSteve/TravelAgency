import { useEffect, useState } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { useNavigate } from "react-router-dom";
import userService from "../services/user.service";

const MyProfile = () => {
  const { keycloak } = useKeycloak();
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Solo estos campos son editables por el propio usuario
  const [form, setForm] = useState({
    phoneNumber: "",
    addressStreet: "",
    addressNumber: "",
    district: "",
    nationality: "",
  });

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = () => {
    setLoading(true);
    userService
      .getMe()
      .then((res) => {
        setProfile(res.data);
        setForm({
          phoneNumber: res.data.phoneNumber || "",
          addressStreet: res.data.addressStreet || "",
          addressNumber: res.data.addressNumber || "",
          district: res.data.district || "",
          nationality: res.data.nationality || "",
        });
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Error al cargar tu perfil");
        setLoading(false);
      });
  };

  const handleChange = (field, value) => {
    setForm({ ...form, [field]: value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setSaving(true);

    userService
      .updateMe(form)
      .then(() => {
        alert("✅ Perfil actualizado correctamente");
        loadProfile();
        setSaving(false);
      })
      .catch((err) => {
        console.error(err);
        alert(
          "❌ Error al actualizar: " +
            (err.response?.data?.message || err.response?.data || err.message),
        );
        setSaving(false);
      });
  };

  const handleLogout = () => {
    if (window.confirm("¿Cerrar sesión?")) {
      keycloak.logout({ redirectUri: window.location.origin });
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "-";
    return new Date(dateStr).toLocaleDateString("es-CL");
  };

  if (loading) {
    return (
      <div className="text-center py-5">
        <div className="spinner-border text-primary"></div>
      </div>
    );
  }

  if (error) {
    return <div className="alert alert-danger">{error}</div>;
  }

  if (!profile) return null;

  const isAdmin = profile.userRole === "ADMIN";

  return (
    <div>
      <h2 className="mb-4">👤 Mi Perfil</h2>

      <div className="row">
        {/* Tarjeta de info no editable */}
        <div className="col-md-4">
          <div className="card shadow-sm mb-3">
            <div className="card-body text-center">
              <div
                className="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center mx-auto mb-3"
                style={{ width: "80px", height: "80px", fontSize: "2rem" }}
              >
                {profile.fullName?.charAt(0).toUpperCase()}
              </div>
              <h5 className="mb-1">{profile.fullName}</h5>
              <p className="text-muted mb-2">{profile.email}</p>
              <span
                className={`badge ${isAdmin ? "bg-danger" : "bg-info"} mb-2`}
              >
                {profile.userRole}
              </span>
              <p className="mb-1">
                <small className="text-muted">Estado de cuenta</small>
                <br />
                <span
                  className={`badge ${
                    profile.accountStatus === "ACTIVE"
                      ? "bg-success"
                      : "bg-secondary"
                  }`}
                >
                  {profile.accountStatus}
                </span>
              </p>
            </div>
          </div>

          <div className="card shadow-sm mb-3">
            <div className="card-header bg-light">
              <h6 className="mb-0">📋 Información de la cuenta</h6>
            </div>
            <div className="card-body small">
              <p className="mb-2">
                <strong>Documento:</strong>
                <br />
                <span className="text-muted">
                  {profile.idDocument || "(no registrado)"}
                </span>
              </p>
              <p className="mb-2">
                <strong>Fecha de nacimiento:</strong>
                <br />
                <span className="text-muted">
                  {formatDate(profile.birthDate)}
                </span>
              </p>
              <p className="mb-0">
                <strong>Miembro desde:</strong>
                <br />
                <span className="text-muted">
                  {formatDate(profile.registrationDate)}
                </span>
              </p>
            </div>
          </div>

          <button
            className="btn btn-outline-danger w-100"
            onClick={handleLogout}
          >
            🚪 Cerrar sesión
          </button>
        </div>

        {/* Formulario editable */}
        <div className="col-md-8">
          <div className="card shadow-sm">
            <div className="card-header bg-primary text-white">
              <h5 className="mb-0">✏️ Editar mis datos</h5>
            </div>
            <div className="card-body">
              <div className="alert alert-info small">
                ℹ️ Puedes actualizar tu información de contacto. Los datos de
                identificación (nombre, email, rol) solo pueden ser modificados
                por un administrador.
              </div>

              <form onSubmit={handleSubmit}>
                <div className="row g-3">
                  <div className="col-md-12">
                    <label className="form-label fw-bold">📞 Teléfono</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="+56912345678"
                      value={form.phoneNumber}
                      onChange={(e) =>
                        handleChange("phoneNumber", e.target.value)
                      }
                    />
                  </div>

                  <div className="col-md-8">
                    <label className="form-label fw-bold">🏠 Calle</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Ej: Av. Libertador"
                      value={form.addressStreet}
                      onChange={(e) =>
                        handleChange("addressStreet", e.target.value)
                      }
                    />
                  </div>
                  <div className="col-md-4">
                    <label className="form-label fw-bold">Número</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="1234"
                      value={form.addressNumber}
                      onChange={(e) =>
                        handleChange("addressNumber", e.target.value)
                      }
                    />
                  </div>

                  <div className="col-md-6">
                    <label className="form-label fw-bold">
                      📍 Distrito/Comuna
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Las Condes"
                      value={form.district}
                      onChange={(e) => handleChange("district", e.target.value)}
                    />
                  </div>
                  <div className="col-md-6">
                    <label className="form-label fw-bold">
                      🌎 Nacionalidad
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Chilena"
                      value={form.nationality}
                      onChange={(e) =>
                        handleChange("nationality", e.target.value)
                      }
                    />
                  </div>
                </div>

                <hr />

                <div className="d-flex justify-content-end gap-2">
                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => navigate(-1)}
                  >
                    ← Volver
                  </button>
                  <button
                    type="submit"
                    className="btn btn-success"
                    disabled={saving}
                  >
                    {saving ? "Guardando..." : "💾 Guardar cambios"}
                  </button>
                </div>
              </form>
            </div>
          </div>

          {/* Atajo para clientes a sus reservas */}
          {!isAdmin && (
            <div className="card shadow-sm mt-3 bg-light">
              <div className="card-body text-center">
                <h6 className="mb-2">🌍 ¿Listo para tu próximo viaje?</h6>
                <button
                  className="btn btn-primary me-2"
                  onClick={() => navigate("/")}
                >
                  Ver catálogo
                </button>
                <button
                  className="btn btn-outline-primary"
                  onClick={() => navigate("/my-bookings")}
                >
                  Mis reservas
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default MyProfile;
