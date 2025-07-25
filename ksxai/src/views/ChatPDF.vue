<template>
  <div class="chat-pdf-layout">
    <aside class="sidebar">
      <div class="sidebar-header">PDF聊天（历史会话）</div>
      <div class="chat-list">
        <div
          v-for="chat in chatList"
          :key="chat.chatId"
          :class="['chat-item', chat.chatId === currentChatId ? 'active' : '']"
          @click="switchChat(chat.chatId)"
        >
          <div class="chat-summary">
            {{ chat.summary || '（空会话）' }}
          </div>
        </div>
      </div>
      <button class="new-chat-btn" @click="newChat">+ 新建会话</button>
    </aside>
    
    <main class="chat-main">
      <!-- PDF查看区域 -->
      <div class="pdf-viewer-container" v-if="pdfUrl">
        <div class="pdf-viewer-header">
          <span>PDF文档: {{ currentPdfName }}</span>
          <button @click="closePdf" class="close-pdf-btn">关闭</button>
        </div>
        <iframe 
          :src="pdfUrl" 
          class="pdf-viewer"
          frameborder="0"
        ></iframe>
      </div>
      
      <!-- PDF上传区域 -->
      <div class="pdf-upload-area" v-if="!pdfUrl">
        <div class="upload-placeholder" @click="$refs.fileInput.click()">
          <p>点击上传PDF文件</p>
          <p class="upload-hint">支持PDF格式文件</p>
        </div>
        <input 
          type="file" 
          ref="fileInput" 
          @change="handleFileUpload" 
          accept=".pdf"
          style="display: none;"
        />
      </div>
      
      <div class="chat-window">
        <div class="messages">
          <div v-for="(msg, idx) in currentMessages" :key="idx" :class="['message', msg.role]">
            <span class="role">{{ msg.role === 'user' ? '我' : 'AI' }}：</span>
            <div class="message-content">
              <!-- 用户消息 -->
              <template v-if="msg.role === 'user'">
                <div class="user-message">
                  <span class="text">{{ msg.text }}</span>
                </div>
              </template>
              
              <!-- AI消息 -->
              <template v-else>
                <!-- 加载动画，当这是最后一条消息且正在加载时显示 -->
                <div v-if="loading && idx === currentMessages.length - 1 && !msg.text" class="loading-dots">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
                
                <!-- 最终回复部分 -->
                <div v-if="msg.text" class="ai-response">{{ msg.text }}</div>
              </template>
            </div>
          </div>
        </div>
        
        <div class="input-bar">
          <input 
            v-model="input" 
            @keyup.enter="send" 
            placeholder="请输入内容..." 
            :disabled="loading || !pdfUrl"
          />
          <button 
            @click="send" 
            :disabled="loading || !pdfUrl"
          >
            {{ loading ? '发送中...' : '发送' }}
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'

function genId() {
  return 'chat_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
}

export default {
  name: 'ChatPDF',
  setup() {
    const chatList = ref([])
    const currentChatId = ref('')
    const input = ref('')
    const loading = ref(false)
    const currentMessages = ref([])
    const pdfUrl = ref('')
    const currentPdfName = ref('')
    
    // 为每个会话维护独立的消息
    const sessionMessages = ref({}) // { chatId: messages[] }
    const sessionLoading = ref({}) // { chatId: boolean }
    const sessionPdf = ref({}) // { chatId: { url, name } }

    // 获取会话列表
    const fetchChatList = async () => {
      try {
        const resp = await axios.get('/ai/history/pdf')
        // chatId列表
        const chatIds = resp.data
        const chatListWithTitles = []
        
        // 为每个会话获取第一条消息作为标题
        for (const chatId of chatIds) {
          try {
            const messagesResp = await axios.get(`/ai/history/pdf/${chatId}`)
            const messages = messagesResp.data
            let title = '（空会话）'
            
            // 找到第一条用户消息作为标题
            const firstUserMessage = messages.find(msg => msg.role === 'user')
            if (firstUserMessage && firstUserMessage.content) {
              title = firstUserMessage.content.slice(0, 20) + (firstUserMessage.content.length > 20 ? '...' : '')
            }
            
            chatListWithTitles.push({ chatId, summary: title })
          } catch (error) {
            // 如果获取消息失败，使用默认标题
            chatListWithTitles.push({ chatId, summary: '（空会话）' })
          }
        }
        
        chatList.value = chatListWithTitles
        // 自动选中第一个
        if (chatList.value.length > 0 && !currentChatId.value) {
          currentChatId.value = chatList.value[0].chatId
        }
      } catch (error) {
        console.error('获取会话列表失败:', error)
      }
    }

    // 获取当前会话消息
    const fetchMessages = async (chatId) => {
      if (!chatId) return
      try {
        const resp = await axios.get(`/ai/history/pdf/${chatId}`)
        // resp.data: [{role, content}]
        // 兼容原有结构
        const messages = resp.data.map(msg => {
          if (msg.role === 'assistant') {
            return { role: 'ai', text: msg.content }
          } else {
            return { role: 'user', text: msg.content }
          }
        })
        
        // 更新会话消息
        sessionMessages.value[chatId] = messages
        // 如果是当前会话，更新currentMessages
        if (chatId === currentChatId.value) {
          currentMessages.value = messages
          // 滚动到底部
          scrollToBottom()
        }
        
        // 如果该会话有PDF文件，加载PDF信息
        if (sessionPdf.value[chatId]) {
          pdfUrl.value = sessionPdf.value[chatId].url
          currentPdfName.value = sessionPdf.value[chatId].name
        }
      } catch (error) {
        console.error('获取消息失败:', error)
      }
    }

    // 新建会话
    const newChat = async () => {
      const id = genId()
      chatList.value.unshift({ chatId: id, summary: '' })
      currentChatId.value = id
      sessionMessages.value[id] = []
      currentMessages.value = []
      pdfUrl.value = ''
      currentPdfName.value = ''
      sessionPdf.value[id] = { url: '', name: '' }
    }

    // 切换会话
    const switchChat = async (id) => {
      currentChatId.value = id
      // 如果会话消息已存在，直接使用
      if (sessionMessages.value[id]) {
        currentMessages.value = sessionMessages.value[id]
        // 检查当前会话是否有PDF
        if (sessionPdf.value[id]) {
          pdfUrl.value = sessionPdf.value[id].url
          currentPdfName.value = sessionPdf.value[id].name
        }
        // 滚动到底部
        scrollToBottom()
      } else {
        // 否则从后端获取
        await fetchMessages(id)
      }
    }

    // 滚动到底部函数
    const scrollToBottom = () => {
      setTimeout(() => {
        const messagesEl = document.querySelector('.messages')
        if (messagesEl) {
          messagesEl.scrollTop = messagesEl.scrollHeight
        }
      }, 10)
    }

    // 处理文件上传
    const handleFileUpload = async (event) => {
      const file = event.target.files[0]
      if (!file || file.type !== 'application/pdf') {
        alert('请选择PDF文件')
        return
      }
      
      // 检查文件大小，限制为100MB
      const maxSize = 100 * 1024 * 1024; // 100MB in bytes
      if (file.size > maxSize) {
        alert('文件大小不能超过100MB')
        event.target.value = '' // 清空文件输入框
        return
      }
      
      const formData = new FormData()
      formData.append('file', file)
      
      try {
        const response = await axios.post('/ai/upload/pdf', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        // 保存PDF信息到当前会话
        const chatId = currentChatId.value
        const pdfInfo = {
          url: response.data.url,
          name: file.name
        }
        
        sessionPdf.value[chatId] = pdfInfo
        pdfUrl.value = pdfInfo.url
        currentPdfName.value = pdfInfo.name
        
        // 清空文件输入框
        event.target.value = ''
      } catch (error) {
        console.error('上传PDF失败:', error)
        alert('上传PDF失败，请重试')
      }
    }

    // 关闭PDF查看器
    const closePdf = () => {
      pdfUrl.value = ''
      currentPdfName.value = ''
      // 从当前会话中移除PDF信息
      const chatId = currentChatId.value
      if (sessionPdf.value[chatId]) {
        sessionPdf.value[chatId] = { url: '', name: '' }
      }
    }

    // 发送消息
    const send = async () => {
      if (!input.value.trim() || loading.value || !pdfUrl.value) return
      
      const chatId = currentChatId.value
      const prompt = input.value
      input.value = ''
      // 设置当前会话为加载状态
      sessionLoading.value[chatId] = true
      loading.value = true
      
      // 获取当前会话的消息列表
      let messages = sessionMessages.value[chatId] || []
      
      // 先添加用户消息到界面
      messages.push({ role: 'user', text: prompt })
      sessionMessages.value[chatId] = messages
      
      // 立即创建一个空的AI消息，显示"AI："，避免卡顿感
      messages.push({ role: 'ai', text: '' })
      sessionMessages.value[chatId] = messages
      
      // 如果是当前会话，更新currentMessages
      if (chatId === currentChatId.value) {
        currentMessages.value = messages
        // 滚动到底部
        scrollToBottom()
      }
      
      // 如果是第一条消息，更新会话标题
      if (messages.length === 2) { // 用户消息 + AI消息
        const currentChat = chatList.value.find(c => c.chatId === chatId)
        if (currentChat) {
          currentChat.summary = prompt.slice(0, 20) + (prompt.length > 20 ? '...' : '')
        }
      }
      
      // 后台处理AI回复
      processAIResponse(chatId, prompt, messages)
    }

    // 后台处理AI回复
    const processAIResponse = async (chatId, prompt, messages) => {
      try {
        // 发送请求到后端，包含chatId、prompt和PDF信息
        const response = await axios.get('/ai/chat/pdf', {
          params: { 
            prompt, 
            chatId,
            pdfName: currentPdfName.value
          }
        })
        
        const aiText = response.data.content || response.data.text || ''
        
        // 更新AI消息
        const tempMsgIdx = messages.length - 1
        messages[tempMsgIdx] = {
          role: 'ai',
          text: aiText
        }
        
        sessionMessages.value[chatId] = [...messages]
        
        // 如果是当前会话，更新currentMessages
        if (chatId === currentChatId.value) {
          currentMessages.value = [...messages]
          // 滚动到底部
          scrollToBottom()
        }
        
        // 发送后只刷新会话列表
        await fetchChatList()
      } catch (e) {
        const errorMsgIdx = messages.length - 1
        messages[errorMsgIdx] = { role: 'ai', text: '[AI异常] ' + e.message }
        sessionMessages.value[chatId] = [...messages]
        if (chatId === currentChatId.value) {
          currentMessages.value = [...messages]
          // 滚动到底部
          scrollToBottom()
        }
      } finally {
        // 清除会话状态
        sessionLoading.value[chatId] = false
        // 如果是当前会话，清除全局状态
        if (chatId === currentChatId.value) {
          loading.value = false
        }
      }
    }

    // 初始化加载
    onMounted(async () => {
      await fetchChatList()
      if (currentChatId.value) {
        await fetchMessages(currentChatId.value)
      }
    })

    return {
      chatList,
      currentChatId,
      currentMessages,
      input,
      send,
      newChat,
      switchChat,
      loading,
      pdfUrl,
      currentPdfName,
      handleFileUpload,
      closePdf
    }
  }
}
</script>

<style scoped>
.chat-pdf-layout {
  display: flex;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  background: #fff;
  overflow: hidden;
}

body.dark .chat-pdf-layout {
  background: #23272f;
  color: #f5f6fa;
}

.sidebar {
  width: 200px;
  background: #f7f8fa;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

body.dark .sidebar {
  background: #23272f;
  border-right: 1px solid #444;
}

.sidebar-header {
  font-weight: bold;
  padding: 18px 0 12px 0;
  text-align: center;
  border-bottom: 1px solid #eee;
}

body.dark .sidebar-header {
  border-bottom: 1px solid #444;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
}

.chat-item {
  padding: 14px 18px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  font-size: 0.98rem;
  color: #333;
  background: none;
  transition: background 0.2s, color 0.2s;
}

.chat-item.active {
  background: #e6f7ff;
  color: #42b983;
}

body.dark .chat-item {
  color: #f5f6fa;
  border-bottom: 1px solid #444;
}

body.dark .chat-item.active {
  background: #2d3642;
  color: #42b983;
}

.chat-summary {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.new-chat-btn {
  margin: 16px;
  padding: 8px 0;
  border: none;
  border-radius: 8px;
  background: #42b983;
  color: #fff;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
}

.new-chat-btn:hover {
  background: #369870;
}

body.dark .new-chat-btn {
  background: #e67e22;
}

body.dark .new-chat-btn:hover {
  background: #b95c00;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 0;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

.pdf-viewer-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 40%;
  background: #f0f0f0;
  border-bottom: 1px solid #ddd;
  z-index: 10;
  display: flex;
  flex-direction: column;
}

body.dark .pdf-viewer-container {
  background: #2d3642;
  border-bottom: 1px solid #444;
}

.pdf-viewer-header {
  padding: 10px 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #e0e0e0;
  font-weight: bold;
}

body.dark .pdf-viewer-header {
  background: #1a2633;
  color: #f5f6fa;
}

.close-pdf-btn {
  background: #e74c3c;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
}

.pdf-viewer {
  flex: 1;
  width: 100%;
}

.pdf-upload-area {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 40%;
  background: #f8f9fa;
  border-bottom: 1px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

body.dark .pdf-upload-area {
  background: #2d3642;
  border-bottom: 1px solid #444;
}

.upload-placeholder {
  text-align: center;
  padding: 30px;
  border: 2px dashed #ccc;
  border-radius: 8px;
  cursor: pointer;
  width: 80%;
}

body.dark .upload-placeholder {
  border: 2px dashed #444;
}

.upload-placeholder:hover {
  border-color: #42b983;
}

.upload-hint {
  font-size: 0.9rem;
  color: #666;
  margin-top: 10px;
}

body.dark .upload-hint {
  color: #aaa;
}

.chat-window {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 20px 20px 0 20px;
  background: #fff;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  overflow: hidden;
  flex: 1 1 0%;
  margin-top: 40%;
}

body.dark .chat-window {
  background: #23272f;
  color: #f5f6fa;
}

.messages {
  flex: 1 1 0%;
  overflow-y: auto;
  margin-bottom: 0;
  text-align: left;
  padding-right: 10px;
  min-height: 0;
  height: 0;
}

.message {
  margin-bottom: 12px;
  display: flex;
  align-items: flex-start;
}

/* 用户消息从右侧展开 */
.message.user {
  flex-direction: row-reverse;
  text-align: right;
}

.message.user .role {
  color: #42b983;
  margin-left: 8px;
}

.message.ai .role {
  color: #e67e22;
  margin-right: 8px;
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 用户消息样式 */
.user-message {
  display: inline-block;
  background-color: #e6f7ff;
  border-radius: 8px;
  padding: 8px 12px;
  max-width: 80%;
  min-width: 40px;
  text-align: right;
  margin-left: auto;
}

body.dark .user-message {
  background-color: #2d3642;
}

/* AI响应样式调整 */
.ai-response {
  display: inline-block;
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 8px 12px;
  max-width: 80%;
}

body.dark .ai-response {
  background-color: #1a2633;
}

.input-bar {
  display: flex;
  gap: 8px;
  margin-top: 0;
  padding: 16px 20px 10px 0;
  background: transparent;
  justify-content: flex-start;
}

.input-bar input {
  flex: 1;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #ddd;
  font-size: 1rem;
  margin-right: 8px;
}

.input-bar button {
  padding: 8px 18px;
  border-radius: 6px;
  border: none;
  background: #42b983;
  color: #fff;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
  margin-right: 0;
}

.input-bar button:hover {
  background: #369870;
}

body.dark .input-bar input {
  background: #23272f;
  color: #f5f6fa;
  border: 1px solid #444;
}

body.dark .input-bar button {
  background: #e67e22;
}

body.dark .input-bar button:hover {
  background: #b95c00;
}

/* 加载动画样式 */
.loading-dots {
  display: inline-flex;
  align-items: center;
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 12px;
  max-width: 80px;
}

body.dark .loading-dots {
  background-color: #1a2633;
}

.loading-dots .dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #e67e22;
  margin: 0 4px;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots .dot:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots .dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% { 
    transform: scale(0);
    opacity: 0.6;
  }
  40% { 
    transform: scale(1);
    opacity: 1;
  }
}
</style>