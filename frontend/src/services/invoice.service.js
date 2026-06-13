import httpClient from "./http-common";

const getAll = () => httpClient.get("/invoices");
const getById = (id) => httpClient.get(`/invoices/${id}`);
const getByFolio = (folio) => httpClient.get(`/invoices/folio/${folio}`);
const getByPaymentId = (paymentId) =>
  httpClient.get(`/invoices/payment/${paymentId}`);

export default { getAll, getById, getByFolio, getByPaymentId };
