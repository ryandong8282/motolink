import { createRouter, createWebHistory } from 'vue-router'

import DashboardView from './views/DashboardView.vue'
import RoomsView from './views/RoomsView.vue'
import UsersView from './views/UsersView.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: DashboardView },
    { path: '/users', component: UsersView },
    { path: '/rooms', component: RoomsView },
  ],
})
