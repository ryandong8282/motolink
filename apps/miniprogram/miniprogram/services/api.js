const config = require('../config/index');
const sessionStore = require('./session');

function request({ path, method = 'GET', data }) {
  return new Promise((resolve, reject) => {
    const session = sessionStore.getSession();
    wx.request({
      url: `${config.apiBaseUrl}${path}`,
      method,
      data,
      header: session
        ? { Authorization: `Bearer ${session.token}` }
        : {},
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data);
          return;
        }
        if (response.statusCode === 401) {
          sessionStore.clearSession();
        }
        const message = response.data && response.data.message
          ? response.data.message
          : `请求失败（${response.statusCode}）`;
        const error = new Error(message);
        error.statusCode = response.statusCode;
        error.code = response.data && response.data.code;
        reject(error);
      },
      fail(error) {
        reject(new Error(error.errMsg || '无法连接服务器'));
      },
    });
  });
}

module.exports = {
  devLogin(nickname) {
    return request({
      path: '/api/v1/auth/dev-login',
      method: 'POST',
      data: { nickname },
    });
  },

  createTeam(data) {
    return request({ path: '/api/v1/teams', method: 'POST', data });
  },

  joinTeam(roomCode) {
    return request({
      path: '/api/v1/teams/join',
      method: 'POST',
      data: { roomCode },
    });
  },

  listTeams() {
    return request({ path: '/api/v1/teams' });
  },

  getTeam(teamId) {
    return request({ path: `/api/v1/teams/${teamId}` });
  },

  leaveTeam(teamId) {
    return request({ path: `/api/v1/teams/${teamId}/leave`, method: 'POST' });
  },

  updateLocation(location) {
    return request({
      path: '/api/v1/locations/me',
      method: 'POST',
      data: location,
    });
  },

  getNearby(latitude, longitude, radiusMeters) {
    const query = [
      `latitude=${encodeURIComponent(latitude)}`,
      `longitude=${encodeURIComponent(longitude)}`,
      `radiusMeters=${encodeURIComponent(radiusMeters)}`,
    ].join('&');
    return request({ path: `/api/v1/locations/nearby?${query}` });
  },

  getRtcTicket(teamId) {
    return request({ path: `/api/v1/teams/${teamId}/rtc-ticket` });
  },
};
