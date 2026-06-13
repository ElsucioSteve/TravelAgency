import { useState } from "react";
import reportService from "../../services/report.service";

const Reports = () => {
  // Fechas por defecto: del 1° del mes actual hasta hoy
  const today = new Date();
  const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);

  const formatForInput = (date) => date.toISOString().slice(0, 10);

  const [startDate, setStartDate] = useState(formatForInput(firstDayOfMonth));
  const [endDate, setEndDate] = useState(formatForInput(today));

  const [salesReport, setSalesReport] = useState(null);
  const [rankingReport, setRankingReport] = useState(null);
  const [loadingSales, setLoadingSales] = useState(false);
  const [loadingRanking, setLoadingRanking] = useState(false);

  const formatPrice = (price) =>
    new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      maximumFractionDigits: 0,
    }).format(price || 0);

  const formatDate = (dateStr) => {
    if (!dateStr) return "-";
    return new Date(dateStr).toLocaleString("es-CL");
  };

  // Convertir las fechas a formato ISO completo (con hora)
  const buildDateRange = () => {
    const start = `${startDate}T00:00:00`;
    const end = `${endDate}T23:59:59`;
    return { start, end };
  };

  const handleLoadSales = () => {
    if (!startDate || !endDate) {
      alert("⚠️ Selecciona ambas fechas");
      return;
    }
    if (new Date(startDate) > new Date(endDate)) {
      alert(
        "⚠️ La fecha de inicio debe ser anterior o igual a la fecha de fin",
      );
      return;
    }

    setLoadingSales(true);
    const { start, end } = buildDateRange();
    reportService
      .getSalesByPeriod(start, end)
      .then((res) => {
        setSalesReport(res.data);
        setLoadingSales(false);
      })
      .catch((err) => {
        console.error(err);
        alert("❌ Error: " + (err.response?.data?.message || err.message));
        setLoadingSales(false);
      });
  };

  const handleLoadRanking = () => {
    if (!startDate || !endDate) {
      alert("⚠️ Selecciona ambas fechas");
      return;
    }
    if (new Date(startDate) > new Date(endDate)) {
      alert("⚠️ Fechas inválidas");
      return;
    }

    setLoadingRanking(true);
    const { start, end } = buildDateRange();
    reportService
      .getPackageRanking(start, end)
      .then((res) => {
        setRankingReport(res.data);
        setLoadingRanking(false);
      })
      .catch((err) => {
        console.error(err);
        alert("❌ Error: " + (err.response?.data?.message || err.message));
        setLoadingRanking(false);
      });
  };

  const handleQuickRange = (rangeType) => {
    const t = new Date();
    let start;
    if (rangeType === "week") {
      start = new Date(t);
      start.setDate(t.getDate() - 7);
    } else if (rangeType === "month") {
      start = new Date(t);
      start.setDate(t.getDate() - 30);
    } else if (rangeType === "year") {
      start = new Date(t.getFullYear(), 0, 1);
    } else {
      start = new Date(t.getFullYear(), t.getMonth(), 1);
    }
    setStartDate(formatForInput(start));
    setEndDate(formatForInput(t));
  };

  return (
    <div>
      <h2 className="mb-4">📈 Reportes Administrativos</h2>

      {/* Selector de fechas */}
      <div className="card shadow-sm mb-4">
        <div className="card-header bg-light">
          <h6 className="mb-0">🗓️ Rango de fechas</h6>
        </div>
        <div className="card-body">
          <div className="row g-3 align-items-end">
            <div className="col-md-3">
              <label className="form-label fw-bold">Desde</label>
              <input
                type="date"
                className="form-control"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="col-md-3">
              <label className="form-label fw-bold">Hasta</label>
              <input
                type="date"
                className="form-control"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
            <div className="col-md-6">
              <label className="form-label fw-bold">Rangos rápidos</label>
              <div className="d-flex gap-2 flex-wrap">
                <button
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => handleQuickRange("week")}
                >
                  Últimos 7 días
                </button>
                <button
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => handleQuickRange("monthCurrent")}
                >
                  Este mes
                </button>
                <button
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => handleQuickRange("month")}
                >
                  Últimos 30 días
                </button>
                <button
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => handleQuickRange("year")}
                >
                  Este año
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* REPORTE 1: VENTAS */}
      <div className="card shadow-sm mb-4">
        <div className="card-header bg-primary text-white d-flex justify-content-between align-items-center">
          <h5 className="mb-0">💰 Reporte de Ventas (CONFIRMADAS)</h5>
          <button
            className="btn btn-light btn-sm"
            onClick={handleLoadSales}
            disabled={loadingSales}
          >
            {loadingSales ? "Cargando..." : "🔄 Generar reporte"}
          </button>
        </div>
        <div className="card-body">
          {!salesReport && (
            <p className="text-muted text-center py-3 mb-0">
              Click en "Generar reporte" para ver las ventas
            </p>
          )}

          {salesReport && (
            <>
              {/* Totales */}
              <div className="row g-3 mb-4">
                <div className="col-md-3">
                  <div className="card bg-light">
                    <div className="card-body py-2 text-center">
                      <small className="text-muted">Reservas</small>
                      <h4 className="mb-0">{salesReport.totalBookings}</h4>
                    </div>
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="card bg-light">
                    <div className="card-body py-2 text-center">
                      <small className="text-muted">Pasajeros</small>
                      <h4 className="mb-0">{salesReport.totalPassengers}</h4>
                    </div>
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="card bg-light">
                    <div className="card-body py-2 text-center">
                      <small className="text-muted">Descuentos</small>
                      <h4 className="mb-0 text-success">
                        -{formatPrice(salesReport.totalDiscountAmount)}
                      </h4>
                    </div>
                  </div>
                </div>
                <div className="col-md-3">
                  <div className="card bg-primary text-white">
                    <div className="card-body py-2 text-center">
                      <small>Ingresos totales</small>
                      <h4 className="mb-0">
                        {formatPrice(salesReport.totalFinalAmount)}
                      </h4>
                    </div>
                  </div>
                </div>
              </div>

              {/* Detalle */}
              <h6 className="mb-2">📋 Detalle de ventas</h6>
              <div className="table-responsive">
                <table className="table table-sm table-hover">
                  <thead className="table-dark">
                    <tr>
                      <th>Código</th>
                      <th>Fecha</th>
                      <th>Cliente</th>
                      <th>Paquete</th>
                      <th>Pax</th>
                      <th>Bruto</th>
                      <th>Desc.</th>
                      <th>Final</th>
                    </tr>
                  </thead>
                  <tbody>
                    {salesReport.items && salesReport.items.length === 0 && (
                      <tr>
                        <td colSpan="8" className="text-center text-muted py-3">
                          No hay ventas confirmadas en este período
                        </td>
                      </tr>
                    )}
                    {salesReport.items &&
                      salesReport.items.map((item) => (
                        <tr key={item.bookingId}>
                          <td>
                            <strong>{item.bookingCode}</strong>
                          </td>
                          <td>
                            <small>{formatDate(item.bookingDate)}</small>
                          </td>
                          <td>
                            <small>{item.clientName}</small>
                            <br />
                            <small className="text-muted">
                              {item.clientEmail}
                            </small>
                          </td>
                          <td>
                            <small>{item.packageName}</small>
                            <br />
                            <small className="text-muted">
                              📍 {item.destination}
                            </small>
                          </td>
                          <td>{item.passengerCount}</td>
                          <td>
                            <small>{formatPrice(item.grossAmount)}</small>
                          </td>
                          <td>
                            <small className="text-success">
                              -{formatPrice(item.discountAmount)}
                            </small>
                          </td>
                          <td>
                            <strong className="text-primary">
                              {formatPrice(item.finalAmount)}
                            </strong>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      </div>

      {/* REPORTE 2: RANKING */}
      <div className="card shadow-sm mb-4">
        <div className="card-header bg-warning text-dark d-flex justify-content-between align-items-center">
          <h5 className="mb-0">🏆 Ranking de Paquetes Más Vendidos</h5>
          <button
            className="btn btn-dark btn-sm"
            onClick={handleLoadRanking}
            disabled={loadingRanking}
          >
            {loadingRanking ? "Cargando..." : "🔄 Generar ranking"}
          </button>
        </div>
        <div className="card-body">
          {!rankingReport && (
            <p className="text-muted text-center py-3 mb-0">
              Click en "Generar ranking" para ver los paquetes más vendidos
            </p>
          )}

          {rankingReport && (
            <div className="table-responsive">
              <table className="table table-hover">
                <thead className="table-dark">
                  <tr>
                    <th style={{ width: "80px" }}>Ranking</th>
                    <th>Paquete</th>
                    <th>Destino</th>
                    <th>Reservas confirmadas</th>
                  </tr>
                </thead>
                <tbody>
                  {rankingReport.items && rankingReport.items.length === 0 && (
                    <tr>
                      <td colSpan="4" className="text-center text-muted py-3">
                        No hay datos para este período
                      </td>
                    </tr>
                  )}
                  {rankingReport.items &&
                    rankingReport.items.map((item) => (
                      <tr key={item.packageId}>
                        <td>
                          <h4 className="mb-0">
                            {item.rank === 1
                              ? "🥇"
                              : item.rank === 2
                                ? "🥈"
                                : item.rank === 3
                                  ? "🥉"
                                  : `#${item.rank}`}
                          </h4>
                        </td>
                        <td>
                          <strong>{item.packageName}</strong>
                        </td>
                        <td>📍 {item.destination}</td>
                        <td>
                          <span className="badge bg-primary fs-6">
                            {item.totalBookings}
                          </span>
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Reports;
