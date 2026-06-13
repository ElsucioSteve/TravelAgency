import httpClient from "./http-common";

const getAllVisible = () => httpClient.get("/travel-packages");
const getAll = () => httpClient.get("/travel-packages/all");
const getById = (id) => httpClient.get(`/travel-packages/${id}`);
const search = (params) =>
  httpClient.get("/travel-packages/search", { params });
const create = (data) => httpClient.post("/travel-packages", data);
const update = (id, data) => httpClient.put(`/travel-packages/${id}`, data);
const remove = (id) => httpClient.delete(`/travel-packages/${id}`);

export default {
  getAllVisible,
  getAll,
  getById,
  search,
  create,
  update,
  remove,
};
