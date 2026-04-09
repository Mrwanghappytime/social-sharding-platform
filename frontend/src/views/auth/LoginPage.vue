<template>
  <div class="login-page">
    <div class="login-card">
      <div class="card-header">
        <span class="logo-icon">🍃</span>
        <h1>欢迎回来</h1>
        <p>登录以继续您的社交之旅</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="submit-btn"
            native-type="submit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        还没有账号？
        <router-link to="/register" class="link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const success = await authStore.login(form.username, form.password)
    if (success) {
      ElMessage.success('登录成功')
      const redirect = route.query.redirect as string
      router.push(redirect || '/')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f9f5 0%, #e8f5e9 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(76, 175, 130, 0.1);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;

  .logo-icon {
    font-size: 48px;
    margin-bottom: 16px;
    display: block;
  }

  h1 {
    font-size: 24px;
    color: #333;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: #999;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 4px 16px;
  }
}

.submit-btn {
  width: 100%;
  border-radius: 12px;
  height: 48px;
  background: linear-gradient(135deg, #4CAF82, #5DC495);
  border: none;
  font-size: 16px;
  font-weight: 500;

  &:hover {
    background: linear-gradient(135deg, #5DC495, #4CAF82);
  }
}

.card-footer {
  text-align: center;
  font-size: 14px;
  color: #666;

  .link {
    color: #4CAF82;
    text-decoration: none;
    font-weight: 500;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
