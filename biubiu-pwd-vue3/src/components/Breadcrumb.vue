<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
    <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
      <router-link v-if="item.path" :to="item.path">{{ item.title }}</router-link>
      <span v-else>{{ item.title }}</span>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const routeMap = {
  '/dashboard': '首页概览',
  '/orders': '订单管理',
  '/players': '陪玩师管理',
  '/boss': '老板管理',
  '/finance': '财务管理',
  '/withdrawals': '提现审核',
  '/settings': '系统设置',
  '/grab-hall': '抢单大厅',
  '/profile': '个人信息'
}

const breadcrumbs = computed(() => {
  const path = route.path
  const title = routeMap[path]
  if (title && path !== '/dashboard') {
    return [{ title, path }]
  }
  return []
})
</script>

<style scoped>
.el-breadcrumb {
  font-size: 14px;
}
</style>
