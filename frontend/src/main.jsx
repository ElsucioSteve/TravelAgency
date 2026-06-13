import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.jsx";
import { ReactKeycloakProvider } from "@react-keycloak/web";
import keycloak from "./services/keycloak";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";

const eventiaInitOptions = {
  onLoad: "check-sso",
  checkLoginIframe: false,
  pkceMethod: "S256",
};

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={eventiaInitOptions}
    >
      <App />
    </ReactKeycloakProvider>
  </StrictMode>,
);
