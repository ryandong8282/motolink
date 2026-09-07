function getCurrentLocation() {
  return new Promise((resolve, reject) => {
    wx.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      highAccuracyExpireTime: 4000,
      success(result) {
        resolve({
          latitude: result.latitude,
          longitude: result.longitude,
          accuracy: normalNumber(result.accuracy),
          speed: normalNumber(result.speed),
          bearing: null,
        });
      },
      fail(error) {
        reject(new Error(error.errMsg || '无法获取当前位置'));
      },
    });
  });
}

function normalNumber(value) {
  return typeof value === 'number' && value >= 0 ? value : null;
}

module.exports = {
  getCurrentLocation,
};
