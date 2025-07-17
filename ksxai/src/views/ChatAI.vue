<template>
  <div class="chat-ai-layout">
    <aside class="sidebar">
      <div class="sidebar-header">AI聊天（历史会话）</div>
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
                <!-- 思考过程部分 -->
                <div v-if="msg.think" class="ai-think">
                  <div class="ai-think-header" @click="toggleThink(idx)">
                    <span class="think-toggle">{{ thinkStates[idx] ? '▼' : '▶' }}</span>
                    {{ (isThinking && idx === currentMessages.length - 1 && loading) ? '思考中' : '思考过程' }}
                    <span v-if="isThinking && idx === currentMessages.length - 1 && loading" class="thinking-dot"></span>
                  </div>
                  <div v-show="thinkStates[idx]" class="ai-think-content">{{ msg.think }}</div>
                </div>
                
                <!-- 最终回复部分 -->
                <div class="ai-response">{{ msg.text }}</div>
              </template>
            </div>
          </div>
          
        </div>
        <div class="input-bar">
          <input v-model="input" @keyup.enter="send" placeholder="请输入内容..." :disabled="loading" />
          <button @click="send" :disabled="loading">{{ loading ? '发送中...' : '发送' }}</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { ref, watch, onMounted } from 'vue'
import axios from 'axios'

function genId() {
  return 'chat_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
}

export default {
  name: 'ChatAI',
  setup() {
    const chatList = ref([])
    const currentChatId = ref('')
    const input = ref('')
    const loading = ref(false)
    const streamingText = ref('')
    const thinkStates = ref({})
    const isThinking = ref(false)
    const thinkingContent = ref('')
    const showThinkingContent = ref(true)
const isThinkingComplete = ref(false)
    const currentMessages = ref([])
    
    // 为每个会话维护独立的消息和状态
    const sessionMessages = ref({}) // { chatId: messages[] }
    const sessionLoading = ref({}) // { chatId: boolean }
    const sessionStreaming = ref({}) // { chatId: string }
    const sessionThinkStates = ref({}) // { chatId: { msgIndex: boolean } }

    // 获取会话列表
    const fetchChatList = async () => {
      const resp = await axios.get('/ai/history/chat')
      // chatId列表
      const chatIds = resp.data
      const chatListWithTitles = []
      
      // 为每个会话获取第一条消息作为标题
      for (const chatId of chatIds) {
        try {
          const messagesResp = await axios.get(`/ai/history/chat/${chatId}`)
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
    }

    // 获取当前会话消息
    const fetchMessages = async (chatId) => {
      if (!chatId) return
      const resp = await axios.get(`/ai/history/chat/${chatId}`)
      // resp.data: [{role, content}]
      // 兼容原有结构
      const messages = resp.data.map(msg => {
        // 兼容AI思考过程（如果content里有<think>标签）
        if (msg.role === 'assistant' && msg.content && msg.content.includes('<think>')) {
          const thinkMatch = msg.content.match(/<think>([\s\S]*?)<\/think>/)
          return {
            role: 'ai',
            text: msg.content.replace(/<think>[\s\S]*?<\/think>/, '').trim(),
            think: thinkMatch ? thinkMatch[1].trim() : ''
          }
        } else if (msg.role === 'assistant') {
          return { role: 'ai', text: msg.content, think: '' }
        } else {
          return { role: 'user', text: msg.content }
        }
      })
      
      // 更新会话消息
      sessionMessages.value[chatId] = messages
      // 如果是当前会话，更新currentMessages
      if (chatId === currentChatId.value) {
        currentMessages.value = messages
        // 为有思考过程的消息设置默认展开状态
        const thinkStatesForChat = {}
        messages.forEach((msg, idx) => {
          if (msg.think) {
            thinkStatesForChat[idx] = true
          }
        })
        sessionThinkStates.value[chatId] = thinkStatesForChat
        thinkStates.value = thinkStatesForChat
        // 滚动到底部
        scrollToBottom()
      }
    }

    // 新建会话
    const newChat = async () => {
      const id = genId()
      chatList.value.unshift({ chatId: id, summary: '' })
      currentChatId.value = id
      sessionMessages.value[id] = []
      currentMessages.value = []
      // 后端会在首次发送消息时自动保存chatId
    }

    // 切换会话
    const switchChat = async (id) => {
      currentChatId.value = id
      // 如果会话消息已存在，直接使用
      if (sessionMessages.value[id]) {
        currentMessages.value = sessionMessages.value[id]
        thinkStates.value = sessionThinkStates.value[id] || {}
        // 检查当前会话是否正在思考中 - 只有在真正思考中且未完成时才显示
        const isCurrentlyThinking = sessionLoading.value[id] && 
          sessionStreaming.value[id] && 
          sessionStreaming.value[id].includes('<think>') && 
          !sessionStreaming.value[id].includes('</think>')
        isThinking.value = isCurrentlyThinking
        streamingText.value = sessionStreaming.value[id] || ''
        // 滚动到底部
        scrollToBottom()
      } else {
        // 否则从后端获取
        await fetchMessages(id)
        // 新获取的会话不会有思考状态
        isThinking.value = false
        streamingText.value = ''
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

    // 发送消息（支持后台处理）
    const send = async () => {
      if (!input.value.trim() || loading.value) return
      
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
      messages.push({ role: 'ai', text: '', think: '' })
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
        // 用 fetch 处理流
        const url = axios.getUri({ url: '/ai/chat', params: { prompt, chatId } })
        const resp = await fetch(url, { method: 'GET' })
        if (!resp.body) throw new Error('无流式响应')
        const reader = resp.body.getReader()
        let aiText = ''
        let inThinkingMode = false
        let thinkingCompleted = false
        
        // 初始化会话状态
        sessionStreaming.value[chatId] = ''
        sessionThinkStates.value[chatId] = sessionThinkStates.value[chatId] || {}
        
        // 使用已经创建的AI消息（最后一个消息）
        const tempMsgIdx = messages.length -1
        // 默认展开思考过程
        sessionThinkStates.value[chatId][tempMsgIdx] = true
        
        // 如果是当前会话，更新currentMessages
        if (chatId === currentChatId.value) {
          currentMessages.value = messages
          // 滚动到底部
          scrollToBottom()
        }
        
        // eslint-disable-next-line no-constant-condition
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          const chunk = new TextDecoder('utf-8').decode(value)
          aiText += chunk
          sessionStreaming.value[chatId] = aiText
          
          // 检查是否进入思考模式
          if (aiText.includes('<think>') && !inThinkingMode && !thinkingCompleted) {
            inThinkingMode = true
            const thinkStartIndex = aiText.indexOf('<think>') + '<think>'.length
            const thinkingContent = aiText.substring(thinkStartIndex).trim()
            messages[tempMsgIdx].think = thinkingContent
            // 流式时正式回答区域不显示思考内容
            messages[tempMsgIdx].text = ''
          }
          // 检查是否结束思考模式
          else if (inThinkingMode && aiText.includes('</think>')) {
            inThinkingMode = false
            thinkingCompleted = true
            const thinkMatch = aiText.match(/<think>([\s\S]*?)<\/think>/)
            if (thinkMatch) {
              messages[tempMsgIdx].think = thinkMatch[1].trim()
            }
            // 只显示<think>...</think>之后的内容
            let cleanedText = aiText.split(/<think>[\s\S]*?<\/think>/)[1] || ''
            cleanedText = cleanedText.replace(/<think>|<\/think>/g, '').trim()
            cleanedText = cleanedText.replace(/\n\s*\n/g, '\n').trim()
            messages[tempMsgIdx].text = cleanedText
          } 
          // 在思考模式中更新内容
          else if (inThinkingMode && !thinkingCompleted) {
            const thinkStartIndex = aiText.indexOf('<think>') + '<think>'.length
            const thinkingContent = aiText.substring(thinkStartIndex).trim()
            messages[tempMsgIdx].think = thinkingContent
            // 流式时正式回答区域不显示思考内容
            messages[tempMsgIdx].text = ''
          } 
          // 思考完成后或没有思考过程，直接更新文本
          else {
            // 只显示<think>...</think>之后的内容（如果有），否则全部内容
            if (aiText.includes('<think>') && aiText.includes('</think>')) {
              let cleanedText = aiText.split(/<think>[\s\S]*?<\/think>/)[1] || ''
              cleanedText = cleanedText.replace(/<think>|<\/think>/g, '').trim()
              cleanedText = cleanedText.replace(/\n\s*\n/g, '\n').trim()
              messages[tempMsgIdx].text = cleanedText
          } else {
              let cleanedText = aiText.replace(/<think>[\s\S]*?<\/think>/g, '').trim()
              cleanedText = cleanedText.replace(/<think>|<\/think>/g, '').trim()
              cleanedText = cleanedText.replace(/\n\s*\n/g, '\n').trim()
              messages[tempMsgIdx].text = cleanedText
            }
          }
          
          // 更新会话消息
          sessionMessages.value[chatId] = [...messages]
          
          // 如果是当前会话，更新currentMessages并滚动
          if (chatId === currentChatId.value) {
            currentMessages.value = [...messages]
            // 更新全局思考状态用于UI显示 - 只有在真正思考中时才显示
            isThinking.value = inThinkingMode && !thinkingCompleted
            streamingText.value = aiText
            // 滚动到底部
            scrollToBottom()
            }
        }
        
        // 解析AI回复中的思考过程
        let thinkContent = ''
        let finalText = aiText
        
        // 彻底清理所有<think>标签及其内容，并确保msg.text只显示正式回复
        const thinkMatch = aiText.match(/<think>([\s\S]*?)<\/think>/)
        if (thinkMatch) {
          thinkContent = thinkMatch[1].trim()
          // 只取<think>...</think>之后的内容作为正式回复
          const afterThink = aiText.split(/<think>[\s\S]*?<\/think>/)[1]
          finalText = afterThink ? afterThink.trim() : ''
        } else {
          // 没有思考标签，全部内容为正式回复
          finalText = aiText.trim()
        }
        // 额外清理残留标签
        finalText = finalText.replace(/<think>|<\/think>/g, '').trim()
        // 清理多余空行
        finalText = finalText.replace(/\n\s*\n/g, '\n').trim()
        
        messages[tempMsgIdx] = {
          role: 'ai',
          text: finalText,
          think: thinkContent
        }
        
        sessionMessages.value[chatId] = [...messages]
        if (thinkContent) {
          // 默认展开思考过程
          sessionThinkStates.value[chatId][tempMsgIdx] = true
        }
        
        // 如果是当前会话，更新currentMessages
        if (chatId === currentChatId.value) {
          currentMessages.value = [...messages]
          // 思考完成后，确保不显示思考中状态
          isThinking.value = false
          streamingText.value = ''
          // 滚动到底部
          scrollToBottom()
        }
        
        // 发送后只刷新会话列表
        await fetchChatList()
      } catch (e) {
        const errorMsgIdx = messages.length -1
        messages[errorMsgIdx] = { role: 'ai', text: '[AI流式异常] ' + e.message, think: '' }
        sessionMessages.value[chatId] = [...messages]
        if (chatId === currentChatId.value) {
          currentMessages.value = [...messages]
          // 滚动到底部
          scrollToBottom()
        }
      } finally {
        // 清除会话状态
        sessionLoading.value[chatId] = false
        sessionStreaming.value[chatId] = ''
        // 如果是当前会话，清除全局状态
        if (chatId === currentChatId.value) {
        loading.value = false
        streamingText.value = ''
        isThinking.value = false
        isThinkingComplete.value = false
        thinkingContent.value = ''
          showThinkingContent.value = true
        }
      }
    }
    
    const toggleThink = (idx) => {
      const chatId = currentChatId.value
      if (!sessionThinkStates.value[chatId]) {
        sessionThinkStates.value[chatId] = {}
      }
      sessionThinkStates.value[chatId][idx] = !sessionThinkStates.value[chatId][idx]
      thinkStates.value = sessionThinkStates.value[chatId]
    }
    
    const toggleThinkingState = () => {
      showThinkingContent.value = !showThinkingContent.value
    }

    // 初始化加载
    onMounted(async () => {
      await fetchChatList()
      if (currentChatId.value) {
        await fetchMessages(currentChatId.value)
      }
      // 页面加载完成后滚动到底部
      scrollToBottom()
    })

    // 监听会话切换自动拉取消息
    watch(currentChatId, async (id) => {
      if (id) {
        if (sessionMessages.value[id]) {
          currentMessages.value = sessionMessages.value[id]
          thinkStates.value = sessionThinkStates.value[id] || {}
          // 滚动到底部
          scrollToBottom()
        } else {
          await fetchMessages(id)
        }
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
      streamingText,
      thinkStates,
      toggleThink,
      isThinking,
      thinkingContent,
      showThinkingContent,
      toggleThinkingState
    }
  }
}
</script>

<style scoped>
.chat-ai-layout {
  display: flex;
  width: 100%;
  height: 100%; /* 使用100%高度，因为父容器已经处理了顶部导航栏 */
  margin: 0;
  padding: 0;
  background: #fff;
  overflow: hidden;
}
body.dark .chat-ai-layout {
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
}
.chat-main h2 {
  margin: 0 0 20px 0;
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
/* AI思考过程样式 */
.ai-think {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  transition: all 0.2s ease;
  max-width: 80%;
  display: inline-block;
}
.ai-think:hover {
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}
body.dark .ai-think {
  background: #1a2633;
  border-color: #2d4a63;
}
.ai-think-header {
  font-weight: bold;
  color: #2b6cb0;
  cursor: pointer;
  display: flex;
  align-items: center;
  user-select: none;
  padding: 10px 16px;
  font-size: 0.9rem;
  transition: background-color 0.2s ease;
}

/* 思考中状态的特殊样式 */
.message.ai .ai-think-header.thinking::after {
  content: "";
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-left: 8px;
  background-color: #2b6cb0;
  border-radius: 50%;
  animation: thinking-pulse 1.5s infinite;
}

@keyframes thinking-pulse {
  0% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.8); }
  100% { opacity: 1; transform: scale(1); }
}
.ai-think-header:hover {
  background-color: rgba(0, 0, 0, 0.05);
}
body.dark .ai-think-header {
  color: #63b3ed;
}
body.dark .ai-think-header:hover {
  background-color: rgba(255, 255, 255, 0.05);
}
.think-toggle {
  display: inline-block;
  width: 16px;
  margin-right: 6px;
  font-size: 12px;
}
.ai-think-content {
  color: #2d3748;
  white-space: pre-wrap;
  padding: 12px 16px;
  font-size: 0.95rem;
  line-height: 1.5;
  border-top: 1px solid #e9ecef;
  transition: max-height 0.3s ease;
  background-color: #f8f9fa;
}
body.dark .ai-think-content {
  color: #e2e8f0;
  border-top-color: #2d4a63;
  background-color: #1a2633;
}

/* AI最终回复样式 */
.ai-response {
  color: #333;
  white-space: pre-wrap;
  padding: 12px 16px;
  font-size: 1rem;
  line-height: 1.6;
  background: #fff;
  border-radius: 8px;
  border-left: 3px solid #42b983;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  display: inline-block;
  max-width: 80%;
  min-width: 40px;
}
.ai-response.streaming {
  border-left-color: #e67e22;
  position: relative;
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.8; }
  100% { opacity: 1; }
}
body.dark .ai-response {
  color: #f5f6fa;
  background: #23272f;
  border-left-color: #e67e22;
}
/* 思考中动画圆圈 */
.thinking-dot {
  display: inline-block;
  width: 14px;
  height: 14px;
  margin-left: 8px;
  border-radius: 50%;
  background: #2b6cb0;
  animation: thinking-scale 1.2s infinite cubic-bezier(0.4,0,0.2,1);
}
@keyframes thinking-scale {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(0.6); opacity: 0.6; }
  100% { transform: scale(1); opacity: 1; }
}
</style>