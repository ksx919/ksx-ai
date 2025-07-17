<template>
  <div class="home">
    <div class="welcome-section">
      <h1 class="welcome-title">欢迎使用AI助手</h1>
      <p class="welcome-subtitle">智能工具集，让生活更高效</p>
      <div class="divider"></div>
    </div>
    
    <div class="feature-intro">
      <h2>探索智能工具</h2>
      <p>选择以下功能开启智能体验</p>
    </div>
    
    <div class="card-container">
      <div class="card-list">
        <!-- AI聊天卡片 -->
        <router-link class="card card-chat" to="/chat">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
            </svg>
          </div>
          <div class="card-content">
            <h3>AI聊天</h3>
            <p>与智能助手对话，获取即时解答</p>
          </div>
        </router-link>
        
        <!-- 哄哄模拟器卡片 -->
        <router-link class="card card-love" to="/honghong">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
            </svg>
          </div>
          <div class="card-content">
            <h3>哄哄模拟器</h3>
            <p>情感互动小游戏，化解甜蜜烦恼</p>
          </div>
        </router-link>
        
        <!-- 计划通卡片 -->
        <router-link class="card card-plan" to="/plan">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"/>
            </svg>
          </div>
          <div class="card-content">
            <h3>计划通</h3>
            <p>智能规划你的日程与目标</p>
          </div>
        </router-link>
        
        <!-- CHATPDF卡片 -->
        <router-link class="card card-pdf" to="/chatpdf">
          <div class="card-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
              <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
          </div>
          <div class="card-content">
            <h3>CHATPDF</h3>
            <p>上传PDF文档，与AI讨论内容</p>
          </div>
        </router-link>
      </div>
    </div>
    
    <div class="feature-tips">
      <div class="tip-card">
        <h3>✨ 今日推荐</h3>
        <p>试试用<span>CHATPDF</span>快速提取PDF文档要点</p>
      </div>
      <div class="tip-card">
        <h3>💡 使用技巧</h3>
        <p>在<span>AI聊天</span>中提问越具体，回答越精准</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HomeView',
}
</script>

<style scoped>
.home {
  text-align: center;
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-section {
  margin-bottom: 40px;
}

.welcome-title {
  font-size: 2.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, #42b883, #2c3e50);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  margin-bottom: 12px;
}

.welcome-subtitle {
  font-size: 1.2rem;
  color: #7f8c8d;
  margin-bottom: 24px;
}

.divider {
  height: 4px;
  width: 80px;
  background: linear-gradient(to right, #42b883, #35495e);
  margin: 0 auto;
  border-radius: 2px;
}

.feature-intro {
  margin-bottom: 40px;
}

.feature-intro h2 {
  font-size: 1.8rem;
  color: #34495e;
  margin-bottom: 12px;
}

.feature-intro p {
  color: #7f8c8d;
  font-size: 1.1rem;
}

.card-container {
  display: flex;
  justify-content: center;
  margin: 40px auto;
  max-width: 1200px;
}

.card-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  width: 100%;
}

@media (max-width: 1200px) {
  .card-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .card-list {
    grid-template-columns: 1fr;
  }
}

.card {
  display: flex;
  flex-direction: column;
  height: 220px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  padding: 24px;
  text-decoration: none;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
}

.card-chat::before { background: linear-gradient(90deg, #42b883, #347474); }
.card-love::before { background: linear-gradient(90deg, #ff6b6b, #ff8e8e); }
.card-plan::before { background: linear-gradient(90deg, #4dabf7, #339af0); }
.card-pdf::before { background: linear-gradient(90deg, #9c36b5, #cc5de8); }

.card-icon {
  margin-bottom: 16px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-icon svg {
  width: 48px;
  height: 48px;
}

.card-chat .card-icon svg { fill: #42b883; }
.card-love .card-icon svg { fill: #ff6b6b; }
.card-plan .card-icon svg { fill: #4dabf7; }
.card-pdf .card-icon svg { fill: #9c36b5; }

.card-content h3 {
  font-size: 1.4rem;
  color: #2c3e50;
  margin-bottom: 8px;
}

.card-content p {
  color: #7f8c8d;
  font-size: 0.95rem;
  line-height: 1.5;
}

.card:hover {
  transform: translateY(-8px) scale(1.03);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}

.feature-tips {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
  margin-top: 60px;
  max-width: 1200px;
  margin: 60px auto 0;
}

.tip-card {
  background: #f8f9fa;
  padding: 24px;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.tip-card h3 {
  font-size: 1.3rem;
  color: #2c3e50;
  margin-bottom: 16px;
}

.tip-card p {
  color: #7f8c8d;
  font-size: 1.05rem;
}

.tip-card span {
  color: #42b983;
  font-weight: 600;
}
</style>