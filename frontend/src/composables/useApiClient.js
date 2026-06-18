// API facade used by both the Windows/FastAPI build and the Android WebView build.
// Android calls are routed through JavascriptInterface -> Kotlin repositories.

import { callAndroid, isAndroidBridgeAvailable } from './useAndroidBridge'

function apiFetch(path, options) {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  return fetch(`${baseUrl}${path}`, options)
}

async function callNativeOrFetch(method, payload, fetcher) {
  if (isAndroidBridgeAvailable()) {
    return callAndroid(method, payload)
  }
  return fetcher()
}

export function useApiClient() {
  async function refreshScan() {
    return callNativeOrFetch('refreshScan', {}, () => apiFetch('/api/refresh', { method: 'POST' }))
  }

  async function getStatus() {
    return callNativeOrFetch('getStatus', {}, async () => {
      const res = await apiFetch('/api/status')
      return res.json()
    })
  }

  async function checkChannel(query, title) {
    return callNativeOrFetch('checkChannel', { query, title: title || '' }, async () => {
      const resp = await apiFetch('/api/check', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query, title: title || '' }),
      })
      return resp.json()
    })
  }

  async function getChannels() {
    return callNativeOrFetch('getChannels', {}, async () => {
      const res = await apiFetch('/api/channels')
      return res.json()
    })
  }

  async function getNetworkStatus() {
    return callNativeOrFetch('getNetworkStatus', {}, async () => {
      const res = await apiFetch('/api/network/status')
      return res.json()
    })
  }

  async function reportNetworkStatus(payload) {
    return callNativeOrFetch('reportNetworkStatus', payload, async () => {
      const res = await apiFetch('/api/network/report', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      return res.json()
    })
  }

  async function requestNetworkCheck() {
    return callNativeOrFetch('requestNetworkCheck', {}, async () => {
      const res = await apiFetch('/api/network/check', { method: 'POST' })
      return res.json()
    })
  }

  return { refreshScan, getStatus, checkChannel, getChannels, getNetworkStatus, reportNetworkStatus, requestNetworkCheck }
}
