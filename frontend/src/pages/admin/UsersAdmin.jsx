import { useEffect, useState } from "react";
import userService from "../../services/user.service";

const UsersAdmin = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const [form, setForm] = useState({
    fullName: "",
    email: "",
    phoneNumber: "",
    idDocument: "",
    nationality: "",
    district: "",
    userRole: "CLIENT",
    accountStatus: "ACTIVE",
  });

  // Filtro
  const [filterRole, setFilterRole] = useState("ALL");
  const [searchText, setSearchText] = useState("");

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = () => {
    setLoading(true);
    userService
      .getAll()
      .then((res) => {
        setUsers(res.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  };

  const openNewModal = () => {
    setEditingId(null);
    setForm({
      fullName: "",
      email: "",
      phoneNumber: "",
      idDocument: "",
      nationality: "",
      district: "",
      userRole: "CLIENT",
      accountStatus: "ACTIVE",
    });
    setShowModal(true);
  };

  const openEditModal = (user) => {
    setEditingId(user.id);
    setForm({
      fullName: user.fullName || "",
      email: user.email || "",
      phoneNumber: user.phoneNumber || "",
      idDocument: user.idDocument || "",
      nationality: user.nationality || "",
      district: user.district || "",
      userRole: user.userRole || "CLIENT",
      accountStatus: user.accountStatus || "ACTIVE",
    });
    setShowModal(true);
  };

  const handleChange = (field, value) => {
    setForm({ ...form, [field]: value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!form.fullName || !form.email) {
      alert("⚠️ Nombre y email son obligatorios");
      return;
    }

    const action = editingId
      ? userService.update(editingId, form)
      : userService.create(form);

    action
      .then(() => {
        alert(`✅ Usuario ${editingId ? "actualizado" : "creado"}`);
        setShowModal(false);
        loadUsers();
      })
      .catch((err) => {
        console.error(err);
        alert("❌ Error: " + (err.response?.data?.message || err.message));
      });
  };

  const handleDelete = (id, email) => {
    if (
      !window.confirm(
        `¿Eliminar al usuario ${email}? Esta acción no se puede deshacer.`,
      )
    )
      return;

    userService
      .remove(id)
      .then(() => {
        alert("✅ Usuario eliminado");
        loadUsers();
      })
      .catch((err) => {
        console.error(err);
        alert("❌ Error: " + (err.response?.data?.message || err.message));
      });
  };

  // Filtros
  const filtered = users.filter((u) => {
    if (filterRole !== "ALL" && u.userRole !== filterRole) return false;
    if (
      searchText &&
      !u.fullName?.toLowerCase().includes(searchText.toLowerCase()) &&
      !u.email?.toLowerCase().includes(searchText.toLowerCase())
    )
      return false;
    return true;
  });

  if (loading) {
    return (
      <div className="text-center py-5">
        <div className="spinner-border text-primary"></div>
      </div>
    );
  }

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>👥 Gestión de Usuarios</h2>
        <button className="btn btn-primary" onClick={openNewModal}>
          + Nuevo Usuario
        </button>
      </div>

      {/* Filtros */}
      <div className="card shadow-sm mb-3">
        <div className="card-body">
          <div className="row g-3">
            <div className="col-md-4">
              <label className="form-label fw-bold">Filtrar por rol</label>
              <select
                className="form-select"
                value={filterRole}
                onChange={(e) => setFilterRole(e.target.value)}
              >
                <option value="ALL">Todos</option>
                <option value="ADMIN">Administradores</option>
                <option value="CLIENT">Clientes</option>
              </select>
            </div>
            <div className="col-md-4">
              <label className="form-label fw-bold">Buscar</label>
              <input
                type="text"
                className="form-control"
                placeholder="Nombre o email..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
              />
            </div>
            <div className="col-md-4 d-flex align-items-end">
              <button
                className="btn btn-outline-secondary w-100"
                onClick={() => {
                  setFilterRole("ALL");
                  setSearchText("");
                }}
              >
                ✖ Limpiar
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Tabla */}
      <div className="card shadow-sm">
        <div className="table-responsive">
          <table className="table table-hover mb-0">
            <thead className="table-dark">
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>Email</th>
                <th>Teléfono</th>
                <th>Documento</th>
                <th>Distrito</th>
                <th>Rol</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 && (
                <tr>
                  <td colSpan="9" className="text-center text-muted py-4">
                    No hay usuarios
                  </td>
                </tr>
              )}
              {filtered.map((u) => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>
                    <strong>{u.fullName}</strong>
                  </td>
                  <td>{u.email}</td>
                  <td>
                    <small>{u.phoneNumber}</small>
                  </td>
                  <td>
                    <small>{u.idDocument}</small>
                  </td>
                  <td>
                    <small>{u.district}</small>
                  </td>
                  <td>
                    <span
                      className={`badge ${
                        u.userRole === "ADMIN" ? "bg-danger" : "bg-info"
                      }`}
                    >
                      {u.userRole}
                    </span>
                  </td>
                  <td>
                    <span
                      className={`badge ${
                        u.accountStatus === "ACTIVE"
                          ? "bg-success"
                          : "bg-secondary"
                      }`}
                    >
                      {u.accountStatus}
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn btn-sm btn-outline-primary me-1"
                      onClick={() => openEditModal(u)}
                    >
                      ✏️
                    </button>
                    <button
                      className="btn btn-sm btn-outline-danger"
                      onClick={() => handleDelete(u.id, u.email)}
                    >
                      🗑️
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <small className="text-muted d-block mt-2">
        Mostrando {filtered.length} de {users.length} usuarios
      </small>

      {/* MODAL */}
      {showModal && (
        <div
          className="modal show d-block"
          style={{ backgroundColor: "rgba(0,0,0,0.5)" }}
          tabIndex="-1"
        >
          <div
            className="modal-dialog modal-lg modal-dialog-centered"
            style={{ maxHeight: "90vh" }}
          >
            <div
              className="modal-content"
              style={{
                maxHeight: "90vh",
                display: "flex",
                flexDirection: "column",
              }}
            >
              <div
                className="modal-header bg-primary text-white"
                style={{ flexShrink: 0 }}
              >
                <h5 className="modal-title">
                  {editingId ? "✏️ Editar Usuario" : "+ Nuevo Usuario"}
                </h5>
                <button
                  type="button"
                  className="btn-close btn-close-white"
                  onClick={() => setShowModal(false)}
                ></button>
              </div>

              <form
                onSubmit={handleSubmit}
                style={{
                  display: "flex",
                  flexDirection: "column",
                  flex: 1,
                  minHeight: 0,
                }}
              >
                <div
                  className="modal-body"
                  style={{ overflowY: "auto", flex: 1 }}
                >
                  <div className="row g-3">
                    <div className="col-md-12">
                      <label className="form-label fw-bold">
                        Nombre completo *
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        value={form.fullName}
                        onChange={(e) =>
                          handleChange("fullName", e.target.value)
                        }
                        required
                      />
                    </div>
                    <div className="col-md-6">
                      <label className="form-label fw-bold">Email *</label>
                      <input
                        type="email"
                        className="form-control"
                        value={form.email}
                        onChange={(e) => handleChange("email", e.target.value)}
                        required
                      />
                    </div>
                    <div className="col-md-6">
                      <label className="form-label fw-bold">Teléfono</label>
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
                    <div className="col-md-6">
                      <label className="form-label fw-bold">
                        Documento (RUT/Pasaporte)
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        value={form.idDocument}
                        onChange={(e) =>
                          handleChange("idDocument", e.target.value)
                        }
                      />
                    </div>
                    <div className="col-md-6">
                      <label className="form-label fw-bold">Nacionalidad</label>
                      <input
                        type="text"
                        className="form-control"
                        value={form.nationality}
                        onChange={(e) =>
                          handleChange("nationality", e.target.value)
                        }
                      />
                    </div>
                    <div className="col-md-12">
                      <label className="form-label fw-bold">
                        Distrito/Comuna
                      </label>
                      <input
                        type="text"
                        className="form-control"
                        value={form.district}
                        onChange={(e) =>
                          handleChange("district", e.target.value)
                        }
                      />
                    </div>
                    <div className="col-md-6">
                      <label className="form-label fw-bold">Rol</label>
                      <select
                        className="form-select"
                        value={form.userRole}
                        onChange={(e) =>
                          handleChange("userRole", e.target.value)
                        }
                      >
                        <option value="CLIENT">Cliente</option>
                        <option value="ADMIN">Administrador</option>
                      </select>
                    </div>
                    <div className="col-md-6">
                      <label className="form-label fw-bold">
                        Estado de cuenta
                      </label>
                      <select
                        className="form-select"
                        value={form.accountStatus}
                        onChange={(e) =>
                          handleChange("accountStatus", e.target.value)
                        }
                      >
                        <option value="ACTIVE">Activo</option>
                        <option value="INACTIVE">Inactivo</option>
                      </select>
                    </div>
                  </div>

                  <div className="alert alert-info small mt-3 mb-0">
                    ℹ️ <strong>Nota:</strong> los usuarios también deben existir
                    en Keycloak para poder iniciar sesión. El email debe
                    coincidir.
                  </div>
                </div>

                <div className="modal-footer" style={{ flexShrink: 0 }}>
                  <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => setShowModal(false)}
                  >
                    Cancelar
                  </button>
                  <button type="submit" className="btn btn-success">
                    {editingId ? "Actualizar" : "Crear"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default UsersAdmin;
