import httpClient from "./http-common";

const getAll = () => httpClient.get("/payments");
const getById = (id) => httpClient.get(`/payments/${id}`);
const getByBookingId = (bookingId) =>
  httpClient.get(`/payments/booking/${bookingId}`);
const processPayment = (bookingId, data) =>
  httpClient.post(`/payments/booking/${bookingId}`, data);

export default { getAll, getById, getByBookingId, processPayment };
