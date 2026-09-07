const api = require('../../services/api');
const locationService = require('../../services/location');
const sessionStore = require('../../services/session');

const RADIUS_OPTIONS = [
  { label: '1 km', value: 1000 },
  { label: '3 km', value: 3000 },
  { label: '5 km', value: 5000 },
  { label: '10 km', value: 10000 },
];

Page({
  data: {
    latitude: 39.9042,
    longitude: 116.4074,
    radiusOptions: RADIUS_OPTIONS,
    radiusIndex: 1,
    nearby: [],
    markers: [],
    loading: false,
    lastUpdatedText: '尚未定位',
  },

  onShow() {
    if (!sessionStore.requireSession()) return;
    if (!this.data.loading) this.refreshNearby();
  },

  onPullDownRefresh() {
    this.refreshNearby().finally(() => wx.stopPullDownRefresh());
  },

  onRadiusChange(event) {
    this.setData({ radiusIndex: Number(event.detail.value) });
    this.refreshNearby();
  },

  onRefreshTap() {
    this.refreshNearby();
  },

  async refreshNearby() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    wx.showNavigationBarLoading();
    try {
      const location = await locationService.getCurrentLocation();
      await api.updateLocation(location);
      const radius = RADIUS_OPTIONS[this.data.radiusIndex].value;
      const nearby = await api.getNearby(
        location.latitude,
        location.longitude,
        radius,
      );
      const nearbyView = nearby.map((user) => ({
        ...user,
        avatarText: user.nickname.substring(0, 1),
        distanceText: formatDistance(user.distanceMeters),
      }));
      const markers = nearbyView.map((user, index) => ({
        id: index + 1,
        latitude: user.latitude,
        longitude: user.longitude,
        iconPath: '/assets/marker-rider.png',
        width: 28,
        height: 28,
        callout: {
          content: `${user.nickname} · ${user.distanceText}`,
          display: 'BYCLICK',
          padding: 8,
          borderRadius: 8,
        },
      }));
      markers.push({
        id: 0,
        latitude: location.latitude,
        longitude: location.longitude,
        iconPath: '/assets/marker-me.png',
        width: 32,
        height: 32,
        callout: {
          content: '我',
          display: 'ALWAYS',
          padding: 7,
          borderRadius: 12,
        },
      });
      this.setData({
        latitude: location.latitude,
        longitude: location.longitude,
        nearby: nearbyView,
        markers,
        lastUpdatedText: `刚刚更新 · ${nearby.length} 位车友`,
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none', duration: 2500 });
    } finally {
      this.setData({ loading: false });
      wx.hideNavigationBarLoading();
    }
  },
});

function formatDistance(meters) {
  if (meters < 1000) return `${meters} m`;
  return `${(meters / 1000).toFixed(1)} km`;
}
