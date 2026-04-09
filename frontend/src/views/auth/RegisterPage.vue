<template>
  <div class="register-page">
    <div class="register-card">
      <div class="card-header">
        <span class="logo-icon">🍃</span>
        <h1>加入我们</h1>
        <p>创建账号开始您的社交之旅</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="register-form"
        @submit.prevent="handleRegister"
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

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="确认密码"
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
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        已有账号？
        <router-link to="/login" class="link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const success = await authStore.register(form.username, form.password)
    if (success) {
      ElMessage.success('注册成功')
      router.push('/')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f9f5 0%, #e8f5e9 100%);
}

.register-card {
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

.register-form {
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
