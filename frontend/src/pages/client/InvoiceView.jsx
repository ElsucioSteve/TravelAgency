import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import invoiceService from "../../services/invoice.service";

const InvoiceView = () => {
  const { id } = useParams();
  const [invoice, setInvoice] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    invoiceService
      .getById(id)
      .then((res) => {
        setInvoice(res.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("No se pudo cargar la factura");
        setLoading(false);
      });
  }, [id]);

  const formatDate = (dateStr) => {
    if (!dateStr) return "-";
    return new Date(dateStr).toLocaleString("es-CL");
  };

  if (loading) {
    return (
      <div className="text-center py-5">
        <div className="spinner-border text-primary"></div>
      </div>
    );
  }

  if (error || !invoice) {
    return (
      <div>
        <div className="alert alert-danger">
          {error || "Factura no encontrada"}
        </div>
        <Link to="/my-bookings" className="btn btn-secondary">
          ← Volver
        </Link>
      </div>
    );
  }

  return (
    <div>
      <Link to="/my-bookings" className="btn btn-outline-secondary btn-sm mb-3">
        ← Volver a mis reservas
      </Link>

      <div className="card shadow">
        <div className="card-header bg-dark text-white d-flex justify-content-between align-items-center">
          <h4 className="mb-0">
            🧾 {invoice.invoiceType === "RECEIPT" ? "Boleta" : "Factura"}{" "}
            Electrónica
          </h4>
          <button
            className="btn btn-outline-light btn-sm"
            onClick={() => window.print()}
          >
            🖨️ Imprimir
          </button>
        </div>

        <div className="card-body">
          <div className="row mb-4">
            <div className="col-md-6">
              <h5 className="text-muted">Agencia de Viajes</h5>
              <small>Sistema de gestión de reservas</small>
            </div>
            <div className="col-md-6 text-end">
              <div>
                <small className="text-muted">Folio</small>
                <br />
                <h3 className="text-primary mb-0">{invoice.folioNumber}</h3>
              </div>
              <small className="text-muted">
                Emitida: {formatDate(invoice.issueDate)}
              </small>
            </div>
          </div>

          <hr />

          {/* Snapshot completo */}
          <div className="bg-light p-3 rounded mb-3">
            <h6 className="mb-3">📋 Detalle de la transacción</h6>
            <pre
              style={{
                whiteSpace: "pre-wrap",
                fontFamily: "monospace",
                fontSize: "0.9rem",
                margin: 0,
              }}
            >
              {invoice.snapshotDetails}
            </pre>
          </div>

          <div className="alert alert-success small mb-0">
            ✅ Esta {invoice.invoiceType === "RECEIPT" ? "boleta" : "factura"}{" "}
            es válida como comprobante de pago. Conserva el folio para futuras
            referencias.
          </div>
        </div>
      </div>
    </div>
  );
};

export default InvoiceView;
