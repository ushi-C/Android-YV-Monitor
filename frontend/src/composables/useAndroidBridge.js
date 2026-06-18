const BRIDGE_NAME = 'YVMonitorAndroid'
const callbacks = new Map()
let callbackSeq = 0

function getBridge() {
  return typeof window !== 'undefined' ? window[BRIDGE_NAME] : null
}

export function isAndroidBridgeAvailable() {
  const bridge = getBridge()
  return !!bridge && typeof bridge.call === 'function'
}

if (typeof window !== 'undefined') {
  window.__yvAndroidBridgeResolve = function resolveAndroidBridge(callbackId, response) {
    const callback = callbacks.get(callbackId)
    if (!callback) return
    callbacks.delete(callbackId)

    if (response && response.ok) {
      callback.resolve(response.data)
    } else {
      callback.reject(new Error(response?.error || 'Android bridge call failed'))
    }
  }
}

export function callAndroid(method, payload = {}) {
  const bridge = getBridge()
  if (!bridge || typeof bridge.call !== 'function') {
    return Promise.reject(new Error('Android bridge is not available'))
  }

  const callbackId = `android-${Date.now()}-${++callbackSeq}`
  return new Promise((resolve, reject) => {
    callbacks.set(callbackId, { resolve, reject })
    try {
      bridge.call(method, JSON.stringify(payload || {}), callbackId)
    } catch (error) {
      callbacks.delete(callbackId)
      reject(error)
    }
  })
}
