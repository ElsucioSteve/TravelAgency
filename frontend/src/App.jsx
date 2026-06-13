import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar";
import PrivateRoute from "./components/PrivateRoute";
import PublicRoute from "./components/PublicRoute";

// Publico
import Register from "./pages/Register";

// Compartido (autenticado)
import MyProfile from "./pages/MyProfile";

// Cliente
import PackageCatalog from "./pages/client/PackageCatalog";
import PackageDetail from "./pages/client/PackageDetail";
import MyBookings from "./pages/client/MyBookings";
import BookingPayment from "./pages/client/BookingPayment";
import InvoiceView from "./pages/client/InvoiceView";

// Admin
import PackagesAdmin from "./pages/admin/PackagesAdmin";
import DiscountsAdmin from "./pages/admin/DiscountsAdmin";
import BookingsAdmin from "./pages/admin/BookingsAdmin";
import UsersAdmin from "./pages/admin/UsersAdmin";
import Reports from "./pages/admin/Reports";

function App() {
  return (
    <Router>
      <Navbar />
      <div className="container mt-4">
        <Routes>
          {/* PUBLICO: Registro (sin login) */}
          <Route
            path="/register"
            element={
              <PublicRoute>
                <Register />
              </PublicRoute>
            }
          />

          {/* CLIENTE */}
          <Route
            path="/"
            element={
              <PrivateRoute>
                <PackageCatalog />
              </PrivateRoute>
            }
          />
          <Route
            path="/packages/:id"
            element={
              <PrivateRoute>
                <PackageDetail />
              </PrivateRoute>
            }
          />
          <Route
            path="/my-bookings"
            element={
              <PrivateRoute>
                <MyBookings />
              </PrivateRoute>
            }
          />
          <Route
            path="/payment/:bookingId"
            element={
              <PrivateRoute>
                <BookingPayment />
              </PrivateRoute>
            }
          />
          <Route
            path="/invoice/:id"
            element={
              <PrivateRoute>
                <InvoiceView />
              </PrivateRoute>
            }
          />

          {/* COMPARTIDO */}
          <Route
            path="/profile"
            element={
              <PrivateRoute>
                <MyProfile />
              </PrivateRoute>
            }
          />

          {/* ADMIN */}
          <Route
            path="/admin/packages"
            element={
              <PrivateRoute role="ADMIN">
                <PackagesAdmin />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/discounts"
            element={
              <PrivateRoute role="ADMIN">
                <DiscountsAdmin />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/bookings"
            element={
              <PrivateRoute role="ADMIN">
                <BookingsAdmin />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <PrivateRoute role="ADMIN">
                <UsersAdmin />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/reports"
            element={
              <PrivateRoute role="ADMIN">
                <Reports />
              </PrivateRoute>
            }
          />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
