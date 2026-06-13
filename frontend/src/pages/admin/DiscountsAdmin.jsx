import { useEffect, useState } from "react";
import discountService from "../../services/discount.service";

const DiscountsAdmin = () => {
  const [discounts, setDiscounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const [form, setForm] = useState({
    name: "",
    description: "",
    valueType: "PERCENTAGE",
    discountValue: "",
    maxLimit: "",
    discountStatus: "ACTIVE",
    startDate: "",
    endDate: "",
    applicationCriteria: "GROUP",
    isStackable: true,
  });

  useEffect(() => {
    loadDiscounts();
  }, []);

  const loadDiscounts = () => {
    setLoading(true);
    discountService
      .getAll()
      .then((res) => {
        setDiscounts(res.data);
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
      description: "",
      valueType: "PERCENTAGE",
      discountValue: "",
      maxLimit: "",
      discountStatus: "ACTIVE",
      startDate: "",
      endDate: "",
      applicationCriteria: "GROUP",
      isStackable: true,
    });
    setShowModal(true);
  };

  const openEditModal = (d) => {
    setEditingId(d.id);
    setForm({
      name: d.name || "",
      description: d.description || "",
      valueType: d.valueType || "PERCENTAGE",
      discountValue: d.discountValue || "",
      maxLimit: d.maxLimit || "",
      discountStatus: d.discountStatus || "ACTIVE",
      startDate: d.startDate ? d.startDate.substring(0, 16) : "",
      endDate: d.endDate ? d.endDate.substring(0, 16) : "",
      applicationCriteria: d.applicationCriteria || "GROUP",
      isStackable: d.isStackable !== false,
    });
    setShowModal(true);
  };

  const handleChange = (field, value) => {
    setForm({ ...form, [field]: value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    // Validación 1: valor del descuento
    const discountVal = parseFloat(form.discountValue);
    if (discountVal <= 0) {
      alert("⚠️ El valor del descuento debe ser mayor a 0");
      return;
    }
    if (form.valueType === "PERCENTAGE" && discountVal > 100) {
      alert("⚠️ El porcentaje no puede ser mayor a 100");
      return;
    }

    // Validación 2: tope no negativo
    if (form.maxLimit !== "" && parseFloat(form.maxLimit) < 0) {
      alert(
        "⚠️ El tope máximo no puede ser negativo. Déjalo vacío si no hay tope.",
      );
      return;
    }

    // Validación 3: fechas coherentes
    if (form.startDate && form.endDate) {
      const start = new Date(form.startDate);
      const end = new Date(form.endDate);
      if (end <= start) {
        alert("⚠️ La fecha de fin debe ser posterior a la fecha de inicio");
        return;
      }
    }

    // Validación 4: fecha de fin no puede estar en el pasado (al crear)
    if (!editingId && form.endDate) {
      const end = new Date(form.endDate);
      const now = new Date();
      if (end < now) {
        alert("⚠️ No tiene sentido crear un descuento que ya está vencido");
        return;
      }
    }

    const payload = {
      ...form,
      discountValue: discountVal,
      maxLimit: form.maxLimit ? parseFloat(form.maxLimit) : null,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
    };

    const action = editingId
      ? discountService.update(editingId, payload)
      : discountService.create(payload);

    action
      .then(() => {
        alert(`✅ Descuento ${editingId ? "actualizado" : "creado"}`);
        setShowModal(false);
        loadDiscounts();
      })
      .catch((err) => {
        console.error(err);
        alert("❌ Error: " + (err.response?.data?.message || err.message));
      });
  };

  const handleDelete = (id) => {
    if (
      !window.confirm("¿Desactivar este descuento? (se marcará como INACTIVE)")
    )
      return;

    discountService
      .remove(id)
      .then(() => {
        alert("✅ Descuento desactivado");
        loadDiscounts();
      })
      .catch((err) =>
        alert("❌ Error: " + (err.response?.data || err.message)),
      );
  };

  const formatPrice = (price) =>
    new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      maximumFractionDigits: 0,
    }).format(price);

  // Calcula el estado real considerando vigencia
  const getEffectiveStatus = (d) => {
    if (d.discountStatus === "INACTIVE") {
      return {
        label: "INACTIVE",
        color: "bg-secondary",
        reason: "Desactivado por admin",
      };
    }

    const now = new Date();
    if (d.startDate && new Date(d.startDate) > now) {
      return {
        label: "SCHEDULED",
        color: "bg-info text-dark",
        reason: "Aún no comienza vigencia",
      };
    }
    if (d.endDate && new Date(d.endDate) < now) {
      return {
        label: "EXPIRED",
        color: "bg-danger",
        reason: "Vigencia ya terminó",
      };
    }
    return {
      label: "ACTIVE",
      color: "bg-success",
      reason: "Vigente y aplicable",
    };
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
        <h2>🎟️ Gestión de Descuentos</h2>
        <button className="btn btn-primary" onClick={openNewModal}>
          + Nuevo Descuento
        </button>
      </div>

      <div className="card shadow-sm">
        <div className="table-responsive">
          <table className="table table-hover mb-0">
            <thead className="table-dark">
              <tr>
                <th>Nombre</th>
                <th>Criterio</th>
                <th>Tipo</th>
                <th>Valor</th>
                <th>Tope</th>
                <th>Apilable</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {discounts.map((d) => {
                const eff = getEffectiveStatus(d);
                return (
                  <tr key={d.id}>
                    <td>
                      <strong>{d.name}</strong>
                      <br />
                      <small className="text-muted">{d.description}</small>
                    </td>
                    <td>
                      <span className="badge bg-info">
                        {d.applicationCriteria}
                      </span>
                    </td>
                    <td>{d.valueType === "PERCENTAGE" ? "%" : "Monto fijo"}</td>
                    <td>
                      {d.valueType === "PERCENTAGE"
                        ? `${d.discountValue}%`
                        : formatPrice(d.discountValue)}
                    </td>
                    <td>{d.maxLimit ? formatPrice(d.maxLimit) : "-"}</td>
                    <td>{d.isStackable ? "✅" : "❌"}</td>
                    <td>
                      <span className={`badge ${eff.color}`} title={eff.reason}>
                        {eff.label}
                      </span>
                    </td>
                    <td>
                      <button
                        className="btn btn-sm btn-outline-primary me-1"
                        onClick={() => openEditModal(d)}
                      >
                        ✏️
                      </button>
                      <button
                        className="btn btn-sm btn-outline-danger"
                        onClick={() => handleDelete(d.id)}
                        disabled={d.discountStatus === "INACTIVE"}
                      >
                        🗑️
                      </button>
                    </td>
                  </tr>
                );
              })}
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
                  {editingId ? "✏️ Editar Descuento" : "+ Nuevo Descuento"}
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
                      <label className="form-label fw-bold">Estado</label>
                      <select
                        className="form-select"
                        value={form.discountStatus}
                        onChange={(e) =>
                          handleChange("discountStatus", e.target.value)
                        }
                      >
                        <option value="ACTIVE">Activo</option>
                        <option value="INACTIVE">Inactivo</option>
                      </select>
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
                      <label className="form-label fw-bold">Criterio</label>
                      <select
                        className="form-select"
                        value={form.applicationCriteria}
                        onChange={(e) =>
                          handleChange("applicationCriteria", e.target.value)
                        }
                      >
                        <option value="GROUP">Grupo (4+ pasajeros)</option>
                        <option value="FREQUENT">Cliente frecuente</option>
                        <option value="MULTI_PACKAGE">Multi-paquete</option>
                        <option value="PROMO">Promoción</option>
                      </select>
                    </div>
                    <div className="col-md-4">
                      <label className="form-label fw-bold">
                        Tipo de valor
                      </label>
                      <select
                        className="form-select"
                        value={form.valueType}
                        onChange={(e) =>
                          handleChange("valueType", e.target.value)
                        }
                      >
                        <option value="PERCENTAGE">Porcentaje (%)</option>
                        <option value="FIXED_AMOUNT">Monto fijo ($)</option>
                      </select>
                    </div>
                    <div className="col-md-4">
                      <label className="form-label fw-bold">
                        Valor {form.valueType === "PERCENTAGE" ? "(%)" : "($)"}
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        min="0"
                        max={form.valueType === "PERCENTAGE" ? "100" : ""}
                        className="form-control"
                        value={form.discountValue}
                        onChange={(e) =>
                          handleChange("discountValue", e.target.value)
                        }
                        required
                      />
                    </div>

                    <div className="col-md-4">
                      <label className="form-label fw-bold">
                        Tope máximo ($)
                      </label>
                      <input
                        type="number"
                        className="form-control"
                        value={form.maxLimit}
                        onChange={(e) =>
                          handleChange("maxLimit", e.target.value)
                        }
                        placeholder="Sin límite"
                      />
                    </div>
                    <div className="col-md-4">
                      <label className="form-label fw-bold">
                        Inicio vigencia
                      </label>
                      <input
                        type="datetime-local"
                        className="form-control"
                        value={form.startDate}
                        onChange={(e) =>
                          handleChange("startDate", e.target.value)
                        }
                      />
                    </div>
                    <div className="col-md-4">
                      <label className="form-label fw-bold">Fin vigencia</label>
                      <input
                        type="datetime-local"
                        className="form-control"
                        value={form.endDate}
                        onChange={(e) =>
                          handleChange("endDate", e.target.value)
                        }
                      />
                    </div>

                    <div className="col-md-12">
                      <div className="form-check">
                        <input
                          type="checkbox"
                          className="form-check-input"
                          id="isStackable"
                          checked={form.isStackable}
                          onChange={(e) =>
                            handleChange("isStackable", e.target.checked)
                          }
                        />
                        <label
                          className="form-check-label"
                          htmlFor="isStackable"
                        >
                          Apilable (puede combinarse con otros descuentos)
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

export default DiscountsAdmin;
