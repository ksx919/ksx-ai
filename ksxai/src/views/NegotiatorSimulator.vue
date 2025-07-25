<template>
  <div class="negotiator-simulator">
    <h2>谈判专家模拟器</h2>
    <p>你将扮演警方谈判专家，与AI扮演的劫持犯展开10轮心理博弈，通过话术提升信任值、挖掘诉求、化解危机，最终解救人质！</p>
    <ul>
      <li>目标：10轮内将信任值提升至100，解救人质</li>
      <li>信任值：初始20/100，≥60可合作，≤30有威胁</li>
      <li>每轮限时60秒，超时扣分</li>
      <li>可用指令：/hint（获取线索），/score（查看分数）</li>
      <li>高压事件：第3/6/9轮自动触发危机</li>
      <li>多结局分支，体验真实谈判挑战！</li>
    </ul>
    <p style="color: #888; font-size: 14px;">点击下方开始，与AI展开一场心理较量吧！</p>
  </div>
</template>

<script>
export default {
  name: 'NegotiatorSimulator',
}
</script>

<style scoped>
.negotiator-simulator {
  text-align: center;
  margin-top: 60px;
}
ul {
  text-align: left;
  display: inline-block;
  margin: 0 auto 20px auto;
  padding: 0 20px;
  font-size: 16px;
}
</style> 