import axios from "axios";
import keycloak from "./keycloak";

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8090/api/v1",
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
