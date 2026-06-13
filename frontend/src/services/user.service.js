import httpClient from "./http-common";

const getAll = () => httpClient.get("/users");
const getById = (id) => httpClient.get(`/users/${id}`);
const create = (data) => httpClient.post("/users", data);
const update = (id, data) => httpClient.put(`/users/${id}`, data);
const remove = (id) => httpClient.delete(`/users/${id}`);

// Endpoints del usuario actual
const getMe = () => httpClient.get("/users/me");
const updateMe = (data) => httpClient.put("/users/me", data);

export default { getAll, getById, create, update, remove, getMe, updateMe };
