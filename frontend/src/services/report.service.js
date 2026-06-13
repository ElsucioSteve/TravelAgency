import httpClient from "./http-common";

const getSalesByPeriod = (startDate, endDate) =>
  httpClient.get("/reports/sales", {
    params: { startDate, endDate },
  });

const getPackageRanking = (startDate, endDate) =>
  httpClient.get("/reports/package-ranking", {
    params: { startDate, endDate },
  });

export default { getSalesByPeriod, getPackageRanking };
