import { useEffect, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import bookingService from "../../services/booking.service";
import paymentService from "../../services/payment.service";

const BookingPayment = () => {
  const { bookingId } = useParams();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);

  // Datos del formulario
  const [cardNumber, setCardNumber] = useState("");
  const [expiration, setExpiration] = useState("");
  const [cvv, setCvv] = useState("");
  const [invoiceType, setInvoiceType] = useState("RECEIPT");

  useEffect(() => {
    bookingService
      .getById(bookingId)
      .then((res) => {
        setBooking(res.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        alert("No se pudo cargar la reserva");
        navigate("/my-bookings");
      });
  }, [bookingId, navigate]);

  const handlePay = (e) => {
    e.preventDefault();

    // Validación 1: Número de tarjeta
    if (cardNumber.replace(/\s/g, "").length < 13) {
      alert("Número de tarjeta inválido");
      return;
    }

    // Validación 2: Formato MM/YY
    if (!/^\d{2}\/\d{2}$/.test(expiration)) {
      alert("Fecha de expiración debe tener formato MM/YY");
      return;
    }

    // Validación 3: Mes válido (01-12)
    const [monthStr, yearStr] = expiration.split("/");
    const month = parseInt(monthStr);
    const year = parseInt(yearStr) + 2000; // YY → 20YY

    if (month < 1 || month > 12) {
      alert("Mes inválido. Debe estar entre 01 y 12");
      return;
    }

    // Validación 4: Tarjeta NO vencida
    // Una tarjeta de 05/26 expira el último día de mayo 2026
    const expirationDate = new Date(year, month, 0, 23, 59, 59);
    // month en Date va de 0-11, pero usando "month" sin restar 1 y day=0,
    // obtenemos el último día del mes correcto.
    const now = new Date();

    if (expirationDate < now) {
      alert(
        `❌ Tarjeta vencida. La tarjeta expiró en ${monthStr}/${yearStr}. Por favor usa una tarjeta vigente.`,
      );
      return;
    }

    // Validación 5: CVV
    if (!/^\d{3,4}$/.test(cvv)) {
      alert("CVV debe ser de 3 o 4 dígitos");
      return;
    }

    if (
      !window.confirm(`¿Confirmar pago de ${formatPrice(booking.finalAmount)}?`)
    )
      return;

    setPaying(true);
    paymentService
      .processPayment(bookingId, {
        amount: booking.finalAmount,
        cardNumber: cardNumber,
        cardExpirationDate: expiration,
        cvv: cvv,
        invoiceType: invoiceType,
      })
      .then((res) => {
        alert(
          `✅ ¡Pago aprobado!\nTransacción: ${res.data.internalTransactionId}\nSe ha generado tu ${
            invoiceType === "RECEIPT" ? "boleta" : "factura"
          } automáticamente.`,
        );
        navigate("/my-bookings");
      })
      .catch((err) => {
        console.error(err);
        alert(
          "❌ Error al procesar pago: " +
            (err.response?.data?.message || err.response?.data || err.message),
        );
        setPaying(false);
      });
  };

  const formatPrice = (price) =>
    new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      maximumFractionDigits: 0,
    }).format(price);

  // Formatear el número de tarjeta con espacios (ej: 4111 1111 1111 1111)
  const handleCardNumberChange = (value) => {
    const clean = value.replace(/\s/g, "");
    const formatted = clean.match(/.{1,4}/g)?.join(" ") || clean;
    setCardNumber(formatted);
  };

  if (loading) {
    return (
      <div className="text-center py-5">
        <div className="spinner-border text-primary"></div>
      </div>
    );
  }

  if (!booking) return null;

  if (booking.bookingStatus !== "PENDING") {
    return (
      <div>
        <div className="alert alert-warning">
          Esta reserva no está pendiente de pago (estado:{" "}
          {booking.bookingStatus}).
        </div>
        <Link to="/my-bookings" className="btn btn-secondary">
          ← Volver a mis reservas
        </Link>
      </div>
    );
  }

  return (
    <div>
      <Link to="/my-bookings" className="btn btn-outline-secondary btn-sm mb-3">
        ← Volver a mis reservas
      </Link>

      <div className="row">
        {/* Resumen de la reserva */}
        <div className="col-md-5">
          <div className="card shadow-sm mb-3">
            <div className="card-header bg-light">
              <h6 className="mb-0">Resumen de tu reserva</h6>
            </div>
            <div className="card-body">
              <p className="mb-1">
                <small className="text-muted">Código</small>
                <br />
                <strong>{booking.bookingCode}</strong>
              </p>
              <p className="mb-1">
                <small className="text-muted">Paquete</small>
                <br />
                <strong>{booking.travelPackage?.name}</strong>
              </p>
              <p className="mb-1">
                <small className="text-muted">Destino</small>
                <br />
                {booking.travelPackage?.destination}
              </p>
              <p className="mb-1">
                <small className="text-muted">Pasajeros</small>
                <br />
                {booking.passengerCount}
              </p>
              <hr />
              <div className="d-flex justify-content-between">
                <span>Subtotal:</span>
                <span>{formatPrice(booking.grossAmount)}</span>
              </div>
              {booking.discountAmount > 0 && (
                <div className="d-flex justify-content-between text-success">
                  <span>Descuento:</span>
                  <span>-{formatPrice(booking.discountAmount)}</span>
                </div>
              )}
              <hr />
              <div className="d-flex justify-content-between fs-4 fw-bold">
                <span>Total:</span>
                <span className="text-primary">
                  {formatPrice(booking.finalAmount)}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Formulario de pago */}
        <div className="col-md-7">
          <div className="card shadow-sm">
            <div className="card-header bg-primary text-white">
              <h5 className="mb-0">💳 Información de pago</h5>
            </div>
            <div className="card-body">
              <div className="alert alert-info small">
                ℹ️ Sistema de pago simulado. Ingresa cualquier tarjeta de
                prueba.
              </div>

              <form onSubmit={handlePay}>
                <div className="mb-3">
                  <label className="form-label fw-bold">
                    Número de tarjeta
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="4111 1111 1111 1111"
                    maxLength="19"
                    value={cardNumber}
                    onChange={(e) => handleCardNumberChange(e.target.value)}
                    required
                  />
                </div>

                <div className="row">
                  <div className="col-md-6 mb-3">
                    <label className="form-label fw-bold">
                      Vencimiento (MM/YY)
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="12/28"
                      maxLength="5"
                      value={expiration}
                      onChange={(e) => setExpiration(e.target.value)}
                      required
                    />
                  </div>
                  <div className="col-md-6 mb-3">
                    <label className="form-label fw-bold">CVV</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="123"
                      maxLength="4"
                      value={cvv}
                      onChange={(e) => setCvv(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="mb-3">
                  <label className="form-label fw-bold">
                    Tipo de comprobante
                  </label>
                  <div>
                    <div className="form-check form-check-inline">
                      <input
                        className="form-check-input"
                        type="radio"
                        name="invoiceType"
                        value="RECEIPT"
                        checked={invoiceType === "RECEIPT"}
                        onChange={(e) => setInvoiceType(e.target.value)}
                      />
                      <label className="form-check-label">📄 Boleta</label>
                    </div>
                    <div className="form-check form-check-inline">
                      <input
                        className="form-check-input"
                        type="radio"
                        name="invoiceType"
                        value="INVOICE"
                        checked={invoiceType === "INVOICE"}
                        onChange={(e) => setInvoiceType(e.target.value)}
                      />
                      <label className="form-check-label">🏢 Factura</label>
                    </div>
                  </div>
                </div>

                <button
                  type="submit"
                  className="btn btn-success btn-lg w-100"
                  disabled={paying}
                >
                  {paying
                    ? "Procesando..."
                    : `💳 Pagar ${formatPrice(booking.finalAmount)}`}
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default BookingPayment;
