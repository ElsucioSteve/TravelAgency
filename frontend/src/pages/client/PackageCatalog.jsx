import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import travelPackageService from "../../services/travelPackage.service";

const PackageCatalog = () => {
  const [packages, setPackages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filtros
  const [destination, setDestination] = useState("");
  const [minPrice, setMinPrice] = useState("");
  const [maxPrice, setMaxPrice] = useState("");
  const [startDate, setStartDate] = useState("");

  const navigate = useNavigate();

  useEffect(() => {
    loadPackages();
  }, []);

  const loadPackages = () => {
    setLoading(true);
    travelPackageService
      .getAllVisible()
      .then((res) => {
        setPackages(res.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Error al cargar los paquetes");
        setLoading(false);
      });
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setLoading(true);

    const params = {};
    if (destination) params.destination = destination;
    if (minPrice) params.minPrice = minPrice;
    if (maxPrice) params.maxPrice = maxPrice;
    if (startDate) params.startDate = startDate;

    travelPackageService
      .search(params)
      .then((res) => {
        setPackages(res.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError("Error al buscar paquetes");
        setLoading(false);
      });
  };

  const clearFilters = () => {
    setDestination("");
    setMinPrice("");
    setMaxPrice("");
    setStartDate("");
    loadPackages();
  };

  const formatPrice = (price) =>
    new Intl.NumberFormat("es-CL", {
      style: "currency",
      currency: "CLP",
      maximumFractionDigits: 0,
    }).format(price);

  const getCategoryBadge = (category) => {
    const colors = {
      ADVENTURE: "bg-success",
      BEACH: "bg-info",
      CULTURAL: "bg-warning text-dark",
      FAMILY: "bg-primary",
      LUXURY: "bg-dark",
    };
    return colors[category] || "bg-secondary";
  };

  return (
    <div>
      <h2 className="mb-4">🌍 Catálogo de Paquetes</h2>

      {/* Filtros */}
      <div className="card shadow-sm mb-4">
        <div className="card-body">
          <form onSubmit={handleSearch}>
            <div className="row g-3">
              <div className="col-md-3">
                <label className="form-label fw-bold">Destino</label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="Ej: Chile, Cancun..."
                  value={destination}
                  onChange={(e) => setDestination(e.target.value)}
                />
              </div>
              <div className="col-md-2">
                <label className="form-label fw-bold">Precio mínimo</label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="0"
                  value={minPrice}
                  onChange={(e) => setMinPrice(e.target.value)}
                />
              </div>
              <div className="col-md-2">
                <label className="form-label fw-bold">Precio máximo</label>
                <input
                  type="number"
                  className="form-control"
                  placeholder="∞"
                  value={maxPrice}
                  onChange={(e) => setMaxPrice(e.target.value)}
                />
              </div>
              <div className="col-md-3">
                <label className="form-label fw-bold">Salida desde</label>
                <input
                  type="date"
                  className="form-control"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </div>
              <div className="col-md-2 d-flex align-items-end gap-2">
                <button type="submit" className="btn btn-primary flex-grow-1">
                  🔍 Buscar
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={clearFilters}
                  title="Limpiar"
                >
                  ✖
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>

      {/* Estado de carga / error */}
      {loading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary"></div>
          <p className="mt-2">Cargando paquetes...</p>
        </div>
      )}

      {error && <div className="alert alert-danger">{error}</div>}

      {!loading && packages.length === 0 && (
        <div className="alert alert-info text-center">
          😢 No se encontraron paquetes con los filtros aplicados.
        </div>
      )}

      {/* Grid de paquetes */}
      <div className="row g-4">
        {packages.map((pkg) => (
          <div key={pkg.id} className="col-md-6 col-lg-4">
            <div className="card h-100 shadow-sm">
              <div className="card-body d-flex flex-column">
                <div className="d-flex justify-content-between align-items-start mb-2">
                  <span className={`badge ${getCategoryBadge(pkg.category)}`}>
                    {pkg.category}
                  </span>
                  <span className="badge bg-light text-dark">
                    {pkg.travelType === "NATIONAL"
                      ? "🇨🇱 Nacional"
                      : "🌎 Internacional"}
                  </span>
                </div>

                <h5 className="card-title">{pkg.name}</h5>
                <p className="text-muted small mb-2">📍 {pkg.destination}</p>

                <p className="card-text small">
                  {pkg.description?.length > 100
                    ? pkg.description.substring(0, 100) + "..."
                    : pkg.description}
                </p>

                <div className="mt-auto">
                  <div className="d-flex justify-content-between small text-muted mb-2">
                    <span>📅 {pkg.startDate}</span>
                    <span>⏱️ {pkg.durationDays} días</span>
                  </div>
                  <div className="d-flex justify-content-between small text-muted mb-3">
                    <span>👥 {pkg.availableSlots} cupos</span>
                    <span className="fw-bold text-primary fs-5">
                      {formatPrice(pkg.basePrice)}
                    </span>
                  </div>

                  <button
                    className="btn btn-primary w-100"
                    onClick={() => navigate(`/packages/${pkg.id}`)}
                  >
                    Ver detalles
                  </button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default PackageCatalog;
