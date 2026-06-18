/**
 * useBackground.js
 * ─────────────────
 * 背景图片持久化 composable。
 *
 * 存储策略：
 *   Android WebView 使用 IndexedDB 保存完整 base64，无 5 MB localStorage 限制。
 *
 * 原 Win10/FastAPI 背景 API 已移除。
 */

import { ref } from 'vue'
import { appState } from '../stores/appState'

// ── IndexedDB helpers ──────────────────────────────────────────────────────

const IDB_NAME = 'yv-store'
const IDB_STORE = 'kv'
const IDB_KEY = 'user_background'

function openIdb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(IDB_NAME, 1)
    req.onupgradeneeded = (e) => {
      e.target.result.createObjectStore(IDB_STORE)
    }
    req.onsuccess = (e) => resolve(e.target.result)
    req.onerror = () => reject(req.error)
  })
}

async function idbGet(key) {
  const db = await openIdb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IDB_STORE, 'readonly')
    const req = tx.objectStore(IDB_STORE).get(key)
    req.onsuccess = () => resolve(req.result ?? null)
    req.onerror = () => reject(req.error)
  })
}

async function idbSet(key, value) {
  const db = await openIdb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IDB_STORE, 'readwrite')
    tx.objectStore(IDB_STORE).put(value, key)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

async function idbDelete(key) {
  const db = await openIdb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(IDB_STORE, 'readwrite')
    tx.objectStore(IDB_STORE).delete(key)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

// ── Android storage strategy ────────────────────────────────────────────────

// Android WebView keeps user-selected background images in IndexedDB.
// The former FastAPI background endpoint has been removed with the Windows runtime.

// ── Composable ─────────────────────────────────────────────────────────────

export function useBackground() {
  const saving = ref(false)
  const error = ref('')

  /**
   * 从持久化存储恢复背景（在 onMounted 中调用）。
   */
  async function restoreBackground() {
    try {
      const dataUrl = await idbGet(IDB_KEY)
      if (dataUrl) {
        appState.userBackgroundUrl = dataUrl
      }
    } catch (err) {
      console.warn('[useBackground] restoreBackground failed:', err)
    }
  }

  /**
   * 处理文件选择事件，保存背景。
   * @param {File} file  - 用户选择的图片文件
   * @returns {Promise<boolean>} 是否成功
   */
  async function saveBackground(file) {
    if (!file) return false
    saving.value = true
    error.value = ''

    try {
      const dataUrl = await fileToDataUrl(file)

      // 先更新界面（立即生效）
      appState.userBackgroundUrl = dataUrl

      // Android WebView 持久化
      await idbSet(IDB_KEY, dataUrl)

      return true
    } catch (err) {
      console.error('[useBackground] saveBackground failed:', err)
      error.value = '背景保存失败，请重试'
      return false
    } finally {
      saving.value = false
    }
  }

  /**
   * 清除背景。
   */
  async function clearBackground() {
    appState.userBackgroundUrl = ''
    try {
      await idbDelete(IDB_KEY)
    } catch (err) {
      console.warn('[useBackground] clearBackground failed:', err)
    }
  }

  return { saving, error, restoreBackground, saveBackground, clearBackground }
}

// ── Util ───────────────────────────────────────────────────────────────────

function fileToDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(/** @type {string} */ (reader.result))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}
