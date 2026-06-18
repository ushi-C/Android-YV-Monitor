// Android-only API facade. Vue calls Android JavascriptInterface instead of FastAPI.

import { callAndroid } from './useAndroidBridge'

export function useApiClient() {
  async function refreshScan() {
    return callAndroid('refreshScan')
  }

  async function getStatus() {
    return callAndroid('getStatus')
  }

  async function checkChannel(query, title) {
    return callAndroid('checkChannel', { query, title: title || '' })
  }

  async function getChannels() {
    return callAndroid('getChannels')
  }

  async function getNetworkStatus() {
    return callAndroid('getNetworkStatus')
  }

  async function reportNetworkStatus(payload) {
    return callAndroid('reportNetworkStatus', payload)
  }

  async function requestNetworkCheck() {
    return callAndroid('requestNetworkCheck')
  }

  return { refreshScan, getStatus, checkChannel, getChannels, getNetworkStatus, reportNetworkStatus, requestNetworkCheck }
}
