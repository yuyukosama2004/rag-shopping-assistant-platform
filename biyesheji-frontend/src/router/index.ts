import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { guest: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { guest: true },
  },
  {
    path: '/',
    component: () => import('../components/Layout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'products', name: 'ProductList', component: () => import('../views/ProductList.vue') },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('../views/ProductDetail.vue') },
      { path: 'account', name: 'Account', component: () => import('../views/Account.vue'), meta: { auth: true } },
      { path: 'cart', name: 'Cart', component: () => import('../views/Cart.vue'), meta: { auth: true } },
      { path: 'checkout', name: 'Checkout', component: () => import('../views/Checkout.vue'), meta: { auth: true } },
      { path: 'orders', name: 'OrderList', component: () => import('../views/OrderList.vue'), meta: { auth: true } },
      { path: 'order/:orderNo', name: 'OrderDetail', component: () => import('../views/OrderDetail.vue'), meta: { auth: true } },
      { path: 'ai-assistant', name: 'AiAssistant', component: () => import('../views/AiAssistant.vue'), meta: { auth: true } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：未登录跳转
router.beforeEach((to) => {
  if (to.meta.auth && !localStorage.getItem('accessToken')) {
    return '/login'
  }
  if (to.meta.guest && localStorage.getItem('accessToken')) {
    return '/'
  }
})

export default router
