import axios from "axios";

// Public client without JWT interceptor (for public endpoints)
const baseURL =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8090/api/v1";

const publicClient = axios.create({
  baseURL: baseURL,
  headers: {
    "Content-type": "application/json",
  },
});

const register = (data) => publicClient.post("/auth/register", data);

export default { register };
