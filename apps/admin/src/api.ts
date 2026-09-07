import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 8000,
})

export interface Overview {
  users: number
  rooms: number
  activeRides: number
}

export interface UserItem {
  id: string
  phone: string
  nickname: string
  motorcycle: string
  createdAt: string
}

export interface RoomMember {
  userId: string
  nickname: string
  role: string
  online: boolean
}

export interface RoomItem {
  id: string
  roomCode: string
  name: string
  maxMembers: number
  publicRoom: boolean
  status: string
  createdAt: string
  members: RoomMember[]
}
