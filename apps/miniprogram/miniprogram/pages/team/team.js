const api = require('../../services/api');
const sessionStore = require('../../services/session');

Page({
  data: {
    teams: [],
    loading: false,
    creating: false,
    joining: false,
    teamName: '周末骑行小队',
    routeNote: '',
    maxMembers: '10',
    publicRoom: true,
    roomCode: '',
  },

  onShow() {
    if (!sessionStore.requireSession()) return;
    this.loadTeams();
  },

  onPullDownRefresh() {
    this.loadTeams().finally(() => wx.stopPullDownRefresh());
  },

  onInput(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ [key]: event.detail.value });
  },

  onPublicChange(event) {
    this.setData({ publicRoom: event.detail.value });
  },

  async loadTeams() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const teams = await api.listTeams();
      this.setData({
        teams: teams.map((team) => ({
          ...team,
          memberCountText: `${team.members.length}/${team.maxMembers} 人`,
        })),
      });
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onCreateTeam() {
    const name = this.data.teamName.trim();
    const maxMembers = Number(this.data.maxMembers);
    if (name.length < 2) {
      wx.showToast({ title: '车队名称至少 2 个字符', icon: 'none' });
      return;
    }
    if (!Number.isInteger(maxMembers) || maxMembers < 2 || maxMembers > 50) {
      wx.showToast({ title: '人数上限应为 2-50', icon: 'none' });
      return;
    }

    this.setData({ creating: true });
    try {
      const team = await api.createTeam({
        name,
        routeNote: this.data.routeNote.trim(),
        maxMembers,
        publicRoom: this.data.publicRoom,
      });
      wx.showToast({ title: '车队已创建', icon: 'success' });
      this.openRoom(team.id);
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' });
    } finally {
      this.setData({ creating: false });
    }
  },

  async onJoinTeam() {
    const roomCode = this.data.roomCode.trim();
    if (!/^\d{6}$/.test(roomCode)) {
      wx.showToast({ title: '请输入 6 位房间码', icon: 'none' });
      return;
    }

    this.setData({ joining: true });
    try {
      const team = await api.joinTeam(roomCode);
      wx.showToast({ title: '已加入车队', icon: 'success' });
      this.openRoom(team.id);
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' });
    } finally {
      this.setData({ joining: false });
    }
  },

  onTeamTap(event) {
    this.openRoom(event.currentTarget.dataset.teamId);
  },

  onCopyCode(event) {
    wx.setClipboardData({ data: event.currentTarget.dataset.code });
  },

  openRoom(teamId) {
    wx.navigateTo({ url: `/pages/room/room?teamId=${encodeURIComponent(teamId)}` });
  },
});
