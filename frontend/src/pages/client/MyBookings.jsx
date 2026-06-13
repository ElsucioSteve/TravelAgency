import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import bookingService from "../../services/booking.service";
import invoiceService from "../../services/invoice.service";

const MyBookings = () => {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    loadBookings();
  }, []);

  const loadBookings = () => {
    setLoading(true);
    bookingService
      .getMyBookings()
      .then((res) => {
        setBookings(res.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Error al cargar tus reservas");
        setLoading(false);
      });
  };

  const handleCancel = (bookingId) => {
    if (!window.confirm("¿Seguro que deseas cancelar esta reserva?")) return;

    bookingService
      .cancel(bookingId)
      .then(() => {
        alert("✅ Reserva cancelada");
        loadBookings();
      })
      .catch((err) => {
        console.error(err);
        alert(
          "❌ Error al cancelar: " +
            (err.response?.data?.message || err.response?.data || err.message),
        );
      });
  };

  const handleViewInvoice = (bookingId) => {
    // Buscar la factura asociada al pago de esta reserva
    bookingService.getById(bookingId).then(() => {
      // Necesitamos primero el paymentId. Lo tomamos del endpoint /payments/booking/{id}
      import("../../services/payment.service").then((module) => {
        const paymentService = module.default;
        paymentService
          .getByBookingId(bookingId)
          .then((res) => {
            const paymentId = res.data.id;
            invoiceService
              .getByPaymentId(paymentId)
              .then((invRes) => navigate(`/invoice/${invRes.data.id}`))
              .catch(() => alert("No se encontró factura"));
          })
          .catch(() => alert("No se encontró pago"));
      });
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
      PENDING: {
        color: "bg-warning text-dark",
        icon: "⏳",
        label: "Pendiente de pago",
      },
      CONFIRMED: { color: "bg-success", icon: "✅", label: "Confirmada" },
      CANCELED: { color: "bg-secondary", icon: "❌", label: "Cancelada" },
      EXPIRED: { color: "bg-danger", icon: "⏰", label: "Expirada" },
    };
    return (
      config[status] || {
        color: "bg-light text-dark",
        icon: "❓",
        label: status,
      }
    );
  };

  if (loading) {
    return (
      <div className="text-center py-5">
        <div className="spinner-border text-primary"></div>
        <p className="mt-2">Cargando tus reservas...</p>
      </div>
    );
  }

  if (error) return <div className="alert alert-danger">{error}</div>;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>📋 Mis Reservas</h2>
        <Link to="/" className="btn btn-outline-primary">
          + Nueva Reserva
        </Link>
      </div>

      {bookings.length === 0 && (
        <div className="alert alert-info text-center">
          😢 Aún no tienes reservas. <Link to="/">Explora el catálogo</Link> y
          reserva tu primer viaje.
        </div>
      )}

      <div className="row g-4">
        {bookings.map((b) => {
          const status = getStatusBadge(b.bookingStatus);
          return (
            <div key={b.id} className="col-md-6">
              <div className="card shadow-sm h-100">
                <div className="card-header d-flex justify-content-between align-items-center">
                  <span className="fw-bold">Código: {b.bookingCode}</span>
                  <span className={`badge ${status.color}`}>
                    {status.icon} {status.label}
                  </span>
                </div>
                <div className="card-body">
                  <h5 className="card-title">{b.travelPackage?.name}</h5>
                  <p className="text-muted small mb-2">
                    📍 {b.travelPackage?.destination}
                  </p>

                  <div className="row small text-muted mb-2">
                    <div className="col-6">
                      📅 Viaje: {b.travelPackage?.startDate}
                    </div>
                    <div className="col-6">👥 {b.passengerCount} pasajeros</div>
                  </div>

                  <hr />

                  <div className="d-flex justify-content-between small mb-1">
                    <span>Bruto:</span>
                    <span>{formatPrice(b.grossAmount)}</span>
                  </div>
                  {b.discountAmount > 0 && (
                    <div className="d-flex justify-content-between small text-success mb-1">
                      <span>Descuento:</span>
                      <span>-{formatPrice(b.discountAmount)}</span>
                    </div>
                  )}
                  <div className="d-flex justify-content-between fw-bold fs-5 mt-2">
                    <span>Total:</span>
                    <span className="text-primary">
                      {formatPrice(b.finalAmount)}
                    </span>
                  </div>

                  {b.bookingStatus === "PENDING" && b.paymentExpirationDate && (
                    <div className="alert alert-warning small mt-2 mb-0">
                      ⏰ Pagar antes de:{" "}
                      <strong>{formatDate(b.paymentExpirationDate)}</strong>
                    </div>
                  )}
                </div>

                <div className="card-footer d-flex gap-2">
                  {b.bookingStatus === "PENDING" && (
                    <>
                      <button
                        className="btn btn-success flex-grow-1"
                        onClick={() => navigate(`/payment/${b.id}`)}
                      >
                        💳 Pagar ahora
                      </button>
                      <button
                        className="btn btn-outline-danger"
                        onClick={() => handleCancel(b.id)}
                      >
                        ❌ Cancelar
                      </button>
                    </>
                  )}

                  {b.bookingStatus === "CONFIRMED" && (
                    <button
                      className="btn btn-info flex-grow-1"
                      onClick={() => handleViewInvoice(b.id)}
                    >
                      🧾 Ver Factura
                    </button>
                  )}

                  {(b.bookingStatus === "CANCELED" ||
                    b.bookingStatus === "EXPIRED") && (
                    <button
                      className="btn btn-outline-secondary flex-grow-1"
                      disabled
                    >
                      Sin acciones disponibles
                    </button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default MyBookings;
