import { createRouter, createWebHistory } from 'vue-router'

const HomeView = () => import('../views/Home.vue')
const ChatAI = () => import('../views/ChatAI.vue')
const NegotiatorSimulator = () => import('../views/NegotiatorSimulator.vue')
const PlanMaster = () => import('../views/PlanMaster.vue')
const ChatPDF = () => import('../views/ChatPDF.vue')

const routes = [
  { path: '/', name: 'HomeView', component: HomeView },
  { path: '/chat', name: 'ChatAI', component: ChatAI },
  { path: '/negotiator', name: 'NegotiatorSimulator', component: NegotiatorSimulator },
  { path: '/plan', name: 'PlanMaster', component: PlanMaster },
  { path: '/chatpdf', name: 'ChatPDF', component: ChatPDF },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router