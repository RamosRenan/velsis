import axios from 'axios'

export const api = axios.create({
  baseURL: new URL('/api', import.meta.env.VITE_API_URL || window.location.origin).href,
  withCredentials: true,
})
