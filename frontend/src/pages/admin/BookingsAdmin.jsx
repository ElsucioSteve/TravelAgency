import { useEffect, useState } from "react";
import bookingService from "../../services/booking.service";

const BookingsAdmin = () => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filtros
  const [filterStatus, setFilterStatus] = useState("ALL");
  const [filterEmail, setFilterEmail] = useState("");

  useEffect(() => {
    loadBookings();
  }, []);

  const loadBookings = () => {
    setLoading(true);
    bookingService
      .getAll()
      .then((res) => {
        setBookings(res.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
      });
  };

  const handleForceExpire = () => {
    if (
      !window.confirm(
        "¿Forzar revisión de expiraciones? Las reservas PENDING vencidas pasarán a EXPIRED.",
      )
    )
      return;

    import("../../services/http-common").then((mod) => {
      const http = mod.default;
      http
        .post("/bookings/expire-pending")
        .then((res) => {
          alert(`✅ ${res.data.expiredBookings} reservas expiradas`);
          loadBookings();
        })
        .catch((err) =>
          alert("❌ Error: " + (err.response?.data?.message || err.message)),
        );
    });
  };

  const formatPrice = (price) =>
    new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      maximumFractionDigits: 0,
    }).format(price);

  const formatDate = (dateStr) => {
    if (!dateStr) return "-";
    return new Date(dateStr).toLocaleString("es-CL");
  };

  const getStatusBadge = (status) => {
    const config = {
      PENDING: { color: "bg-warning text-dark", label: "Pendiente" },
      CONFIRMED: { color: "bg-success", label: "Confirmada" },
      CANCELED: { color: "bg-secondary", label: "Cancelada" },
      EXPIRED: { color: "bg-danger", label: "Expirada" },
    };
    return config[status] || { color: "bg-light text-dark", label: status };
  };

  // Aplicar filtros
  const filtered = bookings.filter((b) => {
    if (filterStatus !== "ALL" && b.bookingStatus !== filterStatus)
      return false;
    if (
      filterEmail &&
      !b.user?.email?.toLowerCase().includes(filterEmail.toLowerCase())
    )
      return false;
    return true;
  });

  // Resumen rápido
  const summary = {
    total: bookings.length,
    pending: bookings.filter((b) => b.bookingStatus === "PENDING").length,
    confirmed: bookings.filter((b) => b.bookingStatus === "CONFIRMED").length,
    canceled: bookings.filter((b) => b.bookingStatus === "CANCELED").length,
    expired: bookings.filter((b) => b.bookingStatus === "EXPIRED").length,
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
        <h2>📊 Gestión de Reservas</h2>
        <button className="btn btn-outline-warning" onClick={handleForceExpire}>
          ⏰ Forzar revisión de expiraciones
        </button>
      </div>

      {/* Tarjetas resumen */}
      <div className="row g-3 mb-4">
        <div className="col-md-2">
          <div className="card bg-light shadow-sm">
            <div className="card-body py-2 text-center">
              <small className="text-muted">Total</small>
              <h4 className="mb-0">{summary.total}</h4>
            </div>
          </div>
        </div>
        <div className="col-md-2">
          <div className="card bg-warning bg-opacity-10 shadow-sm">
            <div className="card-body py-2 text-center">
              <small className="text-muted">Pendientes</small>
              <h4 className="mb-0 text-warning">{summary.pending}</h4>
            </div>
          </div>
        </div>
        <div className="col-md-2">
          <div className="card bg-success bg-opacity-10 shadow-sm">
            <div className="card-body py-2 text-center">
              <small className="text-muted">Confirmadas</small>
              <h4 className="mb-0 text-success">{summary.confirmed}</h4>
            </div>
          </div>
        </div>
        <div className="col-md-2">
          <div className="card bg-secondary bg-opacity-10 shadow-sm">
            <div className="card-body py-2 text-center">
              <small className="text-muted">Canceladas</small>
              <h4 className="mb-0 text-secondary">{summary.canceled}</h4>
            </div>
          </div>
        </div>
        <div className="col-md-2">
          <div className="card bg-danger bg-opacity-10 shadow-sm">
            <div className="card-body py-2 text-center">
              <small className="text-muted">Expiradas</small>
              <h4 className="mb-0 text-danger">{summary.expired}</h4>
            </div>
          </div>
        </div>
      </div>

      {/* Filtros */}
      <div className="card shadow-sm mb-3">
        <div className="card-body">
          <div className="row g-3">
            <div className="col-md-4">
              <label className="form-label fw-bold">Filtrar por estado</label>
              <select
                className="form-select"
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
              >
                <option value="ALL">Todas</option>
                <option value="PENDING">Pendientes</option>
                <option value="CONFIRMED">Confirmadas</option>
                <option value="CANCELED">Canceladas</option>
                <option value="EXPIRED">Expiradas</option>
              </select>
            </div>
            <div className="col-md-4">
              <label className="form-label fw-bold">
                Filtrar por email del cliente
              </label>
              <input
                type="text"
                className="form-control"
                placeholder="ej: juan@gmail.com"
                value={filterEmail}
                onChange={(e) => setFilterEmail(e.target.value)}
              />
            </div>
            <div className="col-md-4 d-flex align-items-end">
              <button
                className="btn btn-outline-secondary w-100"
                onClick={() => {
                  setFilterStatus("ALL");
                  setFilterEmail("");
                }}
              >
                ✖ Limpiar filtros
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
                <th>Código</th>
                <th>Cliente</th>
                <th>Paquete</th>
                <th>Fecha reserva</th>
                <th>Pasajeros</th>
                <th>Bruto</th>
                <th>Desc.</th>
                <th>Final</th>
                <th>Estado</th>
                <th>Expira</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 && (
                <tr>
                  <td colSpan="10" className="text-center text-muted py-4">
                    No hay reservas que coincidan con los filtros
                  </td>
                </tr>
              )}
              {filtered.map((b) => {
                const status = getStatusBadge(b.bookingStatus);
                return (
                  <tr key={b.id}>
                    <td>
                      <strong>{b.bookingCode}</strong>
                    </td>
                    <td>
                      <small>{b.user?.fullName}</small>
                      <br />
                      <small className="text-muted">{b.user?.email}</small>
                    </td>
                    <td>
                      <small>{b.travelPackage?.name}</small>
                      <br />
                      <small className="text-muted">
                        📍 {b.travelPackage?.destination}
                      </small>
                    </td>
                    <td>
                      <small>{formatDate(b.bookingDate)}</small>
                    </td>
                    <td>{b.passengerCount}</td>
                    <td>
                      <small>{formatPrice(b.grossAmount)}</small>
                    </td>
                    <td>
                      <small className="text-success">
                        -{formatPrice(b.discountAmount || 0)}
                      </small>
                    </td>
                    <td>
                      <strong className="text-primary">
                        {formatPrice(b.finalAmount)}
                      </strong>
                    </td>
                    <td>
                      <span className={`badge ${status.color}`}>
                        {status.label}
                      </span>
                    </td>
                    <td>
                      {b.bookingStatus === "PENDING" ? (
                        <small className="text-warning">
                          {formatDate(b.paymentExpirationDate)}
                        </small>
                      ) : (
                        <small className="text-muted">-</small>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      <small className="text-muted d-block mt-2">
        Mostrando {filtered.length} de {bookings.length} reservas
      </small>
    </div>
  );
};

export default BookingsAdmin;
