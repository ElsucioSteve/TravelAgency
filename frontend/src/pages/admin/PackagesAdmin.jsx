import { useEffect, useState } from "react";
import travelPackageService from "../../services/travelPackage.service";

const PackagesAdmin = () => {
  const [packages, setPackages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);

  // Form fields
  const [form, setForm] = useState({
    name: "",
    destination: "",
    description: "",
    startDate: "",
    endDate: "",
    basePrice: "",
    totalSlots: "",
    travelType: "NATIONAL",
    season: "MEDIUM",
    category: "ADVENTURE",
    packageStatus: "AVAILABLE",
    isVisibleWeb: true,
    includedServices: "",
    conditions: "",
    restrictions: "",
  });

  useEffect(() => {
    loadPackages();
  }, []);

  const loadPackages = () => {
    setLoading(true);
    travelPackageService
      .getAll()
      .then((res) => {
        setPackages(res.data);
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
      name: "",
      destination: "",
      description: "",
      startDate: "",
      endDate: "",
      basePrice: "",
      totalSlots: "",
      travelType: "NATIONAL",
      season: "MEDIUM",
      category: "ADVENTURE",
      packageStatus: "AVAILABLE",
      isVisibleWeb: true,
      includedServices: "",
      conditions: "",
      restrictions: "",
    });
    setShowModal(true);
  };

  const openEditModal = (pkg) => {
    setEditingId(pkg.id);
    setForm({
      name: pkg.name || "",
      destination: pkg.destination || "",
      description: pkg.description || "",
      startDate: pkg.startDate || "",
      endDate: pkg.endDate || "",
      basePrice: pkg.basePrice || "",
      totalSlots: pkg.totalSlots || "",
      travelType: pkg.travelType || "NATIONAL",
      season: pkg.season || "MEDIUM",
      category: pkg.category || "ADVENTURE",
      packageStatus: pkg.packageStatus || "AVAILABLE",
      isVisibleWeb: pkg.isVisibleWeb !== false,
      includedServices: pkg.includedServices || "",
      conditions: pkg.conditions || "",
      restrictions: pkg.restrictions || "",
    });
    setShowModal(true);
  };

  const handleChange = (field, value) => {
    setForm({ ...form, [field]: value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    // Validación 1: precio positivo
    if (parseFloat(form.basePrice) <= 0) {
      alert("⚠️ El precio debe ser mayor a 0");
      return;
    }

    // Validación 2: cupos positivos
    if (parseInt(form.totalSlots) <= 0) {
      alert("⚠️ Los cupos totales deben ser mayor a 0");
      return;
    }

    // Validación 3: fechas coherentes
    const start = new Date(form.startDate);
    const end = new Date(form.endDate);
    if (end <= start) {
      alert("⚠️ La fecha de fin debe ser posterior a la de inicio");
      return;
    }

    // Validación 4: fecha de inicio no puede ser pasada (solo al crear)
    if (!editingId) {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (start < today) {
        alert("⚠️ La fecha de inicio no puede ser anterior a hoy");
        return;
      }
    }

    const payload = {
      ...form,
      basePrice: parseFloat(form.basePrice),
      totalSlots: parseInt(form.totalSlots),
    };

    const action = editingId
      ? travelPackageService.update(editingId, payload)
      : travelPackageService.create(payload);

    action
      .then(() => {
        alert(`✅ Paquete ${editingId ? "actualizado" : "creado"} con éxito`);
        setShowModal(false);
        loadPackages();
      })
      .catch((err) => {
        console.error(err);
        alert(
          "❌ Error: " +
            (err.response?.data?.message || err.response?.data || err.message),
        );
      });
  };

  const handleDelete = (id) => {
    if (
      !window.confirm(
        "¿Eliminar este paquete? Si tiene reservas, solo se marcará como CANCELED.",
      )
    )
      return;

    travelPackageService
      .remove(id)
      .then(() => {
        alert("✅ Paquete eliminado/cancelado");
        loadPackages();
      })
      .catch((err) => {
        console.error(err);
        alert("❌ Error: " + (err.response?.data || err.message));
      });
  };

  const formatPrice = (price) =>
    new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      maximumFractionDigits: 0,
    }).format(price);

  const getStatusBadge = (status) => {
    const colors = {
      AVAILABLE: "bg-success",
      SOLD_OUT: "bg-warning text-dark",
      EXPIRED: "bg-secondary",
      CANCELED: "bg-danger",
    };
    return colors[status] || "bg-light text-dark";
  };

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
        <h2>📦 Gestión de Paquetes</h2>
        <button className="btn btn-primary" onClick={openNewModal}>
          + Nuevo Paquete
        </button>
      </div>

      <div className="card shadow-sm">
        <div className="table-responsive">
          <table className="table table-hover mb-0">
            <thead className="table-dark">
              <tr>
                <th>ID</th>
                <th>Nombre</th>
                <th>Destino</th>
                <th>Salida</th>
                <th>Precio</th>
                <th>Cupos</th>
                <th>Estado</th>
                <th>Visible</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {packages.map((p) => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>
                    <strong>{p.name}</strong>
                    <br />
                    <small className="text-muted">{p.category}</small>
                  </td>
                  <td>{p.destination}</td>
                  <td>{p.startDate}</td>
                  <td>{formatPrice(p.basePrice)}</td>
                  <td>
                    {p.availableSlots} / {p.totalSlots}
                  </td>
                  <td>
                    <span
                      className={`badge ${getStatusBadge(p.packageStatus)}`}
                    >
                      {p.packageStatus}
                    </span>
                  </td>
                  <td>{p.isVisibleWeb ? "✅" : "❌"}</td>
                  <td>
                    <button
                      className="btn btn-sm btn-outline-primary me-1"
                      onClick={() => openEditModal(p)}
                    >
                      ✏️
                    </button>
                    <button
                      className="btn btn-sm btn-outline-danger"
                      onClick={() => handleDelete(p.id)}
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
              {/* HEADER fijo */}
              <div
                className="modal-header bg-primary text-white"
                style={{ flexShrink: 0 }}
              >
                <h5 className="modal-title">
                  {editingId ? "✏️ Editar Paquete" : "+ Nuevo Paquete"}
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
                {/* BODY con scroll */}
                <div
                  className="modal-body"
                  style={{ overflowY: "auto", flex: 1 }}
                >
                  <div className="row g-3">
                    <div className="col-md-8">
                      <label className="form-label fw-bold">Nombre *</label>
                      <input
                        type="text"
                        className="form-control"
                        value={form.name}
                        onChange={(e) => handleChange("name", e.target.value)}
                        required
                      />
                    </div>
                    <div className="col-md-4">
                      <label className="form-label fw-bold">Categoría</label>
                      <select
                        className="form-select"
                        value={form.category}
                        onChange={(e) =>
                          handleChange("category", e.target.value)
                        }
                      >
                        <option value="ADVENTURE">Aventura</option>
                        <option value="BEACH">Playa</option>
                        <option value="CULTURAL">Cultural</option>
                        <option value="FAMILY">Familiar</option>
                        <option value="LUXURY">Lujo</option>
                      </select>
                    </div>

                    <div className="col-md-12">
                      <label className="form-label fw-bold">Destino *</label>
                      <input
                        type="text"
                        className="form-control"
                        value={form.destination}
                        onChange={(e) =>
                          handleChange("destination", e.target.value)
                        }
                        required
                      />
                    </div>

                    <div className="col-md-12">
                      <label className="form-label fw-bold">Descripción</label>
                      <textarea
                        className="form-control"
                        rows="2"
                        value={form.description}
                        onChange={(e) =>
                          handleChange("description", e.target.value)
                        }
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label fw-bold">
                        Fecha inicio *
                      </label>
                      <input
                        type="date"
                        className="form-control"
                        value={form.startDate}
                        onChange={(e) =>
                          handleChange("startDate", e.target.value)
                        }
                        required
                      />
                    </div>
                    <div className="col-md-4">
                      <label className="form-label fw-bold">Fecha fin *</label>
                      <input
                        type="date"
                        className="form-control"
                        value={form.endDate}
                        onChange={(e) =>
                          handleChange("endDate", e.target.value)
                        }
                        required
                      />
                    </div>
                    <div className="col-md-4">
                      <label className="form-label fw-bold">
                        Precio base (CLP) *
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        min="1"
                        value={form.basePrice}
                        onChange={(e) =>
                          handleChange("basePrice", e.target.value)
                        }
                        required
                      />
                    </div>

                    <div className="col-md-3">
                      <label className="form-label fw-bold">
                        Cupos totales *
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        min="1"
                        value={form.totalSlots}
                        onChange={(e) =>
                          handleChange("totalSlots", e.target.value)
                        }
                        required
                      />
                    </div>
                    <div className="col-md-3">
                      <label className="form-label fw-bold">Tipo viaje</label>
                      <select
                        className="form-select"
                        value={form.travelType}
                        onChange={(e) =>
                          handleChange("travelType", e.target.value)
                        }
                      >
                        <option value="NATIONAL">Nacional</option>
                        <option value="INTERNATIONAL">Internacional</option>
                      </select>
                    </div>
                    <div className="col-md-3">
                      <label className="form-label fw-bold">Temporada</label>
                      <select
                        className="form-select"
                        value={form.season}
                        onChange={(e) => handleChange("season", e.target.value)}
                      >
                        <option value="LOW">Baja</option>
                        <option value="MEDIUM">Media</option>
                        <option value="HIGH">Alta</option>
                      </select>
                    </div>
                    <div className="col-md-3">
                      <label className="form-label fw-bold">Estado</label>
                      <select
                        className="form-select"
                        value={form.packageStatus}
                        onChange={(e) =>
                          handleChange("packageStatus", e.target.value)
                        }
                      >
                        <option value="AVAILABLE">Disponible</option>
                        <option value="SOLD_OUT">Agotado</option>
                        <option value="EXPIRED">Vencido</option>
                        <option value="CANCELED">Cancelado</option>
                      </select>
                    </div>

                    <div className="col-md-12">
                      <label className="form-label fw-bold">
                        Servicios incluidos
                      </label>
                      <textarea
                        className="form-control"
                        rows="2"
                        value={form.includedServices}
                        onChange={(e) =>
                          handleChange("includedServices", e.target.value)
                        }
                      />
                    </div>

                    <div className="col-md-6">
                      <label className="form-label fw-bold">Condiciones</label>
                      <textarea
                        className="form-control"
                        rows="2"
                        value={form.conditions}
                        onChange={(e) =>
                          handleChange("conditions", e.target.value)
                        }
                      />
                    </div>
                    <div className="col-md-6">
                      <label className="form-label fw-bold">
                        Restricciones
                      </label>
                      <textarea
                        className="form-control"
                        rows="2"
                        value={form.restrictions}
                        onChange={(e) =>
                          handleChange("restrictions", e.target.value)
                        }
                      />
                    </div>

                    <div className="col-md-12">
                      <div className="form-check">
                        <input
                          type="checkbox"
                          className="form-check-input"
                          id="isVisibleWeb"
                          checked={form.isVisibleWeb}
                          onChange={(e) =>
                            handleChange("isVisibleWeb", e.target.checked)
                          }
                        />
                        <label
                          className="form-check-label"
                          htmlFor="isVisibleWeb"
                        >
                          Visible en la web
                        </label>
                      </div>
                    </div>
                  </div>
                </div>

                {/* FOOTER fijo */}
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

export default PackagesAdmin;
