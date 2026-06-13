import axios from "axios";
import keycloak from "./keycloak";

const backendServer = import.meta.env.VITE_BACKEND_SERVER || "localhost";
const backendPort = import.meta.env.VITE_BACKEND_PORT || "8090";

const httpClient = axios.create({
  baseURL: `http://${backendServer}:${backendPort}/api/v1`,
  headers: {
    "Content-type": "application/json",
  },
});

httpClient.interceptors.request.use(
  (config) => {
    if (keycloak.token) {
      config.headers["Authorization"] = `Bearer ${keycloak.token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

export default httpClient;
