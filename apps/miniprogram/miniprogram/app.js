App({
  globalData: {
    session: null,
  },

  onLaunch() {
    const session = wx.getStorageSync('motolink.session');
    this.globalData.session = session || null;
  },
});
