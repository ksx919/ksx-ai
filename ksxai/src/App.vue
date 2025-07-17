<template>
  <div id="app">
    <header class="app-header">
      <div class="logo-title" @click="goHome">
        <img alt="Vue logo" src="./assets/logo.png" class="logo" />
        <span class="title">AIHub</span>
      </div>
      <button class="theme-toggle" @click="toggleTheme">
        {{ isDark ? '🌙' : '🌞' }}
      </button>
    </header>
    <router-view class="router-view-container" />
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

export default {
  name: 'App',
  setup() {
    const isDark = ref(false)
    const router = useRouter()

    // 读取本地存储
    onMounted(() => {
      const saved = localStorage.getItem('theme')
      if (saved === 'dark') {
        isDark.value = true
        document.body.classList.add('dark')
      }
    })

    const toggleTheme = () => {
      isDark.value = !isDark.value
      if (isDark.value) {
        document.body.classList.add('dark')
        localStorage.setItem('theme', 'dark')
      } else {
        document.body.classList.remove('dark')
        localStorage.setItem('theme', 'light')
      }
    }

    const goHome = () => {
      router.push('/')
    }

    return { isDark, toggleTheme, goHome }
  }
}
</script>

<style>
html, body {
  margin: 0;
  padding: 0;
  overflow: hidden;
  height: 100%;
}

#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  height: 100%;
  background: #f5f6fa;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.router-view-container {
  flex: 1;
  overflow: hidden;
}
body.dark #app {
  background: #23272f;
  color: #f5f6fa;
}
.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
}
body.dark .app-header {
  background: #23272f;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}
.logo-title {
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: opacity 0.2s ease;
  user-select: none;
}
.logo-title:hover {
  opacity: 0.8;
}
.logo-title:active {
  opacity: 0.6;
}
.logo {
  height: 40px;
  margin-right: 12px;
}
.title {
  font-size: 1.5rem;
  font-weight: bold;
}
.theme-toggle {
  font-size: 1.2rem;
  background: none;
  border: none;
  cursor: pointer;
  color: inherit;
}
</style>
