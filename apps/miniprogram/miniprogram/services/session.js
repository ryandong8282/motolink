const STORAGE_KEY = 'motolink.session';

function getSession() {
  return wx.getStorageSync(STORAGE_KEY) || null;
}

function saveSession(session) {
  wx.setStorageSync(STORAGE_KEY, session);
  getApp().globalData.session = session;
}

function clearSession() {
  wx.removeStorageSync(STORAGE_KEY);
  getApp().globalData.session = null;
}

function requireSession() {
  const session = getSession();
  if (!session) {
    wx.reLaunch({ url: '/pages/login/login' });
    return null;
  }
  return session;
}

module.exports = {
  getSession,
  saveSession,
  clearSession,
  requireSession,
};
