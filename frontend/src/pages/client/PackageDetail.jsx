import { useEffect, useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import travelPackageService from "../../services/travelPackage.service";
import bookingService from "../../services/booking.service";
import discountService from "../../services/discount.service";

const PackageDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [pkg, setPkg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Formulario de reserva
  const [passengerCount, setPassengerCount] = useState(1);
  const [passengers, setPassengers] = useState([
    { fullName: "", idDocument: "", birthDate: "", nationality: "" },
  ]);

  // Promociones disponibles
  const [availablePromos, setAvailablePromos] = useState([]);
  const [selectedPromoId, setSelectedPromoId] = useState("");

  // Preview de precio
  const [preview, setPreview] = useState(null);
  const [previewLoading, setPreviewLoading] = useState(false);

  // Estado de creación de reserva
  const [reserving, setReserving] = useState(false);

  useEffect(() => {
    loadPackage();
  }, [id]);

  useEffect(() => {
    discountService
      .getActivePromos()
      .then((res) => setAvailablePromos(res.data))
      .catch((err) => console.error("Error cargando promos:", err));
  }, []);

  const loadPackage = () => {
    travelPackageService
      .getById(id)
      .then((res) => {
        setPkg(res.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("No se pudo cargar el paquete");
        setLoading(false);
      });
  };

  // Cuando cambia el número de pasajeros, ajustamos la lista
  const handlePassengerCountChange = (newCount) => {
    const count = parseInt(newCount) || 1;
    setPassengerCount(count);

    if (count > passengers.length) {
      // Agregar pasajeros vacíos
      const toAdd = count - passengers.length;
      const newPassengers = [
        ...passengers,
        ...Array(toAdd).fill({
          fullName: "",
          idDocument: "",
          birthDate: "",
          nationality: "",
        }),
      ];
      setPassengers(newPassengers);
    } else if (count < passengers.length) {
      // Quitar pasajeros del final
      setPassengers(passengers.slice(0, count));
    }
  };

  const handlePassengerChange = (index, field, value) => {
    const updated = [...passengers];
    updated[index] = { ...updated[index], [field]: value };
    setPassengers(updated);
  };

  const handlePreview = () => {
    setPreviewLoading(true);
    bookingService
      .calculatePreview({
        packageId: parseInt(id),
        passengerCount: passengerCount,
        promoDiscountId: selectedPromoId ? parseInt(selectedPromoId) : null,
      })
      .then((res) => {
        setPreview(res.data);
        setPreviewLoading(false);
      })
      .catch((err) => {
        console.error(err);
        alert(
          "Error al calcular precio: " + (err.response?.data || err.message),
        );
        setPreviewLoading(false);
      });
  };

  const handleReserve = () => {
    // Validar que todos los pasajeros tengan al menos nombre
    const incompletePassengers = passengers.filter(
      (p) => !p.fullName || p.fullName.trim() === "",
    );
    if (incompletePassengers.length > 0) {
      alert("⚠️ Debes completar el nombre de todos los pasajeros");
      return;
    }

    if (!window.confirm("¿Confirmar la creación de la reserva?")) return;

    setReserving(true);
    bookingService
      .create({
        packageId: parseInt(id),
        passengerCount: passengerCount,
        passengers: passengers,
        promoDiscountId: selectedPromoId ? parseInt(selectedPromoId) : null,
      })
      .then((res) => {
        alert(
          `✅ Reserva creada con éxito!\nCódigo: ${res.data.bookingCode}\n\nTienes 24h para pagarla.`,
        );
        navigate("/my-bookings");
      })
      .catch((err) => {
        console.error(err);
        alert(
          "❌ Error al crear reserva: " +
            (err.response?.data?.message || err.response?.data || err.message),
        );
        setReserving(false);
      });
  };

  const formatPrice = (price) =>
    new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      maximumFractionDigits: 0,
    }).format(price);

  if (loading) {
    return (
      <div className="text-center py-5">
        <div className="spinner-border text-primary"></div>
        <p className="mt-2">Cargando paquete...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div>
        <div className="alert alert-danger">{error}</div>
        <Link to="/" className="btn btn-secondary">
          ← Volver al catálogo
        </Link>
      </div>
    );
  }

  if (!pkg) return null;

  const canReserve =
    pkg.packageStatus === "AVAILABLE" && pkg.availableSlots >= passengerCount;

  // Helper: la promo seleccionada actual (si la hay)
  const selectedPromo = selectedPromoId
    ? availablePromos.find((p) => p.id === parseInt(selectedPromoId))
    : null;

  return (
    <div>
      <Link to="/" className="btn btn-outline-secondary btn-sm mb-3">
        ← Volver al catálogo
      </Link>

      {/* INFO DEL PAQUETE */}
      <div className="card shadow-sm mb-4">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start mb-3">
            <div>
              <h2 className="mb-1">{pkg.name}</h2>
              <h5 className="text-muted">📍 {pkg.destination}</h5>
            </div>
            <div className="text-end">
              <h3 className="text-primary mb-0">
                {formatPrice(pkg.basePrice)}
              </h3>
              <small className="text-muted">por persona</small>
            </div>
          </div>

          <div className="row g-3 mb-3">
            <div className="col-md-3">
              <small className="text-muted d-block">📅 Salida</small>
              <strong>{pkg.startDate}</strong>
            </div>
            <div className="col-md-3">
              <small className="text-muted d-block">🏁 Regreso</small>
              <strong>{pkg.endDate}</strong>
            </div>
            <div className="col-md-2">
              <small className="text-muted d-block">⏱️ Duración</small>
              <strong>{pkg.durationDays} días</strong>
            </div>
            <div className="col-md-2">
              <small className="text-muted d-block">👥 Cupos</small>
              <strong className={pkg.availableSlots < 5 ? "text-danger" : ""}>
                {pkg.availableSlots} disponibles
              </strong>
            </div>
            <div className="col-md-2">
              <small className="text-muted d-block">📊 Estado</small>
              <span
                className={`badge ${
                  pkg.packageStatus === "AVAILABLE"
                    ? "bg-success"
                    : "bg-secondary"
                }`}
              >
                {pkg.packageStatus}
              </span>
            </div>
          </div>

          <hr />

          <h6 className="text-muted">Descripción</h6>
          <p>{pkg.description}</p>

          {pkg.includedServices && (
            <>
              <h6 className="text-muted">Servicios incluidos</h6>
              <p>{pkg.includedServices}</p>
            </>
          )}

          {pkg.conditions && (
            <>
              <h6 className="text-muted">Condiciones</h6>
              <p>{pkg.conditions}</p>
            </>
          )}

          {pkg.restrictions && (
            <>
              <h6 className="text-muted">Restricciones</h6>
              <p>{pkg.restrictions}</p>
            </>
          )}
        </div>
      </div>

      {/* FORMULARIO DE RESERVA */}
      {pkg.packageStatus !== "AVAILABLE" && (
        <div className="alert alert-warning">
          ⚠️ Este paquete no está disponible para reservar (estado:{" "}
          {pkg.packageStatus}).
        </div>
      )}

      {pkg.packageStatus === "AVAILABLE" && (
        <div className="card shadow-sm">
          <div className="card-header bg-primary text-white">
            <h5 className="mb-0">📝 Reservar este paquete</h5>
          </div>
          <div className="card-body">
            <div className="mb-3">
              <label className="form-label fw-bold">Número de pasajeros</label>
              <input
                type="number"
                min="1"
                max={pkg.availableSlots}
                className="form-control"
                value={passengerCount}
                onChange={(e) => handlePassengerCountChange(e.target.value)}
              />
              <small className="text-muted">
                Máximo {pkg.availableSlots} (cupos disponibles)
              </small>
            </div>

            <h6 className="mt-4 mb-3">👥 Datos de los pasajeros</h6>
            {passengers.map((p, idx) => (
              <div key={idx} className="card mb-2 bg-light">
                <div className="card-body py-2">
                  <small className="text-muted">Pasajero #{idx + 1}</small>
                  <div className="row g-2">
                    <div className="col-md-4">
                      <input
                        type="text"
                        className="form-control form-control-sm"
                        placeholder="Nombre completo *"
                        value={p.fullName}
                        onChange={(e) =>
                          handlePassengerChange(idx, "fullName", e.target.value)
                        }
                      />
                    </div>
                    <div className="col-md-3">
                      <input
                        type="text"
                        className="form-control form-control-sm"
                        placeholder="RUT/Pasaporte"
                        value={p.idDocument}
                        onChange={(e) =>
                          handlePassengerChange(
                            idx,
                            "idDocument",
                            e.target.value,
                          )
                        }
                      />
                    </div>
                    <div className="col-md-3">
                      <input
                        type="date"
                        className="form-control form-control-sm"
                        value={p.birthDate}
                        onChange={(e) =>
                          handlePassengerChange(
                            idx,
                            "birthDate",
                            e.target.value,
                          )
                        }
                      />
                    </div>
                    <div className="col-md-2">
                      <input
                        type="text"
                        className="form-control form-control-sm"
                        placeholder="Nacionalidad"
                        value={p.nationality}
                        onChange={(e) =>
                          handlePassengerChange(
                            idx,
                            "nationality",
                            e.target.value,
                          )
                        }
                      />
                    </div>
                  </div>
                </div>
              </div>
            ))}

            {/* SELECTOR DE PROMO */}
            {availablePromos.length > 0 && (
              <div className="mb-3 mt-4">
                <label className="form-label fw-bold">
                  🎟️ Descuento promocional (opcional)
                </label>
                <select
                  className="form-select"
                  value={selectedPromoId}
                  onChange={(e) => setSelectedPromoId(e.target.value)}
                >
                  <option value="">
                    -- Sin promoción (aplicar descuentos automáticos) --
                  </option>
                  {availablePromos.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} (
                      {p.valueType === "PERCENTAGE"
                        ? `${p.discountValue}%`
                        : `$${p.discountValue}`}
                      ){!p.isStackable && " — Exclusivo"}
                    </option>
                  ))}
                </select>
                {selectedPromo && !selectedPromo.isStackable && (
                  <small className="text-warning d-block mt-1">
                    ⚠️ Esta promoción es exclusiva: reemplazará a los descuentos
                    automáticos (grupo, frecuente, multi-paquete).
                  </small>
                )}
              </div>
            )}

            {/* PREVIEW DE PRECIO */}
            <div className="mt-4">
              <button
                type="button"
                className="btn btn-outline-info me-2"
                onClick={handlePreview}
                disabled={previewLoading}
              >
                {previewLoading ? "Calculando..." : "💰 Calcular precio"}
              </button>
            </div>

            {preview && (
              <div className="alert alert-info mt-3">
                <h6 className="mb-2">📊 Resumen de precio</h6>
                <div className="d-flex justify-content-between">
                  <span>Monto bruto:</span>
                  <strong>{formatPrice(preview.grossAmount)}</strong>
                </div>
                {preview.discountAmount > 0 && (
                  <>
                    <div className="d-flex justify-content-between text-success">
                      <span>Descuento ({preview.discountPercent}%):</span>
                      <strong>-{formatPrice(preview.discountAmount)}</strong>
                    </div>
                    {preview.discountReasons && (
                      <small className="text-muted d-block">
                        🎟️ {preview.discountReasons}
                      </small>
                    )}
                  </>
                )}
                <hr className="my-2" />
                <div className="d-flex justify-content-between fs-5">
                  <strong>Total a pagar:</strong>
                  <strong className="text-primary">
                    {formatPrice(preview.finalAmount)}
                  </strong>
                </div>
              </div>
            )}

            <hr />

            <button
              type="button"
              className="btn btn-success btn-lg w-100"
              onClick={handleReserve}
              disabled={!canReserve || reserving}
            >
              {reserving ? "Reservando..." : "✅ Confirmar Reserva"}
            </button>
            <small className="text-muted d-block mt-2 text-center">
              Tendrás 24 horas para realizar el pago una vez creada la reserva.
            </small>
          </div>
        </div>
      )}
    </div>
  );
};

export default PackageDetail;
