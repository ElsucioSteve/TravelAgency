import axios from "axios";

// Cliente axios SIN interceptor de JWT (para endpoints publicos)
const publicClient = axios.create({
  baseURL: "http://localhost:8090/api/v1",
  headers: {
    "Content-type": "application/json",
  },
});

const register = (data) => publicClient.post("/auth/register", data);

export default { register };
