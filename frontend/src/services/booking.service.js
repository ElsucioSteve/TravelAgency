import httpClient from "./http-common";

const getAll = () => httpClient.get("/bookings");
const getById = (id) => httpClient.get(`/bookings/${id}`);
const getMyBookings = () => httpClient.get("/bookings/my-bookings");
const getByUserId = (userId) => httpClient.get(`/bookings/user/${userId}`);
const calculatePreview = (data) =>
  httpClient.post("/bookings/calculate-preview", data);
const create = (data) => httpClient.post("/bookings", data);
const cancel = (id) => httpClient.delete(`/bookings/${id}`);

export default {
  getAll,
  getById,
  getMyBookings,
  getByUserId,
  calculatePreview,
  create,
  cancel,
};
