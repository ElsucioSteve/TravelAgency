import httpClient from "./http-common";

const getAllActive = () => httpClient.get("/discounts");
const getAll = () => httpClient.get("/discounts/all");
const getById = (id) => httpClient.get(`/discounts/${id}`);
const create = (data) => httpClient.post("/discounts", data);
const update = (id, data) => httpClient.put(`/discounts/${id}`, data);
const remove = (id) => httpClient.delete(`/discounts/${id}`);

// Filtrar por criterio (PROMO, GROUP, FREQUENT, MULTI_PACKAGE)
const getActiveByCriteria = (criteria) =>
  httpClient.get("/discounts", { params: { criteria } });

const getActivePromos = () => getActiveByCriteria("PROMO");

export default {
  getAllActive,
  getAll,
  getById,
  create,
  update,
  remove,
  getActiveByCriteria,
  getActivePromos,
};
