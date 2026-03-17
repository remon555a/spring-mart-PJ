import apiClient from './client';

export const getProducts = async () => {
  const response = await apiClient.get('/api/products');
  return response.data;
};

export const getProduct = async (id) => {
  const response = await apiClient.get(`/api/products/${id}`);
  return response.data;
};

export const createProduct = async (productData) => {
  const response = await apiClient.post('/api/products', productData);
  return response.data;
};

export const updateProduct = async (id, productData) => {
  const response = await apiClient.put(`/api/products/${id}`, productData);
  return response.data;
};

export const deleteProduct = async (id) => {
  const response = await apiClient.delete(`/api/products/${id}`);
  return response.data;
};
